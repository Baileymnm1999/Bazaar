package edu.rosehulman.bazaar

import android.net.Uri
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask

class DatabaseManager {

    companion object {

        // Some Constants for collections and other db stuff
        const val LOAD_RATE = 10L
        const val USER_COLLECTION = "users"
        const val SCHOOL_COLLECTION = "schools"
        const val LISTINGS_COLLECTION = "listings"
        const val SOLD_COLLECTION = "sold"
        val START_OF_TIME = Timestamp(0, 0)

        // Adds user obj to database
        fun createUser(user: User) {
            FirebaseFirestore.getInstance().collection(USER_COLLECTION).document(user.uid).set(user)
        }

        // Gets user from current logged in user
        fun getUser(onCompleteListener: (User) -> Unit) {
            FirebaseFirestore.getInstance().collection(USER_COLLECTION).document(FirebaseAuth.getInstance().currentUser!!.uid)
                .get().addOnSuccessListener {
                    if(it != null) onCompleteListener(it.toObject(User::class.java)!!)
                }
        }

        // Adds school obj to database
        fun createSchool(school: School) {
            FirebaseFirestore.getInstance().collection(SCHOOL_COLLECTION).document(school.domain).set(school)
        }

        @Deprecated("Unneeded, school objects may even be redundant")
        fun getSchool(user: User, onCompleteListener: (School?) -> Unit) {
            FirebaseFirestore.getInstance().collection(SCHOOL_COLLECTION).document(user.domain).get()
                .addOnCompleteListener {
                    onCompleteListener(it.result?.toObject(School::class.java))
                }
        }

        // Removes listing from database and adds it to sold
        fun markAsSold(listing: Listing) {
            FirebaseFirestore.getInstance().collection(SOLD_COLLECTION).document(listing.id).set(listing)
            deleteListing(listing)
        }

        // Uploads image one at a time sequentially, when all uploaded, adds listing to db and executes callback
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

        // Starts the upload for a listing and it's images
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

        // Takes existing listing in db and updates content
        fun updateListing(listing: Listing, onCompleteListener: () -> Unit = {}) {
            FirebaseFirestore.getInstance().collection(LISTINGS_COLLECTION).document(listing.id).set(listing)
                .addOnSuccessListener {
                    onCompleteListener()
                }
        }

        // Removes listing from db
        fun deleteListing(listing: Listing) {
            listing.images.forEach {
                FirebaseStorage.getInstance().getReferenceFromUrl(it).delete()
            }
            FirebaseFirestore.getInstance().collection(LISTINGS_COLLECTION).document(listing.id).delete()
        }
    }
}