package com.comp3074_101384549.projectui.ui.admin

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.comp3074_101384549.projectui.BuildConfig
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.local.AuthPreferences
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.data.remote.AuthInterceptor
import com.comp3074_101384549.projectui.model.Ticket
import com.comp3074_101384549.projectui.model.TicketRespondRequest
import com.comp3074_101384549.projectui.model.UpdateTicketStatusRequest
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.*

class AdminDashboardFragment : Fragment() {

    private lateinit var apiService: ApiService
    private lateinit var adapter: AdminTicketAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        val prefs = AuthPreferences(context)
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(prefs))
            .build()
        apiService = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_admin_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = AdminTicketAdapter(
            onRespond = { ticket -> showRespondDialog(ticket) },
            onClose = { ticket -> closeTicket(ticket) }
        )

        view.findViewById<RecyclerView>(R.id.recyclerAdminTickets).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@AdminDashboardFragment.adapter
        }

        loadTickets(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadTickets(it) }
    }

    private fun loadTickets(view: View) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val tickets = apiService.getAllTickets()
                if (!isAdded) return@launch
                val empty = view.findViewById<LinearLayout>(R.id.emptyStateAdmin)
                val recycler = view.findViewById<RecyclerView>(R.id.recyclerAdminTickets)
                if (tickets.isEmpty()) {
                    empty.visibility = View.VISIBLE
                    recycler.visibility = View.GONE
                } else {
                    empty.visibility = View.GONE
                    recycler.visibility = View.VISIBLE
                    adapter.updateTickets(tickets)
                }
            } catch (e: Exception) {
                if (isAdded) Toast.makeText(requireContext(), "Failed to load tickets: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showRespondDialog(ticket: Ticket) {
        val input = EditText(requireContext()).apply {
            hint = "Type your response..."
            minLines = 3
            setPadding(48, 24, 48, 24)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Respond to: ${ticket.subject}")
            .setMessage("User: ${ticket.username ?: "Unknown"}\nStatus: ${ticket.status}")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val msg = input.text.toString().trim()
                if (msg.isEmpty()) {
                    Toast.makeText(requireContext(), "Response cannot be empty", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        apiService.respondToTicket(ticket.id, TicketRespondRequest(msg))
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Response sent", Toast.LENGTH_SHORT).show()
                            view?.let { loadTickets(it) }
                        }
                    } catch (e: Exception) {
                        if (isAdded) Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun closeTicket(ticket: Ticket) {
        AlertDialog.Builder(requireContext())
            .setTitle("Close Ticket")
            .setMessage("Close \"${ticket.subject}\"? This cannot be undone.")
            .setPositiveButton("Close Ticket") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        apiService.updateTicketStatus(ticket.id, UpdateTicketStatusRequest("closed"))
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Ticket closed", Toast.LENGTH_SHORT).show()
                            view?.let { loadTickets(it) }
                        }
                    } catch (e: Exception) {
                        if (isAdded) Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

class AdminTicketAdapter(
    private val onRespond: (Ticket) -> Unit,
    private val onClose: (Ticket) -> Unit
) : RecyclerView.Adapter<AdminTicketAdapter.VH>() {

    private var tickets: List<Ticket> = emptyList()

    class VH(view: View) : RecyclerView.ViewHolder(view) {
        val username: TextView = view.findViewById(R.id.textTicketUsername)
        val subject: TextView = view.findViewById(R.id.textTicketSubject)
        val description: TextView = view.findViewById(R.id.textTicketDescription)
        val date: TextView = view.findViewById(R.id.textTicketDate)
        val status: TextView = view.findViewById(R.id.textTicketStatus)
        val respond: Button = view.findViewById(R.id.buttonRespond)
        val close: Button = view.findViewById(R.id.buttonCloseTicket)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_admin_ticket, parent, false))

    override fun getItemCount() = tickets.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val ticket = tickets[position]
        holder.username.text = ticket.username ?: "Unknown user"
        holder.subject.text = ticket.subject
        holder.description.text = ticket.description
        holder.date.text = formatDate(ticket.createdAt)
        holder.status.text = ticket.status.replace("_", " ").replaceFirstChar { it.uppercase() }

        val (bgColor, textColor) = when (ticket.status) {
            "open" -> "#F57C00" to "#FFFFFF"
            "in_progress" -> "#1565C0" to "#FFFFFF"
            "resolved" -> "#1A5A2B" to "#FFFFFF"
            "closed" -> "#757575" to "#FFFFFF"
            else -> "#757575" to "#FFFFFF"
        }
        holder.status.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor(bgColor))
        holder.status.setTextColor(Color.parseColor(textColor))

        holder.close.visibility = if (ticket.status == "closed") View.GONE else View.VISIBLE
        holder.respond.visibility = if (ticket.status == "closed") View.GONE else View.VISIBLE

        holder.respond.setOnClickListener { onRespond(ticket) }
        holder.close.setOnClickListener { onClose(ticket) }
    }

    private fun formatDate(raw: String?): String {
        if (raw == null) return ""
        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
            val output = SimpleDateFormat("MMM d, yyyy", Locale.US)
            output.format(input.parse(raw) ?: return raw)
        } catch (e: Exception) { raw.take(10) }
    }

    fun updateTickets(newTickets: List<Ticket>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = tickets.size
            override fun getNewListSize() = newTickets.size
            override fun areItemsTheSame(o: Int, n: Int) = tickets[o].id == newTickets[n].id
            override fun areContentsTheSame(o: Int, n: Int) = tickets[o] == newTickets[n]
        })
        tickets = newTickets
        diff.dispatchUpdatesTo(this)
    }
}
