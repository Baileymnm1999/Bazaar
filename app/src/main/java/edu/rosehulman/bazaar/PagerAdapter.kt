package edu.rosehulman.bazaar

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;


class PagerAdapter(fm: FragmentManager, private val user: User) : FragmentStatePagerAdapter(fm) {

    override fun getItem(position: Int): Fragment? {
        return when (position) {
            0 -> UserListingsFragment.newInstance(user)
            1 -> WatchingListingsFragment.newInstance(user)
            else -> return null
        }
    }

    override fun getCount() = 2
}