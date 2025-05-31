package com.yourname.budgetbee.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yourname.budgetbee.R
import com.yourname.budgetbee.data.models.MonthlySummary
import java.text.NumberFormat
import java.util.*

class SummaryAdapter(
    private val summaries: MutableList<MonthlySummary>
) : RecyclerView.Adapter<SummaryAdapter.SummaryViewHolder>() {

    class SummaryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val month: TextView = view.findViewById(R.id.textMonth)
        val income: TextView = view.findViewById(R.id.textIncome)
        val expense: TextView = view.findViewById(R.id.textExpense)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SummaryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_month_summary, parent, false)
        return SummaryViewHolder(view)
    }

    override fun getItemCount(): Int = summaries.size

    override fun onBindViewHolder(holder: SummaryViewHolder, position: Int) {
        val item = summaries[position]
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))

        holder.month.text = item.month
        holder.income.text = "Income: ${formatter.format(item.totalIncome)}"
        holder.expense.text = "Expense: ${formatter.format(item.totalExpense)}"
    }

    fun updateData(newList: List<MonthlySummary>) {
        summaries.clear()
        summaries.addAll(newList)
        notifyDataSetChanged()
    }
}
