package com.example.simpleeditingpictureapp.opengl_es

import android.content.Context
import android.opengl.GLES20
import java.io.BufferedReader
import java.io.InputStreamReader
import android.util.Log

class ShaderHelper {
    companion object{
        private const val TAG = "ShaderHelper"

        fun readShaderFileFromResource(
            resourceId: Int,
            context: Context
        ): String? {
            val stringBuilder = StringBuilder()
            try {
                val inputStream = context.resources.openRawResource(resourceId)
                val inputStreamReader = InputStreamReader(inputStream)
                val bufferedReader = BufferedReader(inputStreamReader)
                var nextLine: String? = bufferedReader.readLine()
                while (nextLine != null){
                    stringBuilder.append(nextLine)
                    stringBuilder.append("\n")
                    nextLine = bufferedReader.readLine()
                }
                bufferedReader.close()
                inputStreamReader.close()
                inputStream.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error reading shader resource $resourceId", e)
                return null
            }
            return stringBuilder.toString()
        }

        fun compileVertexShader(shaderCode: String?): Int{
            return compileShader(GLES20.GL_VERTEX_SHADER, shaderCode)
        }

        fun compileFragmentShader(shaderCode: String?): Int{
            return compileShader(GLES20.GL_FRAGMENT_SHADER, shaderCode)
        }

        private fun compileShader(type: Int, shaderCode: String?): Int{
            val shaderObjectId = GLES20.glCreateShader(type)

            if (shaderObjectId == 0){
                Log.e(TAG, "Could not create new shader")
                return 0
            }

            // 添加编译错误信息获取
            if (shaderCode.isNullOrEmpty()) {
                Log.e(TAG, "Shader code is null or empty")
                return 0
            }

            GLES20.glShaderSource(shaderObjectId, shaderCode)
            GLES20.glCompileShader(shaderObjectId)

            // 检查编译状态
            val compileStatus = IntArray(1)
            GLES20.glGetShaderiv(shaderObjectId, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

            if (compileStatus[0] == 0){
                // 获取编译错误信息
                val error = GLES20.glGetShaderInfoLog(shaderObjectId)
                Log.e(TAG, "Shader compilation failed: $error")
                Log.e(TAG, "Shader code: $shaderCode")
                GLES20.glDeleteShader(shaderObjectId)
                return 0
            }
            return shaderObjectId
        }

        fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int{
            val programObjectId = GLES20.glCreateProgram()
            if (programObjectId == 0){
                Log.e(TAG, "Could not create new program")
                return 0
            }
            GLES20.glAttachShader(programObjectId, vertexShaderId)
            GLES20.glAttachShader(programObjectId, fragmentShaderId)
            GLES20.glLinkProgram(programObjectId)

            // 检查链接状态
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(programObjectId, GLES20.GL_LINK_STATUS, linkStatus, 0)

            if (linkStatus[0] == 0){
                // 获取链接错误信息
                val error = GLES20.glGetProgramInfoLog(programObjectId)
                Log.e(TAG, "Program linking failed: $error")
                GLES20.glDeleteProgram(programObjectId)
                return 0
            }

            // 分离并删除着色器，它们已经链接到程序中
            GLES20.glDetachShader(programObjectId, vertexShaderId)
            GLES20.glDetachShader(programObjectId, fragmentShaderId)
            GLES20.glDeleteShader(vertexShaderId)
            GLES20.glDeleteShader(fragmentShaderId)

            return programObjectId
        }
    }
}