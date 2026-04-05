package com.comp3074_101384549.projectui.ui.notifications

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.comp3074_101384549.projectui.BuildConfig
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.data.remote.AuthInterceptor
import com.comp3074_101384549.projectui.model.Notification
import com.comp3074_101384549.projectui.ui.adapter.NotificationAdapter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NotificationsFragment : Fragment() {

    private lateinit var apiService: ApiService
    private lateinit var authPreferences: AuthPreferences
    private lateinit var notificationAdapter: NotificationAdapter
    private var recyclerView: RecyclerView? = null
    private var emptyState: LinearLayout? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        authPreferences = AuthPreferences(context)
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(authPreferences))
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_notifications, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.recyclerViewNotifications)
        emptyState = view.findViewById(R.id.emptyState)

        recyclerView?.layoutManager = LinearLayoutManager(requireContext())
        notificationAdapter = NotificationAdapter(
            notifications = emptyList(),
            onMarkRead = { notification -> markAsRead(notification) },
            onDelete = { notification -> deleteNotification(notification) }
        )
        recyclerView?.adapter = notificationAdapter

        loadNotifications()
    }

    override fun onResume() {
        super.onResume()
        loadNotifications()
    }

    private fun loadNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (!isAdded) return@launch
                val userId = authPreferences.userId.first() ?: return@launch
                val notifications = apiService.getUserNotifications(userId)
                if (isAdded) {
                    notificationAdapter.updateNotifications(notifications)
                    showEmptyState(notifications.isEmpty())
                }
            } catch (e: Exception) {
                if (isAdded) showEmptyState(true)
            }
        }
    }

    private fun markAsRead(notification: Notification) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                apiService.markNotificationRead(notification.id)
                loadNotifications()
            } catch (e: Exception) {
                // silently ignore
            }
        }
    }

    private fun deleteNotification(notification: Notification) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                apiService.deleteNotification(notification.id)
                loadNotifications()
            } catch (e: Exception) {
                if (isAdded) Toast.makeText(requireContext(), "Could not delete notification", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEmptyState(show: Boolean) {
        emptyState?.visibility = if (show) View.VISIBLE else View.GONE
        recyclerView?.visibility = if (show) View.GONE else View.VISIBLE
    }
}
