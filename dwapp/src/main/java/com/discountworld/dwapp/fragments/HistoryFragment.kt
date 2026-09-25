package com.discountworld.dwapp.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.discountworld.dwapp.adapters.HistoryAdapter
import com.discountworld.dwapp.databinding.FragmentHistoryBinding
import com.discountworld.dwapp.managers.SessionManager
import com.discountworld.dwapp.viewmodels.HistoryUiState
import com.discountworld.dwapp.viewmodels.HistoryViewModel

class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HistoryViewModel by viewModels()
    private lateinit var sessionManager: SessionManager
    private val historyAdapter = HistoryAdapter()

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
                is HistoryUiState.LoadingMore -> {
                    // Keep existing items visible while loading next page
                }
                is HistoryUiState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.tvCount.text = state.totalCount.toString()

                    if (state.history.isNotEmpty()) {
                        binding.rvHistory.visibility = View.VISIBLE
                        binding.tvNoData.visibility = View.GONE
                        historyAdapter.updateData(state.history)

                        binding.rvHistory.post {
                            if (_binding != null && !binding.rvHistory.canScrollVertically(1) && !state.isLastPage) {
                                viewModel.loadNextPage()
                            }
                        }
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
        val layoutManager = LinearLayoutManager(requireContext())
        binding.rvHistory.layoutManager = layoutManager
        binding.rvHistory.adapter = historyAdapter

        binding.rvHistory.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!recyclerView.canScrollVertically(1) && newState != RecyclerView.SCROLL_STATE_IDLE) {
                    viewModel.loadNextPage()
                }
            }

            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                if (lastVisibleItemPosition >= totalItemCount - 2) {
                    viewModel.loadNextPage()
                }
            }
        })
    }

    private fun setupSearch() {
        val performSearch = {
            val query = binding.etSearch.text?.toString()?.trim()
            val imm = requireContext().getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
            imm?.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
            viewModel.loadHistory(query?.ifEmpty { null })
        }

        binding.ivSearch.setOnClickListener {
            performSearch()
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if ((actionId == EditorInfo.IME_ACTION_SEARCH) || (actionId == EditorInfo.IME_ACTION_DONE)) {
                performSearch()
                true
            } else {
                false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
