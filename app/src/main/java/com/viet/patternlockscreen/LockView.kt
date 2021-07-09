package com.viet.patternlockscreen

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.os.Build
import android.util.AttributeSet
import android.util.SparseIntArray
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import java.util.*
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

class LockView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val DEFAULT_NORMAL_COLOR = Color.GRAY
    private val DEFAULT_MOVE_COLOR = Color.BLUE
    private val DEFAULT_ERROR_COLOR = Color.RED
    private val DEFAULT_ROW_COUNT = 3

    private val STATE_NORMAL = 0
    private val STATE_MOVE = 1
    private val STATE_ERROR = 2

    private var normalColor: Int = 0
    private var moveColor: Int = 0
    private var errorColor: Int = 0

    private var radius: Float = 0F

    private var rowCount: Int = 0

    private var points: MutableList<PointF> = mutableListOf()

    private var innerCirclePaint: Paint

    private var outerCirclePaint: Paint

    private var stateSparseArray: SparseIntArray

    private var selectedList: MutableList<PointF> = mutableListOf()

    private var selectedList2: MutableList<PointF> = mutableListOf()

    private var linePath: Path = Path()

    private var linePaint: Paint

    private var timer: Timer? = null

    private var touchPoint: PointF? = null

    private var listener: OnDrawCompleteListener? = null

    init {
        readAttrs(context, attrs)

        stateSparseArray = SparseIntArray(rowCount * rowCount)
        innerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        innerCirclePaint.style = Paint.Style.FILL

        outerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        outerCirclePaint.style = Paint.Style.FILL

        linePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeCap = Paint.Cap.ROUND
        linePaint.strokeJoin = Paint.Join.ROUND
        linePaint.strokeWidth = 30F
        linePaint.color = moveColor
    }

    private fun readAttrs(context: Context, attrs: AttributeSet) {
        val typedArray: TypedArray = context.obtainStyledAttributes(attrs, R.styleable.LockView)
        normalColor = typedArray.getColor(
            R.styleable.LockView_normalColor,
            DEFAULT_NORMAL_COLOR
        )
        moveColor = typedArray.getColor(R.styleable.LockView_moveColor, DEFAULT_MOVE_COLOR)
        errorColor = typedArray.getColor(
            R.styleable.LockView_errorColor,
            DEFAULT_ERROR_COLOR
        )
        rowCount = typedArray.getInteger(R.styleable.LockView_rowCount, DEFAULT_ROW_COUNT)
        typedArray.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        radius = min(w, h) / (2 * rowCount + rowCount - 1) * 1.0f

        for (i in 0 until rowCount * rowCount) {
            points.add(PointF((i % rowCount * 3 + 1) * radius, (i / rowCount * 3 + 1) * radius))
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val desiredWidth = 1000
        val desiredHeight = 1000

        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        val width: Int
        val height: Int

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = min(desiredWidth, widthSize)
        } else {
            width = desiredWidth
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = min(desiredHeight, heightSize)
        } else {
            height = desiredHeight
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (canvas != null) {
            drawCircle(canvas)
            drawLinePath(canvas)
        }
    }

    private fun drawCircle(canvas: Canvas) {
        for (index in 0 until rowCount * rowCount) {
            when (stateSparseArray.get(index)) {
                STATE_NORMAL -> {
                    innerCirclePaint.color = normalColor
                    outerCirclePaint.color = normalColor and 0x66ffffff
                }
                STATE_MOVE -> {
                    innerCirclePaint.color = moveColor
                    outerCirclePaint.color = moveColor and 0x66ffffff
                }
                STATE_ERROR -> {
                    innerCirclePaint.color = errorColor
                    outerCirclePaint.color = errorColor and 0x66ffffff
                }
            }
            canvas.drawCircle(points[index].x, points[index].y, radius, outerCirclePaint)
            canvas.drawCircle(points[index].x, points[index].y, radius / 2f, innerCirclePaint)
        }
    }


    private fun drawLinePath(canvas: Canvas) {
        linePath.reset()
        if (selectedList.size > 0) {
            linePath.moveTo(selectedList[0].x, selectedList[0].y)
            for (i in 1 until selectedList.size) {
                linePath.lineTo(selectedList[i].x, selectedList[i].y)
            }

            if (touchPoint != null) {
                linePath.lineTo(touchPoint!!.x, touchPoint!!.y)
            }
            canvas.drawPath(linePath, linePaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> reset()
            MotionEvent.ACTION_MOVE -> {
                if (touchPoint == null) {
                    touchPoint = PointF(event.x, event.y)
                } else {
                    touchPoint!!.set(event.x, event.y)
                }

                for (i in 0 until rowCount * rowCount) {
                    if (getDistance(touchPoint!!, points[i]) < radius) {
                        stateSparseArray.put(i, STATE_MOVE)
                        if (!selectedList.contains(points[i])) {
                            selectedList.add(points[i])
                            selectedList2.add(points[i])
                        }
                        break
                    }
                }
            }
            MotionEvent.ACTION_UP -> {
                listener?.onComplete(selectedList2)

                touchPoint = null
                if (timer == null) {
                    timer = Timer()
                }
                timer!!.schedule(object : TimerTask() {
                    override fun run() {
                        linePath.reset()
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            linePaint.setColor(0xee0000ff)
                        }
                        selectedList.clear()
                        stateSparseArray.clear()
                        postInvalidate()
                    }
                }, 300)
            }
        }
        invalidate()
        return true
    }

    fun setColorError() {
        for (i in 0 until stateSparseArray.size()) {
            val index = stateSparseArray.keyAt(i)
            stateSparseArray.put(index, STATE_ERROR)
        }
        linePaint.color = Color.RED

        touchPoint = null
        if (timer == null) {
            timer = Timer()
        }
        timer!!.schedule(object : TimerTask() {
            override fun run() {
                linePath.reset()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    linePaint.setColor(0xee0000ff)
                }
                selectedList.clear()
                stateSparseArray.clear()
                postInvalidate()
            }
        }, 300)
    }

    private fun reset() {
        timer = null
        innerCirclePaint.reset()
        outerCirclePaint.reset()
        touchPoint = null
        linePath.reset()
        linePaint.color = Color.BLUE
        selectedList.clear()
        selectedList2.clear()
        stateSparseArray.clear()
    }

    interface OnDrawCompleteListener {
        fun onComplete(selectedList: MutableList<PointF>)
    }

    fun setOnDrawCompleteListener(listener: OnDrawCompleteListener) {
        this.listener = listener
    }

    private fun getDistance(centerPoint: PointF, downPoint: PointF): Float {
        return sqrt((centerPoint.x - downPoint.x).pow(2) + (centerPoint.y - downPoint.y).pow(2))
    }

    private fun dp2Px(dpValue: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dpValue.toFloat(), resources.displayMetrics
        )
            .roundToInt()
    }
}