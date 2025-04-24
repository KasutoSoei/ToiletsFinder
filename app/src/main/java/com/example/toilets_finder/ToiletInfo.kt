package com.example.toilets_finder

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ToiletApiResponse(
    val results: List<ToiletInfo>
)

@Serializable
data class ToiletInfo(

    @SerialName("location")
    val location: GeoPoint2D,

    @SerialName("type")
    val type: String,

    @SerialName("address")
    val address: String,

    @SerialName("horaire")
    val schedule: String,

    @SerialName("acces_pmr")
    val pmrAccess: Boolean? = null,

    @SerialName("relais_bebe")
    val babyRelay: Boolean? = null,

    @SerialName("url_fiche_equipement")
    val ficheUrl: String

)

@Serializable
data class GeoPoint2D(
    val lat: Double,
    val lon: Double
)
