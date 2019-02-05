package edu.rosehulman.bazaar

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_gallery.*

class GalleryActivity : AppCompatActivity() {

    companion object {
        const val URL_ARRAY = "URLS"
        const val POSITION_OPENED = "POS"
    }

    private var images: ArrayList<String>? = null
    private var openedPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.nothing, R.anim.nothing)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gallery)

        // Get images and start gallery
        images = intent.getStringArrayListExtra(URL_ARRAY)
        openedPosition = intent.getIntExtra(POSITION_OPENED, 0)
        if(images != null) {
            inflateGallery()
        }else {
            finish()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        if(isFinishing) overridePendingTransition(R.anim.nothing, R.anim.nothing)
    }

    private fun inflateGallery() {
        // Create adapter for image pager and set
        val adapter = object: PhotoPagerAdapter(this, images!!) {
            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                // Create image view and add image
                val imageView = ImageView(this.context)
                Picasso.with(this.context).load(this.images[position]).into(imageView)

                container.addView(imageView)
                return imageView
            }
        }
        gallery_pager.adapter = adapter
        gallery_pager.setCurrentItem(openedPosition, false)
    }
}
