#version 100
attribute vec4 a_position;
attribute vec2 a_tex_coord;
uniform mat4 u_matrix;       // 顶点变换矩阵 (缩放/平移)
uniform mat4 u_tex_matrix;   // 纹理坐标变换矩阵 (裁切)
uniform bool u_is_cropping;  // 裁切模式开关
varying vec2 v_tex_coord;

void main() {
    vec4 transformedPosition = u_matrix * a_position;

    // 裁切模式下额外应用裁切变换
    if (u_is_cropping) {
        gl_Position = u_tex_matrix * transformedPosition;
    } else {
        gl_Position = transformedPosition;
    }

    v_tex_coord = a_tex_coord;
}