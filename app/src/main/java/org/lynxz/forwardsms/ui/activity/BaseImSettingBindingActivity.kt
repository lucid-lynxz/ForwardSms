package org.lynxz.forwardsms.ui.activity

import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import androidx.databinding.ViewDataBinding
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.showKeyBoard
import org.lynxz.forwardsms.showToast
import org.lynxz.forwardsms.ui.BaseBindingActivity

/**
 * 钉钉参数配置
 * */
abstract class BaseImSettingBindingActivity<B : ViewDataBinding> : BaseBindingActivity<B>() {
    /**
     * 从sp中提取配置参数,并设置到布局 dataBinding中
     * */
    abstract fun initImSettingInfo()

    /**
     * 子类实现参数验证及保存
     * */
    abstract fun saveImSetting()

    /**
     * 页面标题
     * */
    abstract fun getPageTitle(): String

    final override fun getContentFragment() = null

    override fun afterViewCreated() {
        supportActionBar?.apply {
            title = getPageTitle()
            setHomeButtonEnabled(true)
            setDisplayHomeAsUpEnabled(true)
        }

        initImSettingInfo()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.im_setting_save, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        var consumed = true
        when (itemId) {
            android.R.id.home -> finish()
            R.id.action_ok -> saveImSetting()
            else -> consumed = false
        }

        return consumed or super.onOptionsItemSelected(item)
    }

    /**
     * 检查输入值是否正确
     * 仅判断非空即可
     * @param v 输入框控件, 若为空,自动requestFocus
     * @param toastMsg 若输入为空,则弹出toast
     * @return 若输入空白,则返回null,方便调用方直接判空返回
     * */
    fun getAndCheckInputValid(v: EditText, toastMsg: String? = ""): String? {
        val input = v.text.toString().trim()
        if (input.isBlank()) {
            v.text = null
            showToast(toastMsg)
            showKeyBoard(v)
            return null
        }
        return input
    }
}