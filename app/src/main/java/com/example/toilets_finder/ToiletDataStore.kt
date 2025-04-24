package com.example.toilets_finder

// create an object to store the toilets data fetched from the API
object ToiletDataStore {
    val toiletList = mutableListOf<Triple<Double, Double, String>>() // lat, lon, address
}