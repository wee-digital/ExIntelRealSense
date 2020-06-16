package wee.digital.camera

import android.app.Application
import android.app.PendingIntent
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import androidx.core.content.ContextCompat
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

    val manager: UsbManager =
        ContextCompat.getSystemService(app, UsbManager::class.java) as UsbManager

    val deviceList: List<UsbDevice>
        get() {
            val list = mutableListOf<UsbDevice>()
            val map: HashMap<String, UsbDevice> = manager.deviceList
            map.forEach { list.add(it.value) }
            return list
        }

    val device: UsbDevice?
        get() {
            deviceList.forEach { if (VENDOR_ID == it.vendorId) return it }
            return null
        }

    fun requestPermission() {
        val usb = device ?: return
        if (!manager.hasPermission(usb)) {
            val intent =
                PendingIntent.getBroadcast(app, 1234, Intent(".USB_PERMISSION"), 0)
            manager.requestPermission(usb, intent)
        }
    }

    fun forceClose() {
        val usb = device ?: return
        val connection = manager.openDevice(usb)
        for (i in usb.interfaceCount - 1 downTo 0) {
            connection.releaseInterface(usb.getInterface(i))
        }
        connection.close()
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





