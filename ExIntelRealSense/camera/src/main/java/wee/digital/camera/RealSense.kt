package wee.digital.camera

import android.app.Application
import androidx.lifecycle.MutableLiveData
import com.intel.realsense.librealsense.RsContext
import com.intel.realsense.librealsense.UsbUtilities
import wee.digital.camera.core.RealSenseControl

class RealSense {

    companion object {

        const val VENDOR_ID: Int = 32902
        const val PRODUCT_ID: Int = 2888

        private var mApp: Application? = null

        var app: Application
            set(value) {
                mApp = value
            }
            get() {
                if (null == mApp) throw NullPointerException("module not be set")
                return mApp!!
            }

        val instance: RealSense by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            RsContext.init(mApp)
            UsbUtilities.grantUsbPermissionIfNeeded(mApp)
            RealSense()
        }

    }

    /**
     * Callback when [RealSenseControl] device control initialized or reinitialized
     */
    val controlLiveData = MutableLiveData<RealSenseControl?>()

    var listener: RealSenseControl.Listener?
        get() = controlLiveData.value?.listener
        set(value) {
            controlLiveData.value?.listener = value
        }

    fun start() {
        Thread {
            RealSenseControl().also {
                Thread.sleep(2400)
                it.onCreate()
                controlLiveData.postValue(it)
            }
        }.start()
    }

    fun destroy() {
        val control = controlLiveData.value
        Thread {
            control?.onPause()
        }.start()
    }

    fun hasFace() {
        controlLiveData.value?.hasFace()
    }

}





