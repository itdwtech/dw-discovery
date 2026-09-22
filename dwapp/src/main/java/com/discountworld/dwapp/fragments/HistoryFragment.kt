package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.discountworld.dwapp.adapters.HistoryAdapter
import com.discountworld.dwapp.databinding.FragmentHistoryBinding
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.viewmodels.HistoryUiState
import com.discountworld.dwapp.viewmodels.HistoryViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
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
        observeViewModel()
        viewModel.loadHistory()
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is HistoryUiState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.tvNoData.visibility = View.GONE
                }
                is HistoryUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvCount.text = state.totalCount.toString()

                    if (state.history.isNotEmpty()) {
                        binding.rvHistory.visibility = View.VISIBLE
                        binding.tvNoData.visibility = View.GONE
                        historyAdapter.updateData(state.history)
                    } else {
                        binding.rvHistory.visibility = View.GONE
                        binding.tvNoData.visibility = View.VISIBLE
                        historyAdapter.updateData(emptyList())
                    }
                }
                is HistoryUiState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.rvHistory.visibility = View.GONE
                    binding.tvNoData.visibility = View.VISIBLE
                    historyAdapter.updateData(emptyList())
                }
                HistoryUiState.Idle -> {}
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvHistory.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.adapter = historyAdapter
    }

    private fun setupSearch() {
        binding.ivSearch.setOnClickListener {
            val query = binding.etSearch.text?.toString()?.trim()
            viewModel.loadHistory(query)
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if ((actionId == EditorInfo.IME_ACTION_SEARCH) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                val query = binding.etSearch.text?.toString()?.trim()
                viewModel.loadHistory(query)
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
                    viewModel.loadHistory(query)
                }
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        _binding = null
    }
}
