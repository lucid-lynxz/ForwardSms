package org.lynxz.forwardsms.ui.fragment.smslist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SmsListViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "短信列表"
    }
    val text: LiveData<String> = _text


    /**
     * 分页加载数据
     * 首页为0
     * */
    fun loadData(pageNo: Int = 0) {

    }
}