package com.comp3074_101384549.projectui.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.remote.ApiClient
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AdminBookingsFragment : Fragment() {

    private lateinit var api: ApiService
    private lateinit var adapter: AdminBookingsAdapter
    private var searchJob: Job? = null

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        api = ApiClient.api(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_admin_bookings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerAdminBookings)
        val progress = view.findViewById<ProgressBar>(R.id.progressAdminBookings)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshAdminBookings)
        val empty = view.findViewById<TextView>(R.id.textAdminBookingsEmpty)
        val searchEdit = view.findViewById<TextInputEditText>(R.id.editSearchReservations)

        swipeRefresh.setColorSchemeResources(R.color.parkspot_green)

        adapter = AdminBookingsAdapter(emptyList()) { row ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.adminFragmentContainer, AdminBookingDetailFragment.newInstance(row.id))
                .addToBackStack(null)
                .commit()
        }
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        fun setEmpty(visible: Boolean) {
            empty.visibility = if (visible) View.VISIBLE else View.GONE
            recycler.visibility = View.VISIBLE
        }

        fun load(query: String, fromSwipe: Boolean = false) {
            viewLifecycleOwner.lifecycleScope.launch {
                if (fromSwipe) {
                    swipeRefresh.isRefreshing = true
                } else {
                    progress.visibility = View.VISIBLE
                }
                try {
                    val list = if (query.isBlank()) {
                        api.adminGetBookings()
                    } else {
                        api.adminSearchBookings(query)
                    }
                    adapter.submit(list)
                    setEmpty(list.isEmpty())
                } catch (_: HttpException) {
                    Toast.makeText(requireContext(), "Could not load reservations", Toast.LENGTH_SHORT).show()
                    setEmpty(true)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), e.message ?: "Error", Toast.LENGTH_SHORT).show()
                    setEmpty(true)
                } finally {
                    progress.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                }
            }
        }

        swipeRefresh.setOnRefreshListener {
            load(searchEdit.text?.toString()?.trim() ?: "", fromSwipe = true)
        }

        searchEdit.doAfterTextChanged { text ->
            searchJob?.cancel()
            searchJob = viewLifecycleOwner.lifecycleScope.launch {
                delay(450)
                load(text?.toString()?.trim() ?: "")
            }
        }

        searchEdit.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                load(searchEdit.text?.toString()?.trim() ?: "")
                true
            } else false
        }

        load("")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
    }
}
