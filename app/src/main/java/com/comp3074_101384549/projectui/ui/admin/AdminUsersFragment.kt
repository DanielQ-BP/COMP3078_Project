package com.comp3074_101384549.projectui.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.remote.ApiClient
import com.comp3074_101384549.projectui.data.remote.ApiService
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AdminUsersFragment : Fragment() {

    private lateinit var api: ApiService
    private lateinit var adapter: AdminUsersAdapter

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        api = ApiClient.api(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_admin_users, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerAdminUsers)
        val progress = view.findViewById<ProgressBar>(R.id.progressAdminUsers)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefreshAdminUsers)
        val empty = view.findViewById<TextView>(R.id.textAdminUsersEmpty)

        swipeRefresh.setColorSchemeResources(R.color.parkspot_green)

        adapter = AdminUsersAdapter(emptyList()) { row ->
            parentFragmentManager.beginTransaction()
                .replace(R.id.adminFragmentContainer, AdminUserDetailFragment.newInstance(row.id))
                .addToBackStack(null)
                .commit()
        }
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        fun load(fromSwipe: Boolean = false) {
            viewLifecycleOwner.lifecycleScope.launch {
                if (fromSwipe) {
                    swipeRefresh.isRefreshing = true
                } else {
                    progress.visibility = View.VISIBLE
                }
                try {
                    val list = api.adminGetUsers()
                    adapter.submit(list)
                    empty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
                    recycler.visibility = View.VISIBLE
                } catch (_: HttpException) {
                    Toast.makeText(requireContext(), "Could not load users", Toast.LENGTH_SHORT).show()
                    empty.visibility = View.VISIBLE
                    recycler.visibility = View.VISIBLE
                } catch (_: Exception) {
                    Toast.makeText(requireContext(), "Could not load users", Toast.LENGTH_SHORT).show()
                    empty.visibility = View.VISIBLE
                    recycler.visibility = View.VISIBLE
                } finally {
                    progress.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                }
            }
        }

        swipeRefresh.setOnRefreshListener { load(fromSwipe = true) }

        load(fromSwipe = false)
    }
}
