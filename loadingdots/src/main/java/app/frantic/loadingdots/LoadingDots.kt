package app.frantic.loadingdots

import android.view.animation.Animation
import android.support.v4.view.ViewCompat.setTranslationY
import android.animation.ValueAnimator
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.content.res.TypedArray
import android.os.Build
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout


class LoadingDots : LinearLayout {

    private var mDots: MutableList<View>? = null
    private var mAnimation: ValueAnimator? = null
    private var mIsAttachedToWindow: Boolean = false

    // Setters and getters

    /**
     * Set AutoPlay to true to play the loading animation automatically when view is attached and visible.
     * xml: LoadingDots_auto_play
     * @param autoPlay
     */
    var autoPlay: Boolean = false

    // Dots appearance attributes
    private var mDotsColor: Int = 0
    private var mDotsCount: Int = 0
    private var mDotSize: Int = 0
    private var mDotSpace: Int = 0

    // Animation time attributes
    private var mLoopDuration: Int = 0
    private var mLoopStartDelay: Int = 0

    // Animation behavior attributes
    var jumpDuration: Int = 0
        private set
    private var mJumpHeight: Int = 0

    // Cached Calculations
    private var mJumpHalfTime: Int = 0
    private var mDotsStartTime: IntArray? = null
    private var mDotsJumpUpEndTime: IntArray? = null
    private var mDotsJumpDownEndTime: IntArray? = null

    /**
     * Set the color to be used for the dots fill color
     * xml: LoadingDots_dots_color
     * @param color resolved color value
     */
    var dotsColor: Int
        get() = mDotsColor
        set(color) {
            verifyNotRunning()
            mDotsColor = color
        }

    /**
     * Set the number of dots
     * xml: LoadingDots_dots_count
     * @param count dots count
     */
    var dotsCount: Int
        get() = mDotsCount
        set(count) {
            verifyNotRunning()
            mDotsCount = count
        }

    /**
     * Set the dots size
     * xml: LoadingDots_dots_size
     * @param size size in pixels
     */
    var dotsSize: Int
        get() = mDotSize
        set(size) {
            verifyNotRunning()
            mDotSize = size
        }

    /**
     * Set the space between dots
     * xml: LoadingDots_dots_space
     * @param space space in pixels
     */
    var dotsSpace: Int
        get() = mDotSpace
        set(space) {
            verifyNotRunning()
            mDotSpace = space
        }

    /**
     * Set the loop duration. This is the duration for the entire animation loop (including start delay)
     * xml: LoadingDots_loop_duration
     * @param duration duration in milliseconds
     */
    var loopDuration: Int
        get() = mLoopDuration
        set(duration) {
            verifyNotRunning()
            mLoopDuration = duration
        }

    /**
     * Set the loop start delay. Each loop will delay the animation by the given value.
     * xml: LoadingDots_loop_start_delay
     * @param startDelay delay duration in milliseconds
     */
    var loopStartDelay: Int
        get() = mLoopStartDelay
        set(startDelay) {
            verifyNotRunning()
            mLoopStartDelay = startDelay
        }

    /**
     * Set the jump height of the dots. The entire view will include this height to allow the dots
     * animation to draw properly. The entire view height will be DotsSize + JumpHeight.
     * xml: LoadingDots_jump_height
     * @param height size in pixels
     */
    var jumpHeight: Int
        get() = mJumpHeight
        set(height) {
            verifyNotRunning()
            mJumpHeight = height
        }

