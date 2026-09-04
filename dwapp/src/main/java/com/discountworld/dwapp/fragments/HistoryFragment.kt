package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.discountworld.dwapp.adapters.HistoryAdapter
import com.discountworld.dwapp.databinding.FragmentHistoryBinding
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.repositories.RedemptionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val redemptionRepository = RedemptionRepository()
    private lateinit var sessionManager: SessionManager
    private val historyAdapter = HistoryAdapter()
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sessionManager = SessionManager(requireContext())

        setupRecyclerView()
        setupSearch()
        loadHistory()
    }

    private fun setupRecyclerView() {
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = historyAdapter
    }

    private fun setupSearch() {
        binding.ivSearch.setOnClickListener {
            val query = binding.etSearch.text?.toString()?.trim()
            loadHistory(query)
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if ((actionId == EditorInfo.IME_ACTION_SEARCH) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                val query = binding.etSearch.text?.toString()?.trim()
                loadHistory(query)
                true
            } else {
                false
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(400)
                    val query = s?.toString()?.trim()
                    loadHistory(query)
                }
            }
        })
    }

    private fun loadHistory(searchQuery: String? = null) {
        viewLifecycleOwner.lifecycleScope.launch {
            binding.progressBar.visibility = View.VISIBLE
            binding.tvNoData.visibility = View.GONE

            val query = if (searchQuery.isNullOrEmpty()) null else searchQuery
            Log.d("HistoryFragment", "Loading history with query: $query, token: ${sessionManager.getAuthToken()}")

            val response = redemptionRepository.listCustomerRedemptions(search = query)

            binding.progressBar.visibility = View.GONE

            if (response == null) {
                Log.e("HistoryFragment", "Response is null!")
            } else {
                Log.d("HistoryFragment", "Full response object: ${response.toString()}")
            }

            val items = response?.itemsList ?: emptyList()
            val totalCount = response?.totalCount ?: items.size.toLong()

            Log.d("HistoryFragment", "History loaded: ${items.size} items, totalCount: $totalCount")

            binding.tvCount.text = totalCount.toString()

            if (items.isNotEmpty()) {
                binding.rvHistory.visibility = View.VISIBLE
                binding.tvNoData.visibility = View.GONE
                historyAdapter.updateData(items)
            } else {
                binding.rvHistory.visibility = View.GONE
                binding.tvNoData.visibility = View.VISIBLE
                historyAdapter.updateData(emptyList())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}
