package com.doctorblue.colordetector.base

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.recyclerview.widget.RecyclerView
import com.doctorblue.colordetector.R
import com.google.android.material.card.MaterialCardView

abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId())

        initControls(savedInstanceState)
        initEvents()
    }

    abstract fun initControls(savedInstanceState: Bundle?)

    abstract fun initEvents()

    abstract fun getLayoutId(): Int
}