    constructor(context: Context) : super(context) {
        init(null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(
        context,
        attrs,
        defStyleAttr,
        defStyleRes
    ) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        val context = context
        val resources = context.resources

        val a = context.obtainStyledAttributes(attrs, R.styleable.LoadingDots)

        autoPlay = a.getBoolean(R.styleable.LoadingDots_LoadingDots_auto_play, true)

        mDotsColor = a.getColor(R.styleable.LoadingDots_LoadingDots_dots_color, Color.GRAY)
        mDotsCount = a.getInt(R.styleable.LoadingDots_LoadingDots_dots_count, DEFAULT_DOTS_COUNT)
        mDotSize = a.getDimensionPixelSize(
            R.styleable.LoadingDots_LoadingDots_dots_size,
            resources.getDimensionPixelSize(R.dimen.LoadingDots_dots_size_default)
        )
        mDotSpace = a.getDimensionPixelSize(
            R.styleable.LoadingDots_LoadingDots_dots_space,
            resources.getDimensionPixelSize(R.dimen.LoadingDots_dots_space_default)
        )

        mLoopDuration = a.getInt(R.styleable.LoadingDots_LoadingDots_loop_duration, DEFAULT_LOOP_DURATION)
        mLoopStartDelay = a.getInt(R.styleable.LoadingDots_LoadingDots_loop_start_delay, DEFAULT_LOOP_START_DELAY)

        jumpDuration = a.getInt(R.styleable.LoadingDots_LoadingDots_jump_duration, DEFAULT_JUMP_DURATION)
        mJumpHeight = a.getDimensionPixelSize(
            R.styleable.LoadingDots_LoadingDots_jump_height,
            resources.getDimensionPixelSize(R.dimen.LoadingDots_jump_height_default)
        )

        a.recycle()

        // Setup LinerLayout
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.BOTTOM

        calculateCachedValues()
        initializeDots(context)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // We allow the height to save space for the jump height
        setMeasuredDimension(measuredWidth, measuredHeight + mJumpHeight)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        mIsAttachedToWindow = true

        createAnimationIfAutoPlay()
        if (mAnimation != null && visibility == View.VISIBLE) {
            mAnimation!!.start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        mIsAttachedToWindow = false
        if (mAnimation != null) {
            mAnimation!!.end()
        }
    }

    override fun setVisibility(visibility: Int) {
        super.setVisibility(visibility)
        when (visibility) {
            View.VISIBLE -> {
                createAnimationIfAutoPlay()
                startAnimationIfAttached()
            }
            View.INVISIBLE, View.GONE -> if (mAnimation != null) {
                mAnimation!!.end()
            }
        }
    }

    private fun createDotView(context: Context): View {
        val dot = ImageView(context)
        dot.setImageResource(R.drawable.loading_dots_dot)
        (dot.getDrawable() as GradientDrawable).setColor(mDotsColor)
        return dot
    }

    private fun startAnimationIfAttached() {
        if (mIsAttachedToWindow && !mAnimation!!.isRunning) {
            mAnimation!!.start()
        }
    }

    private fun createAnimationIfAutoPlay() {
        if (autoPlay) {
            createAnimation()
        }
    }

    private fun createAnimation() {
        if (mAnimation != null) {
            // We already have an animation
            return
        }
        calculateCachedValues()
        initializeDots(context)

        mAnimation = ValueAnimator.ofInt(0, mLoopDuration)
        mAnimation!!.addUpdateListener(ValueAnimator.AnimatorUpdateListener { valueAnimator ->
            val dotsCount = mDots!!.size
            val from = 0

            val animationValue = valueAnimator.animatedValue as Int

            if (animationValue < mLoopStartDelay) {
                // Do nothing
                return@AnimatorUpdateListener
            }

            for (i in 0 until dotsCount) {
                val dot = mDots!![i]

                val dotStartTime = mDotsStartTime!![i]

                val animationFactor: Float
                if (animationValue < dotStartTime) {
                    // No animation is needed for this dot yet
                    animationFactor = 0f
                } else if (animationValue < mDotsJumpUpEndTime!![i]) {
                    // Animate jump up
                    animationFactor = (animationValue - dotStartTime).toFloat() / mJumpHalfTime
                } else if (animationValue < mDotsJumpDownEndTime!![i]) {
                    // Animate jump down
                    animationFactor = 1 - (animationValue - dotStartTime - mJumpHalfTime).toFloat() / mJumpHalfTime
                } else {
                    // Dot finished animation for this loop
                    animationFactor = 0f
                }

                val translationY = (-mJumpHeight - from) * animationFactor
                dot.setTranslationY(translationY)
            }
        })
        mAnimation!!.duration = mLoopDuration.toLong()
        mAnimation!!.repeatCount = Animation.INFINITE
    }

    fun startAnimation() {
        if (mAnimation != null && mAnimation!!.isRunning) {
            // We are already running
            return
        }

        createAnimation()
        startAnimationIfAttached()
    }

    fun stopAnimation() {
        if (mAnimation != null) {
            mAnimation!!.end()
            mAnimation = null
        }
    }

    private fun calculateCachedValues() {
        verifyNotRunning()

        // The offset is the time delay between dots start animation
        val startOffset = (mLoopDuration - (jumpDuration + mLoopStartDelay)) / (mDotsCount - 1)

        // Dot jump half time ( jumpTime/2 == going up == going down)
        mJumpHalfTime = jumpDuration / 2

        mDotsStartTime = IntArray(mDotsCount)
        mDotsJumpUpEndTime = IntArray(mDotsCount)
        mDotsJumpDownEndTime = IntArray(mDotsCount)

        for (i in 0 until mDotsCount) {
            val startTime = mLoopStartDelay + startOffset * i
            mDotsStartTime!![i] = startTime
            mDotsJumpUpEndTime!![i] = startTime + mJumpHalfTime
            mDotsJumpDownEndTime!![i] = startTime + jumpDuration
        }
    }

    private fun verifyNotRunning() {
        if (mAnimation != null) {
            throw IllegalStateException("Can't change properties while animation is running!")
        }
    }

    private fun initializeDots(context: Context) {
        verifyNotRunning()
        removeAllViews()

        // Create the dots
        mDots = ArrayList(mDotsCount)
        val dotParams = LinearLayout.LayoutParams(mDotSize, mDotSize)
        val spaceParams = LinearLayout.LayoutParams(mDotSpace, mDotSize)
        for (i in 0 until mDotsCount) {
            // Add dot
            val dotView = createDotView(context)
            addView(dotView, dotParams)
            mDots!!.add(dotView)

            // Add space
            if (i < mDotsCount - 1) {
                addView(View(context), spaceParams)
            }
        }
    }

    /**
     * Set the color to be used for the dots fill color
     * xml: LoadingDots_dots_color
     * @param colorRes color resource
     */
    fun setDotsColorRes(colorRes: Int) {
        dotsColor = context.resources.getColor(colorRes)
    }

    /**
     * Set the dots size
     * xml: LoadingDots_dots_size
     * @param sizeRes size resource
     */
    fun setDotsSizeRes(sizeRes: Int) {
        dotsSize = context.resources.getDimensionPixelSize(sizeRes)
    }

    /**
     * Set the space between dots
     * xml: LoadingDots_dots_space
     * @param spaceRes space size resource
     */
    fun setDotsSpaceRes(spaceRes: Int) {
        dotsSpace = context.resources.getDimensionPixelSize(spaceRes)
    }

    /**
     * Set the dots jump duration. This is the duration it takes a single dot to complete the jump.
     * Jump duration starts when the dot first start to rise until it settle back to base location.
     * xml: LoadingDots_jump_duration
     * @param jumpDuration
     */
    fun setJumpDuraiton(jumpDuration: Int) {
        verifyNotRunning()
        this.jumpDuration = jumpDuration
    }

    /**
     * Set the jump height of the dots. The entire view will include this height to allow the dots
     * animation to draw properly. The entire view height will be DotsSize + JumpHeight.
     * xml: LoadingDots_jump_height
     * @param heightRes size resource
     */
    fun setJumpHeightRes(heightRes: Int) {
        jumpHeight = context.resources.getDimensionPixelSize(heightRes)
    }

    companion object {

        val DEFAULT_DOTS_COUNT = 3
        val DEFAULT_LOOP_DURATION = 600
        val DEFAULT_LOOP_START_DELAY = 100
        val DEFAULT_JUMP_DURATION = 400
    }
}

