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
import com.github.mikephil.charting.formatter.PercentFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.yourname.budgetbee.R
import com.yourname.budgetbee.data.database.AppDatabase
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.widget.TextView

class ChartFragment : Fragment() {

    private lateinit var pieChart: PieChart
    private lateinit var chipGroupChartType: ChipGroup
    private lateinit var chipGroupChartDate: ChipGroup
    private var selectedType: String? = null
    private var selectedDateRange: String? = null // "today", "this_week", etc.

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        pieChart = view.findViewById(R.id.pieChart)
        chipGroupChartType = view.findViewById(R.id.chipGroupChartType)
        chipGroupChartDate = view.findViewById(R.id.chipGroupChartDate)

        chipGroupChartType.setOnCheckedChangeListener { _, checkedId ->
            selectedType = when (checkedId) {
                R.id.chipCredit -> "credit"
                R.id.chipDebit -> "debit"
                else -> null // All
            }
            loadChartData()
        }

        chipGroupChartDate.setOnCheckedChangeListener { _, checkedId ->
            selectedDateRange = when (checkedId) {
                R.id.chipToday -> "today"
                R.id.chipThisWeek -> "this_week"
                R.id.chipThisMonth -> "this_month"
                R.id.chipPrevMonth -> "prev_month"
                R.id.chipThisYear -> "this_year"
                else -> null // All
            }
            loadChartData()
        }

        // Initial chart load
        loadChartData()
    }

    private fun loadChartData() {
        val dao = AppDatabase.getDatabase(requireContext()).transactionDao()
        CoroutineScope(Dispatchers.IO).launch {
            val transactions = dao.getAllTransactionsOnce()
            val sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.getDefault())
            val today = LocalDate.now()

            val (startDate, endDate) = when (selectedDateRange) {
                "today" -> today to today
                "this_week" -> {
                    val start = today.with(java.time.DayOfWeek.MONDAY)
                    val end = today.with(java.time.DayOfWeek.SUNDAY)
                    start to end
                }
                "this_month" -> {
                    val start = today.withDayOfMonth(1)
                    val end = today.withDayOfMonth(today.lengthOfMonth())
                    start to end
                }
                "prev_month" -> {
                    val prevMonth = today.minusMonths(1)
                    val start = prevMonth.withDayOfMonth(1)
                    val end = prevMonth.withDayOfMonth(prevMonth.lengthOfMonth())
                    start to end
                }
                "this_year" -> {
                    val start = today.withDayOfYear(1)
                    val end = today.withDayOfYear(today.lengthOfYear())
                    start to end
                }
                else -> null to null
            }

            val filteredTx = transactions
                .filter { selectedType == null || it.type == selectedType }
                .filter {
                    if (startDate != null && endDate != null) {
                        val date = LocalDate.parse(it.date, sdf)
                        !date.isBefore(startDate) && !date.isAfter(endDate)
                    } else true
                }

            val grouped = filteredTx.groupBy { it.category }
                .mapValues { it.value.sumOf { tx -> tx.amount } }

            val entries = grouped.map { PieEntry(it.value.toFloat(), it.key) }

            val pastelColors = listOf(
                Color.parseColor("#4FC3F7"),
                Color.parseColor("#FFB74D"),
                Color.parseColor("#81C784"),
                Color.parseColor("#FF8A65"),
                Color.parseColor("#9575CD"),
                Color.parseColor("#AED581"),
                Color.parseColor("#FFD54F"),
                Color.parseColor("#E57373"),
                Color.parseColor("#BA68C8"),
                Color.parseColor("#4DD0E1")
            )

            val dataSet = PieDataSet(entries, "By Category").apply {
                valueTextSize = 14f
                valueTextColor = Color.WHITE
                colors = pastelColors
                valueFormatter = PercentFormatter(pieChart)
            }
            dataSet.valueFormatter = object : ValueFormatter() {
                override fun getPieLabel(value: Float, pieEntry: PieEntry?): String {
                    // value = percent; entry.value = actual amount
                    val actualValue = pieEntry?.value ?: 0f
                    return "₹%.0f (%.1f%%)".format(actualValue, value)
                }
            }

            withContext(Dispatchers.Main) {
                if (entries.isEmpty()) {
                    pieChart.clear()
                    pieChart.centerText = "No data"
                } else {
                    pieChart.data = PieData(dataSet)
                    pieChart.description.isEnabled = false
                    pieChart.centerText = when (selectedType) {
                        "credit" -> "Credit Breakdown"
                        "debit" -> "Expense Breakdown"
                        else -> "All Transactions"
                    }
                    pieChart.legend.isWordWrapEnabled = true
                    pieChart.legend.textSize = 14f
                    pieChart.setUsePercentValues(true)
                    pieChart.animateY(1000)
                    pieChart.invalidate()

                    val textSummary = requireView().findViewById<TextView>(R.id.textSummary)
                    textSummary.text = grouped.entries.joinToString("\n") { (cat, amt) ->
                        "$cat: ₹%.2f".format(amt)
                    }
                }
            }
        }
    }
}
