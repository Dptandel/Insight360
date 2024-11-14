package com.app.insight360.StackLayoutManager

import android.view.View

abstract class StackLayout(
    scrollOrientation: StackLayoutManager.ScrollOrientation,
    visibleCount: Int,
    perItemOffset: Int
) {

    protected val mScrollOrientation = scrollOrientation
    protected var mVisibleCount = visibleCount
    protected var mPerItemOffset = perItemOffset

    internal fun setItemOffset(offset: Int) {
        mPerItemOffset = offset
    }

    internal fun getItemOffset(): Int {
        return mPerItemOffset
    }

    abstract fun doLayout(
        stackLayoutManager: StackLayoutManager,
        scrollOffset: Int,
        firstMovePercent: Float,
        itemView: View,
        position: Int
    )

    abstract fun requestLayout()
}