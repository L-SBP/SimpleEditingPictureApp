package com.example.simpleeditingpictureapp.opengl_es

import android.content.Context
import android.opengl.GLES20.glCompileShader
import android.opengl.GLES20.glDeleteProgram
import android.opengl.GLES20.glDeleteShader
import android.opengl.GLES20.glGetShaderiv
import android.opengl.GLES20.glShaderSource
import android.opengl.GLES30
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

class ShaderHelper {
    companion object{
        fun readShaderFileFromResource(
            resourceId: Int,
            context: Context
        ): String? {
            val stringBuilder = StringBuilder()
            val inputStream = context.resources.openRawResource(resourceId)
            val inputStreamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            var nextLine: String ?= bufferedReader.readLine()
            while (nextLine != null){
                stringBuilder.append(nextLine)
                stringBuilder.append("\n")
                nextLine = bufferedReader.readLine()
            }
            return stringBuilder.toString()
        }

        fun compileVertexShader(shaderCode: String?): Int{
            return compileShader(GLES30.GL_VERTEX_SHADER, shaderCode)
        }

        fun compileFrameShader(shaderCode: String?): Int{
            return compileShader(GLES30.GL_FRAGMENT_SHADER, shaderCode)
        }

        private fun compileShader(type: Int, shaderCode: String?): Int{
            val shaderObjectId = GLES30.glCreateShader(type)

            if (shaderObjectId == 0){
                Log.i("ShaderHelper", "Could not create new shader")
                return 0
            }
            glShaderSource(shaderObjectId, shaderCode)
            glCompileShader(shaderObjectId)
            val status = IntArray(1)
            glGetShaderiv(shaderObjectId, GLES30.GL_COMPILE_STATUS, status, 0)
            if (status[0] == 0){
                glDeleteShader(shaderObjectId)
                Log.i("ShaderHelper", "Could not compile shader: $shaderObjectId")
                return 0
            }
            return shaderObjectId
        }

        fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int{
            val programObjectId = GLES30.glCreateProgram()
            if (programObjectId == 0){
                Log.i("ShaderHelper", "Could not create new program")
                return 0
            }
            GLES30.glAttachShader(programObjectId, vertexShaderId)
            GLES30.glAttachShader(programObjectId, fragmentShaderId)
            GLES30.glLinkProgram(programObjectId)
            val status = IntArray(1)
            GLES30.glGetProgramiv(programObjectId, GLES30.GL_LINK_STATUS, status, 0)
            if (status[0] == 0){
                glDeleteProgram(programObjectId)
                Log.i("ShaderHelper", "Could not link program: $programObjectId")
                return 0
            }
            return programObjectId
        }
    }

}