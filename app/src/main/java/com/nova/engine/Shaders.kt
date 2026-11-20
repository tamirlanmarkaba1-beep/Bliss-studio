package com.nova.engine

import android.opengl.GLES30

class GlProgram(val id: Int) {
    private val cache = HashMap<String, Int>()
    fun u(name: String): Int = cache.getOrPut(name) { GLES30.glGetUniformLocation(id, name) }
}

object Shaders {

    const val DEPTH_VS = """#version 300 es
layout(location = 0) in vec3 aPos;
uniform mat4 uLightVP;
uniform mat4 uModel;
void main() {
    gl_Position = uLightVP * uModel * vec4(aPos, 1.0);
}
"""

    const val DEPTH_FS = """#version 300 es
precision mediump float;
void main() { }
"""

    const val MAIN_VS = """#version 300 es
layout(location = 0) in vec3 aPos;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUV;
uniform mat4 uModel;
uniform mat4 uNormal;
uniform mat4 uVP;
uniform mat4 uLightVP;
out vec3 vN;
out vec3 vWorld;
out vec4 vLightPos;
out vec2 vUV;
void main() {
    vec4 w = uModel * vec4(aPos, 1.0);
    vWorld = w.xyz;
    vN = mat3(uNormal) * aNormal;
    vLightPos = uLightVP * w;
    vUV = aUV;
    gl_Position = uVP * w;
}
"""

    const val MAIN_FS = """#version 300 es
precision highp float;
precision highp sampler2DShadow;
in vec3 vN;
in vec3 vWorld;
in vec4 vLightPos;
in vec2 vUV;
uniform sampler2DShadow uShadow;
uniform vec3 uLightDir;
uniform vec3 uCamPos;
uniform vec3 uColor;
uniform float uIntensity;
uniform float uAmbient;
uniform float uShadowOn;
uniform float uReceive;
uniform float uChecker;
uniform float uHighlight;
uniform float uTexel;
out vec4 fragColor;

// PCF 3x3 по аппаратному сравнению глубины (sampler2DShadow).
float shadowFactor(vec3 n, vec3 l) {
    vec3 p = vLightPos.xyz / vLightPos.w;
    p = p * 0.5 + 0.5;
    float inside = step(0.0, p.x) * step(p.x, 1.0) * step(0.0, p.y) * step(p.y, 1.0) * step(p.z, 1.0);
    float bias = max(0.002 * (1.0 - dot(n, l)), 0.0006);
    float sum = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 o = vec2(float(x), float(y)) * uTexel;
            sum += texture(uShadow, vec3(p.xy + o, p.z - bias));
        }
    }
    return mix(1.0, sum / 9.0, inside);
}

void main() {
    vec3 n = normalize(vN);
    vec3 l = normalize(uLightDir);
    vec3 v = normalize(uCamPos - vWorld);

    vec3 base = uColor;
    if (uChecker > 0.5) {
        vec2 c = floor(vWorld.xz);
        float k = mod(c.x + c.y, 2.0);
        base *= mix(0.65, 1.0, k);
    }

    float diff = max(dot(n, l), 0.0);
    float sh = mix(1.0, shadowFactor(n, l), uShadowOn * uReceive);

    vec3 h = normalize(l + v);
    float spec = pow(max(dot(n, h), 0.0), 48.0) * 0.35 * step(0.001, diff);

    float hemi = mix(0.65, 1.0, n.y * 0.5 + 0.5);
    vec3 col = base * (uAmbient * hemi + diff * uIntensity * sh) + vec3(spec * uIntensity * sh);
    col = mix(col, vec3(0.2, 0.55, 1.0), uHighlight * 0.18);
    col = pow(col, vec3(1.0 / 2.2));
    fragColor = vec4(col, 1.0);
}
"""

    private fun compile(type: Int, src: String, log: (Boolean, String) -> Unit): Int {
        val s = GLES30.glCreateShader(type)
        GLES30.glShaderSource(s, src)
        GLES30.glCompileShader(s)
        val st = IntArray(1)
        GLES30.glGetShaderiv(s, GLES30.GL_COMPILE_STATUS, st, 0)
        if (st[0] == 0) {
            log(true, "Ошибка шейдера: " + GLES30.glGetShaderInfoLog(s))
            GLES30.glDeleteShader(s)
            return 0
        }
        return s
    }

    fun link(vs: String, fs: String, log: (Boolean, String) -> Unit): GlProgram? {
        val v = compile(GLES30.GL_VERTEX_SHADER, vs, log)
        val f = compile(GLES30.GL_FRAGMENT_SHADER, fs, log)
        if (v == 0 || f == 0) return null
        val p = GLES30.glCreateProgram()
        GLES30.glAttachShader(p, v)
        GLES30.glAttachShader(p, f)
        GLES30.glLinkProgram(p)
        val st = IntArray(1)
        GLES30.glGetProgramiv(p, GLES30.GL_LINK_STATUS, st, 0)
        GLES30.glDeleteShader(v)
        GLES30.glDeleteShader(f)
        if (st[0] == 0) {
            log(true, "Ошибка линковки: " + GLES30.glGetProgramInfoLog(p))
            GLES30.glDeleteProgram(p)
            return null
        }
        return GlProgram(p)
    }
}
