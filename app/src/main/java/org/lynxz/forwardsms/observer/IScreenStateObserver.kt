package org.lynxz.forwardsms.observer

interface IScreenStateObserver {

    /**
     * 解锁屏幕
     * */
    fun onScreenOn()

    /**
     * 屏幕关闭
     * */
    fun onScreenOff()
}