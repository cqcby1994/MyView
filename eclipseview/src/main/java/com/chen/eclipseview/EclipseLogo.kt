package com.chen.eclipseview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.View
import java.text.DecimalFormat


/**
 * Created by chenqc on 2020/3/12 13:45
 */
class EclipseLogo : View {
    private val TAG = "EclipseLogo"
    private var mContext: Context

    //太阳\月亮画笔
    private lateinit var mSunPaint: Paint

    //Logo画笔
    private lateinit var mLogoPaint: Paint
    //背景开始的颜色
    private var mBgStartColor: Int = Color.WHITE
    //背景结束的颜色
    private var mBgEndColor: Int = Color.BLACK
    //太阳的颜色
    private var mSunColor: Int = Color.YELLOW
    //月亮的颜色
    private var mMoonColor: Int = Color.BLACK
    //logo src
    private var mLogoID: Int = R.mipmap.ic_launcher
    //logo bitmap
    private lateinit var mLogoBitmap: Bitmap
    //logo size
    private var logoSize = 1
    private lateinit var mLogoBitmapSrcRect: Rect

    private lateinit var mLogoBitmapDesRect: Rect

    private lateinit var xfermode: PorterDuffXfermode


    private var mSunR = 0
    private var mMoonR = 0
    private var centerX = 0
    private var centerY = 0

    private var mMoonX = 0

    private var mMoonStartX = 0

    private var mMoonEndX = 0

    private var progressOffset = 0f

    private var isLogoShow = false

    private val MSG_IN = 1
    private val MSG_OUT = 2

    private val mAnimationSpace = 10L

    private val mMoonXOffset = 5

    private var eclipseListener: EclipseListener? = null

    private val mHandler = Handler {
        when (it.what) {
            MSG_IN -> {
                if (mMoonX < centerX) {
                    mMoonX += mMoonXOffset
                    invalidate()
                    it.target.sendEmptyMessageDelayed(MSG_IN, mAnimationSpace)
                } else if (kotlin.math.abs(mMoonX - centerX) <= mMoonXOffset) {
                    isLogoShow = true
                    invalidate()
                    it.target.sendEmptyMessageDelayed(MSG_OUT, 500)
                }
                progressOffset = txfloat((mMoonX - mMoonStartX), (4 * mSunR))

            }
            MSG_OUT -> {
                if (mMoonX - centerX < 2 * mSunR) {
                    mMoonX += mMoonXOffset
                    var tempAlpha = 255 * (mMoonX - centerX) / (2 * mSunR)
                    if (tempAlpha > mLogoPaint.alpha) {
                        if (tempAlpha > 255) {
                            tempAlpha = 255
                        }
                        mLogoPaint.alpha = tempAlpha
                    }
                    invalidate()
                    it.target.sendEmptyMessageDelayed(MSG_OUT, mAnimationSpace)
                }
                progressOffset = txfloat((mMoonX - mMoonStartX), (4 * mSunR))

            }


        }
        if (it.what == MSG_IN || it.what == MSG_OUT) {
            val color = if (progressOffset * 2 > 1) {
                getCurrentColor(2 - progressOffset * 2, mBgStartColor, mBgEndColor)
            } else {
                getCurrentColor(progressOffset * 2, mBgStartColor, mBgEndColor)
            }
            eclipseListener?.onColor(color)
        }
        false
    }


    constructor(context: Context) : super(context) {
        mContext = context
        init()
    }

    @SuppressLint("Recycle", "NewApi")
    constructor(context: Context, attributeSet: AttributeSet) : super(context, attributeSet) {
        mContext = context
        val ta = context.obtainStyledAttributes(attributeSet, R.styleable.EclipseLogo)
        mBgStartColor = ta.getColor(R.styleable.EclipseLogo_startBackgroundColor, Color.WHITE)
        mBgEndColor = ta.getColor(R.styleable.EclipseLogo_endBackgroundColor, Color.BLACK)
        mSunColor = ta.getColor(R.styleable.EclipseLogo_sunColor, Color.YELLOW)
        mMoonColor = ta.getColor(R.styleable.EclipseLogo_moonColor, Color.BLACK)
        mLogoID = ta.getResourceId(R.styleable.EclipseLogo_logoSrc, R.mipmap.ic_launcher)
        logoSize = ta.getInt(R.styleable.EclipseLogo_logoSize, 1)

        ta.recycle()

        Log.d(TAG, "2")

        init()
    }

