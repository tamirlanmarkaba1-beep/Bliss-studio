package com.nova.engine

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

/**
 * Рендер сцены: проход 1 — глубина из источника света в FBO, проход 2 — освещение + тени.
 * [log] вызывается из GL-потока: (isError, message).
 */
class SceneRenderer(
    private val scene: Scene,
    private val log: (Boolean, String) -> Unit
) : GLSurfaceView.Renderer {

    @Volatile
    var fps = 0

    private var mainP: GlProgram? = null
    private var depthP: GlProgram? = null
    private var shadow: ShadowMap? = null
    private val meshes = HashMap<MeshType, Mesh>()

    private var width = 1
    private var height = 1

    private val proj = FloatArray(16)
    private val view = FloatArray(16)
    private val vp = FloatArray(16)
    private val model = FloatArray(16)
    private val tmp = FloatArray(16)
    private val normalM = FloatArray(16)
    private val lightView = FloatArray(16)
    private val lightProj = FloatArray(16)
    private val lightVP = FloatArray(16)

    private val startNs = System.nanoTime()
    private var frames = 0
    private var lastFpsNs = startNs

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        meshes.clear()
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)

        mainP = Shaders.link(Shaders.MAIN_VS, Shaders.MAIN_FS, log)
        depthP = Shaders.link(Shaders.DEPTH_VS, Shaders.DEPTH_FS, log)
        meshes[MeshType.CUBE] = Mesh.cube()
        meshes[MeshType.PLANE] = Mesh.plane()
        meshes[MeshType.SPHERE] = Mesh.sphere()
        shadow = ShadowMap(2048).also { it.create(log) }

        log(false, "OpenGL ES " + GLES30.glGetString(GLES30.GL_VERSION) + " | " + GLES30.glGetString(GLES30.GL_RENDERER))
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = w.coerceAtLeast(1)
        height = h.coerceAtLeast(1)
        Matrix.perspectiveM(proj, 0, 55f, width.toFloat() / height, 0.1f, 100f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val mp = mainP ?: return
        val dp = depthP ?: return
        val sm = shadow ?: return
        val t = (System.nanoTime() - startNs) / 1e9f
        val light = scene.light

        // ---- матрица света (ортографическая, направленный свет) ----
        val az = Math.toRadians(light.azimuth.toDouble())
        val el = Math.toRadians(light.elevation.coerceIn(5f, 85f).toDouble())
        val lx = (cos(el) * sin(az)).toFloat()
        val ly = sin(el).toFloat()
        val lz = (cos(el) * cos(az)).toFloat()
        Matrix.setLookAtM(lightView, 0, lx * 18f, ly * 18f, lz * 18f, 0f, 0f, 0f, 0f, 1f, 0f)
        Matrix.orthoM(lightProj, 0, -14f, 14f, -14f, 14f, 1f, 45f)
        Matrix.multiplyMM(lightVP, 0, lightProj, 0, lightView, 0)

        val shadowsOn = light.shadowsEnabled

        // ---- проход 1: карта глубины ----
        if (shadowsOn) {
            sm.begin()
            GLES30.glEnable(GLES30.GL_POLYGON_OFFSET_FILL)
            GLES30.glPolygonOffset(2f, 4f)
            GLES30.glUseProgram(dp.id)
            GLES30.glUniformMatrix4fv(dp.u("uLightVP"), 1, false, lightVP, 0)
            drawObjects(dp, true, t)
            GLES30.glDisable(GLES30.GL_POLYGON_OFFSET_FILL)
            sm.end()
        }

        // ---- проход 2: основная сцена ----
        GLES30.glViewport(0, 0, width, height)
        GLES30.glClearColor(0.10f, 0.11f, 0.13f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

        val yaw = Math.toRadians(scene.camYaw.toDouble())
        val pitch = Math.toRadians(scene.camPitch.toDouble())
        val d = scene.camDist
        val ty = 0.8f
        val ex = (d * cos(pitch) * sin(yaw)).toFloat()
        val ey = (d * sin(pitch)).toFloat() + ty
        val ez = (d * cos(pitch) * cos(yaw)).toFloat()
        Matrix.setLookAtM(view, 0, ex, ey, ez, 0f, ty, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(vp, 0, proj, 0, view, 0)

        GLES30.glUseProgram(mp.id)
        GLES30.glUniformMatrix4fv(mp.u("uVP"), 1, false, vp, 0)
        GLES30.glUniformMatrix4fv(mp.u("uLightVP"), 1, false, lightVP, 0)
        GLES30.glUniform3f(mp.u("uLightDir"), lx, ly, lz)
        GLES30.glUniform3f(mp.u("uCamPos"), ex, ey, ez)
        GLES30.glUniform1f(mp.u("uIntensity"), light.intensity)
        GLES30.glUniform1f(mp.u("uAmbient"), light.ambient)
        GLES30.glUniform1f(mp.u("uShadowOn"), if (shadowsOn) 1f else 0f)
        GLES30.glUniform1f(mp.u("uTexel"), 1f / sm.size)
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, sm.tex)
        GLES30.glUniform1i(mp.u("uShadow"), 0)
        drawObjects(mp, false, t)

        // ---- FPS ----
        frames++
        val now = System.nanoTime()
        if (now - lastFpsNs >= 1_000_000_000L) {
            fps = frames
            frames = 0
            lastFpsNs = now
        }
    }

    private fun drawObjects(p: GlProgram, shadowPass: Boolean, t: Float) {
        val sel = scene.selected
        for (o in scene.objects) {
            if (shadowPass && !o.castShadow) continue
            Matrix.setIdentityM(model, 0)
            Matrix.translateM(model, 0, o.x, o.y, o.z)
            Matrix.rotateM(model, 0, o.rotY + t * o.spin, 0f, 1f, 0f)
            Matrix.scaleM(model, 0, o.sx, o.sy, o.sz)
            GLES30.glUniformMatrix4fv(p.u("uModel"), 1, false, model, 0)
            if (!shadowPass) {
                Matrix.invertM(tmp, 0, model, 0)
                Matrix.transposeM(normalM, 0, tmp, 0)
                GLES30.glUniformMatrix4fv(p.u("uNormal"), 1, false, normalM, 0)
                GLES30.glUniform3f(p.u("uColor"), o.r, o.g, o.b)
                GLES30.glUniform1f(p.u("uChecker"), if (o.checker) 1f else 0f)
                GLES30.glUniform1f(p.u("uReceive"), if (o.receiveShadow) 1f else 0f)
                GLES30.glUniform1f(p.u("uHighlight"), if (sel === o) 1f else 0f)
            }
            meshes[o.mesh]?.draw()
        }
    }
}
