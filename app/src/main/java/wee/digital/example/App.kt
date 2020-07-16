package wee.digital.example

import android.app.Application
import wee.digital.camera.RealSense
import wee.digital.example.util.SystemUtil

class App : Application() {

    companion object {
        lateinit var instance: App private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        RealSense.app = this
        SystemUtil.ledOff()
    }

}