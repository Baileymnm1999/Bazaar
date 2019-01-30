package edu.rosehulman.bazaar

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.storage.FirebaseStorage
import java.lang.Exception


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
    @get: Exclude var image: Bitmap? = null

    companion object {
        fun fromListing(listing: Listing): Listing {
            return Listing(
                listing.author,
                listing.authorId,
                listing.domain,
                listing.type,
                listing.title,
                listing.description,
                listing.price,
                listing.images,
                listing.usersWatching,
                listing.id)
        }
    }

}