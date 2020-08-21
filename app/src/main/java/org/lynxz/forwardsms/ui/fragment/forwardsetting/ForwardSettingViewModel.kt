package org.lynxz.forwardsms.ui.fragment.forwardsetting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ForwardSettingViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "转发设置页"
    }
    val text: LiveData<String> = _text
}