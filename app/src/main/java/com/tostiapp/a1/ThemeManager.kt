package com.tostiapp.a1

import android.view.View
import android.view.ViewGroup
import android.widget.TextView

object ThemeManager {

    fun applyTheme(view: View, color: Int, size: Float) {
        if (view is TextView) {
            if (color != -1) {
                view.setTextColor(color)
            }
            view.textSize = size
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyTheme(view.getChildAt(i), color, size)
            }
        }
    }
}