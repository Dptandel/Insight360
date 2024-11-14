package com.app.insight360.StackLayoutManager

import android.view.View

abstract class StackAnimation(
    scrollOrientation: StackLayoutManager.ScrollOrientation,
    visibleCount: Int
) {

    protected val mScrollOrientation = scrollOrientation
    protected var mVisibleCount = visibleCount

    internal fun setVisibleCount(visibleCount: Int) {
        mVisibleCount = visibleCount
    }

    abstract fun doAnimation(firstMovePercent: Float, itemView: View, position: Int)
}