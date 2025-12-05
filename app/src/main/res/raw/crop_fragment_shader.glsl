precision mediump float;

uniform vec4 u_color;          // 绘制颜色（rgba）
uniform float u_draw_type;       // 0=矩形/直线，1=圆形
uniform vec2 u_circle_center;  // 圆心坐标（归一化）
uniform float u_circle_radius; // 圆半径（归一化）

varying vec2 v_position;       // 顶点坐标

void main() {
    if (u_draw_type == 1.0) {
        float dist = distance(v_position, u_circle_center);
        if (dist > u_circle_radius) {
            discard; // 丢弃超出的像素
        }
    }
    gl_FragColor = u_color;
}