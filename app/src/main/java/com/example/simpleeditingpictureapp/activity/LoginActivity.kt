
package com.example.simpleeditingpictureapp.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.example.simpleeditingpictureapp.R
import com.example.simpleeditingpictureapp.viewmodel.LoginViewModel
import com.example.simpleeditingpictureapp.viewmodel.LoginNavigationEvent
import com.google.android.material.textfield.TextInputEditText

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: Button

    // ViewModel
    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etUsername = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)

        // 观察ViewModel中的数据变化
        observeViewModel()

        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            viewModel.login(username, password)
        }
    }

    /**
     * 观察ViewModel中的数据变化
     */
    private fun observeViewModel() {
        // 观察消息
        viewModel.message.observe(this, Observer { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        })

        // 观察导航事件
        viewModel.navigationEvent.observe(this, Observer { event ->
            when (event) {
                is LoginNavigationEvent.ToMain -> {
                    val intent = Intent(this@LoginActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            }
        })
    }
}
