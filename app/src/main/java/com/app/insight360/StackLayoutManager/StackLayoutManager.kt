package com.app.insight360.StackLayoutManager

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE

class StackLayoutManager(
    scrollOrientation: ScrollOrientation,
    visibleCount: Int,
    animation: Class<out StackAnimation>,
    layout: Class<out StackLayout>
) : RecyclerView.LayoutManager() {
    private enum class FlingOrientation { NONE, LEFT_TO_RIGHT, RIGHT_TO_LEFT, TOP_TO_BOTTOM, BOTTOM_TO_TOP }

    enum class ScrollOrientation { LEFT_TO_RIGHT, RIGHT_TO_LEFT, TOP_TO_BOTTOM, BOTTOM_TO_TOP }

    private var mVisibleItemCount = visibleCount

    private var mScrollOrientation = scrollOrientation

    private var mScrollOffset: Int

    private lateinit var mOnScrollListener: RecyclerView.OnScrollListener
    private lateinit var mOnFlingListener: RecyclerView.OnFlingListener

    private var mAnimation: StackAnimation? = null

    private var mLayout: StackLayout? = null

    private var mPagerMode = true

    private var mPagerFlingVelocity = 0

    private var mFixScrolling = false

    private var mFlingOrientation = FlingOrientation.NONE

    private var itemPosition = 0

    private var isItemPositionChanged = false

    private var itemChangedListener: ItemChangedListener? = null

    interface ItemChangedListener {
        fun onItemChanged(position: Int)
    }

    fun setPagerMode(isPagerMode: Boolean) {
        mPagerMode = isPagerMode
    }

    fun getPagerMode(): Boolean {
        return mPagerMode
    }

    fun setPagerFlingVelocity(velocity: Int) {
        // Perform the range check manually
        mPagerFlingVelocity = Math.min(Int.MAX_VALUE, Math.max(0, velocity))
    }

    fun getPagerFlingVelocity(): Int {
        return mPagerFlingVelocity
    }

    fun setVisibleItemCount(count: Int) {
        // Manually enforce the range check
        mVisibleItemCount = Math.min(itemCount - 1, Math.max(1, count))
        mAnimation?.setVisibleCount(mVisibleItemCount)
    }

    fun getVisibleItemCount(): Int {
        return mVisibleItemCount
    }

    fun setItemOffset(offset: Int) {
        mLayout?.setItemOffset(offset)
    }

    fun getItemOffset(): Int {
        return if (mLayout == null) {
            0
        } else {
            mLayout!!.getItemOffset()
        }
    }

    fun setAnimation(animation: StackAnimation) {
        mAnimation = animation
    }

    fun getAnimation(): StackAnimation? {
        return mAnimation
    }

    fun getScrollOrientation(): ScrollOrientation {
        return mScrollOrientation
    }

    fun getFirstVisibleItemPosition(): Int {
        if (width == 0 || height == 0) {
            return 0
        }
        return when (mScrollOrientation) {
            ScrollOrientation.RIGHT_TO_LEFT -> Math.floor((mScrollOffset * 1.0 / width)).toInt()
            ScrollOrientation.LEFT_TO_RIGHT -> itemCount - 1 - Math.ceil((mScrollOffset * 1.0 / width))
                .toInt()

            ScrollOrientation.BOTTOM_TO_TOP -> Math.floor((mScrollOffset * 1.0 / height)).toInt()
            ScrollOrientation.TOP_TO_BOTTOM -> itemCount - 1 - Math.ceil((mScrollOffset * 1.0 / height))
                .toInt()
        }
    }

    fun setItemChangedListener(listener: ItemChangedListener) {
        itemChangedListener = listener
    }

    constructor(scrollOrientation: ScrollOrientation) : this(
        scrollOrientation,
        3,
        DefaultAnimation::class.java,
        DefaultLayout::class.java
    )

    constructor(scrollOrientation: ScrollOrientation, visibleCount: Int) : this(
        scrollOrientation,
        visibleCount,
        DefaultAnimation::class.java,
        DefaultLayout::class.java
    )

    constructor() : this(ScrollOrientation.RIGHT_TO_LEFT)

    init {
        mScrollOffset = when (mScrollOrientation) {
            ScrollOrientation.RIGHT_TO_LEFT, ScrollOrientation.BOTTOM_TO_TOP -> 0
            else -> Int.MAX_VALUE
        }

        if (StackAnimation::class.java.isAssignableFrom(animation)) {
            try {
                val cla = animation.getDeclaredConstructor(
                    ScrollOrientation::class.java,
                    Int::class.javaPrimitiveType
                )
                mAnimation = cla.newInstance(scrollOrientation, visibleCount) as StackAnimation
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (StackLayout::class.java.isAssignableFrom(layout)) {
            try {
                val cla = layout.getDeclaredConstructor(
                    ScrollOrientation::class.java,
                    Int::class.javaPrimitiveType,
                    Int::class.javaPrimitiveType
                )
                mLayout = cla.newInstance(scrollOrientation, visibleCount, 30) as StackLayout
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onAttachedToWindow(view: RecyclerView) {
        super.onAttachedToWindow(view)
        mOnFlingListener = object : RecyclerView.OnFlingListener() {
            override fun onFling(velocityX: Int, velocityY: Int): Boolean {
                if (mPagerMode) {
                    when (mScrollOrientation) {
                        ScrollOrientation.RIGHT_TO_LEFT, ScrollOrientation.LEFT_TO_RIGHT -> {
                            mFlingOrientation = when {
                                velocityX > mPagerFlingVelocity -> FlingOrientation.RIGHT_TO_LEFT
                                velocityX < -mPagerFlingVelocity -> FlingOrientation.LEFT_TO_RIGHT
                                else -> FlingOrientation.NONE
                            }
                            if (mScrollOffset in 1 until width * (itemCount - 1)) { //边界不需要滚动
                                mFixScrolling = true
                            }
                        }

                        else -> {
                            mFlingOrientation = when {
                                velocityY > mPagerFlingVelocity -> FlingOrientation.BOTTOM_TO_TOP
                                velocityY < -mPagerFlingVelocity -> FlingOrientation.TOP_TO_BOTTOM
                                else -> FlingOrientation.NONE
                            }
                            if (mScrollOffset in 1 until width * (itemCount - 1)) { //边界不需要滚动
                                mFixScrolling = true
                            }
                        }
                    }
                    calculateAndScrollToTarget(view)
                }
                return mPagerMode
            }
        }
        view.onFlingListener = mOnFlingListener

        mOnScrollListener = object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                if (newState == SCROLL_STATE_IDLE) {
                    if (!mFixScrolling) {
                        mFixScrolling = true
                        calculateAndScrollToTarget(view)
                    } else {
                        //表示此次 IDLE 是由修正位置结束触发的
                        mFixScrolling = false
                    }
                } else if (newState == SCROLL_STATE_DRAGGING) {
                    mFixScrolling = false
                }
            }
        }
        view.addOnScrollListener(mOnScrollListener)
    }

    override fun onDetachedFromWindow(view: RecyclerView?, recycler: RecyclerView.Recycler?) {
        super.onDetachedFromWindow(view, recycler)
        if (view?.onFlingListener == mOnFlingListener) {
            view.onFlingListener = null
        }
        view?.removeOnScrollListener(mOnScrollListener)
    }

    override fun canScrollHorizontally(): Boolean {
        if (itemCount == 0) {
            return false
        }
        return when (mScrollOrientation) {
            ScrollOrientation.LEFT_TO_RIGHT, ScrollOrientation.RIGHT_TO_LEFT -> true
            else -> false
        }
    }

    override fun canScrollVertically(): Boolean {
        if (itemCount == 0) {
            return false
        }
        return when (mScrollOrientation) {
            ScrollOrientation.TOP_TO_BOTTOM, ScrollOrientation.BOTTOM_TO_TOP -> true
            else -> false
        }
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {

        mLayout?.requestLayout()

        removeAndRecycleAllViews(recycler)

        if (itemCount > 0) {
            mScrollOffset = getValidOffset(mScrollOffset)
            loadItemView(recycler)
        }
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State
    ): Int {
        return handleScrollBy(dx, recycler)
    }

    override fun scrollVerticallyBy(
        dy: Int,
        recycler: RecyclerView.Recycler,
        state: RecyclerView.State?
    ): Int {
        return handleScrollBy(dy, recycler)
    }

    override fun scrollToPosition(position: Int) {
        if (position < 0 || position >= itemCount) {
            throw ArrayIndexOutOfBoundsException("$position is out of bound [0..$itemCount-1]")
        }
        mScrollOffset = getPositionOffset(position)
        requestLayout()
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State?,
        position: Int
    ) {
        if (position < 0 || position >= itemCount) {
            throw ArrayIndexOutOfBoundsException("$position is out of bound [0..$itemCount-1]")
        }
        mFixScrolling = true
        scrollToCenter(position, recyclerView, true)
    }

    private fun updatePositionRecordAndNotify(position: Int) {
        if (itemChangedListener == null) {
            return
        }
        if (position != itemPosition) {
            isItemPositionChanged = true
            itemPosition = position
            itemChangedListener?.onItemChanged(itemPosition)
        } else {
            isItemPositionChanged = false
        }
    }

    private fun handleScrollBy(offset: Int, recycler: RecyclerView.Recycler): Int {

        val expectOffset = mScrollOffset + offset

        mScrollOffset = getValidOffset(expectOffset)

        val exactMove = mScrollOffset - expectOffset + offset

        if (exactMove == 0) {
            return 0
        }

        detachAndScrapAttachedViews(recycler)

        loadItemView(recycler)
        return exactMove
    }

    private fun loadItemView(recycler: RecyclerView.Recycler) {
        val firstVisiblePosition = getFirstVisibleItemPosition()
        val lastVisiblePosition = getLastVisibleItemPosition()

        val movePercent = getFirstVisibleItemMovePercent()

        for (i in lastVisiblePosition downTo firstVisiblePosition) {
            val view = recycler.getViewForPosition(i)

            addView(view)

            measureChild(view, 0, 0)

            mLayout?.doLayout(this, mScrollOffset, movePercent, view, i - firstVisiblePosition)

            mAnimation?.doAnimation(movePercent, view, i - firstVisiblePosition)
        }

        updatePositionRecordAndNotify(firstVisiblePosition)

        if (firstVisiblePosition - 1 >= 0) {
            val view = recycler.getViewForPosition(firstVisiblePosition - 1)
            resetViewAnimateProperty(view)
            removeAndRecycleView(view, recycler)
        }
        if (lastVisiblePosition + 1 < itemCount) {
            val view = recycler.getViewForPosition(lastVisiblePosition + 1)
            resetViewAnimateProperty(view)
            removeAndRecycleView(view, recycler)
        }
    }

    private fun resetViewAnimateProperty(view: View) {
        view.rotationY = 0f
        view.rotationX = 0f
        view.scaleX = 1f
        view.scaleY = 1f
        view.alpha = 1f
    }

    private fun calculateAndScrollToTarget(view: RecyclerView) {
        val targetPosition = calculateCenterPosition(getFirstVisibleItemPosition())
        scrollToCenter(targetPosition, view, true)
    }

    private fun scrollToCenter(
        targetPosition: Int,
        recyclerView: RecyclerView,
        animation: Boolean
    ) {
        val targetOffset = getPositionOffset(targetPosition)
        when (mScrollOrientation) {
            ScrollOrientation.LEFT_TO_RIGHT, ScrollOrientation.RIGHT_TO_LEFT -> {
                if (animation) {
                    recyclerView.smoothScrollBy(targetOffset - mScrollOffset, 0)
                } else {
                    recyclerView.scrollBy(targetOffset - mScrollOffset, 0)
                }
            }

            else -> {
                if (animation) {
                    recyclerView.smoothScrollBy(0, targetOffset - mScrollOffset)
                } else {
                    recyclerView.scrollBy(0, targetOffset - mScrollOffset)
                }
            }
        }
    }

    private fun getValidOffset(expectOffset: Int): Int {
        return when (mScrollOrientation) {
            ScrollOrientation.RIGHT_TO_LEFT, ScrollOrientation.LEFT_TO_RIGHT -> Math.max(
                Math.min(
                    width * (itemCount - 1),
                    expectOffset
                ), 0
            )

            else -> Math.max(Math.min(height * (itemCount - 1), expectOffset), 0)
        }
    }

    private fun getPositionOffset(position: Int): Int {
        return when (mScrollOrientation) {
            ScrollOrientation.RIGHT_TO_LEFT -> position * width
            ScrollOrientation.LEFT_TO_RIGHT -> (itemCount - 1 - position) * width
            ScrollOrientation.BOTTOM_TO_TOP -> position * height
            ScrollOrientation.TOP_TO_BOTTOM -> (itemCount - 1 - position) * height
        }
    }

    private fun getLastVisibleItemPosition(): Int {
        val firstVisiblePosition = getFirstVisibleItemPosition()
        return if (firstVisiblePosition + mVisibleItemCount > itemCount - 1) {
            itemCount - 1
        } else {
            firstVisiblePosition + mVisibleItemCount
        }
    }

    private fun getFirstVisibleItemMovePercent(): Float {
        if (width == 0 || height == 0) {
            return 0f
        }
        return when (mScrollOrientation) {
            ScrollOrientation.RIGHT_TO_LEFT -> (mScrollOffset % width) * 1.0f / width
            ScrollOrientation.LEFT_TO_RIGHT -> {
                val targetPercent = 1 - (mScrollOffset % width) * 1.0f / width
                return if (targetPercent == 1f) {
                    0f
                } else {
                    targetPercent
                }
            }

            ScrollOrientation.BOTTOM_TO_TOP -> (mScrollOffset % height) * 1.0f / height
            ScrollOrientation.TOP_TO_BOTTOM -> {
                val targetPercent = 1 - (mScrollOffset % height) * 1.0f / height
                return if (targetPercent == 1f) {
                    0f
                } else {
                    targetPercent
                }
            }
        }
    }

    private fun calculateCenterPosition(position: Int): Int {

        val triggerOrientation = mFlingOrientation
        mFlingOrientation = FlingOrientation.NONE
        when (mScrollOrientation) {
            ScrollOrientation.RIGHT_TO_LEFT -> {
                if (triggerOrientation == FlingOrientation.RIGHT_TO_LEFT) {
                    return position + 1
                } else if (triggerOrientation == FlingOrientation.LEFT_TO_RIGHT) {
                    return position
                }
            }

            ScrollOrientation.LEFT_TO_RIGHT -> {
                if (triggerOrientation == FlingOrientation.LEFT_TO_RIGHT) {
                    return position + 1
                } else if (triggerOrientation == FlingOrientation.RIGHT_TO_LEFT) {
                    return position
                }
            }

            ScrollOrientation.BOTTOM_TO_TOP -> {
                if (triggerOrientation == FlingOrientation.BOTTOM_TO_TOP) {
                    return position + 1
                } else if (triggerOrientation == FlingOrientation.TOP_TO_BOTTOM) {
                    return position
                }
            }

            ScrollOrientation.TOP_TO_BOTTOM -> {
                if (triggerOrientation == FlingOrientation.TOP_TO_BOTTOM) {
                    return position + 1
                } else if (triggerOrientation == FlingOrientation.BOTTOM_TO_TOP) {
                    return position
                }
            }
        }

        val percent = getFirstVisibleItemMovePercent()

        return if (percent < 0.5) {
            position
        } else {
            position + 1
        }
    }
}
