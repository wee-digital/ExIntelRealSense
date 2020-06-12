package wee.digital.example

import android.app.Application
import wee.digital.camera.Camera

class App : Application() {

    companion object {
        lateinit var instance: App private set
    }

    override fun onCreate() {
        super.onCreate()
        Camera.app = this
        Camera.instance.start()
    }

    override fun onTerminate() {
        super.onTerminate()
        Camera.instance.destroy()
    }

}