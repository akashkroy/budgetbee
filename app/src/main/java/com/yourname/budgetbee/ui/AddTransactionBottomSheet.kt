package com.yourname.budgetbee.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.yourname.budgetbee.R
import com.yourname.budgetbee.data.models.Transaction
import java.text.SimpleDateFormat
import java.util.*

class AddTransactionBottomSheet(
    private val onSubmit: (Transaction) -> Unit
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

        // Set default date
        textDate.text = selectedDate

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

        // Category suggestions (you can replace this list)
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
                val transaction = Transaction(
                    amount = amount,
                    type = type,
                    category = category,
                    merchant = merchant,
                    date = selectedDate
                )
                onSubmit(transaction)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Please enter valid amount and type", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
