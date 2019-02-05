package edu.rosehulman.bazaar

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

data class Listing(
    val author: String = "",
    val authorId: String = "",
    val domain: String = "",
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val price: Int = -1,
    val images: ArrayList<String> = ArrayList(),
    val usersWatching: ArrayList<String> = ArrayList(),
    var id: String = "") {
    @ServerTimestamp var timestamp: Timestamp? = null
    @get: Exclude var adapter: PhotoPagerAdapter? = null
}