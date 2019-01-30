package edu.rosehulman.bazaar

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.ImageView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_add_listing.*
import java.io.ByteArrayOutputStream

private const val SELECT_IMAGE_REQ = 1

class AddListingActivity: AppCompatActivity() {

    private val images = ArrayList<ByteArray>()
    private var user: User = User()

    override fun onCreate(savedInstanceState: Bundle?) {
        overridePendingTransition(R.anim.fade_in, R.anim.nothing)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_listing)

        DatabaseManager.getUser {
            user = it
        }

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
            overridePendingTransition(R.anim.nothing, R.anim.nothing)
        }
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == SELECT_IMAGE_REQ && data != null) {
            val image = BitmapFactory.decodeStream(contentResolver.openInputStream(data.data!!))
            val out = ByteArrayOutputStream()
            image.compress(Bitmap.CompressFormat.JPEG, 55, out)
            val outputByteArray = out.toByteArray()
            val imageView = layoutInflater.inflate(R.layout.layout_thumbnail, null) as ImageView
            Picasso.with(this).load(data.data).resize(512, 512).into(imageView)
            listing_gallery.addView(imageView)
            images.add(outputByteArray)
        }
    }

    private fun addListing() {
        val type = listing_type.selectedItem

        var price = if(listing_price.text.toString() != "") listing_price.text.toString().toInt() else -1

        val listing = Listing(
            user.name,
            user.uid,
            user.domain,
            type.toString(),
            listing_author.text.toString(),
            listing_title.text.toString(),
            price)

        DatabaseManager.uploadListing(listing, images) { finish() }
    }

    private fun cancel() {
        if(listing_author.text.isEmpty() && listing_title.text.isEmpty() && listing_gallery.childCount == 0) {
            finish()
            return
        }
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Discard Post?")
        builder.setNegativeButton(android.R.string.cancel) { _, _ -> }
        builder.setPositiveButton(android.R.string.yes) { _, _ -> finish() }
        builder.create().show()
    }

    private fun getImage() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, SELECT_IMAGE_REQ)
        }
    }

}