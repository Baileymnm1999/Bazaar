package edu.rosehulman.bazaar

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.animation.Animation
import android.widget.ViewFlipper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_feed.*
import android.widget.ArrayAdapter
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.layout_add_listing.view.*
import java.lang.Math.max
import java.lang.Math.min


class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private var dbUser: User? = null
    private var dbSchool: School = School()
    private lateinit var feedAdapter: ListingAdapter
    private lateinit var flipper: ViewFlipper

    private val onNavigationListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                flipper.displayedChild = 0
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                flipper.displayedChild = 1
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                flipper.displayedChild = 2
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    companion object {
        val context = this
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(auth.currentUser != null) {
            Log.d("BAZZAARR", "USER NOT NULL")
            FirebaseFirestore.getInstance().collection("users")
                .document(auth.currentUser!!.uid)
                .get().addOnCompleteListener {
                    val result = it.result
                    dbUser = result?.toObject(User::class.java)
                    if(dbUser != null) {
                        Log.d("BAZZAARR", "DBUSER NULL?: ${dbUser.toString()}")
                        DatabaseManager.getSchool(dbUser!!) { school ->
                            Log.d("BAZZAARR", dbSchool.domain.toString())
                            if (school != null) {

                                dbSchool = school
                                feedAdapter = ListingAdapter(this, dbSchool.domain, 8)
                                feed_recycler.adapter = feedAdapter
                                // Set refresh pull down listener
                                feed_refresher.setOnRefreshListener {
                                    feedAdapter.forceRefresh {
                                        feed_refresher.isRefreshing = false
                                        feed_recycler.scrollToPosition(0)
                                    }
                                }
                            } else {
                                val users = ArrayList<String>()
                                val listings = ArrayList<String>()
                                users.add(dbUser!!.uid)
                                dbSchool = School(dbUser!!.domain, dbUser!!.domain, users, listings)
                                DatabaseManager.createSchool(dbSchool)
                            }
                        }
                    }
                }
        }

        navigation.setOnNavigationItemSelectedListener(onNavigationListener)
        flipper = main_flipper
        feed_recycler.layoutManager = LinearLayoutManager(this)
        feed_recycler.setHasFixedSize(false)

        // Logout if user goes null
        auth.addAuthStateListener {
            if(auth.currentUser == null) {
                startActivity(Intent(this, SigninActivity::class.java))
                finish()
            }
        }

        feed_recycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if(dy > 5) {
                    fab_add_listing.hide()
                    navigation.translationY = max(0f, min(navigation.height.toFloat(), navigation.translationY + dy))
                }else if(dy < -10) {
                    fab_add_listing.show()
                    navigation.translationY = max(0f, min(navigation.height.toFloat(), navigation.translationY + dy))
                }
            }
        })

        profile_image.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        fab_add_listing.setOnClickListener {
            buildAddListingDialog().show()
        }
    }

    private fun buildAddListingDialog(): AlertDialog {
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.layout_add_listing, null, false)
        val spinnerAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,resources.getStringArray(R.array.listing_item_types))
        view.add_listing_type.adapter = spinnerAdapter
        builder.setTitle("Sell an item")
        // On post create listing object and add it to database
        builder.setPositiveButton("Post") { _, _ ->
            val urls = ArrayList<String>()
            val listing = Listing(
                auth.currentUser!!.displayName!!,
                dbUser!!.domain,
                view.add_listing_type.selectedItem.toString(),
                view.add_listing_title.text.toString(),
                view.add_listing_description.text.toString(),
                urls)
//            feedAdapter.add(listing)
            DatabaseManager.uploadListing(listing) {
            feed_recycler.smoothScrollToPosition(0)
            }
        }
        builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
        builder.setView(view)
        return builder.create()
    }

}
