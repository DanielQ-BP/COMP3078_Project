package com.comp3074_101384549.projectui.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.model.Listing

class MyListingAdapter(
    private var listings: List<Listing>,
    private val onEditClick: (Listing) -> Unit,
    private val onDeleteClick: (Listing) -> Unit
) : RecyclerView.Adapter<MyListingAdapter.MyListingViewHolder>() {

    class MyListingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val address: TextView = itemView.findViewById(R.id.textAddress)
        val price: TextView = itemView.findViewById(R.id.textPrice)
        val availability: TextView = itemView.findViewById(R.id.textAvailability)
        val editButton: Button = itemView.findViewById(R.id.buttonEdit)
        val deleteButton: Button = itemView.findViewById(R.id.buttonDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyListingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_my_listing, parent, false)
        return MyListingViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyListingViewHolder, position: Int) {
        val listing = listings[position]
        holder.address.text = listing.address
        holder.price.text = "$${listing.pricePerHour}/hour"
        holder.availability.text = listing.availability

        holder.editButton.setOnClickListener {
            onEditClick(listing)
        }

        holder.deleteButton.setOnClickListener {
            onDeleteClick(listing)
        }
    }

    override fun getItemCount(): Int = listings.size

    fun updateListings(newListings: List<Listing>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = listings.size
            override fun getNewListSize() = newListings.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                listings[oldPos].id == newListings[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                listings[oldPos] == newListings[newPos]
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.listings = newListings
        diffResult.dispatchUpdatesTo(this)
    }
}
