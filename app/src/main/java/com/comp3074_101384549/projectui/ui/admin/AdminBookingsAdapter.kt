package com.comp3074_101384549.projectui.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.model.AdminBookingRow

class AdminBookingsAdapter(
    private var items: List<AdminBookingRow>,
    private val onClick: (AdminBookingRow) -> Unit,
) : RecyclerView.Adapter<AdminBookingsAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val conflictBadge: TextView = view.findViewById(R.id.textConflictBadge)
        val refCode: TextView = view.findViewById(R.id.textRefCode)
        val address: TextView = view.findViewById(R.id.textAddress)
        val driverOwner: TextView = view.findViewById(R.id.textDriverOwner)
        val times: TextView = view.findViewById(R.id.textTimes)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_booking, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        holder.refCode.text = row.referenceCode?.ifEmpty { "—" } ?: "—"
        holder.address.text = row.listingAddress
        holder.driverOwner.text = "Driver: ${row.driverUsername}  •  Owner: ${row.ownerUsername}"
        holder.times.text = "${row.startTime} → ${row.endTime}  •  ${row.status}  •  \$${String.format("%.2f", row.totalPrice)}"
        holder.conflictBadge.visibility = if (row.hasActiveConflict) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onClick(row) }
    }

    override fun getItemCount() = items.size

    fun submit(list: List<AdminBookingRow>) {
        items = list
        notifyDataSetChanged()
    }
}
