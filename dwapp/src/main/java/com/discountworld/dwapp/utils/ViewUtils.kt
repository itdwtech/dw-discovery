package com.discountworld.dwapp.utils

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View

@SuppressLint("ClickableViewAccessibility")
fun View.makeDraggable() {
    var dX = 0f
    var dY = 0f
    var initialX = 0f
    var initialY = 0f
    var isDragging = false
    val clickThreshold = 10f

    this.setOnTouchListener { view, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dX = view.x - event.rawX
                dY = view.y - event.rawY
                initialX = event.rawX
                initialY = event.rawY
                isDragging = false
                view.isPressed = true
                true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.rawX - initialX
                val dy = event.rawY - initialY
                if (Math.abs(dx) > clickThreshold || Math.abs(dy) > clickThreshold) {
                    isDragging = true
                    view.isPressed = false
                }

                if (isDragging) {
                    val parentWidth = (view.parent as? View)?.width ?: view.width
                    val parentHeight = (view.parent as? View)?.height ?: view.height

                    var newX = event.rawX + dX
                    var newY = event.rawY + dY

                    if (parentWidth > 0 && parentHeight > 0) {
                        newX = Math.max(0f, Math.min(newX, parentWidth - view.width.toFloat()))
                        newY = Math.max(0f, Math.min(newY, parentHeight - view.height.toFloat()))
                    }

                    view.x = newX
                    view.y = newY
                }
                true
            }
            MotionEvent.ACTION_UP -> {
                view.isPressed = false
                if (!isDragging) {
                    view.performClick()
                }
                true
            }
            MotionEvent.ACTION_CANCEL -> {
                view.isPressed = false
                true
            }
            else -> false
        }
    }
}
