package edu.rose_hulman.bazaar

import java.util.ArrayList

data class School(
    val name: String = "",
    val domain: String = "",
    val users: ArrayList<String> = ArrayList(),
    val listings: ArrayList<String> = ArrayList()
)