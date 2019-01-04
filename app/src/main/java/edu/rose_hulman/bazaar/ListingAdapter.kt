package edu.rose_hulman.bazaar

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

class ListingAdapter(val context: Context): RecyclerView.Adapter<ListingViewHolder>() {

    private val listings = ArrayList<Listing>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_listing, parent, false)
        return ListingViewHolder(context, view)
    }

    override fun getItemCount() = listings.size

    override fun onBindViewHolder(viewHolder: ListingViewHolder, pos: Int) = viewHolder.bind(listings[pos])

    fun add(listing: Listing) {
        listings.add(0, listing)
        notifyItemInserted(0)
    }

    fun has(listingToFind: Listing): Boolean {
        for(listing in listings) {
            if(listing.equals(listingToFind)) return true
        }
        return false
    }
}