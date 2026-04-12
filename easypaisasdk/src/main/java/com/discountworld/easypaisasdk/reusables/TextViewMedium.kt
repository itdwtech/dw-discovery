package com.discountworld.easypaisasdk.reusables

import com.discountworld.easypaisasdk.variables.Constants.Companion.FONT_MED
import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView
import com.discountworld.easypaisasdk.utils.TypeFaceUtils

class TextViewMedium : AppCompatTextView {

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
            typeface = TypeFaceUtils.get(context, FONT_MED)
        }catch (e: Exception) { }
    }
}