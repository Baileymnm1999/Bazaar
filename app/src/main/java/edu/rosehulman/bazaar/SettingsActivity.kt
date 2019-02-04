package edu.rosehulman.bazaar

import android.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_settings.*

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.slide_in_top, R.anim.nothing)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        logout_btn.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Sign out?")
            builder.setMessage("Are you sure you want to sign out?")
            builder.setIcon(R.drawable.ic_exit_to_app_black_24dp)
            builder.setPositiveButton(android.R.string.yes) { _, _ ->
                val auth = FirebaseAuth.getInstance()
                auth.signOut()
                finish()
            }
            builder.setNegativeButton(android.R.string.cancel) { _, _ ->

            }
            builder.create().show()
        }

    }

    override fun onPause() {
        if(isFinishing) {
            overridePendingTransition(R.anim.nothing, R.anim.slide_out_top)
        }
        super.onPause()
    }
}
