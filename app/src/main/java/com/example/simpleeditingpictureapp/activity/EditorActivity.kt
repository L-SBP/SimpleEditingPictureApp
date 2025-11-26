package com.example.simpleeditingpictureapp.activity

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.simpleeditingpictureapp.R

class EditorActivity : AppCompatActivity() {
    private lateinit var btnBack: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        btnBack = findViewById(R.id.btn_editor_back)

        btnBack.setOnClickListener {
            val intent = Intent(this@EditorActivity, GalleryActivity::class.java)
            startActivity(intent)
        }
    }
}