package edu.rosehulman.bazaar

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage

class DatabaseManager {

    companion object {

        fun createUser(user: User) {
            if(user.uid.isEmpty()) {
                // TODO: Write exception to throw
            }else {
                FirebaseFirestore.getInstance().collection("users")
                    .document(user.uid).set(user)
            }
        }

        fun createSchool(school: School) {
            if(school.domain.isEmpty()) {
                // TODO: Write exception to throw
            }else {
                Log.d("BAZZAARR", school.domain.toString())
                FirebaseFirestore.getInstance().collection("schools")
                    .document(school.domain).set(school)
            }
        }

        fun getSchool(user: User, onCompleteListener: (School?) -> Unit) {
            FirebaseFirestore.getInstance().collection("schools")
                .document(user.domain)
                .get()
                .addOnCompleteListener {
                    onCompleteListener(it.result?.toObject(School::class.java))
                }
        }

        fun uploadListing(listing: Listing, image: Bitmap?, imageUri: Uri?, onCompleteListener:  () -> Unit? = {}) {
            val listingRef = FirebaseFirestore.getInstance().collection("listings").document()
            val storageRef = FirebaseStorage.getInstance().reference
            listing.id = listingRef.id
            if(image != null && imageUri != null) {
//                listing.image = image
                listing.images.add("images/${listing.id}/0")
                storageRef.child(listing.images.last()).putFile(imageUri).addOnSuccessListener { _ ->
                    listingRef.set(listing).addOnCompleteListener {
                        onCompleteListener()
                    }
                }
            }
        }

        fun getListingUpdates(count: Long, domain: String, onCompleteListener: (List<Listing>) -> Unit) {
            FirebaseFirestore.getInstance().collection("listings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .whereEqualTo("domain", domain)
                .limit(count)
                .get().addOnCompleteListener {
                    val result = it.result
                    if(result != null) {
                        val listings = result.toObjects(Listing::class.java)
                        listings.forEach { listing ->
                            listing.images.forEach { imageUri ->
                                FirebaseStorage.getInstance().reference.child(imageUri).getBytes(1024*1024).addOnCompleteListener {
                                    Log.d("BAZZAARR", it.result.toString())
                                    if(it.result != null) {
                                        listing.image = BitmapFactory.decodeByteArray(it.result, 0, it.result!!.size)
                                    }
                                }
                            }
                        }
                        onCompleteListener(listings)
                    }
                }
        }

        fun getNextListingsFromDomain(timestamp: Timestamp?,count: Long, domain: String, onCompleteListener: (List<Listing>) -> Unit) {
            FirebaseFirestore.getInstance().collection("listings")
                .whereLessThan("timestamp", timestamp ?: Timestamp(0,0))
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .whereEqualTo("domain", domain)
                .limit(count)
                .get().addOnCompleteListener {
                    val result = it.result
                    if(result != null) {
                        onCompleteListener(result.toObjects(Listing::class.java))
                    }
                }
        }
    }
}