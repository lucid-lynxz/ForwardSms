package org.lynxz.forwardsms.ui

import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

abstract class BaseBindingActivity<B : ViewDataBinding> : BaseActivity() {
    lateinit var dataBinding: B

    override fun onGetLayoutResSuccessful(layoutRes: Int) {
        dataBinding = DataBindingUtil.setContentView(this, layoutRes)
    }
}