package edu.rosehulman.bazaar

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

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

        fun uploadListing(listing: Listing, onCompleteListener:  () -> Unit? = {}) {
            val listingRef = FirebaseFirestore.getInstance().collection("listings").document()
                listing.id = listingRef.id
                listingRef.set(listing)
                    .addOnCompleteListener {
                        onCompleteListener()
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
                        onCompleteListener(result.toObjects(Listing::class.java))
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
                        Log.d("BAZZAARR", "RESULT: ${result.toObjects(Listing::class.java).toString()}")
                        onCompleteListener(result.toObjects(Listing::class.java))
                    }
                }
        }
    }
}