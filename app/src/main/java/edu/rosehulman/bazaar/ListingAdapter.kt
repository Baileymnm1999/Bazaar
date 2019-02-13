package edu.rosehulman.bazaar

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import kotlinx.android.synthetic.main.layout_listing.view.*

class ListingAdapter(
    private val context: Context,
    private val searchField: String = "",
    private val searchTerm: String = "",
    private val listingAddedListener: OnListingAddedListener,
    private val query: Query = FirebaseFirestore.getInstance().collection(DatabaseManager.LISTINGS_COLLECTION)
        .whereEqualTo(searchField, searchTerm)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .limit(DatabaseManager.LOAD_RATE),
    private val getNextQuery: Query = FirebaseFirestore.getInstance().collection(DatabaseManager.LISTINGS_COLLECTION)
        .orderBy("timestamp", Query.Direction.DESCENDING)
        .whereEqualTo(searchField, searchTerm)
): RecyclerView.Adapter<ListingAdapter.ListingViewHolder>() {

    private var listings = ArrayList<Listing>()
    private var loadRate = DatabaseManager.LOAD_RATE

    init {
        query.addSnapshotListener { snapshot, _ ->
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

    private fun add(listing: Listing) {
        listings.add(0, listing)
        notifyItemInserted(0)
        listingAddedListener.onListingAdded(listing)
    }

    private fun remove(listing: Listing) {
        val pos = positionForId(listing.id)
        if(pos != -1) {
            listings.removeAt(pos)
            notifyDataSetChanged()
        }
    }

    private fun update(listing: Listing) {
        val pos = positionForId(listing.id)
        if(pos != -1) {
            listings[pos] = listing
            notifyItemChanged(pos)
        }
    }

    fun forceRefresh(onCompleteListener: () -> Unit = {}) {
        query.get()
        onCompleteListener()
    }

    fun searchNextListings(timestamp: Timestamp?, searchField: String, searchTerm: String, onCompleteListener: (List<Listing>) -> Unit) {
        getNextQuery
            .whereLessThan("timestamp", timestamp ?: DatabaseManager.START_OF_TIME)
            .limit(DatabaseManager.LOAD_RATE)
            .get().addOnSuccessListener {
                onCompleteListener(it.toObjects(Listing::class.java))
            }
    }

    interface OnListingAddedListener {
        fun onListingAdded(listing: Listing)
    }

    inner class ListingViewHolder(val context: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val author = itemView.listing_author
        private val title = itemView.listing_title
        private val description = itemView.listing_description
        private val imagePager = itemView.img_pager
        private val watchBtn = itemView.watch_btn
        private val icons = HashMap<String, Int>()
        private val userId = FirebaseAuth.getInstance().currentUser!!.uid

        init {
            val types = context.resources.getStringArray(R.array.listing_item_types)

            types.forEach { listingType ->
                icons[listingType] = context.resources
                    .getIdentifier("ic_$listingType".toLowerCase(), "drawable", context.packageName)
            }

            itemView.watch_btn.setOnClickListener {
                toggleWatching()
            }

            itemView.messege_btn.setOnClickListener {
                sendMessage()
            }

            itemView.setOnLongClickListener {
                handleLongClick()
                true
            }
        }


        fun bind(listing: Listing) {
            // Set icons and author
            val ico = icons[listing.type] ?: 0
            author.setCompoundDrawablesWithIntrinsicBounds(0, 0, ico, 0)
            author.text = listing.author

            // Format title with price, set title and description
            val spannable = SpannableString("${listing.title} $${listing.price}")
            val span = ForegroundColorSpan(ContextCompat.getColor(context, android.R.color.holo_green_dark))
            spannable.setSpan(span, listing.title.length, spannable.length, Spannable.SPAN_EXCLUSIVE_INCLUSIVE)
            title.text = spannable
            description.text = listing.description

            // Hide empty descriptions
            if(listing.description == "") {
                description.layoutParams.height = 0
            }

            // Hide empty image space
            if(listing.images.isEmpty()) {
                imagePager.layoutParams.height = 0;
            }

            // Create adapter for image pager and set
            if(listing.adapter == null) listing.adapter = PhotoPagerAdapter(context, listing.images)
            imagePager.adapter = listing.adapter

            // Set watching button color correctly
            if(listing.usersWatching.contains(FirebaseAuth.getInstance().currentUser!!.uid)) {
                watchBtn.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_star_watching, 0, 0, 0)
            }else {
                watchBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_star, 0, 0, 0)
            }

            // If this is the last item, load the next x items, x is load rate in Databasemanager
            if(adapterPosition == listings.size - 1 && listings.size >= loadRate) {
                searchNextListings(listings.lastOrNull()?.timestamp, searchField, searchTerm) {
                    if(!it.isEmpty()) {
                        listings.addAll(it)
                        notifyDataSetChanged()
                    }
                }
            }
        }

        private fun toggleWatching() {
            if(listings[adapterPosition].usersWatching.contains(userId)) {
                listings[adapterPosition].usersWatching.remove(userId)
                DatabaseManager.updateListing(listings[adapterPosition])
                Toast.makeText(context, "No longer watching", Toast.LENGTH_SHORT).show()
            }else {
                listings[adapterPosition].usersWatching.add(userId)
                DatabaseManager.updateListing(listings[adapterPosition])
                Toast.makeText(context, "Now watching this item!", Toast.LENGTH_SHORT).show()
            }

            DatabaseManager.updateListing(listings[adapterPosition])
        }

        private fun sendMessage() {
            val messageIntent = Intent(context, MessageActivity::class.java)
            messageIntent.putStringArrayListExtra(MessageActivity.MEMBER_ARRAY, arrayListOf(userId, listings[adapterPosition].authorId))
            context.startActivity(messageIntent)
        }

        private fun handleLongClick() {
            val builder = AlertDialog.Builder(context)
            builder.setTitle(listings[adapterPosition].title)
            if(listings[adapterPosition].authorId == userId) {
                builder.setPositiveButton("Mark as sold") { _, _ ->
                    DatabaseManager.markAsSold(listings[adapterPosition])
                }
                builder.setNegativeButton("Delete item") { _, _ ->
                    DatabaseManager.deleteListing(listings[adapterPosition])
                }
                builder.setNeutralButton(android.R.string.cancel) { _, _ -> }
            }else {
                builder.setPositiveButton("Message") { _, _ ->
                    sendMessage()
                }
            }
            builder.create().show()
        }
    }
}