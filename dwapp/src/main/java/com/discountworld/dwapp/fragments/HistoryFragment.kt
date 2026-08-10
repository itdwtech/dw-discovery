package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.discountworld.dwapp.R
import com.discountworld.dwapp.adapters.HistoryAdapter
import com.discountworld.dwapp.databinding.FragmentHistoryBinding
import com.discountworld.dwapp.models.HistoryItem

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        val dummyData = listOf(
            HistoryItem(R.drawable.ic_allurebeauty, "Deal 1 - Mr Chinese Food", "12345678", "2026-08-04 13:11:56"),
            HistoryItem(R.drawable.ic_allurebeauty, "Deal 1 - Mr Chinese Food", "12345678", "2026-08-04 13:11:56"),
            HistoryItem(R.drawable.ic_allurebeauty, "Deal 1 - Mr Chinese Food", "12345678", "2026-08-04 13:11:56"),
            HistoryItem(R.drawable.ic_allurebeauty, "Deal 1 - Mr Chinese Food", "12345678", "2026-08-04 13:11:56"),
            HistoryItem(R.drawable.ic_allurebeauty, "Deal 1 - Mr Chinese Food", "12345678", "2026-08-04 13:11:56"),
            HistoryItem(R.drawable.ic_allurebeauty, "Deal 1 - Mr Chinese Food", "12345678", "2026-08-04 13:11:56")
        )
        binding.rvHistory.adapter = HistoryAdapter(dummyData)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
