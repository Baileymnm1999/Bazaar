package edu.rose_hulman.bazaar


data class Listing(
    val author: String = "",
    val type: String = "",
    val title: String = "",
    val description: String = "",
    val images: ArrayList<String> = ArrayList())