package com.nova.engine

import android.opengl.GLES30
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Меш с интерлив-вершинами: pos(3) + normal(3) + uv(2), VAO + индексы short.
 * Создавать и рисовать только в GL-потоке.
 */
class Mesh private constructor(vertices: FloatArray, indices: ShortArray) {
    private val vao: Int
    private val buffers = IntArray(2)
    private val count = indices.size

    init {
        val a = IntArray(1)
        GLES30.glGenVertexArrays(1, a, 0)
        vao = a[0]
        GLES30.glBindVertexArray(vao)
        GLES30.glGenBuffers(2, buffers, 0)

        val vb = ByteBuffer.allocateDirect(vertices.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        vb.put(vertices).position(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[0])
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, vb, GLES30.GL_STATIC_DRAW)

        val ib = ByteBuffer.allocateDirect(indices.size * 2).order(ByteOrder.nativeOrder()).asShortBuffer()
        ib.put(indices).position(0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, buffers[1])
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, indices.size * 2, ib, GLES30.GL_STATIC_DRAW)

        val stride = 8 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 12)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, stride, 24)
        GLES30.glBindVertexArray(0)
    }

    fun draw() {
        GLES30.glBindVertexArray(vao)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, count, GLES30.GL_UNSIGNED_SHORT, 0)
        GLES30.glBindVertexArray(0)
    }

    fun release() {
        GLES30.glDeleteBuffers(2, buffers, 0)
        GLES30.glDeleteVertexArrays(1, intArrayOf(vao), 0)
    }

    companion object {
        private class Builder {
            val v = ArrayList<Float>()
            val i = ArrayList<Short>()
            fun vert(px: Float, py: Float, pz: Float, nx: Float, ny: Float, nz: Float, u: Float, w: Float) {
                v.add(px); v.add(py); v.add(pz); v.add(nx); v.add(ny); v.add(nz); v.add(u); v.add(w)
            }
            fun tri(a: Int, b: Int, c: Int) { i.add(a.toShort()); i.add(b.toShort()); i.add(c.toShort()) }
            fun vertexCount() = v.size / 8
            fun build() = Mesh(v.toFloatArray(), i.toShortArray())
        }

        /** Куб 1x1x1 с центром в начале координат. */
        fun cube(): Mesh {
            // каждая грань: нормаль n, оси u и v (u × v = n → обход против часовой снаружи)
            val faces = arrayOf(
                floatArrayOf(0f, 0f, 1f, 1f, 0f, 0f, 0f, 1f, 0f),
                floatArrayOf(0f, 0f, -1f, -1f, 0f, 0f, 0f, 1f, 0f),
                floatArrayOf(1f, 0f, 0f, 0f, 0f, -1f, 0f, 1f, 0f),
                floatArrayOf(-1f, 0f, 0f, 0f, 0f, 1f, 0f, 1f, 0f),
                floatArrayOf(0f, 1f, 0f, 1f, 0f, 0f, 0f, 0f, -1f),
                floatArrayOf(0f, -1f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
            )
            val b = Builder()
            val su = floatArrayOf(-1f, 1f, 1f, -1f)
            val sv = floatArrayOf(-1f, -1f, 1f, 1f)
            for (f in faces) {
                val base = b.vertexCount()
                for (k in 0 until 4) {
                    val px = f[0] * 0.5f + f[3] * su[k] * 0.5f + f[6] * sv[k] * 0.5f
                    val py = f[1] * 0.5f + f[4] * su[k] * 0.5f + f[7] * sv[k] * 0.5f
                    val pz = f[2] * 0.5f + f[5] * su[k] * 0.5f + f[8] * sv[k] * 0.5f
                    b.vert(px, py, pz, f[0], f[1], f[2], (su[k] + 1f) / 2f, (sv[k] + 1f) / 2f)
                }
                b.tri(base, base + 1, base + 2)
                b.tri(base, base + 2, base + 3)
            }
            return b.build()
        }

        /** Плоскость 1x1 в XZ (нормаль +Y). Размер задаётся масштабом объекта. */
        fun plane(): Mesh {
            val b = Builder()
            b.vert(-0.5f, 0f, 0.5f, 0f, 1f, 0f, 0f, 1f)
            b.vert(0.5f, 0f, 0.5f, 0f, 1f, 0f, 1f, 1f)
            b.vert(0.5f, 0f, -0.5f, 0f, 1f, 0f, 1f, 0f)
            b.vert(-0.5f, 0f, -0.5f, 0f, 1f, 0f, 0f, 0f)
            b.tri(0, 1, 2)
            b.tri(0, 2, 3)
            return b.build()
        }

        /** UV-сфера диаметром 1. */
        fun sphere(stacks: Int = 18, slices: Int = 36): Mesh {
            val b = Builder()
            for (i in 0..stacks) {
                val theta = i * PI / stacks
                for (j in 0..slices) {
                    val phi = j * 2.0 * PI / slices
                    val nx = (sin(theta) * cos(phi)).toFloat()
                    val ny = cos(theta).toFloat()
                    val nz = (sin(theta) * sin(phi)).toFloat()
                    b.vert(nx * 0.5f, ny * 0.5f, nz * 0.5f, nx, ny, nz, j.toFloat() / slices, i.toFloat() / stacks)
                }
            }
            for (i in 0 until stacks) {
                for (j in 0 until slices) {
                    val a = i * (slices + 1) + j
                    val c = a + slices + 1
                    b.tri(a, a + 1, c)
                    b.tri(a + 1, c + 1, c)
                }
            }
            return b.build()
        }
    }
}
