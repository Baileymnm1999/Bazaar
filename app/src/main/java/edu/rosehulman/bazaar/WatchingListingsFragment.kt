package edu.rosehulman.bazaar

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_watching_listings.view.*

private const val ARG_USER = "USER"

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
        val view = inflater.inflate(R.layout.fragment_watching_listings, container, false)


        FirebaseFirestore.getInstance().collection(DatabaseManager.USER_COLLECTION).document(user.uid)
            .addSnapshotListener { snapshot, firestoreException ->
                if(snapshot != null) {

                }
            }
        val query = FirebaseFirestore.getInstance().collection(DatabaseManager.LISTINGS_COLLECTION)
            .whereArrayContains("usersWatching", user.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(DatabaseManager.LOAD_RATE)

        val getNextQuery = FirebaseFirestore.getInstance().collection(DatabaseManager.LISTINGS_COLLECTION)
            .whereArrayContains("usersWatching", user.uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)

        val adapter = ListingAdapter(
            context = context!!,
            listingAddedListener =  object: ListingAdapter.OnListingAddedListener {
                override fun onListingAdded() = view.watching_listings_recycler.scrollToPosition(0)
            },
            query =  query,
            getNextQuery = getNextQuery
        )
        view.watching_listings_recycler.adapter = adapter

        view.watching_listings_refresher.setOnRefreshListener {
            adapter.forceRefresh {
                view.watching_listings_refresher.isRefreshing = false
                view.watching_listings_recycler.scrollToPosition(0)
            }
        }

        view.watching_listings_recycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
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
