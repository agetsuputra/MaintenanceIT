package com.example.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.util.Locale

fun getDeviceLocation(context: Context, fallbackLocation: String): String {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager != null) {
        try {
            val hasCoarse = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            val hasFine = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            
            if (hasCoarse || hasFine) {
                val providers = locationManager.getProviders(true)
                for (provider in providers) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) {
                        return "Lat: ${String.format(Locale.US, "%.4f", loc.latitude)}, Lon: ${String.format(Locale.US, "%.4f", loc.longitude)} (GPS) - $fallbackLocation"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    return fallbackLocation
}

fun fetchRealtimeLocation(context: Context, onResult: (String) -> Unit) {
    val fusedLocationClient = try {
        com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
    } catch (e: Exception) {
        null
    }

    val hasCoarse = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    
    val hasFine = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    if (!hasCoarse && !hasFine) {
        onResult("Location Permission Denied")
        return
    }

    if (fusedLocationClient != null) {
        val cts = com.google.android.gms.tasks.CancellationTokenSource()
        try {
            fusedLocationClient.getCurrentLocation(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).addOnSuccessListener { loc ->
                if (loc != null) {
                    onResult("Lat: ${String.format(Locale.US, "%.5f", loc.latitude)}, Lon: ${String.format(Locale.US, "%.5f", loc.longitude)}")
                } else {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            onResult("Lat: ${String.format(Locale.US, "%.5f", lastLoc.latitude)}, Lon: ${String.format(Locale.US, "%.5f", lastLoc.longitude)}")
                        } else {
                            fallbackLocationManager(context, onResult)
                        }
                    }.addOnFailureListener {
                        fallbackLocationManager(context, onResult)
                    }
                }
            }.addOnFailureListener {
                fallbackLocationManager(context, onResult)
            }
        } catch (e: SecurityException) {
            fallbackLocationManager(context, onResult)
        }
        
        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            cts.cancel()
        }, 1500)
    } else {
        fallbackLocationManager(context, onResult)
    }
}

private fun fallbackLocationManager(context: Context, onResult: (String) -> Unit) {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
        onResult("GPS Unavailable")
        return
    }
    try {
        val providers = locationManager.getProviders(true)
        for (provider in providers) {
            val loc = locationManager.getLastKnownLocation(provider)
            if (loc != null) {
                onResult("Lat: ${String.format(Locale.US, "%.5f", loc.latitude)}, Lon: ${String.format(Locale.US, "%.5f", loc.longitude)}")
                return
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    onResult("GPS Signal Lost")
}
