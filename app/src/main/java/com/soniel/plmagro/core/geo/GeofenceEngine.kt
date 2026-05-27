package com.soniel.plmagro.core.geo

import android.location.Location
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.soniel.plmagro.model.GeofenceEntity

data class Point(val lat: Double, val lng: Double)

object GeofenceEngine {
    private val gson = Gson()

    /**
     * Verifica se um ponto está dentro de uma cerca (Circular ou Polígono)
     */
    fun isPointInGeofence(lat: Double, lng: Double, geofence: GeofenceEntity): Boolean {
        return when (geofence.type) {
            1 -> isPointInCircle(lat, lng, geofence)
            2 -> isPointInPolygon(lat, lng, geofence)
            else -> false
        }
    }

    private fun isPointInCircle(lat: Double, lng: Double, geofence: GeofenceEntity): Boolean {
        val points: List<Point> = gson.fromJson(geofence.pointsJson, object : TypeToken<List<Point>>() {}.type)
        if (points.isEmpty()) return false
        
        val center = points[0]
        val results = FloatArray(1)
        Location.distanceBetween(lat, lng, center.lat, center.lng, results)
        return results[0] <= geofence.radius
    }

    private fun isPointInPolygon(lat: Double, lng: Double, geofence: GeofenceEntity): Boolean {
        val points: List<Point> = gson.fromJson(geofence.pointsJson, object : TypeToken<List<Point>>() {}.type)
        if (points.size < 3) return false

        var intersectCount = 0
        for (i in points.indices) {
            val j = (i + 1) % points.size
            if (((points[i].lng > lng) != (points[j].lng > lng)) &&
                (lat < (points[j].lat - points[i].lat) * (lng - points[i].lng) / (points[j].lng - points[i].lng) + points[i].lat)
            ) {
                intersectCount++
            }
        }
        return intersectCount % 2 != 0
    }
}
