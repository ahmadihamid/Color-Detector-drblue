package com.doctorblue.colordetector.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.doctorblue.colordetector.R
import com.doctorblue.colordetector.adapter.ColorAdapter
import com.doctorblue.colordetector.database.ColorViewModel
import com.google.android.material.button.MaterialButton

class ColorDialog(
    context: Context,
    private val colorViewModel: ColorViewModel,
    private val colorAdapter: ColorAdapter,
    private val onClearColor: () -> Unit
) : Dialog(context) {

    private val rv_color: RecyclerView = findViewById(R.id.rv_color)
    private val btn_add_color: MaterialButton = findViewById(R.id.btn_add_color)
    private val btn_cancel: MaterialButton = findViewById(R.id.btn_cancel)
    private val edt_name_of_list: EditText = findViewById(R.id.edt_name_of_list)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_color)
        setContentView(R.layout.activity_main)

        val layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        rv_color.layoutManager = layoutManager
        rv_color.setHasFixedSize(true)
        rv_color.adapter = colorAdapter

        btn_add_color.setOnClickListener {
            val name = edt_name_of_list.text.toString()

            if (name.isNotEmpty()) {
                colorAdapter.colors.forEach {
                    it.name = name
                    colorViewModel.insertColor(it)
                }
                //colorViewModel.insertAllColor(colorAdapter.colors)
                onClearColor()
                dismiss()
            }
        }

        btn_cancel.setOnClickListener {
            dismiss()
        }
    }
}