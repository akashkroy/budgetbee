package com.yourname.budgetbee.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.yourname.budgetbee.R
import com.yourname.budgetbee.data.database.AppDatabase
import com.yourname.budgetbee.data.models.MonthlySummary
import com.yourname.budgetbee.util.SmsParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*


class SummaryFragment : Fragment() {

    private lateinit var adapter: SummaryAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var fab: ExtendedFloatingActionButton

    private val dao by lazy {
        AppDatabase.getDatabase(requireContext()).transactionDao()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_summary, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        adapter = SummaryAdapter(mutableListOf())
        recyclerView = view.findViewById(R.id.recyclerViewSummary)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        progressBar = view.findViewById(R.id.progressBar)

        val refreshButton = view.findViewById<Button>(R.id.btnRefreshSMS)
        refreshButton.setOnClickListener {
            lifecycleScope.launch {
                progressBar.visibility = View.VISIBLE
                withContext(Dispatchers.IO) { readSMS() }
                progressBar.visibility = View.GONE
            }
        }

        fab = view.findViewById(R.id.fabAddTransaction)
        fab.setOnClickListener {
            AddOrEditTransactionBottomSheet(
                onSubmit = { transaction ->
                    lifecycleScope.launch {
                        dao.insertTransaction(transaction)
                    }
                },
                existingTransaction = null
            ).show(parentFragmentManager, "AddTransactionBottomSheet")
        }


        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS),
                101
            )
        } else {
            lifecycleScope.launch {
                progressBar.visibility = View.VISIBLE
                withContext(Dispatchers.IO) { readSMS() }
                progressBar.visibility = View.GONE
            }
        }

        observeTransactions()
    }

    private fun observeTransactions() {
        lifecycleScope.launch {
            dao.getAllTransactions().collectLatest { transactions ->
                val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                val grouped = transactions.groupBy {
                    monthFormat.format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(it.date) ?: Date())
                }

                val summaryList = grouped.map { (month, items) ->
                    val income = items.filter { it.type == "credit" }.sumOf { it.amount }
                    val expense = items.filter { it.type == "debit" }.sumOf { it.amount }
                    val parsedDate = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).parse(month) ?: Date()
                    Pair(MonthlySummary(month, income, expense), parsedDate)
                }.sortedByDescending { it.second }
                    .map { it.first }

                adapter.updateData(summaryList)
            }
        }
    }

    private suspend fun readSMS() {
        val prefs = requireContext().getSharedPreferences("budgetbee_prefs", Context.MODE_PRIVATE)
        val lastProcessed = prefs.getLong("last_sms_timestamp", 0L)
        var latestTimestamp = lastProcessed

        val cursor = requireContext().contentResolver.query(
            Uri.parse("content://sms/inbox"),
            null,
            "date > ?",
            arrayOf(lastProcessed.toString()),
            "date ASC"
        )

        withContext(Dispatchers.Main) {
            progressBar.visibility = View.VISIBLE
            progressBar.progress = 0
        }

        cursor?.use {
            val total = it.count.coerceAtLeast(1)
            var count = 0

            while (it.moveToNext()) {
                val address = it.getString(it.getColumnIndexOrThrow("address"))
                val body = it.getString(it.getColumnIndexOrThrow("body"))
                val timestamp = it.getLong(it.getColumnIndexOrThrow("date"))

                if (timestamp > latestTimestamp) latestTimestamp = timestamp

                val transaction = SmsParser.parse(body, address, timestamp)
                if (transaction != null) {
                    dao.insertTransaction(transaction)
                }

                count++
                val percent = (count * 100) / total
                withContext(Dispatchers.Main) {
                    progressBar.progress = percent
                }
            }
            prefs.edit().putLong("last_sms_timestamp", latestTimestamp).apply()
        }

        withContext(Dispatchers.Main) {
            progressBar.visibility = View.GONE
            Toast.makeText(requireContext(), "SMS processed ✅", Toast.LENGTH_SHORT).show()
        }
    }
}
