package com.yourname.budgetbee.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.yourname.budgetbee.R
import com.yourname.budgetbee.data.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext



class ChartFragment : Fragment() {

    private lateinit var pieChart: PieChart

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pieChart = view.findViewById(R.id.pieChart)
        loadChartData()
    }

    private fun loadChartData() {
        val dao = AppDatabase.getDatabase(requireContext()).transactionDao()
        CoroutineScope(Dispatchers.IO).launch {
            val transactions = dao.getAllTransactionsOnce()
            val grouped = transactions.filter { it.type == "debit" }
                .groupBy { it.category }
                .mapValues { it.value.sumOf { tx -> tx.amount } }

            val entries = grouped.map { PieEntry(it.value.toFloat(), it.key) }

            val dataSet = PieDataSet(entries, "Expenses by Category").apply {
                valueTextSize = 14f
                valueTextColor = Color.WHITE
                colors = listOf(Color.CYAN, Color.MAGENTA, Color.GREEN, Color.RED)
            }

            withContext(Dispatchers.Main) {
                pieChart.data = PieData(dataSet)
                pieChart.description.isEnabled = false
                pieChart.centerText = "Expenses"
                pieChart.animateY(1000)
                pieChart.invalidate()
            }
        }
    }
}
