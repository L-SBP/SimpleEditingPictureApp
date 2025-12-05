#version 100
precision mediump float;
varying vec2 v_tex_coord;
uniform sampler2D u_texture;

uniform bool u_gray_scale;      // 灰度开关
uniform float u_contrast;       // 对比度[0.0, 2.0]
uniform float u_saturation;     // 饱和度[0.0, 2.0]

void main() {
    vec4 color = texture2D(u_texture, v_tex_coord);

    if (u_gray_scale) {
        float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
        color.rgb = vec3(gray);
    }

    if (u_contrast != 1.0) {
        color.rgb = (color.rgb - 0.5) * u_contrast + 0.5;
    }

    if (u_saturation != 1.0) {
        float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
        color.rgb = mix(vec3(gray), color.rgb, u_saturation);
    }

    gl_FragColor = color;
}