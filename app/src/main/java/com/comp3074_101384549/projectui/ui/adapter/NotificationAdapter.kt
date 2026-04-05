package com.comp3074_101384549.projectui.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.model.Notification
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class NotificationAdapter(
    private var notifications: List<Notification>,
    private val onMarkRead: (Notification) -> Unit,
    private val onDelete: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val unreadDot: View = view.findViewById(R.id.unreadDot)
        val title: TextView = view.findViewById(R.id.notificationTitle)
        val message: TextView = view.findViewById(R.id.notificationMessage)
        val time: TextView = view.findViewById(R.id.notificationTime)
        val deleteBtn: ImageButton = view.findViewById(R.id.btnDeleteNotification)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val notification = notifications[position]

        holder.title.text = notification.title
        holder.message.text = notification.message
        holder.time.text = formatDate(notification.createdAt)
        holder.unreadDot.visibility = if (!notification.isRead) View.VISIBLE else View.INVISIBLE

        holder.itemView.setOnClickListener {
            if (!notification.isRead) onMarkRead(notification)
        }
        holder.deleteBtn.setOnClickListener {
            onDelete(notification)
        }
    }

    override fun getItemCount() = notifications.size

    fun updateNotifications(newList: List<Notification>) {
        notifications = newList
        notifyDataSetChanged()
    }

    private fun formatDate(createdAt: String?): String {
        if (createdAt.isNullOrEmpty()) return ""
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            inputFormat.timeZone = TimeZone.getTimeZone("UTC")
            val date = inputFormat.parse(createdAt) ?: return createdAt
            val outputFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
            outputFormat.format(date)
        } catch (e: Exception) {
            createdAt.take(10)
        }
    }
}
