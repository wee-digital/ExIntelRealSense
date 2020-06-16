package wee.digital.example

import android.app.Application
import wee.digital.camera.RealSense

class App : Application() {

    companion object {
        lateinit var instance: App private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        RealSense.app = this
        RealSense.instance.start()
    }

    override fun onTerminate() {
        super.onTerminate()
        RealSense.instance.destroy()
    }

}