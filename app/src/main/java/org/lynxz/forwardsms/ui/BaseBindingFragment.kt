package org.lynxz.forwardsms.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

abstract class BaseBindingFragment<B : ViewDataBinding> : BaseFragment() {
    protected lateinit var dataBinding: B

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val containerView = inflater.inflate(getLayoutRes(), container, false)
        dataBinding = DataBindingUtil.bind(containerView)!!
        return containerView
    }
}