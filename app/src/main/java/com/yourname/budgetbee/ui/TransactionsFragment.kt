package com.yourname.budgetbee.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.yourname.budgetbee.R
import com.yourname.budgetbee.data.database.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.DayOfWeek
import androidx.recyclerview.widget.ItemTouchHelper
import com.google.android.material.snackbar.Snackbar

class TransactionsFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TransactionAdapter
    private lateinit var chipGroupType: ChipGroup
    private lateinit var chipMoreFilters: Chip

    private var selectedCategory: String? = null
    private var selectedDateRange: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_transactions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.recyclerViewTransactions)
        chipGroupType = view.findViewById(R.id.chipGroupType)
        chipMoreFilters = view.findViewById(R.id.chipMoreFilters)

        val dao = AppDatabase.getDatabase(requireContext()).transactionDao()


        adapter = TransactionAdapter { transaction ->
            AddOrEditTransactionBottomSheet(
                onSubmit = { updatedTransaction ->
                    lifecycleScope.launch {
                        dao.updateTransaction(updatedTransaction)
                    }
                },
                existingTransaction = transaction
            ).show(parentFragmentManager, "EditTransactionBottomSheet")
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val transaction = adapter.currentList[position]

                // Remove from DB
                lifecycleScope.launch {
                    dao.deleteTransaction(transaction)
                }

                Snackbar.make(recyclerView, "Transaction deleted", Snackbar.LENGTH_LONG)
                    .setAction("UNDO") {
                        // Re-insert on undo
                        lifecycleScope.launch {
                            dao.insertTransaction(transaction)
                        }
                    }.show()
            }
        })
        itemTouchHelper.attachToRecyclerView(recyclerView)

        val updateFilter = {
            val selectedType = when (chipGroupType.checkedChipId) {
                R.id.chipCredit -> "credit"
                R.id.chipDebit -> "debit"
                else -> null
            }

            val (startDate, endDate) = when (selectedDateRange) {
                "Today" -> {
                    val today = LocalDate.now()
                    today to today
                }
                "This week" -> {
                    val today = LocalDate.now()
                    val start = today.with(DayOfWeek.MONDAY)
                    val end = today.with(DayOfWeek.SUNDAY)
                    start to end
                }
                "This month" -> {
                    val today = LocalDate.now()
                    val start = today.withDayOfMonth(1)
                    val end = today.withDayOfMonth(today.lengthOfMonth())
                    start to end
                }
                else -> null to null
            }
            val sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val startDateStr = startDate?.format(sdf)
            val endDateStr = endDate?.format(sdf)

            lifecycleScope.launch {
                dao.getFilteredTransactions(selectedType, selectedCategory, startDateStr, endDateStr)
                    .collectLatest { adapter.submitList(it) }
            }
        }

        chipGroupType.setOnCheckedChangeListener { _, _ ->
            updateFilter()
        }

        chipMoreFilters.setOnClickListener {
            lifecycleScope.launch {
                val categories = dao.getAllCategories()

                val bottomSheet = FilterBottomSheetFragment(
                    availableCategories = listOf("All") + categories,
                    onFiltersApplied = { date, category ->
                        selectedDateRange = date
                        selectedCategory = category
                        updateFilter()
                        recyclerView.scrollToPosition(0)
                    },
                    onReset = {
                        chipGroupType.check(R.id.chipAllType)
                        selectedCategory = null
                        selectedDateRange = null
                        updateFilter()
                        recyclerView.scrollToPosition(0)
                    }
                )
                bottomSheet.show(parentFragmentManager, "FilterBottomSheet")
            }
        }

        updateFilter() // Initial load
    }
}
