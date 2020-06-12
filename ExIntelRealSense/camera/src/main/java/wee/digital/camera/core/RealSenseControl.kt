package wee.digital.camera.core

import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import com.intel.realsense.librealsense.*
import wee.digital.camera.RealSenseLiveData
import wee.digital.camera.debug
import wee.digital.camera.getBitmap

/**
 * Manufacture: Intel(R) RealSense(TM) Depth Camera SR305
 * Product : Intel(R) RealSense(TM) Depth Camera SR305
 * Vendor ID : 32902
 * Product ID: 2888
 */
class RealSenseControl {

    companion object {
        const val TAG = "RealSenseControl"

        const val COLOR_WIDTH = 1280
        const val COLOR_HEIGHT = 720
        const val COLOR_SIZE = COLOR_WIDTH * COLOR_HEIGHT * 3

        const val DEPTH_WIDTH = 640
        const val DEPTH_HEIGHT = 480
        const val DEPTH_SIZE = DEPTH_WIDTH * DEPTH_HEIGHT * 3

        const val TIME_WAIT = 2000
        const val FRAME_RATE = 10
        const val FRAME_MAX_COUNT = 200 // Run 10s
        const val FRAME_MAX_SLEEP = -20 // Sleep 1s

    }

    var listener: Listener? = null
    private var mRsContext: RsContext? = null
    private var mColorizerOrg: Colorizer? = null
    private var mPipeline: Pipeline? = null
    private var isDestroy = false
    private var isFrameOK = false
    private var mHandlerThread: HandlerThread? = null
    private var mHandler: Handler? = null
    private var mIsStreaming = false
    private var colorBitmap: Bitmap? = null
    private var depthBitmap: Bitmap? = null
    private var mDevice: Device? = null
    private var isDeviceAttach = false
    private var mFrameCount = FRAME_MAX_COUNT
    var isPauseCamera = false
    private var isSleep = false
    private var isProcessingFrame = false
    private val mStreaming: Runnable = object : Runnable {
        override fun run() {
            val isNext = try {
                FrameReleaser().use { fr ->
                    try {
                        if (!RealSenseLiveData.instance.hasObservers() && listener == null) {
                            isSleep = true
                            true
                        }
                        if (isPauseCamera || isProcessingFrame) {
                            isSleep = true
                            true
                        }
                        //debug("Pipeline Wait.........")
                        isProcessingFrame = true
                        val frames: FrameSet =
                                mPipeline!!.waitForFrames(TIME_WAIT).releaseWith(fr)
                        if (isFrameOK) {
                            //debug("Run $mFrameCount")
                            mFrameCount--
                            when {
                                mFrameCount > 0 -> {
                                    val colorFrame: Frame =
                                            frames.first(StreamType.COLOR).releaseWith(fr)
                                    val processFrame =
                                            frames.applyFilter(mColorizerOrg).releaseWith(fr)
                                    val depthFrame: Frame =
                                            processFrame.first(StreamType.DEPTH).releaseWith(fr)
                                    frameProcessing(colorFrame, depthFrame)
                                }
                                mFrameCount < FRAME_MAX_SLEEP -> {
                                    //debug("Sleep $mFrameCount")
                                    mFrameCount = FRAME_MAX_COUNT
                                    isProcessingFrame = false

                                }
                                else -> {
                                    isProcessingFrame = false
                                }
                            }
                        } else {
                            isFrameOK = true
                            isProcessingFrame = false
                            debug("FrameOK....")
                            listener?.onCameraStarted()
                        }
                        isSleep = false
                        true
                    } catch (e: Throwable) {
                        debug("FrameReleaser ${e.message}")
                        listener?.onCameraMessage("FrameReleaser ${e.message}")
                        isProcessingFrame = false
                        false
                    }
                }

            } catch (e: Exception) {
                debug("streaming, error: " + e.message)
                false
            }
            if (isNext) {
                if (!isSleep) {
                    mHandler?.post(this)
                } else {
                    mHandler?.postDelayed(this, 80)
                }
            } else {
                isFrameOK = false
                // stopStreamThread()
                hardwareReset()
            }

        }
    }

