package edu.rosehulman.bazaar

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.ImageView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_add_listing.*
import java.io.ByteArrayOutputStream

private const val SELECT_IMAGE_REQ = 1

/*
*   Class which manages posting a new item for sale.
*/
class AddListingActivity: AppCompatActivity() {

    private val images = ArrayList<ByteArray>()
    private var user: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Custom animation, this activity fades in, old activity does nothing
        overridePendingTransition(R.anim.fade_in, R.anim.nothing)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_listing)

        // Rather than have factory method just query for current user
        DatabaseManager.getUser {
            user = it
        }

        // Initialize all buttons and spinners and such
        listing_type.adapter = ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,resources.getStringArray(R.array.listing_item_types))
        add_listing_btn.setOnClickListener { addListing() }
        add_image_btn.setOnClickListener { getImage() }
        cancel_btn.setOnClickListener { cancel() }
    }

    override fun onBackPressed() {
        cancel()
    }

    override fun onPause() {
        if(isFinishing) {
            // Custom 'nothing' animation. Just cuts to new activity quickly
            overridePendingTransition(R.anim.nothing, R.anim.nothing)
        }
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // If req was to get image from gallery
        if(requestCode == SELECT_IMAGE_REQ && data != null) {
            // Add image to gallery as feedback to user
            val imageView = layoutInflater.inflate(R.layout.layout_thumbnail, null) as ImageView
            Picasso.with(this).load(data.data).resize(512, 512).into(imageView)
            listing_gallery.addView(imageView)

            // Set on click to remove an image from a listing
            imageView.setOnLongClickListener {
                val builder = AlertDialog.Builder(this)
                builder.setTitle("Remove image?")
                builder.setNegativeButton(android.R.string.no) { _, _ -> }
                builder.setPositiveButton(android.R.string.yes) { _, _ ->
                    images.removeAt(listing_gallery.indexOfChild(imageView))
                    listing_gallery.removeView(imageView)
                }
                builder.create().show()
                true
            }

            // Compress image and add to our images array
            val image = BitmapFactory.decodeStream(contentResolver.openInputStream(data.data!!))
            val out = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 55, out)
            val outputByteArray = out.toByteArray()
            images.add(outputByteArray)
        }
    }

    // Creates and uploads new listing using information currently in form
    private fun addListing() {
        val type = listing_type.selectedItem.toString()
        val title = listing_title.text.toString().trim()
        val description = listing_description.text.toString().trim()
        val price = listing_price.text.toString()

        var exit = false
        if(title == "") {
            listing_title.error = "Give your item a title so people know what you are selling"
            exit = true
        }
        if(price == "") {
            listing_price.error = "Put a price on you item"
            exit = true
        }
        if(exit) return

        val listing = Listing(
            user.name,
            user.uid,
            user.domain,
            type,
            title,
            description,
            price.toInt())

        DatabaseManager.uploadListing(listing, images)
        finish()
    }

    // Insures user doesn't accidentally discard a new listing they may have been making, alerts for confirmation
    private fun cancel() {
        if(listing_title.text.isEmpty() && listing_description.text.isEmpty() && listing_gallery.childCount == 0) {
            finish()
            return
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Discard Post?")
        builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
        builder.setPositiveButton(android.R.string.yes) { _, _ -> finish() }
        builder.create().show()
    }

    // Launch intent to get image for listing
    private fun getImage() {
        // TODO("Change to intent chooser to allow user to take picture as well")
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, SELECT_IMAGE_REQ)
        }
    }

}