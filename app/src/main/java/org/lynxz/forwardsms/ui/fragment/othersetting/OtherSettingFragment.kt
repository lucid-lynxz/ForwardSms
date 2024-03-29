package org.lynxz.forwardsms.ui.fragment.othersetting

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.ViewModelProviders
import com.bigkoo.pickerview.builder.TimePickerBuilder
import com.bigkoo.pickerview.listener.CustomListener
import com.bigkoo.pickerview.view.TimePickerView
import com.noober.background.drawable.DrawableCreator
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.databinding.FragmentOtherSettingBinding
import org.lynxz.forwardsms.hideKeyboard
import org.lynxz.forwardsms.observer.IViewActionHandler
import org.lynxz.forwardsms.para.BatteryListenerManager
import org.lynxz.forwardsms.para.MosaicParaManager
import org.lynxz.forwardsms.para.TimeValidationParaManager
import org.lynxz.forwardsms.showToast
import org.lynxz.forwardsms.ui.BaseBindingFragment
import org.lynxz.forwardsms.ui.widget.LRTextImageView
import org.lynxz.forwardsms.util.ResourceUtil
import org.lynxz.forwardsms.util.ScreenUtil
import org.lynxz.forwardsms.util.ViewUtil
import org.lynxz.utils.log.LoggerUtil
import org.lynxz.utils.otherwise
import org.lynxz.utils.yes
import java.text.SimpleDateFormat
import java.util.*
import androidx.lifecycle.Observer as Observer1


/**
 * 扩展设置项
 * 如: 可转发时间段设置, 低电量提醒,来电提醒等
 * */
