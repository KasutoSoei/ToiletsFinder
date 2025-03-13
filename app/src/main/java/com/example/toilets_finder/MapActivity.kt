package com.example.toilets_finder

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MapActivity : AppCompatActivity() {

    private lateinit var map: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Configuration d'OSMDroid
        Configuration.getInstance().userAgentValue = packageName

        // Initialisation de la carte
        map = findViewById(R.id.map)
        map.setMultiTouchControls(true)

        // Centrer la carte sur une position (ex: Paris)
        val startPoint = GeoPoint(48.980494, 2.300834)
        map.controller.setZoom(12.0)
        map.controller.setCenter(startPoint)

        // Ajouter un marqueur
        val marker = Marker(map)
        marker.position = startPoint
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Maison :)"
        map.overlays.add(marker)
    }

    override fun onDestroy() {
        super.onDestroy()
        map.onDetach() // Nettoyage de la carte pour éviter les fuites de mémoire
    }
}