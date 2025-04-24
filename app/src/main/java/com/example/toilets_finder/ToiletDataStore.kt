package com.example.toilets_finder

data class Toilet(
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val type: String,
    val imageSrc: Int,
    val openingHours: String,
    val pmrAccess: String,
    val averageRating: Float,
    val yourRating: Float
)


// Create an object to store the toilets data fetched from the API and the project database
object ToiletDataStore {
    val toiletList = mutableListOf<Toilet>()
}