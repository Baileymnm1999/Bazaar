package edu.rosehulman.bazaar

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.inputmethod.EditorInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_message.*

class MessageActivity : AppCompatActivity() {

    companion object {
        const val THREAD_ID = "THREAD"
        const val MEMBER_ARRAY = "MEMBERS"
    }

    private val user = FirebaseAuth.getInstance().currentUser?.uid
    private var members = ArrayList<String>()
    private var recipient: String? = null
    private var threadId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message)

        intent.getStringArrayListExtra(MEMBER_ARRAY).forEach {
            if(it != user) recipient = it
        }

        threadId = intent.getStringExtra(THREAD_ID)

        val adapter = if(threadId == null) {
            MessageAdapter(this, "thread")
        }else {
            MessageAdapter(this, "thread", threadId!!)
        }

        messages_recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, true)
        messages_recycler.adapter = adapter

        compose.setOnEditorActionListener { _, actionId, _ ->
            if(actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage(compose.text.toString())
                compose.setText("")
            }
            true
        }

        send_btn.setOnClickListener {
            sendMessage(compose.text.toString())
            compose.setText("")
        }
    }

    private fun sendMessage(content: String) {
        val db = FirebaseFirestore.getInstance()
        if(threadId == null) {
            val thread = db.collection(DatabaseManager.THREADS_COLLECTION).document()
            members.add(user!!)
            members.add(recipient!!)
            thread.set(Thread(members, thread.id))
            threadId = thread.id
            messages_recycler.adapter = MessageAdapter(this, "thread", threadId!!)
        }
        db.collection(DatabaseManager.MESSEGES_COLLECTION).document().set(Message(user!!, threadId!!, content))
    }
}