class OtherSettingFragment : BaseBindingFragment<FragmentOtherSettingBinding>(),
    IViewActionHandler {
    private val vm by lazy { ViewModelProviders.of(this).get(OtherSettingViewModel::class.java) }

    private val paraLayoutParam by lazy {
        ViewGroup.MarginLayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(10, 2, 10, 2)
        }
    }
    private var tvStartTime: TextView? = null
    private var tvEndTime: TextView? = null
    private var bgStartTime: View? = null
    private var bgEndTime: View? = null

    override fun getLayoutRes() = R.layout.fragment_other_setting

    override fun afterViewCreated(view: View) {
        dataBinding.vm = vm
        dataBinding.viewActionHandler = this

        // 是否启用时间段设置
        vm.enableAddTimeDurationLiveData.observe(this,
            { enable -> dataBinding.btnAddForwardTime.isEnabled = enable })

        // 状态变化是,保存数据
        vm.enableDateLiveData.observe(this, { TimeValidationParaManager.updateAndSavePara() })

        // 是否启用不可转发日期设置
        vm.allDateLiveData.observe(this, Observer1 { validateDate ->
            dataBinding.flForwardDate.removeAllViews()
            (1..7).forEach { weekDayIndex ->
                dataBinding.flForwardDate.addView(
                    LRTextImageView(requireActivity())
                        .updateText("周$weekDayIndex".replace("周7", "周日"))
                        .updateImage(if (TimeValidationParaManager.isInWeekDays(weekDayIndex)) R.drawable.ic_checked_24 else R.drawable.ic_uncheck_24)
                        .apply {
                            tag = weekDayIndex
                            setImageOnClickListener {
                                val tagWeekDayIndex: Int = tag as Int
                                vm.enableDateLiveData.value?.yes {
                                    vm.allDateLiveData.value?.let {
                                        if (it.contains(tagWeekDayIndex)) {
                                            it.remove(tagWeekDayIndex)
                                        } else {
                                            it.add(tagWeekDayIndex)
                                        }
                                        vm.allDateLiveData.postValue(it)
                                    }
                                    TimeValidationParaManager.updateAndSavePara() // 保存数据
                                }
                            }
                        }
                )
            }
        })


        // 添加所有时间段配置
        vm.allTimeDurationLiveData.observe(this, { list ->
            dataBinding.flForwardTime.removeAllViews()
            list.forEachIndexed { index, timeDurationBean ->
                dataBinding.flForwardTime.addView(
                    LRTextImageView(requireActivity())
                        .updateText(timeDurationBean.toString())
                        .updateImage(R.drawable.ic_close_24)
                        .updateImageSize(ScreenUtil.dp2px(activity, 16))
                        .apply {
                            tag = index // 指定tag为其序号
                            background = createItemBgDrawable() // 设置背景
                            setPadding(10, 10, 10, 10)
                            // 点击图标删除该时间段设置
                            setImageOnClickListener {
                                vm.enableTimeDurationLiveData.value?.yes {
                                    vm.chooseTimeDurationForEditing(tag as Int, true)
                                    vm.applyTimeDuration()
                                }
                            }
                            setOnClickListener { // 显示时间选择面板
                                vm.enableTimeDurationLiveData.value?.yes {
                                    vm.chooseTimeDurationForEditing(tag as Int)
                                    timePickView.show(true)
                                }
                            }
                        }, index, paraLayoutParam
                )
            }
        })

        // 时间信息变化时,更新UI
        vm.timeDurationBeanLiveData.observe(this, { bean ->
            tvStartTime?.text = bean.startTime.toString()
            tvEndTime?.text = bean.endTime.toString()
            timePickView.setDate(bean.curEditingTime.convert2Date())

            bgStartTime?.isSelected = bean.isEditingStartTime
            bgEndTime?.isSelected = !bean.isEditingStartTime
        })

        // 添加所有马赛克模糊处理设置
        vm.mosaicParaLiveData.observe(this, { mosaicPara ->
            updateMosaicParaViews()
        })
    }

    /**
     * 更新所有马赛克模糊条件view
     * */
    private fun updateMosaicParaViews() {
        dataBinding.flMosaicCondition.removeAllViews()
        var index = 0
        MosaicParaManager.mosaicParaLiveData.value?.detailMosaicMap?.forEach { condition ->
            val mosaicView = LRTextImageView(requireActivity())
                .updateText("${condition.key}->${condition.value}")
                .updateImage(R.drawable.ic_close_24)
                .updateImageSize(ScreenUtil.dp2px(activity, 16))
                .apply {
                    tag = condition.key
//                    background = timeDurationDrawable.mutate() // 设置背景
                    background = createItemBgDrawable() // 设置背景
                    setPadding(10, 10, 10, 10)
                    // 删除图标
                    setImageOnClickListener {
                        (tag as? String)?.let {
                            vm.updateMosaicPara(it, delete = true)
                        }
                    }

                    // 重新编辑
                    setOnClickListener {
                        (tag as? String)?.let { key ->
                            vm.mosaicParaLiveData.value?.detailMosaicMap
                                ?.getOrDefault(key, null)
                                ?.let { value ->
                                    dataBinding.edtMosaicSrc.setText(key)
                                    dataBinding.edtMosaicDest.setText(value)
                                }
                        }
                    }
                }
            dataBinding.flMosaicCondition.addView(mosaicView, index, paraLayoutParam)
            index++
        }
    }

    /**
     * 创建itemView的背景图片
     * 两边圆形
     * */
    private fun createItemBgDrawable() = DrawableCreator.Builder()
        .setCornersRadius(ScreenUtil.dp2px(requireActivity(), 20).toFloat())
        .setSolidColor(Color.WHITE)
        .setStrokeColor(ResourceUtil.getColor(R.color.gray))
        .setStrokeWidth(1f)
        .build()

    // 时间选择面板设置
    private val timePickerCustomLayoutListener by lazy {
        CustomListener { v ->
            tvStartTime = v.findViewById(R.id.tv_start_time) // 起始时间
            tvEndTime = v.findViewById(R.id.tv_end_time) // 结束时间
            vm.timeDurationBeanLiveData.value?.let {
                tvStartTime?.text = it.startTime.toString()
                tvEndTime?.text = it.endTime.toString()
            }

            bgStartTime = v.findViewById(R.id.view_start_time) // 结束时间背景
            bgEndTime = v.findViewById(R.id.view_end_time) // 起始时间背景
            val tvFinish = v.findViewById<TextView>(R.id.tv_finish) // 编辑完成
            val tvDelete = v.findViewById<TextView>(R.id.tv_delete) // 删除

            bgStartTime?.setOnClickListener { vm.setEditingTime(true) }
            bgEndTime?.setOnClickListener { vm.setEditingTime(false) }

            // 完成时间段编辑
            tvFinish.setOnClickListener {
                vm.applyTimeDuration().yes {
                    timePickView.returnData()
                    timePickView.dismiss()
                }.otherwise {
                    showToast("参数异常,请检查终止时间")
                }
            }

            // 删除正在编辑的时间
            tvDelete.setOnClickListener {
                vm.deleteCurrentEditingTimeDuration()
                timePickView.dismiss()
            }
        }
    }

    // 时间选择面板
    private val timePickView: TimePickerView by lazy {
        TimePickerBuilder(activity) { date, _ ->
            // 保存时间配置
            LoggerUtil.d("pvTime", "onTimeSelect")
            dataBinding.tvTime.text = "选择时间 ${SimpleDateFormat("HH:mm", Locale.CHINA).format(date)}"
        }
            .setType(booleanArrayOf(false, false, false, true, true, false))
            .setTimeSelectChangeListener { vm.updateEditingTime(it) }
            .setLayoutRes(R.layout.pickerview_custom_time_duration, timePickerCustomLayoutListener)
            .setDividerColor(Color.DKGRAY)
            .setContentTextSize(20)
            .setOutSideColor(0x00000000)
            .setOutSideCancelable(true)
            .setItemVisibleCount(5) //若设置偶数，实际值会加1（比如设置6，则最大可见条目为7）
            .setLineSpacingMultiplier(2.0f)
            .isDialog(true) // 设置为dialog模式, 否则默认以decoView为父容器,会覆盖底部导航条
            .setLabel("", "", "", "时", "分", "")
            .isAlphaGradient(true)
            .build().apply {
                dialog?.let {
                    val params = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        Gravity.BOTTOM
                    )
                    params.leftMargin = 0
                    params.rightMargin = 0
                    dialogContainerLayout.layoutParams = params
                    it.window?.apply {
                        attributes = attributes.apply { // 设置dialog宽度全屏
                            width = WindowManager.LayoutParams.MATCH_PARENT
                        }
                        setWindowAnimations(com.bigkoo.pickerview.R.style.picker_view_slide_anim)
                        setGravity(Gravity.BOTTOM)
                        setDimAmount(0.3f)
                    }
                }
            }
    }

    override fun onClick(v: View?) {
        when (v) {
            dataBinding.tvTime, dataBinding.btnAddForwardTime -> { // 添加时间段
                vm.chooseTimeDurationForEditing(-1)
                timePickView.show(true) // 显示时间选择面板
            }

            dataBinding.btnConfirmLowBatteryListener -> { // 设置电量监听
                BatteryListenerManager.updateAndSave()
                activity?.hideKeyboard()
                dataBinding.edtLowBattery.clearFocus()
            }
            dataBinding.btnAddMosaicCondition -> { // 添加模糊处理
                updateMosaicCondition()
            }
        }
    }

    private fun updateMosaicCondition() {
        val src = dataBinding.edtMosaicSrc.text.toString()
        val dest = dataBinding.edtMosaicDest.text.toString()
        if (src.isEmpty()) {
            showToast("请输入待替换的原内容")
            dataBinding.edtMosaicSrc.requestFocus()
        } else {
            MosaicParaManager.addMapInfo(src, dest)
            dataBinding.edtMosaicSrc.text = null
            dataBinding.edtMosaicDest.text = null

            // 清除焦点,关闭键盘
            activity?.currentFocus?.let {
                ViewUtil.hideKeyboard(it)
                it.clearFocus()
            }
        }
    }
}