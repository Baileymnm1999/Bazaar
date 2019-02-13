package edu.rosehulman.bazaar

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class Message(val author: String = "", val thread: String = "", val content: String = "") {
    @ServerTimestamp val timestamp: Timestamp? = null
}