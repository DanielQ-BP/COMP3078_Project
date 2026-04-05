package com.comp3074_101384549.projectui.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.comp3074_101384549.projectui.R
import com.comp3074_101384549.projectui.data.remote.ApiClient
import com.comp3074_101384549.projectui.data.remote.ApiService
import com.comp3074_101384549.projectui.model.AdminBookingDetailResponse
import com.comp3074_101384549.projectui.model.AdminResolveConflictRequest
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import retrofit2.HttpException

class AdminBookingDetailFragment : Fragment() {

    private lateinit var api: ApiService
    private lateinit var rootView: View
    private var bookingId: String = ""

    companion object {
        fun newInstance(bookingId: String) = AdminBookingDetailFragment().apply {
            arguments = Bundle().apply { putString("bookingId", bookingId) }
        }
    }

    override fun onAttach(context: android.content.Context) {
        super.onAttach(context)
        api = ApiClient.api(context)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_admin_booking_detail, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rootView = view
        bookingId = arguments?.getString("bookingId") ?: run {
            parentFragmentManager.popBackStack()
            return
        }

        view.findViewById<Button>(R.id.buttonBackBookingDetail).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        loadDetail()
    }

    private fun loadDetail() {
        val title = rootView.findViewById<TextView>(R.id.textBookingDetailTitle)
        val body = rootView.findViewById<TextView>(R.id.textBookingDetailBody)
        val active = rootView.findViewById<TextView>(R.id.textActiveConflicts)
        val actions = rootView.findViewById<LinearLayout>(R.id.layoutConflictActions)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val d = api.adminGetBookingDetail(bookingId)
                bindDetail(d, title, body, active, actions)
            } catch (_: HttpException) {
                Toast.makeText(requireContext(), "Could not load details", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Could not load details", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
        }
    }

    private fun bindDetail(
        d: AdminBookingDetailResponse,
        title: TextView,
        body: TextView,
        active: TextView,
        actions: LinearLayout,
    ) {
        val b = d.booking
        title.text = "Reservation ${b.referenceCode ?: b.id.take(8)}"

        val sb = StringBuilder()
        sb.appendLine("Code: ${b.referenceCode ?: "—"}")
        sb.appendLine("Address: ${b.listingAddress}")
        sb.appendLine("When: ${b.startTime} → ${b.endTime}")
        sb.appendLine("Total: \$${String.format("%.2f", b.totalPrice)}  Status: ${b.status}")
        if (!b.disputeResolutionOutcome.isNullOrBlank()) {
            sb.appendLine("Dispute outcome: ${b.disputeResolutionOutcome}  (${b.disputeResolvedAt ?: "—"})")
        }
        sb.appendLine()
        sb.appendLine("— Driver —")
        sb.appendLine("${d.driver.username} (${d.driver.email ?: "no email"})")
        sb.appendLine("Disputes involving this user (history): ${d.driver.previousConflictCount}")
        d.driver.conflicts.forEach { c ->
            sb.appendLine("• ${c.createdAt}: ${c.subject}")
            sb.appendLine("  Ref: ${c.bookingReferenceCode ?: "—"}  Cause: ${c.cause.take(120)}${if (c.cause.length > 120) "…" else ""}")
        }
        sb.appendLine()
        sb.appendLine("— Owner —")
        sb.appendLine("${d.owner.username} (${d.owner.email ?: "no email"})")
        sb.appendLine("Disputes involving this user (history): ${d.owner.previousConflictCount}")
        d.owner.conflicts.forEach { c ->
            sb.appendLine("• ${c.createdAt}: ${c.subject}")
            sb.appendLine("  Ref: ${c.bookingReferenceCode ?: "—"}  Cause: ${c.cause.take(120)}${if (c.cause.length > 120) "…" else ""}")
        }
        body.text = sb.toString()

        if (d.activeConflictTickets.isEmpty()) {
            active.text = "None"
            actions.removeAllViews()
            actions.visibility = View.GONE
        } else {
            val asb = StringBuilder()
            d.activeConflictTickets.forEach { t ->
                asb.appendLine("• ${t.subject} [${t.status}] (id ${t.id.take(8)}…)")
                asb.appendLine("  ${t.description}")
                asb.appendLine()
            }
            active.text = asb.toString().trim()

            actions.removeAllViews()
            actions.visibility = View.VISIBLE
            d.activeConflictTickets.forEach { t ->
                val btn = MaterialButton(requireContext(), null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                    ).apply { bottomMargin = (8 * resources.displayMetrics.density).toInt() }
                    text = getString(R.string.admin_resolve_dispute_button, t.subject.take(36))
                    setOnClickListener { showResolveDisputeDialog(t.id) }
                }
                actions.addView(btn)
            }
        }
    }

    private fun showResolveDisputeDialog(ticketId: String) {
        val labels = arrayOf(
            getString(R.string.admin_outcome_cancelled),
            getString(R.string.admin_outcome_completed),
            getString(R.string.admin_outcome_unchanged),
        )
        val outcomes = arrayOf("cancelled", "completed", "unchanged")
        var selected = 0

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.admin_resolve_dispute_title)
            .setSingleChoiceItems(labels, 0) { _, which -> selected = which }
            .setPositiveButton(R.string.admin_resolve_confirm) { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        api.adminResolveConflict(
                            ticketId,
                            AdminResolveConflictRequest(bookingOutcome = outcomes[selected]),
                        )
                        Toast.makeText(requireContext(), R.string.admin_resolve_success, Toast.LENGTH_SHORT).show()
                        loadDetail()
                    } catch (_: HttpException) {
                        Toast.makeText(requireContext(), R.string.admin_resolve_failed, Toast.LENGTH_SHORT).show()
                    } catch (_: Exception) {
                        Toast.makeText(requireContext(), R.string.admin_resolve_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
