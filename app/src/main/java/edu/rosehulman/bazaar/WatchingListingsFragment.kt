package edu.rosehulman.bazaar

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_listings.view.*

private const val ARG_USER = "USER"

/*
*   Fragment used in the 'watching' tab of the dashboard.
*   Manages Listing adapter using custom query to get listings
*   which were marked as watching by the user
*/
class WatchingListingsFragment : Fragment() {
    private var user: User = User()
    private var listener: OnWatchingFragmentScrollListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            user = it.getParcelable(ARG_USER) ?: User()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_listings, container, false)

        // Custom query for getting listings which user is watching
        val query = FirebaseFirestore.getInstance().collection(DatabaseManager.LISTINGS_COLLECTION)
            .whereArrayContains("usersWatching", user.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(DatabaseManager.LOAD_RATE)

        // Custom query for getting the next items x items the user is watching
        val getNextQuery = FirebaseFirestore.getInstance().collection(DatabaseManager.LISTINGS_COLLECTION)
            .whereArrayContains("usersWatching", user.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        // Listener to scroll to the top if user marks a listing as watching
        val listingAddedListener =  object: ListingAdapter.OnListingAddedListener {
            override fun onListingAdded(listing: Listing) = view.listings_recycler.scrollToPosition(0)
        }

        // Create the listing adapter with the custom queries
        val adapter = ListingAdapter(
            context = context!!,
            listingAddedListener = listingAddedListener,
            query =  query,
            getNextQuery = getNextQuery
        )
        view.listings_recycler.adapter = adapter

        view.listings_refresher.setOnRefreshListener {
            adapter.forceRefresh {
                view.listings_refresher.isRefreshing = false
                view.listings_recycler.scrollToPosition(0)
            }
        }

        view.listings_recycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                listener?.onScroll(recyclerView, dx, dy)
            }
        })

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnWatchingFragmentScrollListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnWatchingFragmentScrollListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnWatchingFragmentScrollListener {
        fun onScroll(recyclerView: RecyclerView, dx: Int, dy: Int)
    }

    /*
    *   Factory method to create new fragment and give it a user to
    *   be used in the database query
    */
    companion object {
        @JvmStatic
        fun newInstance(user: User) =
            WatchingListingsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_USER, user)
                }
            }
    }
}
