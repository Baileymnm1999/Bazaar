package edu.rosehulman.bazaar

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.squareup.picasso.Picasso

class PhotoPagerAdapter(private val context: Context, private val images: ArrayList<String>): PagerAdapter() {

    override fun getCount() = images.size

    override fun isViewFromObject(view: View, obj: Any): Boolean {
        return view == obj
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        // Create image view and add image
        val imageView = ImageView(context)
        Picasso.with(context).load(images[position]).fit().centerCrop().into(imageView)
        container.addView(imageView)

        return imageView
    }

    override fun destroyItem(container: ViewGroup, position: Int, imageView: Any) {
        container.removeView(imageView as ImageView)
    }

    fun getImages() = images

}