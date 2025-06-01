package com.yourname.budgetbee.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.yourname.budgetbee.R
import com.yourname.budgetbee.data.models.Transaction
import java.text.SimpleDateFormat
import java.util.*

class AddOrEditTransactionBottomSheet(
    private val onSubmit: (Transaction) -> Unit,
    private val existingTransaction: Transaction? = null
) : BottomSheetDialogFragment() {

    private lateinit var inputAmount: TextInputEditText
    private lateinit var chipGroupType: ChipGroup
    private lateinit var inputCategory: AutoCompleteTextView
    private lateinit var inputMerchant: TextInputEditText
    private lateinit var textDate: TextView
    private lateinit var inputNotes: TextInputEditText
    private lateinit var buttonSubmit: Button

    private var selectedDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_add_transaction, container, false)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        inputAmount = view.findViewById(R.id.inputAmount)
        chipGroupType = view.findViewById(R.id.chipGroupType)
        inputCategory = view.findViewById(R.id.inputCategory)
        inputMerchant = view.findViewById(R.id.inputMerchant)
        textDate = view.findViewById(R.id.textDate)
        inputNotes = view.findViewById(R.id.inputNotes)
        buttonSubmit = view.findViewById(R.id.buttonSubmit)
        val textTitle = view.findViewById<TextView>(R.id.textTitle)

        // Prefill if editing
        if (existingTransaction != null) {
            textTitle.text = "Edit Transaction"
            buttonSubmit.text = "Update Transaction"
            inputAmount.setText(existingTransaction.amount.toString())
            inputCategory.setText(existingTransaction.category)
            inputMerchant.setText(existingTransaction.merchant)
            inputNotes.setText(existingTransaction.note)
            textDate.text = existingTransaction.date
            selectedDate = existingTransaction.date
            chipGroupType.check(
                if (existingTransaction.type == "credit") R.id.chipCredit else R.id.chipDebit
            )
        } else {
            textTitle.text = "Add Transaction"
            buttonSubmit.text = "Save Transaction"
            // Set default date only if not editing
            textDate.text = selectedDate
        }

        // Date picker
        textDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(requireContext(), { _, year, month, day ->
                val pickedDate = Calendar.getInstance().apply {
                    set(year, month, day)
                }
                selectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(pickedDate.time)
                textDate.text = selectedDate
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH))
            datePicker.show()
        }

        // Category suggestions
        val categories = listOf("Food", "Transport", "Groceries", "Utilities", "Shopping", "UPI", "Wallet", "Bank")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        inputCategory.setAdapter(adapter)

        buttonSubmit.setOnClickListener {
            val amountText = inputAmount.text.toString()
            val merchant = inputMerchant.text.toString().ifBlank { "Manual" }
            val category = inputCategory.text.toString().ifBlank { "Other" }
            val type = when (chipGroupType.checkedChipId) {
                R.id.chipCredit -> "credit"
                R.id.chipDebit -> "debit"
                else -> null
            }
            val amount = amountText.toDoubleOrNull()
            if (amount != null && type != null) {
                // For edit, keep the same id; for add, id will be 0 (auto-generated)
                val transaction = existingTransaction?.copy(
                    amount = amount,
                    type = type,
                    category = category,
                    merchant = merchant,
                    date = selectedDate,
                    note = inputNotes.text.toString()
                ) ?: Transaction(
                    amount = amount,
                    type = type,
                    category = category,
                    merchant = merchant,
                    date = selectedDate,
                    note = inputNotes.text.toString()
                )
                onSubmit(transaction)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter valid amount and type", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
