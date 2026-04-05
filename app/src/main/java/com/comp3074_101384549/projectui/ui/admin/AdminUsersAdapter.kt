package com.comp3074_101384549.projectui.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.model.AdminUserRow

class AdminUsersAdapter(
    private var items: List<AdminUserRow>,
    private val onClick: (AdminUserRow) -> Unit,
) : RecyclerView.Adapter<AdminUsersAdapter.VH>() {

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.textUsername)
        val email: TextView = view.findViewById(R.id.textEmail)
        val conflictCount: TextView = view.findViewById(R.id.textConflictCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_user, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val row = items[position]
        holder.username.text = row.username + if (row.role == "admin") " (admin)" else ""
        holder.email.text = row.email ?: "—"
        holder.conflictCount.text =
            if (row.conflictCount > 0) "Past reservation disputes: ${row.conflictCount}" else "No dispute history"
        holder.itemView.setOnClickListener { onClick(row) }
    }

    override fun getItemCount() = items.size

    fun submit(list: List<AdminUserRow>) {
        items = list
        notifyDataSetChanged()
    }
}
