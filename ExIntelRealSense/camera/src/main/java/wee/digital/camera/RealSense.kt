package wee.digital.camera

import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import androidx.lifecycle.MutableLiveData
import com.intel.realsense.librealsense.RsContext
import wee.digital.camera.core.RealSenseControl

object RealSense {

    const val VENDOR_ID: Int = 32902
    const val PRODUCT_ID: Int = 2888
    private const val PERMISSION = ".USB_PERMISSION"

    private var mApp: Application? = null

    var app: Application
        set(value) {
            mApp = value
            RsContext.init(value)
        }
        get() {
            if (null == mApp) throw NullPointerException("module not be set")
            return mApp!!
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

    fun stop() {
        Thread {
            controlLiveData.value?.onPause()
        }.start()
        controlLiveData.postValue(null)
    }

    fun hasFace() {
        controlLiveData.value?.hasFace()
    }

    /**
     * Usb util
     */
    @JvmStatic
    val usbManager: UsbManager
        get() = app.getSystemService(Context.USB_SERVICE) as UsbManager

    @JvmStatic
    val usbDevices: Collection<UsbDevice>
        get() = usbManager.deviceList.values

    private val intentFilter: IntentFilter by lazy {
        IntentFilter(PERMISSION).also {
            it.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            it.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }
    }

    @JvmStatic
    val device: UsbDevice?
        get() {
            usbDevices.forEach {
                if (it.vendorId == VENDOR_ID) return it
            }
            return null
        }

    private var usbReceiver: BroadcastReceiver? = null

    private fun usbReceiver(permissionGranted: () -> Unit): BroadcastReceiver {
        return object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val usb = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                if (usb.vendorId != VENDOR_ID) return
                if (intent.action === UsbManager.ACTION_USB_DEVICE_DETACHED) return
                if (intent.action === UsbManager.ACTION_USB_DEVICE_ATTACHED) {
                    if (usbManager.hasPermission(usb)) {
                        permissionGranted()
                    } else {
                        requestPermission(usb)
                    }
                }
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    permissionGranted()
                }
            }
        }
    }

    @JvmStatic
    fun open(usb: UsbDevice?): UsbDeviceConnection {
        return usbManager.openDevice(usb)
    }

    @JvmStatic
    fun requestPermission(usb: UsbDevice?, permissionGranted: () -> Unit = {}) {
        if (hasPermission(usb)) {
            permissionGranted()
            return
        }

        if (usbReceiver == null) {
            usbReceiver = usbReceiver(permissionGranted).also {
                app.registerReceiver(it, intentFilter)
            }
        }

        usb ?: return
        val permissionIntent = PendingIntent.getBroadcast(app, 1234, Intent(PERMISSION), 0)
        usbManager.requestPermission(usb, permissionIntent)
    }

    @JvmStatic
    fun requestPermission(permissionGranted: () -> Unit = {}) {
        requestPermission(device, permissionGranted)
    }

    @JvmStatic
    fun hasPermission(usb: UsbDevice?): Boolean {
        usb ?: return false
        return usbManager.hasPermission(usb)
    }

}





