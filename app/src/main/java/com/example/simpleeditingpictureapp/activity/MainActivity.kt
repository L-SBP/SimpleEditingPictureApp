package com.example.simpleeditingpictureapp.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.simpleeditingpictureapp.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {
    private lateinit var addEditButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        addEditButton = findViewById(R.id.fab_add_photo)

        addEditButton.setOnClickListener {
            // 启动图片编辑页面
            val intent = Intent(this@MainActivity, GalleryActivity::class.java)
            startActivity(intent)
        }
    }
}