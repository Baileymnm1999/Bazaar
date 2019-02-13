package edu.rosehulman.bazaar

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.TabLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.OnScrollListener
import android.widget.ViewFlipper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_feed.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.layout_dash.*
import kotlinx.android.synthetic.main.layout_inbox.*
import kotlinx.android.synthetic.main.layout_verify_email.*

class MainActivity : AppCompatActivity(), UserListingsFragment.OnUserFragmentScrollListener, WatchingListingsFragment.OnWatchingFragmentScrollListener {

    private val auth = FirebaseAuth.getInstance()
    private val data = FirebaseFirestore.getInstance()
    private var dbUser: User? = null
    private var dbSchool: School = School()
    private lateinit var flipper: ViewFlipper

    private val onNavigationListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_home -> {
                flipper.displayedChild = 0
                fab_add_listing.show()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dashboard -> {
                flipper.displayedChild = 1
                fab_add_listing.show()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_notifications -> {
                flipper.displayedChild = 2
                fab_add_listing.hide()
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    private val onScrollListener = object: OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)
            if(dy > 5) {
                fab_add_listing.hide()
            }else if(dy < -10) {
                fab_add_listing.show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Logout if user goes null
        auth.addAuthStateListener {
            if(auth.currentUser == null) {
                startActivity(Intent(this, SigninActivity::class.java))
                finish()
            }
        }

        if(auth.currentUser?.isEmailVerified == true) {
            setContentView(R.layout.activity_main)
            initializeActivity()
        }else {
            auth.currentUser?.reload()?.addOnSuccessListener {
                if(auth.currentUser?.isEmailVerified == true) {
                    setContentView(R.layout.activity_main)
                    initializeActivity()
                }else {
                    setContentView(R.layout.layout_verify_email)
                    refresh_btn.setOnClickListener {
                        recreate() // TODO("Make this look better")
                    }
                }
            }
        }
    }

    override fun onScroll(recyclerView: RecyclerView, dx: Int, dy: Int) {
        onScrollListener.onScrolled(recyclerView, dx, dy)
    }

    // TODO("Break this up for maintainability")
    private fun initializeActivity() {
        if(auth.currentUser != null && auth.currentUser?.isEmailVerified == true) {
            data.collection(DatabaseManager.USER_COLLECTION).document(auth.currentUser!!.uid)
                .addSnapshotListener { userSnapshot, _ ->
                    dbUser = userSnapshot?.toObject(User::class.java)

                    if(dbUser != null && dbUser?.domain != dbSchool.domain) {
                        data.collection(DatabaseManager.SCHOOL_COLLECTION).document(dbUser!!.domain)
                            .addSnapshotListener { schoolSnapshot, _ ->
                                if(schoolSnapshot != null) {
                                    dbSchool = schoolSnapshot.toObject(School::class.java)!!

                                    val listingAddedListener = object: ListingAdapter.OnListingAddedListener {
                                        override fun onListingAdded(listing: Listing) = feed_recycler.scrollToPosition(0)
                                    }

                                    val feedAdapter = ListingAdapter(
                                        this,
                                        "domain",
                                        dbUser!!.domain,
                                        listingAddedListener)
                                    feed_recycler.adapter = feedAdapter

                                    feed_refresher.setOnRefreshListener {
                                        feedAdapter.forceRefresh {
                                            feed_refresher.isRefreshing = false
                                            feed_recycler.scrollToPosition(0)
                                        }
                                    }

                                    initializeDashboard()
                                    initializeMessages()

                                } else {
                                    val users = ArrayList<String>()
                                    users.add(dbUser!!.uid)
                                    dbSchool = School(dbUser!!.domain, dbUser!!.domain, users, ArrayList())
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

        feed_recycler.addOnScrollListener(onScrollListener)

        profile_image.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        fab_add_listing.setOnClickListener {
            startActivity(Intent(this, AddListingActivity::class.java))
        }
    }

    private fun initializeDashboard() {
        val pagerAdapter = PagerAdapter(supportFragmentManager, dbUser!!)
        dash_pager.adapter = pagerAdapter
        dash_pager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(dash_tab))
        dash_tab.addOnTabSelectedListener(object: TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if(tab != null) dash_pager.currentItem = tab.position
            }

            override fun onTabReselected(p0: TabLayout.Tab?) {}

            override fun onTabUnselected(p0: TabLayout.Tab?) {}
        })
    }

    private fun initializeMessages() {
        val listener = object: ThreadAdapter.OnThreadAddedListener{
            override fun onThreadAdded(thread: Thread) = inbox_recycler.scrollToPosition(0)
        }

        val threadAdapter = ThreadAdapter(this, "members", dbUser!!.uid, listener)
        inbox_recycler.adapter = threadAdapter
    }
}
