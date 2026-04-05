package com.comp3074_101384549.projectui.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.model.BookingEntity

class BookingAdapter(
    private var bookings: List<BookingEntity>,
    private val onCancelClick: (BookingEntity) -> Unit
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val status: TextView = itemView.findViewById(R.id.textStatus)
        val address: TextView = itemView.findViewById(R.id.textAddress)
        val reservationCode: TextView = itemView.findViewById(R.id.textReservationCode)
        val date: TextView = itemView.findViewById(R.id.textDate)
        val time: TextView = itemView.findViewById(R.id.textTime)
        val total: TextView = itemView.findViewById(R.id.textTotal)
        val cancelButton: Button = itemView.findViewById(R.id.buttonCancel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]

        holder.address.text = booking.address
        if (booking.referenceCode.isNotBlank()) {
            holder.reservationCode.visibility = View.VISIBLE
            holder.reservationCode.text = "Reservation code: ${booking.referenceCode}"
        } else {
            holder.reservationCode.visibility = View.GONE
        }
        holder.date.text = booking.bookingDate
        holder.time.text = "${booking.startTime} - ${booking.endTime}"
        holder.total.text = "$${String.format("%.2f", booking.totalPrice)}"

        // Set status with appropriate color
        holder.status.text = booking.status.replaceFirstChar { it.uppercase() }
        when (booking.status) {
            "confirmed" -> {
                holder.status.setBackgroundColor(Color.parseColor("#1A5A2B")) // Green
                holder.cancelButton.visibility = View.VISIBLE
            }
            "completed" -> {
                holder.status.setBackgroundColor(Color.parseColor("#757575")) // Gray
                holder.cancelButton.visibility = View.GONE
            }
            "cancelled" -> {
                holder.status.setBackgroundColor(Color.parseColor("#D32F2F")) // Red
                holder.cancelButton.visibility = View.GONE
            }
            "overstay" -> {
                holder.status.setBackgroundColor(Color.parseColor("#E65100")) // Orange
                holder.cancelButton.visibility = View.VISIBLE
            }
            else -> {
                holder.status.setBackgroundColor(Color.parseColor("#1A5A2B"))
                holder.cancelButton.visibility = View.VISIBLE
            }
        }

        holder.cancelButton.setOnClickListener {
            onCancelClick(booking)
        }
    }

    override fun getItemCount(): Int = bookings.size

    fun updateBookings(newBookings: List<BookingEntity>) {
        val diffCallback = object : DiffUtil.Callback() {
            override fun getOldListSize() = bookings.size
            override fun getNewListSize() = newBookings.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                bookings[oldPos].id == newBookings[newPos].id
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                bookings[oldPos] == newBookings[newPos]
        }
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.bookings = newBookings
        diffResult.dispatchUpdatesTo(this)
    }
}
