package com.comp3074_101384549.projectui.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.remote.ApiClient
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AdminUserDetailFragment : Fragment() {

    companion object {
        fun newInstance(userId: String) = AdminUserDetailFragment().apply {
            arguments = Bundle().apply { putString("userId", userId) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_admin_user_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userId = arguments?.getString("userId") ?: return
        val api = ApiClient.api(requireContext())
        val title = view.findViewById<TextView>(R.id.textUserDetailTitle)
        val body = view.findViewById<TextView>(R.id.textUserDetailBody)

        view.findViewById<Button>(R.id.buttonBackUserDetail).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val d = api.adminGetUserDetail(userId)
                val u = d.user
                title.text = u.username
                val sb = StringBuilder()
                sb.appendLine("Email: ${u.email ?: "—"}")
                sb.appendLine("Role: ${u.role}")
                u.createdAt?.let { sb.appendLine("Joined: $it") }
                sb.appendLine()
                sb.appendLine("Reservation disputes involving this user: ${d.previousConflictCount}")
                sb.appendLine()
                d.conflicts.forEach { c ->
                    sb.appendLine("• ${c.createdAt}  [${c.status}]  Ref: ${c.bookingReferenceCode ?: "—"}")
                    sb.appendLine("  ${c.subject}")
                    sb.appendLine("  Cause: ${c.cause}")
                    sb.appendLine()
                }
                if (d.conflicts.isEmpty()) {
                    sb.appendLine("No dispute tickets on file.")
                }
                body.text = sb.toString().trim()
            } catch (_: HttpException) {
                Toast.makeText(requireContext(), "Could not load user", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Could not load user", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }
}
