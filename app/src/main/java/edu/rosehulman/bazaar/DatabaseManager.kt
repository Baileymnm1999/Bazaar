package edu.rosehulman.bazaar

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.lang.Exception
import java.util.concurrent.CountDownLatch

class DatabaseManager {

    companion object {

        const val LOAD_RATE = 10L
        const val USER_COLLECTION = "users"
        const val SCHOOL_COLLECTION = "schools"
        const val LISTINGS_COLLECTION = "listings"
        private const val SOLD_COLLECTION = "sold"
        val START_OF_TIME = Timestamp(0, 0)

        fun createUser(user: User) {
            FirebaseFirestore.getInstance().collection(USER_COLLECTION).document(user.uid).set(user)
        }

        fun getUser(onCompleteListener: (User) -> Unit) {
            FirebaseFirestore.getInstance().collection(USER_COLLECTION).document(FirebaseAuth.getInstance().currentUser!!.uid)
                .get().addOnSuccessListener {
                    if(it != null) onCompleteListener(it.toObject(User::class.java)!!)
                }
        }

        fun createSchool(school: School) {
            FirebaseFirestore.getInstance().collection(SCHOOL_COLLECTION).document(school.domain).set(school)
        }

        fun getSchool(user: User, onCompleteListener: (School?) -> Unit) {
            FirebaseFirestore.getInstance().collection(SCHOOL_COLLECTION).document(user.domain).get()
                .addOnCompleteListener {
                    onCompleteListener(it.result?.toObject(School::class.java))
                }
        }

        fun markAsSold(listing: Listing) {
            val newListingRef = FirebaseFirestore.getInstance().collection(SOLD_COLLECTION).document()
            val oldListing = Listing.fromListing(listing)
            val newListing = Listing.fromListing(listing)
            newListing.id = newListingRef.id
            newListingRef.set(newListing)
            deleteListing(oldListing)
        }

        private fun uploadImages(listing: Listing, images: ArrayList<ByteArray>, index: Int, onCompleteListener: () -> Unit) {
            val storageRef = FirebaseStorage.getInstance().reference

            val imageRef = storageRef.child("images/${listing.id}/$index")
            val uploadTask = imageRef.putBytes(images[index])
            uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception?.let { exception ->
                        throw exception
                    }
                }
                return@Continuation imageRef.downloadUrl
            }).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    listing.images.add(task.result.toString())
                    if(index + 1 < images.size) {
                        uploadImages(listing, images, index + 1, onCompleteListener)
                    }else if(index + 1 == images.size)  {
                        onCompleteListener()
                    }
                } else {
                    // TODO:("Write failed to upload exception")
                }
            }
        }

        fun uploadListing(listing: Listing, images: ArrayList<ByteArray>, onCompleteListener: () -> Unit = {}) {
            val listingRef = FirebaseFirestore.getInstance().collection(LISTINGS_COLLECTION).document()
            listing.id = listingRef.id

            if(images.size > 0) {
                uploadImages(listing, images, 0) {
                    listingRef.set(listing).addOnSuccessListener {
                        onCompleteListener()
                    }
                }
            }else {
                listingRef.set(listing).addOnSuccessListener {
                    onCompleteListener()
                }
            }
        }

        fun updateListing(listing: Listing, onCompleteListener: () -> Unit = {}) {
            FirebaseFirestore.getInstance().collection(LISTINGS_COLLECTION).document(listing.id).set(listing)
                .addOnSuccessListener {
                    onCompleteListener()
                }
        }

        fun deleteListing(listing: Listing) {
            listing.images.forEach {
                FirebaseStorage.getInstance().getReferenceFromUrl(it).delete()
            }
            FirebaseFirestore.getInstance().collection(LISTINGS_COLLECTION).document(listing.id).delete()
        }
    }
}