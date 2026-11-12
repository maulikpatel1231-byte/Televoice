package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.dsp.AudioRecordManager
import com.example.model.VoicePreset
import com.example.telegram.TelegramSender
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingRootView: FrameLayout
    private lateinit var windowParams: WindowManager.LayoutParams

    private lateinit var bubbleLayout: FrameLayout
    private lateinit var bubbleIcon: TextView
    private lateinit var bubbleBadge: TextView

    private lateinit var expandedLayout: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var timerText: TextView
    private lateinit var recordBtn: FrameLayout
    private lateinit var recordBtnText: TextView

    private val audioRecordManager by lazy { AudioRecordManager(applicationContext) }
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private var activePreset: VoicePreset = VoicePreset.SweetGirl
    private var isExpanded = false
    private var isRecording = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null
    private var recordStartTime = 0L

    companion object {
        const val ACTION_START = "com.example.televoice.ACTION_START"
        const val ACTION_STOP = "com.example.televoice.ACTION_STOP"
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "televoice_floating_channel"

        var isRunning = false
            private set
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        initFloatingViews()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "TeleVoice Floating Recorder",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows floating voice recorder over Telegram"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, FloatingBubbleService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("TeleVoice Active over Telegram 🌸")
            .setContentText("Tap floating mic to record & send female voice notes")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Close Bubble", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initFloatingViews() {
        val dp = resources.displayMetrics.density

        windowParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (20 * dp).toInt()
            y = (200 * dp).toInt()
        }

        floatingRootView = FrameLayout(this)

        // 1. COLLAPSED BUBBLE (Circular 56dp)
        bubbleLayout = FrameLayout(this).apply {
            val sizePx = (56 * dp).toInt()
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#171329"))
                setStroke((2.5f * dp).toInt(), Color.parseColor("#FF2A85"))
            }
            background = bg

            bubbleIcon = TextView(context).apply {
                text = "🎙️"
                textSize = 22f
                gravity = Gravity.CENTER
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
            }
            addView(bubbleIcon)

            // Mini preset badge in bottom right corner
            bubbleBadge = TextView(context).apply {
                text = "🌸"
                textSize = 12f
                val badgeParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.END
                    setMargins(0, 0, (2 * dp).toInt(), (2 * dp).toInt())
                }
                layoutParams = badgeParams
            }
            addView(bubbleBadge)
        }

        // 2. EXPANDED TOOLBAR (Width ~300dp)
        expandedLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val widthPx = (290 * dp).toInt()
            layoutParams = FrameLayout.LayoutParams(widthPx, FrameLayout.LayoutParams.WRAP_CONTENT)
            visibility = View.GONE

            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 20 * dp
                setColor(Color.parseColor("#171329"))
                setStroke((1.5f * dp).toInt(), Color.parseColor("#2AABEE"))
            }
            background = bg
            setPadding((14 * dp).toInt(), (12 * dp).toInt(), (14 * dp).toInt(), (14 * dp).toInt())

            // Top Header: Title, Preset Badge & Close
            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                val title = TextView(context).apply {
                    text = "TeleVoice • Telegram 🌸"
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                addView(title)

                val minimizeBtn = TextView(context).apply {
                    text = "✖"
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 14f
                    setPadding((8 * dp).toInt(), (4 * dp).toInt(), (8 * dp).toInt(), (4 * dp).toInt())
                    setOnClickListener {
                        collapseBubble()
                    }
                }
                addView(minimizeBtn)
            }
            addView(headerRow)

            // Preset Selection Row
            val presetRow = LinearLayout(context)
            presetRow.orientation = LinearLayout.HORIZONTAL
            presetRow.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, (10 * dp).toInt(), 0, (10 * dp).toInt())
            }
            presetRow.gravity = Gravity.CENTER_VERTICAL

            val presets = listOf(VoicePreset.SweetGirl, VoicePreset.CuteAnime, VoicePreset.NaturalWoman)
            for (p in presets) {
                val chip = TextView(context).apply {
                    text = "${p.iconEmoji} ${p.name.split(" ")[0]}"
                    textSize = 11f
                    setTextColor(if (p.id == activePreset.id) Color.WHITE else Color.parseColor("#94A3B8"))
                    val chipBg = GradientDrawable().apply {
                        shape = GradientDrawable.RECTANGLE
                        cornerRadius = 12 * dp
                        setColor(if (p.id == activePreset.id) Color.parseColor("#FF2A85") else Color.parseColor("#221B3A"))
                    }
                    background = chipBg
                    setPadding((10 * dp).toInt(), (5 * dp).toInt(), (10 * dp).toInt(), (5 * dp).toInt())
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins((2 * dp).toInt(), 0, (4 * dp).toInt(), 0)
                    }

                    setOnClickListener {
                        activePreset = p
                        bubbleBadge.text = p.iconEmoji
                        updatePresetSelection(presetRow)
                    }
                }
                presetRow.addView(chip)
            }
            addView(presetRow)

            // Status & Timer Display
            val infoRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )

                statusText = TextView(context).apply {
                    text = "Tap Record to speak female voice"
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 11f
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }
                addView(statusText)

                timerText = TextView(context).apply {
                    text = "00:00"
                    setTextColor(Color.parseColor("#FF2A85"))
                    textSize = 13f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                }
                addView(timerText)
            }
            addView(infoRow)

            // Big Record Button
            recordBtn = FrameLayout(context).apply {
                val btnHeight = (44 * dp).toInt()
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    btnHeight
                ).apply {
                    setMargins(0, (10 * dp).toInt(), 0, 0)
                }

                val btnBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 12 * dp
                    setColor(Color.parseColor("#2AABEE"))
                }
                background = btnBg

                recordBtnText = TextView(context).apply {
                    text = "🎙️ Record for Telegram"
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    setTypeface(typeface, android.graphics.Typeface.BOLD)
                    gravity = Gravity.CENTER
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                }
                addView(recordBtnText)

                setOnClickListener {
                    toggleRecording()
                }
            }
            addView(recordBtn)
        }

        floatingRootView.addView(bubbleLayout)
        floatingRootView.addView(expandedLayout)

        // Setup Drag Listener on Root View
        setupTouchListener()

        windowManager.addView(floatingRootView, windowParams)
    }

    private fun updatePresetSelection(row: LinearLayout) {
        val dp = resources.displayMetrics.density
        for (i in 0 until row.childCount) {
            val chip = row.getChildAt(i) as TextView
            val isSelected = chip.text.startsWith(activePreset.iconEmoji)
            chip.setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#94A3B8"))
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12 * dp
                setColor(if (isSelected) Color.parseColor("#FF2A85") else Color.parseColor("#221B3A"))
            }
            chip.background = bg
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchListener() {
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = false

        bubbleLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = windowParams.x
                    initialY = windowParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (abs(dx) > 10 || abs(dy) > 10) {
                        isClick = false
                    }
                    windowParams.x = initialX + dx
                    windowParams.y = initialY + dy
                    windowManager.updateViewLayout(floatingRootView, windowParams)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        expandBubble()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun expandBubble() {
        isExpanded = true
        bubbleLayout.visibility = View.GONE
        expandedLayout.visibility = View.VISIBLE
    }

    private fun collapseBubble() {
        if (isRecording) {
            stopAndSendRecording()
        }
        isExpanded = false
        expandedLayout.visibility = View.GONE
        bubbleLayout.visibility = View.VISIBLE
    }

    private fun toggleRecording() {
        if (!isRecording) {
            startRecording()
        } else {
            stopAndSendRecording()
        }
    }

    private fun startRecording() {
        val started = audioRecordManager.startRecording()
        if (!started) {
            Toast.makeText(this, "Failed to start recording. Check mic permission.", Toast.LENGTH_SHORT).show()
            return
        }

        isRecording = true
        recordStartTime = System.currentTimeMillis()

        // Update UI
        statusText.text = "🔴 Recording (${activePreset.name})..."
        statusText.setTextColor(Color.parseColor("#FF2A85"))
        recordBtnText.text = "⏹️ Stop & Send to Telegram"
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12 * resources.displayMetrics.density
            setColor(Color.parseColor("#EF4444"))
        }
        recordBtn.background = bg

        // Start timer
        timerRunnable = object : Runnable {
            override fun run() {
                val elapsed = System.currentTimeMillis() - recordStartTime
                val sec = (elapsed / 1000) % 60
                val min = (elapsed / (1000 * 60))
                timerText.text = String.format(Locale.US, "%02d:%02d", min, sec)
                mainHandler.postDelayed(this, 500)
            }
        }
        mainHandler.post(timerRunnable!!)
    }

    private fun stopAndSendRecording() {
        if (!isRecording) return
        isRecording = false

        timerRunnable?.let { mainHandler.removeCallbacks(it) }
        timerRunnable = null

        statusText.text = "⏳ Converting to Female Voice..."
        statusText.setTextColor(Color.parseColor("#2AABEE"))
        recordBtnText.text = "Processing Audio..."

        serviceScope.launch {
            val rawPcm = audioRecordManager.stopRecording()
            if (rawPcm.isEmpty()) {
                withContext(Dispatchers.Main) {
                    resetExpandedUi()
                    Toast.makeText(applicationContext, "Recording was too short.", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val audioDir = File(filesDir, "recordings").apply { mkdirs() }
            val outputFile = File(audioDir, "TeleVoice_${activePreset.id}_$timestamp.wav")

            val success = withContext(Dispatchers.IO) {
                audioRecordManager.processAndSaveVoiceNote(
                    rawPcm = rawPcm,
                    pitchSemitones = activePreset.pitchShiftSemitones,
                    formantBrightness = activePreset.formantBrightness,
                    speedFactor = activePreset.speedFactor,
                    destinationFile = outputFile
                )
            }

            withContext(Dispatchers.Main) {
                resetExpandedUi()
                if (success) {
                    Toast.makeText(applicationContext, "Female voice ready! Sending to Telegram 🌸", Toast.LENGTH_SHORT).show()
                    // DIRECT TELEGRAM INTENT DISPATCH OVER TELEGRAM!
                    TelegramSender.sendAudioToTelegram(applicationContext, outputFile)
                    // Auto-collapse back to small bubble
                    collapseBubble()
                } else {
                    Toast.makeText(applicationContext, "Error converting voice.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun resetExpandedUi() {
        val dp = resources.displayMetrics.density
        statusText.text = "Tap Record to speak female voice"
        statusText.setTextColor(Color.parseColor("#94A3B8"))
        timerText.text = "00:00"
        recordBtnText.text = "🎙️ Record for Telegram"
        val bg = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 12 * dp
            setColor(Color.parseColor("#2AABEE"))
        }
        recordBtn.background = bg
    }

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        timerRunnable?.let { mainHandler.removeCallbacks(it) }
        try {
            if (::floatingRootView.isInitialized) {
                windowManager.removeView(floatingRootView)
            }
        } catch (e: Exception) {
            // view already removed
        }
        audioRecordManager.stopRecording()
    }
}
