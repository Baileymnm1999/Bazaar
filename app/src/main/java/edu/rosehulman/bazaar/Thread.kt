package edu.rosehulman.bazaar

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp

data class Thread(val members: ArrayList<String> = ArrayList(), val id: String = "") {
    @ServerTimestamp val timestamp: Timestamp? = null
    @Exclude var preview = ""
}