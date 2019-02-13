package edu.rosehulman.bazaar

import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.layout_thread.view.*

class ThreadAdapter(
    private val context: Context,
    private val searchField: String = "",
    private val searchTerm: String = "",
    private val listingAddedListener: OnThreadAddedListener,
    private val query: Query = FirebaseFirestore.getInstance().collection(DatabaseManager.THREADS_COLLECTION)
        .whereArrayContains(searchField, searchTerm)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .limit(DatabaseManager.LOAD_RATE),
    private val getNextQuery: Query = FirebaseFirestore.getInstance().collection(DatabaseManager.THREADS_COLLECTION)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .whereArrayContains(searchField, searchTerm)
): RecyclerView.Adapter<ThreadAdapter.ThreadViewHolder>() {

    private var threads = ArrayList<Thread>()
    private var loadRate = DatabaseManager.LOAD_RATE

    init {
        query.addSnapshotListener { snapshot, _ ->
                if(snapshot != null) processThreadUpdates(snapshot)
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThreadViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_thread, parent, false)
        return ThreadViewHolder(context, view)
    }

    override fun getItemCount() = threads.size

    override fun onBindViewHolder(viewHolder: ThreadViewHolder, pos: Int) = viewHolder.bind(threads[pos])

    private fun processThreadUpdates(snapshot: QuerySnapshot) {
        for (it in snapshot.documentChanges.reversed()) {
            when(it.type) {
                DocumentChange.Type.ADDED -> {
                    add(it.document.toObject(Thread::class.java))
                }
                DocumentChange.Type.REMOVED -> {
                    remove(it.document.toObject(Thread::class.java))
                }
                DocumentChange.Type.MODIFIED -> {
                    //update(it.document.toObject(Thread::class.java))
                }
                else -> {}
            }
        }
    }

    private fun add(thread: Thread) {
        threads.add(0, thread)
        notifyItemInserted(0)
        listingAddedListener.onThreadAdded(thread)
    }

    private fun remove(thread: Thread) {
        threads.remove(thread)
        notifyDataSetChanged()
    }

    fun searchNextThreads(timestamp: Timestamp?, onCompleteListener: (List<Thread>) -> Unit) {
        getNextQuery
            .whereLessThan("timestamp", timestamp ?: DatabaseManager.START_OF_TIME)
            .limit(DatabaseManager.LOAD_RATE)
            .get().addOnSuccessListener {
                onCompleteListener(it.toObjects(Thread::class.java))
            }
    }

    interface OnThreadAddedListener {
        fun onThreadAdded(thread: Thread)
    }

    inner class ThreadViewHolder(val context: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val recipient = itemView.recipient
        private val preview = itemView.last_msg

        init {
            itemView.setOnClickListener {
                sendMessage()
            }
        }

        fun bind(thread: Thread) {
            thread.members.forEach {
                Log.d("BAZZAAR", it + " " + FirebaseAuth.getInstance().currentUser?.uid)
                if(it != FirebaseAuth.getInstance().currentUser?.uid) {
                    DatabaseManager.getUser(it) { user ->
                        Log.d("BAZZAAR", user.toString())
                        recipient.text = user.name
                    }
                }
            }

            if(thread.preview.isEmpty()) {
                FirebaseFirestore.getInstance().collection(DatabaseManager.MESSEGES_COLLECTION)
                    .whereArrayContains("thread", thread.id)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(1L).get().addOnSuccessListener {
                        it.forEach {
                            preview.text = it.toObject(Message::class.java).content
                        }
                    }
            }else {
                preview.text = thread.preview
            }

            // If this is the last item, load the next x items, x is load rate in Databasemanager
            if(adapterPosition == threads.size - 1 && threads.size >= loadRate) {
                searchNextThreads(threads.lastOrNull()?.timestamp) {
                    if(!it.isEmpty()) {
                        threads.addAll(it)
                        notifyDataSetChanged()
                    }
                }
            }
        }

        private fun sendMessage() {
            val messageIntent = Intent(context, MessageActivity::class.java)
            messageIntent.putStringArrayListExtra(MessageActivity.MEMBER_ARRAY, threads[adapterPosition].members)
            messageIntent.putExtra(MessageActivity.THREAD_ID, threads[adapterPosition].id)
            context.startActivity(messageIntent)
        }
    }
}