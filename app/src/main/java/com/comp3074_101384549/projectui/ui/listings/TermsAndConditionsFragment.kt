package com.comp3074_101384549.projectui.ui.listings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.comp3074_101384549.projectui.R

class TermsAndConditionsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_terms_and_conditions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.buttonAcceptTerms).setOnClickListener {
            // Pass acceptance result back to BecomeOwnerFragment
            parentFragmentManager.setFragmentResult(
                "terms_result",
                Bundle().apply { putBoolean("accepted", true) }
            )
            parentFragmentManager.popBackStack()
        }

        view.findViewById<Button>(R.id.buttonDeclineTerms).setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }
}