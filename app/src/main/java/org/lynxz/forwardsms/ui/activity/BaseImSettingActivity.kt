package org.lynxz.forwardsms.ui.activity

import android.view.Menu
import android.view.MenuItem
import org.lynxz.forwardsms.R
import org.lynxz.forwardsms.ui.BaseActivity

/**
 * 钉钉参数配置
 * */
abstract class BaseImSettingActivity : BaseActivity() {
//    override fun getLayoutRes() = R.layout.activity_dingding_setting

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

}