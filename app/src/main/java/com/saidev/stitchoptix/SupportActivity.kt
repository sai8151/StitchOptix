package com.saidev.stitchoptix

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView

class SupportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_support)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Enable back button
//
//        findViewById<TextView>(R.id.developer_name).text = "Your Name" // Replace with actual name
//        findViewById<TextView>(R.id.developer_email).text = "your.email@example.com" // Replace with actual email
//        findViewById<TextView>(R.id.app_version).text = "Version 1.0.0" // Replace with actual version
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed() // Handle back button press
        return true
    }
}
