package edu.rosehulman.bazaar

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user_listings.view.*
import kotlin.math.max
import kotlin.math.min

private const val ARG_USER = "USER"

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
        val view = inflater.inflate(R.layout.fragment_user_listings, container, false)

        val adapter = ListingAdapter(
            context!!,
            "author",
            user.name,
            object: ListingAdapter.OnListingAddedListener {
                override fun onListingAdded() = view.user_listings_recycler.scrollToPosition(0)
            }
        )
        view.user_listings_recycler.adapter = adapter

        view.user_listings_refresher.setOnRefreshListener {
            adapter.forceRefresh {
                view.user_listings_refresher.isRefreshing = false
                view.user_listings_recycler.scrollToPosition(0)
            }
        }

        view.user_listings_recycler.addOnScrollListener(object: RecyclerView.OnScrollListener() {
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

