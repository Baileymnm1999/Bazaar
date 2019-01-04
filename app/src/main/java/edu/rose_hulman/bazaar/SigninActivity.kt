package edu.rose_hulman.bazaar

import android.content.Context
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.widget.Switch
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseUserMetadata
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import edu.rose_hulman.bazaar.R.id.first_name_text_signup
import edu.rose_hulman.bazaar.R.id.last_name_text_signup
import edu.rose_hulman.bazaar.R.string.email
import edu.rose_hulman.bazaar.R.string.password
import kotlinx.android.synthetic.main.activity_load.*
import kotlinx.android.synthetic.main.activity_signin.*
import kotlinx.android.synthetic.main.activity_signin.view.*
import kotlinx.android.synthetic.main.activity_signup.*
import kotlinx.android.synthetic.main.activity_signup.view.*
import java.util.regex.Pattern
import kotlin.math.sign

class SigninActivity : AppCompatActivity() {

    lateinit var auth: FirebaseAuth
    private var user: FirebaseUser? = null
    private lateinit var signinView: View
    private lateinit var signupView: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get Firebase auth object - can get it and auth.currentUser from anywhere in the app
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser

        // Inflate sign in and sign up views to switch between
        signinView = LayoutInflater.from(this).inflate(R.layout.activity_signin, null)
        signupView = LayoutInflater.from(this).inflate(R.layout.activity_signup, null)

        // Switch to sign up
        signinView.signup_link.setOnClickListener {
            setContentView(signupView)
        }

        // Sign in
        signinView.signin_btn.setOnClickListener {
            var email = signinView.email_text_signin.text.toString()
            var password = signinView.password_text_signin.text.toString()
            if(email.isEmpty()) {
                signinView.email_text_signin.error = "Please enter your email"
            }
            if(password.isEmpty()) {
                signinView.password_text_signin.error = "Please enter your password"
            }
            if(email.isNotEmpty() && password.isNotEmpty()) {
                signIn(email, password)
            }
        }

        // Switch to sign in
        signupView.signin_link.setOnClickListener {
            setContentView(signinView)
        }
        // Sign up
        signupView.signup_btn.setOnClickListener {
            var email = signupView.email_text_signup.text.toString()
            var password = signupView.password_text_signup.text.toString()
            var displayName = signupView.first_name_text_signup.text.toString() + " " + signupView.last_name_text_signup.text.toString()
            if(password.isEmpty()) {
                signupView.password_text_signup.error = "Please enter a password"
            }
            if(displayName.equals(" ")) {
                signupView.first_name_text_signup.error = "Please enter your name"
            }
            if(email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() || !email.contains(".edu")) {
                signupView.email_text_signup.error = "Please sign up with your school's .edu address"
            } else if(password.isNotEmpty() && displayName.isNotEmpty()) {
                signUp(email, password, signupView.first_name_text_signup.text.toString(), signupView.last_name_text_signup.text.toString())
            }
        }

        updateUI(user)
    }

    private fun updateUI(user: FirebaseUser?) {
        if(user != null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        } else {
            setContentView(signinView)
        }
    }

    private fun signIn(email: String, password: String) {
        signinView.signin_load.visibility = View.VISIBLE
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("BAZZAR", "sign in success")
                    user = auth.currentUser
                    updateUI(user)
                } else {
                    signinView.signin_load.visibility = View.GONE
                    Log.w("BAZAAR", "sign in fail")
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun signUp(email: String, password: String, firstName: String, lastName: String) {
        signupView.signup_load.visibility = View.VISIBLE
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("BAZAAR", "create user with email success")
                    user = auth.currentUser
                    if (user != null) {
                        user!!.sendEmailVerification()
                        // Create user object and add to database
                        val dbUser = User(user!!.uid,"$firstName $lastName", email, email.substring(email.indexOf('@')+1, email.length))
                        DatabaseManager.createUser(dbUser)
                        // Update display name
                        var profileUpdate = UserProfileChangeRequest.Builder()
                            .setDisplayName(firstName + " " + lastName)
                            .build()
                        user!!.updateProfile(profileUpdate).addOnCompleteListener {}
                        updateUI(user)
                    }
                } else {
                    signupView.signup_load.visibility = View.GONE
                    Log.w("BAZAAR", task.exception)
                    Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
