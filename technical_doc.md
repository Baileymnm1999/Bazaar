# Technical Challenge - Bazaar

### The issue

The way the listing feeds in Bazaar works is very cool. The adapters for the feeds are completely self sufficient and manage their selves. I instantiate an adapter, assign it to the recycle view, and forget about it. This is very nice because it allows the adapter to be very portable, for instance, I use the same adapter class for all three listing feeds that you see. The feeds always must stay in sync with the database so for them to be self sufficient they must each manage their own content with custom fire base queries. So each adapter has a query snapshot listener which they use to automatically stay in sync with the backed, which looks like this.

```kotlin
init {
    query.addSnapshotListener { snapshot, _ ->
		if(snapshot != null) processListingUpdates(snapshot)
	}
}

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
```

The custom query can be passed in with the constructor or the default could be used which is just a query for all listings at the user's school. This is a very nice feature, but it caused issues for me when it came time to add images to listings when a user posts them. If a user added an image to a listing, once the listing was submitted, the app would jump back to the main feed where the adapter had already gotten the listing and immediately tried to render it. With no images this is fine, but if we add an image the adapter would attempt to download the image added from where we uploaded it to, Firebase storage. Since adding a listing to the database is much quicker than uploading an image, Firebase would throw an error that the url didn't exist and crash. This is because the image was still in the middle of uploading.

### Attempted Solution

At first I thought okay, this is easy, just throw the add listing method in the callback for uploading an image and call it a day. I did this and it worked, or so I thought. As soon as I tried uploading more than one image, the activity would finish when the first callback was reached and the rest of the images had not finished uploading yet. 

### Solution

I realized the issue was not as trivial as I had thought. I needed a way to know when all the upload image tasks had finished. If it was a known number of uploads, I could just nest them in each other's callbacks and finish the last one with the add listing method. That didn't seem like good coding practice (and I use javascript) and I wanted to allow the user to add an abstract amount of images, so I couldn't do that. Looking back now, the solution seems pretty simple but I believe that is just in it's elegance. At the time, this felt like one of the hardest problems I have faced and I wasn't about to let it beat me. I tried everything from atomic integers to countdown latches, some attempts leading to ANRs and others just not working. This was the first time I had ever posted to stackoverflow, to which, someone just criticized me for blocking the thread but I figured it wouldn't be an issue if I just though up a loading screen amirite? It probably was the reason for the ANRs though, anyway I am even more happy with the solution I managed to come up with on my own. Behold.

```kotlin

    // Uploads image one at a time sequentially, when all uploaded, adds listing to db and executes callback
    private fun uploadImages(listing: Listing, images: ArrayList<ByteArray>, index: Int, onCompleteListener: () -> Unit) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("images/${listing.id}/$index")
        val uploadTask = imageRef.putBytes(images[index])
        uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
            if (!task.isSuccessful) {
                task.exception?.let { exception ->
                    throw exception
                }
            }
            return@Continuation imageRef.downloadUrl
        }).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                listing.images.add(task.result.toString())
                if(index + 1 < images.size) {
                    uploadImages(listing, images, index + 1, onCompleteListener)
                }else if(index + 1 == images.size)  {
                    onCompleteListener()
                }
            } else {
                // TODO:("Write failed to upload exception")
            }
        }
    }

    // Starts the upload for a listing and it's images
    fun uploadListing(listing: Listing, images: ArrayList<ByteArray>, onCompleteListener: () -> Unit = {}) {
        val listingRef = FirebaseFirestore.getInstance().collection(LISTINGS_COLLECTION).document()
        listing.id = listingRef.id
        if(images.size > 0) {
            uploadImages(listing, images, 0) {
                listingRef.set(listing).addOnSuccessListener {
                    onCompleteListener()
                }
            }
        }else {
            listingRef.set(listing).addOnSuccessListener {
                onCompleteListener()
            }
        }
    }
```

What the above solution does, is iterate through an array of images in the form of ByteArrays, and uploads each one one at a time. When the last image is uploaded, it calls and oncomplete listener which is the upload listing method ```listingRef.set(listing).addOnSuccessListener { onCompleteListener }``` . Coming up with this solution was very challenging for me and very rewarding once I found it which is why I am writing about it in this technical document.