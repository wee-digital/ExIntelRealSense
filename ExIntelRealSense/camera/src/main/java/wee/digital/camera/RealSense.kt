package wee.digital.camera

import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.intel.realsense.librealsense.RsContext
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit

object RealSense {

    init {
        System.loadLibrary("real_sense")
    }

    /**
     * Application
     */
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
     * Log
     */
    private const val TAG = "HeroFun"

    fun d(s: Any?) {
        if (BuildConfig.DEBUG) Log.d(TAG, s.toString())
    }

    fun d(e: Throwable) {
        if (BuildConfig.DEBUG) Log.e(TAG, e.message)
    }

    /**
     * Usb util
     */
    const val VENDOR_ID: Int = 32902

    private const val PERMISSION = ".USB_PERMISSION"

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

    /**
     * [RealSenseControl]
     */
    private var realSenseControl: RealSenseControl? = null

    val imagesLiveData: MutableLiveData<Pair<Bitmap, Bitmap>?> by lazy {
        MutableLiveData<Pair<Bitmap, Bitmap>?>()
    }

    /**
     * Native
     */
    external fun nVersion(): String

    external fun nStart()

    external fun nStop()

    external fun nWaitForFrames(colorRaw: ByteArray, depthRaw: ByteArray)


    /**
     * Native wrapper
     */
    private var frameObservable: Disposable? = null

    val imageLiveData: MutableLiveData<Bitmap?> by lazy {
        MutableLiveData<Bitmap?>()
    }

    val frames: Bitmap?
        get() {
            val colorRaw = ByteArray(RealSenseControl.COLOR_SIZE)
            val depthRaw = ByteArray(RealSenseControl.DEPTH_SIZE)
            nWaitForFrames(colorRaw, depthRaw)
            return colorRaw.toBitmap(RealSenseControl.COLOR_WIDTH, RealSenseControl.COLOR_HEIGHT)
        }

    fun start() {
        nStart()
        /*Thread {
            realSenseControl = RealSenseControl().also {
                Thread.sleep(2400)
                it.onCreate()
            }
        }.start()*/
    }

    fun stop() {
        pauseStream()
        nStop()
        //realSenseControl?.onPause()
    }

    fun startStream() {
        frameObservable?.dispose()
        frameObservable = Observable
            .interval(0, 80, TimeUnit.MILLISECONDS)
            .map { frames }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableObserver<Bitmap>() {
                override fun onNext(it: Bitmap) {
                    imageLiveData.value = it
                }

                override fun onComplete() {
                }

                override fun onError(e: Throwable) {
                }
            })
    }

    fun pauseStream() {
        frameObservable?.dispose()
        imagesLiveData.postValue(null)
    }

    val framesBytes: ByteArray?
        get() {
            val colorRaw = ByteArray(RealSenseControl.COLOR_SIZE)
            val depthRaw = ByteArray(RealSenseControl.DEPTH_SIZE)
            nWaitForFrames(colorRaw, depthRaw)
            return colorRaw
        }

    fun capture(block: (ByteArray?) -> Unit) {
        Observable
            .fromCallable { framesBytes }
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeWith(object : DisposableObserver<ByteArray>() {
                override fun onNext(it: ByteArray) {
                    block(it)
                }

                override fun onComplete() {
                }

                override fun onError(e: Throwable) {
                    block(null)
                }
            })
    }
}





