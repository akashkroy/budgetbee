package com.yourname.budgetbee.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.yourname.budgetbee.R
import com.yourname.budgetbee.data.models.Transaction

class TransactionAdapter(
    private val onTransactionLongClick: (Transaction) -> Unit
) : ListAdapter<Transaction, TransactionAdapter.TransactionViewHolder>(DiffCallback()) {

    inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textAmount: TextView = itemView.findViewById(R.id.textAmount)
        val textCategory: TextView = itemView.findViewById(R.id.textCategory)
        val textDate: TextView = itemView.findViewById(R.id.textDate)

        init {
            itemView.setOnLongClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onTransactionLongClick(getItem(pos))
                }
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transaction, parent, false)
        return TransactionViewHolder(view)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val transaction = getItem(position)

        holder.textAmount.text = "₹%.2f".format(transaction.amount)
        holder.textCategory.text = "${transaction.type.uppercase()} - ${transaction.category} / ${transaction.merchant}"
        holder.textDate.text = transaction.date

        // ✅ Apply color based on credit/debit
        val color = if (transaction.type.lowercase() == "credit") {
            android.graphics.Color.parseColor("#2E7D32") // Green
        } else {
            android.graphics.Color.parseColor("#C62828") // Red
        }
        holder.textAmount.setTextColor(color)
    }


    class DiffCallback : DiffUtil.ItemCallback<Transaction>() {
        override fun areItemsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Transaction, newItem: Transaction): Boolean {
            return oldItem == newItem
        }
    }
}
