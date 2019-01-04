package edu.rose_hulman.bazaar

import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView

class ListingViewHolder(val context: Context, itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val title = itemView.findViewById<TextView>(R.id.listing_title)
    private val description = itemView.findViewById<TextView> (R.id.listing_description)
    private val featured = itemView.findViewById<ImageView>(R.id.featured_img)
    private val icons = HashMap<String, Int>()

    init {
        val types = context.resources.getStringArray(R.array.listing_item_types)
        icons.put("Textbooks", R.drawable.ic_book_24dp)
        icons.put("Electronics", R.drawable.ic_laptop_24dp)
        icons.put("Furniture", R.drawable.ic_kitchen_24dp)
        icons.put("Service", R.drawable.ic_service_24dp)
    }


    fun bind(listing: Listing) {
        title.text = listing.title
        title.setBackgroundColor(0xFFFFFFFF.toInt())
        var ico = 0
        if(icons.containsKey(listing.type)) {
            Log.d("BAZAARRR", "${listing.type} ${icons.get(listing.type)}")
            ico = icons.get(listing.type)!!
        }
        title.setCompoundDrawablesWithIntrinsicBounds(ico, 0, 0, 0)
        description.text = listing.description
        description.setBackgroundColor(0xFFFFFFFF.toInt())
//        featured.setImageResource(R.drawable.logo)
    }
}