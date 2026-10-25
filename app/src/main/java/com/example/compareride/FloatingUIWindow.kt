package com.example.compareride

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.net.Uri
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.os.Build

class FloatingUIWindow : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View
    private lateinit var params: WindowManager.LayoutParams

    private val priceReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.example.compareride.PRICES_UPDATED") {
                val prices = intent.getStringExtra("UBER_PRICES") ?: ""
                if (::floatingView.isInitialized) {
                    val txtPrices = floatingView.findViewById<TextView>(R.id.txtPrices)
                    txtPrices.text = "Uber Rates:\n$prices"
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        floatingView = LayoutInflater.from(this).inflate(R.layout.layout_floating_widget, null)

        // Set layout parameters
        params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            // Start it lower down (e.g., middle of the screen) so it doesn't block the search bar
            y = 800
        }

        windowManager.addView(floatingView, params)

        // Register Receiver for scraped prices
        val filter = IntentFilter("com.example.compareride.PRICES_UPDATED")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(priceReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(priceReceiver, filter)
        }

        val btnCompare = floatingView.findViewById<Button>(R.id.btnCompare)
        val txtPrices = floatingView.findViewById<TextView>(R.id.txtPrices)
        val edtDestination = floatingView.findViewById<EditText>(R.id.edtDestination)

        // Broadcast to Accessibility Service & Launch Deep Link
        btnCompare.setOnClickListener {
            val destination = edtDestination.text.toString().trim()
            if (destination.isEmpty()) {
                Toast.makeText(this, "Please enter a destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Hide the keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(edtDestination.windowToken, 0)

            txtPrices.text = "Launching Uber..."
            
            // 1. Launch Uber using Deep Link
            val uberIntent = Intent(Intent.ACTION_VIEW).apply {
                // Uber's deep link to set pickup (defaults to current) and dropoff
                data = Uri.parse("uber://?action=setPickup&dropoff[formatted_address]=${Uri.encode(destination)}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            
            try {
                startActivity(uberIntent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "Uber not found", Toast.LENGTH_SHORT).show()
            }

            // 2. We wait a few seconds for Uber to load the prices, then manually trigger the scraper!
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                txtPrices.text = "Scraping Uber prices..."
                val scrapeIntent = Intent("com.example.compareride.START_AUTOMATION")
                sendBroadcast(scrapeIntent)
            }, 6000) // 6 second delay to let Uber load
        }

        // --- Make the Widget Draggable ---
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        floatingView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Remember the initial position
                    initialX = params.x
                    initialY = params.y
                    // Remember where the finger touched
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    // Calculate how far the finger moved
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    // Update the window layout
                    windowManager.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(priceReceiver)
        if (::floatingView.isInitialized) {
            windowManager.removeView(floatingView)
        }
    }
}