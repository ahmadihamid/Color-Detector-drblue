package com.doctorblue.colordetector.dialog

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.RelativeLayout
import android.widget.TextView
import com.doctorblue.colordetector.R
import com.doctorblue.colordetector.model.UserColor
import com.google.android.material.button.MaterialButton
import kotlin.math.sqrt

data class LabColor(val L: Int, val A: Int, val B: Int)
class ColorDetailDialog(
    context: Context,
    private val color: UserColor,
    private val onRemove: (UserColor) -> Unit
) : Dialog(context) {

    private lateinit var view_color_preview: View;
    private lateinit var txt_lab: TextView;
    private lateinit var txt_AE: TextView;
    private lateinit var txt_rgb: TextView;
    private lateinit var txt_hex: TextView;
    private lateinit var txt_hsl: TextView;
    private lateinit var btn_cancel: MaterialButton;
    private lateinit var btn_remove_color: MaterialButton;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_color_detail)
        setTitle(context.resources.getString(R.string.your_color))

        view_color_preview = findViewById(R.id.view_color_preview)
        txt_lab = findViewById(R.id.txt_lab)
        txt_AE = findViewById(R.id.txt_AE)
        txt_rgb = findViewById(R.id.txt_rgb)
        txt_hex = findViewById(R.id.txt_hex)
        txt_hsl = findViewById(R.id.txt_hsl)
        btn_cancel = findViewById(R.id.btn_cancel)
        btn_remove_color = findViewById(R.id.btn_remove_color)

        view_color_preview.setBackgroundColor(Color.parseColor(color.hex))
        val labColor = rgbToLab(color.r.toInt(), color.g.toInt(), color.b.toInt())
        val deltaEColor = deltaE(labColor, labColor);

        txt_lab.text = ("LAB Color: (${labColor.L},${labColor.A},${labColor.B})")
        txt_AE.text = ("ΔE: ${Math.round(deltaEColor * 100.0) / 100.0}")
        //txt_rgb.text = ("RGB: (${color.r}, ${color.g}, ${color.b})")
        //txt_hex.text = ("Hex : ${color.hex}")
        //txt_hsl.text = ("HSL: (${color.h}, ${color.s}, ${color.l})")

        btn_cancel.setOnClickListener { dismiss() }

        btn_remove_color.setOnClickListener {
            onRemove(color)
            dismiss()
        }

    }
    fun rgbToLab(r: Int, g: Int, b: Int): LabColor {
        val rLinear = r / 255.0
        val gLinear = g / 255.0
        val bLinear = b / 255.0

        val rSrgb = if (rLinear <= 0.04045) rLinear / 12.92 else Math.pow((rLinear + 0.055) / 1.055, 2.4)
        val gSrgb = if (gLinear <= 0.04045) gLinear / 12.92 else Math.pow((gLinear + 0.055) / 1.055, 2.4)
        val bSrgb = if (bLinear <= 0.04045) bLinear / 12.92 else Math.pow((bLinear + 0.055) / 1.055, 2.4)

        val x = rSrgb * 0.4124564 + gSrgb * 0.3575761 + bSrgb * 0.1804375
        val y = rSrgb * 0.2126729 + gSrgb * 0.7151522 + bSrgb * 0.0721750
        val z = rSrgb * 0.0193339 + gSrgb * 0.1191920 + bSrgb * 0.9503041

        val xR = x /  0.95047
        val yR = y /  1.00000
        val zR = z / 1.08883

        val fX = if (xR > 0.008856) Math.cbrt(xR) else (903.3 * xR + 16) / 116
        val fY = if (yR > 0.008856) Math.cbrt(yR) else (903.3 * yR + 16) / 116
        val fZ = if (zR > 0.008856) Math.cbrt(zR) else (903.3 * zR + 16) / 116

        val L = 116 * fY - 16
        val a = 500 * (fX - fY)
        val b = 200 * (fY - fZ)

        val roundedL = L.toInt()
        val roundedA = a.toInt()
        val roundedB = b.toInt()

        return LabColor(roundedL, roundedA, roundedB)
    }
    fun deltaE(labA: LabColor, labB: LabColor): Double {
        val labA = LabColor(84, -1, 1)
        val labB = rgbToLab(color.r.toInt(), color.g.toInt(), color.b.toInt())
        val deltaL = labA.L - labB.L
        val deltaA = labA.A - labB.A
        val deltaB = labA.B - labB.B
        val c1 = sqrt(labA.A.toDouble() * labA.A.toDouble() + labA.B.toDouble() * labA.B.toDouble())
        val c2 = sqrt(labB.A.toDouble() * labB.A.toDouble() + labB.B.toDouble() * labB.B.toDouble())
        val deltaC = c1 - c2
        var deltaH = deltaA * deltaA + deltaB * deltaB - deltaC * deltaC
        deltaH = if (deltaH < 0) 0.0 else sqrt(deltaH)
        val sc = 1.0 + 0.045 * c1
        val sh = 1.0 + 0.015 * c1
        val deltaLKlsl = deltaL / 1.0
        val deltaCkcsc = deltaC / sc
        val deltaHkhsh = deltaH / sh
        val i = deltaLKlsl * deltaLKlsl + deltaCkcsc * deltaCkcsc + deltaHkhsh * deltaHkhsh
        return if (i < 0) 0.0 else sqrt(i)
    }
}