    @SuppressLint("NewApi", "Recycle")
    constructor(context: Context, attributeSet: AttributeSet, defStyleAttr: Int) : super(
        context,
        attributeSet,
        defStyleAttr
    ) {
        mContext = context
        val ta = context.obtainStyledAttributes(attributeSet, R.styleable.EclipseLogo)
        mBgStartColor = ta.getColor(R.styleable.EclipseLogo_startBackgroundColor, Color.WHITE)
        mBgEndColor = ta.getColor(R.styleable.EclipseLogo_endBackgroundColor, Color.BLACK)
        mSunColor = ta.getColor(R.styleable.EclipseLogo_sunColor, Color.YELLOW)
        mMoonColor = ta.getColor(R.styleable.EclipseLogo_moonColor, Color.BLACK)
        Log.d(TAG, "3")
        mLogoID = ta.getResourceId(R.styleable.EclipseLogo_logoSrc, R.mipmap.ic_launcher)
        ta.recycle()
        init()
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun init() {
        mLogoBitmap = BitmapFactory.decodeResource(resources, mLogoID)
        mSunPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        mSunPaint.let {
            it.color = mSunColor
            it.style = Paint.Style.FILL
        }

        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP)
        mLogoPaint = Paint()
        mLogoPaint.alpha = 0

        setBackgroundColor(Color.parseColor("#00000000"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            //关闭硬件加速
            setLayerType(LAYER_TYPE_SOFTWARE, null)
        }


    }


    //初始化数据
    private fun initData() {
        centerX = width / 2
        centerY = height / 2
        var modulus = 1
        when (logoSize) {
            0 -> {
                modulus = 3
            }
            1 -> {
                modulus = 2
            }
            2 -> {
                modulus = 1
            }
        }
        mSunR = if (width > height) {
            height / (2 * modulus)
        } else {
            width / (2 * modulus)
        }
        mMoonR = (mSunR * 0.9).toInt()
        mLogoBitmapSrcRect = Rect(0, 0, mLogoBitmap.width, mLogoBitmap.height)

        mLogoBitmapDesRect =
            Rect(centerX - mSunR, centerY - mSunR, centerX + mSunR, centerY + mSunR)
        mMoonX = centerX - mSunR - mSunR
        mMoonStartX = mMoonX
        mMoonEndX = centerX + mSunR + mSunR
        mHandler.sendEmptyMessageDelayed(MSG_IN, 100)

    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        initData()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        mSunPaint.color = mSunColor
        //draw太阳
        canvas?.drawCircle(centerX.toFloat(), centerY.toFloat(), mSunR.toFloat(), mSunPaint)

        if (isLogoShow) {
            mLogoPaint.xfermode = xfermode
            canvas?.drawBitmap(mLogoBitmap, mLogoBitmapSrcRect, mLogoBitmapDesRect, mLogoPaint)
            mLogoPaint.xfermode = null
        }
        //draw月亮
        mSunPaint.color = mMoonColor

        mSunPaint.xfermode = xfermode
        canvas?.drawCircle(mMoonX.toFloat(), centerY.toFloat(), mMoonR.toFloat(), mSunPaint)
        mSunPaint.xfermode = null

    }

    /**
     * 根据fraction值来计算当前的颜色。
     */
    private fun getCurrentColor(fraction: Float, startColor: Int, endColor: Int): Int {
        val redCurrent: Int
        val blueCurrent: Int
        val greenCurrent: Int
        val alphaCurrent: Int
        val redStart = Color.red(startColor)
        val blueStart = Color.blue(startColor)
        val greenStart = Color.green(startColor)
        val alphaStart = Color.alpha(startColor)
        val redEnd = Color.red(endColor)
        val blueEnd = Color.blue(endColor)
        val greenEnd = Color.green(endColor)
        val alphaEnd = Color.alpha(endColor)
        val redDifference = redEnd - redStart
        val blueDifference = blueEnd - blueStart
        val greenDifference = greenEnd - greenStart
        val alphaDifference = alphaEnd - alphaStart
        redCurrent = (redStart + fraction * redDifference).toInt()
        blueCurrent = (blueStart + fraction * blueDifference).toInt()
        greenCurrent = (greenStart + fraction * greenDifference).toInt()
        alphaCurrent = (alphaStart + fraction * alphaDifference).toInt()
        return Color.argb(alphaCurrent, redCurrent, greenCurrent, blueCurrent)
    }

    private fun txfloat(a: Int, b: Int): Float { // TODO 自动生成的方法存根
        val df = DecimalFormat("0.00") //设置保留位数
        return df.format(a.toFloat() / b).toFloat()
    }



    fun release() {
        mHandler.removeCallbacksAndMessages(null)

    }

    fun setOnEclipseListener(listener: EclipseListener) {
        this.eclipseListener = listener
    }


    interface EclipseListener {
        fun onProgress(progress: Int)

        fun onColor(color: Int)
    }
}