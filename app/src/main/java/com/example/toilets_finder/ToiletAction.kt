package com.example.toilets_finder


// Imports for Kotlin serialization, allowing us to convert objects to/from JSON
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ToiletAction(
    @SerialName("toilet_id")
    val toiletId: String,

    @SerialName("is_favorite")
    var isFavorite: Boolean = false,

    @SerialName("is_blacklisted")
    var isBlacklisted: Boolean = false,

    @SerialName("rating")
    var rating: Float? = null,

    @SerialName("comment")
    var comment: String? = null
)
