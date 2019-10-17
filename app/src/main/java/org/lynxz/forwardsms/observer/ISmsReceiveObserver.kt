package org.lynxz.forwardsms.observer

import org.lynxz.forwardsms.bean.SmsDetail

interface ISmsReceiveObserver {
    fun onReceiveSms(smsDetail: SmsDetail?)
}