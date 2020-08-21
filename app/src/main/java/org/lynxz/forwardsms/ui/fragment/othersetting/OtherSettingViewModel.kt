package org.lynxz.forwardsms.ui.fragment.othersetting

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class OtherSettingViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "其他设置项"
    }
    val text: LiveData<String> = _text
}