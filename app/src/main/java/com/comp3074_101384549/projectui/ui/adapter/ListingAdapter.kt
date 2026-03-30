package com.comp3074_101384549.projectui.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.model.Listing

class ListingAdapter(
    private var listings: List<Listing>,
    private val onListingClick: (Listing) -> Unit
) : RecyclerView.Adapter<ListingAdapter.ListingViewHolder>() {

    class ListingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val address: TextView = itemView.findViewById(R.id.textAddress)
        val price: TextView = itemView.findViewById(R.id.textPrice)
        val availability: TextView = itemView.findViewById(R.id.textAvailability)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_listing, parent, false)
        return ListingViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        val listing = listings[position]
        holder.address.text = listing.address
        holder.price.text = "Price: \$${listing.pricePerHour}"
        holder.availability.text = "Available: ${listing.availability}"

        holder.itemView.setOnClickListener {
            onListingClick(listing)
        }
    }

    override fun getItemCount(): Int = listings.size

    /**
     * Replaces the adapter's data set with a new list and refreshes the RecyclerView.
     * This is called from the Fragment's Coroutine Scope after fetching data.


     * Uses DiffUtil for efficient updates.
     */
    fun updateListings(newListings: List<Listing>) {
        val diffCallback = ListingDiffCallback(listings, newListings)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.listings = newListings
        diffResult.dispatchUpdatesTo(this)
    }

    private class ListingDiffCallback(
        private val oldList: List<Listing>,
        private val newList: List<Listing>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}