    init {
        mHandlerThread = HandlerThread("streaming")
        mHandlerThread?.start()
        mHandler = Handler(mHandlerThread!!.looper)
    }

    fun onCreate() {
        //initCustomModel()
        //mAlign = Align(StreamType.COLOR)
        debug("Version: ${RsContext.getVersion()}")
        mColorizerOrg = Colorizer()

        //mColorizerOrg?.setValue(Option.VISUAL_PRESET,2f)
        //mColorizerOrg?.setValue(Option.HISTOGRAM_EQUALIZATION_ENABLED,3f)
        //mColorizerOrg?.setValue(Option.ENABLE_AUTO_EXPOSURE,1f)

        /*0 - Jet
        1 - Classic
        2 - WhiteToBlack
        3 - BlackToWhite
        4 - Bio
        5 - Cold
        6 - Warm
        7 - Quantized
        8 - Pattern*/
        mColorizerOrg?.setValue(Option.COLOR_SCHEME, 0f)
        mRsContext = RsContext()
        mPipeline = Pipeline()
        mRsContext!!.queryDevices(ProductLine.ANY_INTEL).use { dl ->
            if (dl.deviceCount > 0) {
                if (isDeviceAttach) return
                isDeviceAttach = true
                debug("Has ${dl.deviceCount} device")
                startStreamThread()
            } else {
                debug("No device connected")
            }
        }
        mRsContext?.setDevicesChangedCallback(object : DeviceListener {
            override fun onDeviceAttach() {
                debug("onDeviceAttach: $isDeviceAttach")
                if (isDeviceAttach) return
                isDeviceAttach = true
                startStreamThread()
            }

            override fun onDeviceDetach() {
                debug("onDeviceDetach: $isDeviceAttach")

                isDeviceAttach = false
                stopStreamThread().apply {
                    mIsStreaming = false
                    startStreamThread()
                }

            }
        })

    }

    private fun frameProcessing(colorFrame: Frame, depthFrame: Frame) {
        try {
            if (RealSenseLiveData.instance.hasObservers() || listener != null) {
                colorBitmap = colorFrame.getBitmap(COLOR_SIZE, COLOR_WIDTH, COLOR_HEIGHT)
            }
            RealSenseLiveData.instance.postValue(colorBitmap)
            listener?.also {
                depthBitmap = depthFrame.getBitmap(DEPTH_SIZE, DEPTH_WIDTH, DEPTH_HEIGHT)
                it.onCameraData(colorBitmap, depthBitmap)
            }
            isProcessingFrame = false

        } catch (e: java.lang.Exception) {
            isProcessingFrame = false
            e.printStackTrace()
        } catch (e: Throwable) {
            isProcessingFrame = false
            e.printStackTrace()
        }
    }

    fun startStreamThread() {
        if (mIsStreaming) return
        debug("try start streaming")
        try {
            mIsStreaming = true
            mDevice = configAndStart()
            if (mDevice != null) {
                mHandler?.post(mStreaming)
                debug("streaming started successfully")
            }
        } catch (e: java.lang.Exception) {
            debug("failed to start streaming : ${e.message}")
        }

    }

    fun stopStreamThread() {
        if (!mIsStreaming) return
        debug("try stop streaming")
        try {
            mIsStreaming = false
            mPipeline?.stop()
            debug("streaming stopped successfully")
        } catch (e: java.lang.Exception) {
            debug("failed to stop streaming : ${e.message}")
            mPipeline = null
        }
    }

    fun hasFace() {
        // Reset sleep time when has face
        mFrameCount = FRAME_MAX_COUNT
    }

