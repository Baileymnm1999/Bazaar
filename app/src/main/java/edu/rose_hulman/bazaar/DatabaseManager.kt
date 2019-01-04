package edu.rose_hulman.bazaar

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class DatabaseManager {

    companion object {

        fun createUser(user: User) {
            val userRef = FirebaseDatabase.getInstance().getReference("users/${FirebaseAuth.getInstance().currentUser!!.uid}")
            userRef.setValue(user)
        }

        fun createSchool(school: School) {
            val domain = school.domain.substring(0,school.domain.indexOf('.'))
            Log.d("BAZAARRR", domain)
            val userRef = FirebaseDatabase.getInstance().getReference("schools/$domain")
            userRef.setValue(school)
        }

        fun getSchool(user: User): School?{
            var school: School? = null
            val domain = user.domain.substring(0,user.domain.indexOf('.'))
            FirebaseDatabase.getInstance().getReference("schools/${domain}")
                .addListenerForSingleValueEvent(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if(snapshot.exists()) school = snapshot.getValue(School::class.java)
                    }
                    override fun onCancelled(error: DatabaseError) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }
                })
            return school
        }

        fun uploadListing(listing: Listing, domain: String) {
            val listings = FirebaseDatabase.getInstance().getReference("listings/$domain")
            val lid = listings.push().key
            listings.child(lid!!).setValue(listing)
        }

//        fun getNextListings(count: Int): ArrayList<Listing>{
//            val school = FirebaseDatabase.getInstance().getReference("schools")
//        }
    }
}