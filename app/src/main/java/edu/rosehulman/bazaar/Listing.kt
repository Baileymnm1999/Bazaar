package edu.rosehulman.bazaar

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp


data class Listing(
    val author: String = "",
    val domain: String = "",
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val images: ArrayList<String> = ArrayList(),
    var id: String = "") {
    @ServerTimestamp var timestamp: Timestamp? = null
}