    @Synchronized
    fun onPause() {
        isDestroy = true
        stopStreamThread()
        mHandlerThread?.quitSafely()
        mRsContext?.removeDevicesChangedCallback()
        if (mRsContext != null) mRsContext!!.close()
        mPipeline = null
    }

    private fun configAndStart(): Device? {
        return Config().use { config ->
            try {
                config.enableStream(StreamType.COLOR, 0, COLOR_WIDTH, COLOR_HEIGHT, StreamFormat.RGB8, FRAME_RATE)
                config.enableStream(StreamType.DEPTH, 0, DEPTH_WIDTH, DEPTH_HEIGHT, StreamFormat.Z16, FRAME_RATE)
                val device = mPipeline!!.start(config).device
                val firmware = device.getInfo(CameraInfo.FIRMWARE_VERSION)
                val deviceName = device.getInfo(CameraInfo.NAME)
                val productId = device.getInfo(CameraInfo.PRODUCT_ID)
                val sensors = device.querySensors()
                debug("Firmware: $firmware")
                debug("DeviceName: $deviceName")
                debug("ProductId: $productId")
                debug("Sensors: ${sensors.size}")
                device
            } catch (e: Exception) {
                debug("Start Stream Error: ${e.message}")
                mIsStreaming = false
                null
            }

            /*for(sensor in sensors){

            }*/
            /*mDevice!!.querySensors().forEach { sensor->
                if(sensor.`is`(Extension.COLOR_SENSOR)){
                    val brightness = sensor.getValue(Option.BRIGHTNESS)
                    Log.e("brightness","$brightness - Setting.....")
                    val exposure = sensor.getValue(Option.EXPOSURE)
                    Log.e("exposure","$exposure - Setting.....")
                    val contrast = sensor.getValue(Option.CONTRAST)
                    Log.e("contrast","$contrast - Setting.....")
                    val saturation = sensor.getValue(Option.SATURATION)
                    Log.e("saturation","$saturation - Setting.....")
                    val backlight_conpensation = sensor.getValue(Option.BACKLIGHT_COMPENSATION)
                    Log.e("backlight_conpensation","$backlight_conpensation - Setting.....")
                    val gain = sensor.getValue(Option.GAIN)
                    Log.e("gain","$gain - Setting.....")
                    val gama = sensor.getValue(Option.GAMMA)
                    Log.e("gama","$gama - Setting.....")
                    val white_balance = sensor.getValue(Option.WHITE_BALANCE)
                    Log.e("white_balance","$white_balance - Setting.....")
                    val sharpness = sensor.getValue(Option.SHARPNESS)
                    Log.e("sharpness","$sharpness - Setting.....")
                    val hue = sensor.getValue(Option.HUE)
                    Log.e("hue","$hue - Setting.....")
                    val enable_auto_exposure = sensor.getValue(Option.ENABLE_AUTO_EXPOSURE)
                    Log.e("enable_auto_exposure","$enable_auto_exposure - Setting.....")
                    val enable_auto_white_balance = sensor.getValue(Option.ENABLE_AUTO_WHITE_BALANCE)
                    Log.e("enable_auto_wb","$enable_auto_white_balance - Setting.....")

                    //sensor.setValue(Option.ENABLE_AUTO_EXPOSURE,1f)
                    *//*sensor.setValue(Option.GAIN, 100f)
                    sensor.setValue(Option.BRIGHTNESS,16f)
                    sensor.setValue(Option.EXPOSURE,700f)
                    sensor.setValue(Option.SATURATION,68f)*//*
                }
            }*/

        }
    }

    @Throws
    private fun hardwareReset() {
        if (isDeviceAttach) try {
            mDevice?.hardwareReset()
        } catch (ignore: RuntimeException) {
        }
    }

    interface Listener {

        fun onCameraData(colorBitmap: Bitmap?, depthBitmap: Bitmap?)

        fun onCameraStarted() {}

        fun onCameraMessage(message: String) {}
    }

}