package edu.rose_hulman.bazaar

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.widget.ViewFlipper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_feed.*
import android.widget.ArrayAdapter
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.layout_add_listing.view.*


class MainActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val data = FirebaseDatabase.getInstance()
    private var dbUser: User? = null
    private lateinit var dbSchool: School
    private lateinit var feedAdapter: ListingAdapter
    private lateinit var flipper: ViewFlipper

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
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

        flipper = main_flipper
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)
        feedAdapter = ListingAdapter(this)
        feed_recycler.layoutManager = LinearLayoutManager(this)
        feed_recycler.setHasFixedSize(false)
        feed_recycler.adapter = feedAdapter

        // Get user
        var userEventListener = object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                dbUser = snapshot.getValue(User::class.java)!!
                refreshFeed()
                // Check for school
                var tmpSchool = DatabaseManager.getSchool(dbUser!!)
                if(tmpSchool != null) {
                    dbSchool = tmpSchool
                    Log.d("BAZAARRR", dbSchool.toString())
                } else {
                    // Create school
                    var users = ArrayList<String>()
                    var listings = ArrayList<String>()
                    users.add(dbUser!!.uid)
                    dbSchool = School(dbUser!!.domain, dbUser!!.domain, users, listings)
                    DatabaseManager.createSchool(dbSchool)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        }
        data.getReference("users/${auth.currentUser?.uid}").addValueEventListener(userEventListener)

        // Logout if user goes null
        auth.addAuthStateListener {
            if(auth.currentUser == null) {
                startActivity(Intent(this, SigninActivity::class.java))
                finish()
            }
        }

        // Set refresh pull down listener
        feed_refresher.setOnRefreshListener {
            refreshFeed()
        }

        profile_image.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        fab_add_listing.setOnClickListener {
            buildAddListingDialog().show()
        }



    }

    private fun refreshFeed() {
        val domain = dbUser!!.domain.substring(0, dbUser!!.domain.indexOf('.'))
        data.getReference("listings/$domain").addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach{
                    var listing = it.getValue(Listing::class.java)
                    if(it.exists() && !feedAdapter.has(listing!!)) {
                        feedAdapter.add(listing!!)
                    }
                    feed_recycler.scrollToPosition(0)
                    feed_refresher.isRefreshing = false
                }
            }
            override fun onCancelled(p0: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        })
    }

    private fun buildAddListingDialog(): AlertDialog {
        val builder = AlertDialog.Builder(this)
        val view = LayoutInflater.from(this).inflate(R.layout.layout_add_listing, null, false)
        var spinnerAdapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,resources.getStringArray(R.array.listing_item_types))
        view.add_listing_type.adapter = spinnerAdapter
        builder.setTitle("Sell an item")
        // On post create listing object and add it to database
        builder.setPositiveButton("Post") { _, _ ->
            var urls = ArrayList<String>()
            var listing = Listing(auth.currentUser!!.displayName!!, view.add_listing_type.selectedItem.toString(),
                view.add_listing_title.text.toString(), view.add_listing_description.text.toString(), urls)
            feedAdapter.add(listing)
            feed_recycler.smoothScrollToPosition(0)
            DatabaseManager.uploadListing(listing, dbUser!!.domain.substring(0, dbUser!!.domain.indexOf('.')))
        }
        builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
        builder.setView(view)
        return builder.create()
    }

}
