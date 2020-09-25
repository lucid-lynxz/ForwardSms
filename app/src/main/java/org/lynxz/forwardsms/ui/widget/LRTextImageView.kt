package org.lynxz.forwardsms.ui.widget

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import androidx.constraintlayout.widget.ConstraintLayout
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.util.ViewUtil

/**
 * 左文本右图案控件封装
 * 可用属性:
 * <pre>
 *      <declare-styleable name="LRTextImageView">
 *      <attr name="lrtiv_icon_src" format="reference" />
 *      <attr name="lrtiv_icon_size" format="dimension" />
 *      <attr name="lrtiv_enable_ripple" format="boolean" />
 *
 *      <attr name="lrtiv_text" format="string" />
 *      <attr name="lrtiv_text_size" format="dimension" />
 *      <attr name="lrtiv_text_color" format="color" />
 *      </declare-styleable>
 * </pre>
 *
 * */
class LRTextImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {
    private var imageView: ImageView
    private var textView: TextView

    init {
        inflate(context, R.layout.view_lr_text_iamge_view, this)
        textView = findViewById(R.id.tv_left)
        imageView = findViewById(R.id.iv_right)

        val array = context.obtainStyledAttributes(attrs, R.styleable.LRTextImageView)
        val imgDrawable = array.getDrawable(R.styleable.LRTextImageView_lrtiv_icon_src) // 图片资源

        // 图片尺寸,0表示无效
        val imgSizePx = array.getDimensionPixelSize(
            R.styleable.LRTextImageView_lrtiv_icon_size,
            0
        )

        // 文本内容
        val textContent = array.getString(R.styleable.LRTextImageView_lrtiv_text)

        // 文本颜色, Integer.MIN_VALUE表示无效
        @ColorInt
        val textColor = array.getColor(
            R.styleable.LRTextImageView_lrtiv_text_color,
            Int.MIN_VALUE
        )

        // 文本大小, 0表示无效
        val textSizePx = array.getDimensionPixelSize(
            R.styleable.LRTextImageView_lrtiv_text_size,
            0
        )

        // 是否启用前景色水波纹,默认不启用
        val enableRipple = array.getBoolean(
            R.styleable.LRTextImageView_lrtiv_enable_ripple,
            false
        )
        array.recycle()
        if (enableRipple) {
            ViewUtil.setRippleForeground(this)
        }

        imageView.setImageDrawable(imgDrawable)
        updateImageSize(imgSizePx)

        textView.text = textContent
        if (textColor != Int.MIN_VALUE) {
            textView.setTextColor(textColor)
        }
        if (textSizePx != 0) {
            textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSizePx.toFloat())
        }
    }

    /**
     * 更新右侧图片
     */
    fun updateImage(@DrawableRes resId: Int): LRTextImageView {
        imageView.setImageResource(resId)
        return this
    }

    /**
     * 更新左侧文本
     * */
    fun updateText(text: String?): LRTextImageView {
        textView.text = text
        return this
    }

    /**
     * 设置图片尺寸,单位: px
     * */
    fun updateImageSize(imgSizePx: Int): LRTextImageView {
        if (imgSizePx > 0) {
            val layoutParams = imageView.layoutParams
            layoutParams.height = imgSizePx
            layoutParams.width = imgSizePx
            imageView.layoutParams = layoutParams
        }
        return this
    }

    /**
     * 设置文本点击事件
     * */
    fun setTextOnClickListener(listener: OnClickListener): LRTextImageView {
        textView.setOnClickListener(listener)
        return this
    }

    /**
     * 设置图案文本点击事件
     * */
    fun setImageOnClickListener(listener: OnClickListener): LRTextImageView {
        imageView.setOnClickListener(listener)
        return this
    }

    fun getTextView() = textView
    fun getImageView() = imageView

}