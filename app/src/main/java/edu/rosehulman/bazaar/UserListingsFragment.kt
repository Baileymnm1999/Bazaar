package edu.rosehulman.bazaar

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_listings.view.*

private const val ARG_USER = "USER"

/*
*   Fragment used in the 'my listings' tab of the dashboard.
*   Manages Listing adapter using custom query to get listings
*   which were authored by the user
*/
class UserListingsFragment : Fragment() {
    private var user: User = User()
    private var listener: OnUserFragmentScrollListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            user = it.getParcelable(ARG_USER) ?: user
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_listings, container, false)

        // Listener to scroll to top if user posts new listing
        val listingAddedListener = object: ListingAdapter.OnListingAddedListener {
            override fun onListingAdded(listing: Listing) = view.listings_recycler.scrollToPosition(0)
        }

        // Create listing adapter which searched author field for the user's name
        val adapter = ListingAdapter(
            context!!,
            "authorId",
            user.uid,
            listingAddedListener
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
        if (context is OnUserFragmentScrollListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnUserFragmentScrollListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface OnUserFragmentScrollListener {
        fun onScroll(recyclerView: RecyclerView, dx: Int, dy: Int)
    }

    /*
    *   Factory method to create new fragment and give it a user to
    *   be used in the database query
     */
    companion object {
        @JvmStatic
        fun newInstance(user: User) =
            UserListingsFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_USER, user)
                }
            }
    }
}

