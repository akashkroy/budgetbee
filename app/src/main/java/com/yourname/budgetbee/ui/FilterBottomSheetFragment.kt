package com.yourname.budgetbee.ui

import android.app.Dialog
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import android.view.LayoutInflater
import android.view.View
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.yourname.budgetbee.R

class FilterBottomSheetFragment(
    private val availableCategories: List<String>,
    private val onFiltersApplied: (dateRange: String?, category: String?) -> Unit,
    private val onReset: () -> Unit
) : BottomSheetDialogFragment() {

    private lateinit var chipGroupDate: ChipGroup
    private lateinit var chipGroupCategory: ChipGroup
    private lateinit var buttonReset: Button
    private lateinit var buttonApply: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.bottom_sheet_filters, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        chipGroupDate = view.findViewById(R.id.chipGroupDate)
        chipGroupCategory = view.findViewById(R.id.chipGroupCategory)
        buttonReset = view.findViewById(R.id.buttonReset)
        buttonApply = view.findViewById(R.id.buttonApply)

        // Dynamically add category chips
        availableCategories.forEachIndexed { index, category ->
            val chip = Chip(requireContext(), null, com.google.android.material.R.style.Widget_MaterialComponents_Chip_Choice).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                text = category
                isCheckable = true
                isClickable = true
                isEnabled = true
                isCheckedIconVisible = false
                isChecked = index == 0  // "All" selected by default
            }
            chipGroupCategory.addView(chip)
        }


        buttonReset.setOnClickListener {
            chipGroupDate.clearCheck()
            chipGroupCategory.clearCheck()
            onReset()
            dismiss()
        }

        buttonApply.setOnClickListener {
            val selectedDateRange = chipGroupDate.findViewById<Chip>(chipGroupDate.checkedChipId)?.text?.toString()
            val selectedCategory = chipGroupCategory.findViewById<Chip>(chipGroupCategory.checkedChipId)?.text?.toString()

            onFiltersApplied(
                selectedDateRange?.takeIf { it != "All" },
                selectedCategory?.takeIf { it != "All" }
            )
            dismiss()
        }
    }
}
