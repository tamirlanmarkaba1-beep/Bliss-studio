package com.nova.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.CopyOnWriteArrayList

enum class MeshType(val label: String) {
    CUBE("Куб"), SPHERE("Сфера"), PLANE("Плоскость")
}

/** 3D-объект сцены. Поля — Compose-состояние: инспектор пишет, рендер (GL-поток) читает. */
class SceneObject(val id: Int, name: String, val mesh: MeshType) {
    var name by mutableStateOf(name)
    var x by mutableFloatStateOf(0f)
    var y by mutableFloatStateOf(0f)
    var z by mutableFloatStateOf(0f)
    var rotY by mutableFloatStateOf(0f)
    var sx by mutableFloatStateOf(1f)
    var sy by mutableFloatStateOf(1f)
    var sz by mutableFloatStateOf(1f)
    var r by mutableFloatStateOf(0.8f)
    var g by mutableFloatStateOf(0.8f)
    var b by mutableFloatStateOf(0.8f)
    /** Скорость вращения вокруг Y, град/сек (только в рендере, значение rotY не меняется). */
    var spin by mutableFloatStateOf(0f)
    var castShadow by mutableStateOf(true)
    var receiveShadow by mutableStateOf(true)
    var checker by mutableStateOf(false)
}

class Light {
    /** Азимут источника, градусы. */
    var azimuth by mutableFloatStateOf(40f)
    /** Высота над горизонтом, градусы (5..85). */
    var elevation by mutableFloatStateOf(55f)
    var intensity by mutableFloatStateOf(1.0f)
    var ambient by mutableFloatStateOf(0.28f)
    var shadowsEnabled by mutableStateOf(true)
}

class Scene {
    val objects = CopyOnWriteArrayList<SceneObject>()
    var version by mutableIntStateOf(0)
        private set
    var selected by mutableStateOf<SceneObject?>(null)
    val light = Light()

    // Орбитальная камера
    var camYaw by mutableFloatStateOf(35f)
    var camPitch by mutableFloatStateOf(28f)
    var camDist by mutableFloatStateOf(9f)

    private var nextId = 1

    fun add(name: String, mesh: MeshType): SceneObject {
        val o = SceneObject(nextId++, name, mesh)
        objects.add(o)
        selected = o
        version++
        return o
    }

    fun remove(o: SceneObject) {
        objects.remove(o)
        if (selected === o) selected = null
        version++
    }

    fun toJson(): String {
        val arr = JSONArray()
        for (o in objects) {
            arr.put(
                JSONObject()
                    .put("id", o.id).put("name", o.name).put("mesh", o.mesh.name)
                    .put("position", JSONArray(listOf(o.x.toDouble(), o.y.toDouble(), o.z.toDouble())))
                    .put("rotationY", o.rotY.toDouble())
                    .put("scale", JSONArray(listOf(o.sx.toDouble(), o.sy.toDouble(), o.sz.toDouble())))
                    .put("color", JSONArray(listOf(o.r.toDouble(), o.g.toDouble(), o.b.toDouble())))
                    .put("spin", o.spin.toDouble())
                    .put("castShadow", o.castShadow).put("receiveShadow", o.receiveShadow)
            )
        }
        val l = JSONObject()
            .put("azimuth", light.azimuth.toDouble()).put("elevation", light.elevation.toDouble())
            .put("intensity", light.intensity.toDouble()).put("ambient", light.ambient.toDouble())
            .put("shadows", light.shadowsEnabled)
        return JSONObject().put("version", 1).put("light", l).put("objects", arr).toString(2)
    }

    companion object {
        fun createDefault(): Scene {
            val s = Scene()
            s.add("Ground", MeshType.PLANE).apply {
                sx = 24f; sy = 1f; sz = 24f
                r = 0.55f; g = 0.58f; b = 0.62f
                checker = true; castShadow = false
            }
            s.add("Cube", MeshType.CUBE).apply {
                y = 1f; sx = 2f; sy = 2f; sz = 2f
                r = 0.92f; g = 0.36f; b = 0.22f
                spin = 25f
            }
            s.add("Sphere", MeshType.SPHERE).apply {
                x = 3f; y = 0.75f; z = -1.5f
                sx = 1.5f; sy = 1.5f; sz = 1.5f
                r = 0.25f; g = 0.55f; b = 0.95f
            }
            s.selected = s.objects[1]
            return s
        }
    }
}
