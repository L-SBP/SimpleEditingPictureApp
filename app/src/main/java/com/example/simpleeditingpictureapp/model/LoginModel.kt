package com.example.simpleeditingpictureapp.model

/**
 * 登录数据模型
 * 负责处理登录相关的业务逻辑
 */
class LoginModel {

    /**
     * 验证用户名和密码
     * @param username 用户名
     * @param password 密码
     * @return 登录结果
     */
    fun validateCredentials(username: String, password: String): LoginResult {
        // 验证用户名和密码
        return if (username == "admin" && password == "123456") {
            LoginResult.Success
        } else {
            LoginResult.Error("用户名或密码错误")
        }
    }

    /**
     * 验证输入
     * @param username 用户名
     * @param password 密码
     * @return 验证结果
     */
    fun validateInput(username: String, password: String): ValidationResult {
        if (username.isEmpty()) {
            return ValidationResult.Error("请输入用户名")
        }

        if (password.isEmpty()) {
            return ValidationResult.Error("请输入密码")
        }

        return ValidationResult.Success
    }
}

/**
 * 登录结果
 */
sealed class LoginResult {
    object Success : LoginResult()
    data class Error(val message: String) : LoginResult()
}

/**
 * 验证结果
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()
}
