package edu.rosehulman.bazaar

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.firestore.*

class ListingAdapter(val context: Context, val domain: String, val loadRate: Long): RecyclerView.Adapter<ListingAdapter.ListingViewHolder>() {

    var listings = ArrayList<Listing>()
    var query: Query

    init {
        query = FirebaseFirestore.getInstance().collection("listings")
            .whereEqualTo("domain", domain)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(if (loadRate>listings.size) loadRate else listings.size as Long)
        query.addSnapshotListener { snapshot, firestoreException ->
                if(snapshot != null) processListingUpdates(snapshot)
            }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_listing, parent, false)
        return ListingViewHolder(context, view)
    }

    override fun getItemCount() = listings.size

    override fun onBindViewHolder(viewHolder: ListingViewHolder, pos: Int) = viewHolder.bind(listings[pos])

    private fun processListingUpdates(snapshot: QuerySnapshot) {
        for (it in snapshot.documentChanges.reversed()) {
            when(it.type) {
                DocumentChange.Type.ADDED -> {
                    add(it.document.toObject(Listing::class.java))
                }
                DocumentChange.Type.REMOVED -> {
                    remove(it.document.toObject(Listing::class.java))
                }
                DocumentChange.Type.MODIFIED -> {
                    update(it.document.toObject(Listing::class.java))
                }
                else -> {}
            }
        }
    }

    private fun positionForId(id: String): Int {
        listings.forEachIndexed { index, listing ->
            if(listing.id == id) return index
        }
        return -1
    }

    fun add(listing: Listing) {
        listings.add(0, listing)
        notifyItemInserted(0)
    }

    fun remove(listing: Listing) {
        val pos = positionForId(listing.id)
        if(pos != -1) {
            listings.removeAt(pos)
            notifyItemRemoved(pos)
        }
    }

    fun update(listing: Listing) {
        val pos = positionForId(listing.id)
        if(pos != -1) {
            listings[pos] = listing
            notifyItemChanged(pos)
        }
    }

    fun has(listingToFind: Listing): Boolean {
        for(listing in listings) {
            if(listing.equals(listingToFind)) return true
        }
        return false
    }

    fun forceRefresh(onCompleteListener: () -> Unit = {}) {
        query.get()
        onCompleteListener()
    }

    fun onBottomReached() {
        DatabaseManager.getNextListingsFromDomain(listings.lastOrNull()?.timestamp, loadRate, domain) {
            Log.d("BAZZAARR", "BOTTOM")
            listings.addAll(it)
            notifyDataSetChanged()
        }
    }

    inner class ListingViewHolder(val context: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val title = itemView.findViewById<TextView>(R.id.listing_title)
        private val description = itemView.findViewById<TextView> (R.id.listing_description)
        private val featured = itemView.findViewById<ImageView>(R.id.featured_img)
        private val icons = HashMap<String, Int>()

        init {
            val types = context.resources.getStringArray(R.array.listing_item_types)
            types.forEach { listingType ->
                icons[listingType] = context.resources
                    .getIdentifier("ic_$listingType".toLowerCase(), "drawable", context.packageName)
            }
//            icons["Textbooks"] = R.drawable.ic_textbooks
//            icons["Electronics"] = R.drawable.ic_electronics
//            icons["Furniture"] = R.drawable.ic_furniture
//            icons["Service"] = R.drawable.ic_service
        }


        fun bind(listing: Listing) {
            title.text = listing.title
            title.setBackgroundColor(0xFFFFFFFF.toInt())
            var ico = 0
            if(icons.containsKey(listing.type)) ico = icons.get(listing.type)!!
            title.setCompoundDrawablesWithIntrinsicBounds(ico, 0, 0, 0)
            description.text = listing.description
            description.setBackgroundColor(0xFFFFFFFF.toInt())
            if(adapterPosition == listings.size - 1 && listings.size >= loadRate) {
                DatabaseManager.getNextListingsFromDomain(listings.lastOrNull()?.timestamp, loadRate, domain) {
                    if(!it.isEmpty()) {
                        listings.addAll(it)
                        notifyDataSetChanged()
                    }
                }
            }
        }
    }
}