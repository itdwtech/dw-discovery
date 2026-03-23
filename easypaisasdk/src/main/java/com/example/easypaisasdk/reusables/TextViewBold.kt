package com.example.easypaisasdk.reusables

import Constants.Companion.FONT_BOLD
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.example.easypaisasdk.utils.TypeFaceUtils

class TextViewBold : AppCompatTextView {

    constructor(context: Context?) : super(
        context!!
    ) { init(context) }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) { init(context) }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!, attrs, defStyle
    ) { init(context) }


    private fun init(context: Context){
        try{
            typeface = TypeFaceUtils.get(context, FONT_BOLD)
        }catch (e: Exception) { }
    }
}