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
    val domain: String = "",
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val images: ArrayList<String> = ArrayList(),
    var id: String = "") {
    @ServerTimestamp var timestamp: Timestamp? = null
    @get: Exclude var image: Bitmap? = null

    fun updateImg(onCompleteListener: () -> Unit = {}) {
        images.forEach { imageUri ->
            FirebaseStorage.getInstance().reference.child(imageUri)
                .getBytes(1024*1024).addOnCompleteListener {
                    it.addOnSuccessListener {
                        if(it != null) {
                            Log.d("BAZZAARR", "SS" + it.toString())
                            image = BitmapFactory.decodeByteArray(it, 0, it.size)
                            Log.d("BAZZAARR", image.toString())
                            onCompleteListener()
                        }
                    }
                }
        }
    }

    fun setImageFromUri(imageUri: Uri) {
        image = BitmapFactory.decodeFile(imageUri.encodedPath)
    }
}