package edu.rosehulman.bazaar

import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.layout_message.view.*

class MessageAdapter(
    private val context: Context,
    private val searchField: String = "",
    private val searchTerm: String = "",
    private val query: Query = FirebaseFirestore.getInstance().collection(DatabaseManager.MESSEGES_COLLECTION)
        .whereEqualTo(searchField, searchTerm)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .limit(DatabaseManager.LOAD_RATE),
    private val getNextQuery: Query = FirebaseFirestore.getInstance().collection(DatabaseManager.MESSEGES_COLLECTION)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .whereArrayContains(searchField, searchTerm)
): RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    private var messages = ArrayList<Message>()
    private var loadRate = DatabaseManager.LOAD_RATE

    init {
        query.addSnapshotListener { snapshot, _ ->
                if(snapshot != null) processMessageUpdates(snapshot)
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_message, parent, false)
        return MessageViewHolder(context, view)
    }

    override fun getItemCount() = messages.size

    override fun onBindViewHolder(viewHolder: MessageViewHolder, pos: Int) = viewHolder.bind(messages[pos])

    private fun processMessageUpdates(snapshot: QuerySnapshot) {
        for (it in snapshot.documentChanges.reversed()) {
            when(it.type) {
                DocumentChange.Type.ADDED -> {
                    add(it.document.toObject(Message::class.java))
                }
                DocumentChange.Type.REMOVED -> {
                    remove(it.document.toObject(Message::class.java))
                }
                DocumentChange.Type.MODIFIED -> {
                    //update(it.document.toObject(Thread::class.java))
                }
                else -> {}
            }
        }
    }

    private fun add(message: Message) {
        messages.add(0, message)
        notifyItemInserted(0)
    }

    private fun remove(message: Message) {
        messages.remove(message)
        notifyDataSetChanged()
    }

    fun searchNextMessages(timestamp: Timestamp?, onCompleteListener: (List<Message>) -> Unit) {
        getNextQuery
            .whereLessThan("timestamp", timestamp ?: DatabaseManager.START_OF_TIME)
            .limit(DatabaseManager.LOAD_RATE)
            .get().addOnSuccessListener {
                onCompleteListener(it.toObjects(Message::class.java))
            }
    }

    inner class MessageViewHolder(val context: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageView = itemView
        private val user = FirebaseAuth.getInstance().currentUser?.uid

        init {

        }

        fun bind(message: Message) {
            messageView.message.text = message.content
            if(message.author == user) {
                messageView.message_container.gravity = Gravity.END
                messageView.message.background = ContextCompat.getDrawable(context, R.drawable.sent_message_bg)
            }else {
                messageView.message_container.gravity = Gravity.START
                messageView.message.background = ContextCompat.getDrawable(context, R.drawable.recieved_message_bg)
            }

            // If this is the last item, load the next x items, x is load rate in Databasemanager
            if(adapterPosition == messages.size - 1 && messages.size >= loadRate) {
                searchNextMessages(messages.lastOrNull()?.timestamp) {
                    if(!it.isEmpty()) {
                        messages.addAll(it)
                        notifyDataSetChanged()
                    }
                }
            }
        }
    }
}