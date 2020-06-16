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
    private val colorizer: Colorizer = Colorizer().apply {
        setValue(Option.COLOR_SCHEME, 0f)
    }
    private var pipeline: Pipeline? = null
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


    init {
        mHandlerThread = HandlerThread("streaming")
        mHandlerThread?.start()
        mHandler = Handler(mHandlerThread!!.looper)
    }

    fun onCreate() {
        pipeline = Pipeline()
        startStreamThread()
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
        pipeline = null
    }

    private fun initDevice(): Device? {
        return Config().use { config ->
            try {
                config.enableStream(StreamType.COLOR, 0, COLOR_WIDTH, COLOR_HEIGHT, StreamFormat.RGB8, FRAME_RATE)
                config.enableStream(StreamType.DEPTH, 0, DEPTH_WIDTH, DEPTH_HEIGHT, StreamFormat.Z16, FRAME_RATE)
                pipeline?.start(config)?.device
            } catch (e: Exception) {
                debug("Start Stream Error: ${e.message}")
                mIsStreaming = false
                null
            }
        }
    }

    private fun startStreamThread() {
        if (mIsStreaming) return
        debug("try start streaming")
        try {
            mIsStreaming = true
            mDevice = initDevice()
            if (mDevice != null) {
                mHandler?.post(mStreaming)
                debug("streaming started successfully")
            }
        } catch (e: Throwable) {
            debug("failed to start streaming : ${e.message}")
        }

    }

    private fun stopStreamThread() {
        if (!mIsStreaming) return
        debug("try stop streaming")
        try {
            mIsStreaming = false
            pipeline?.stop()
            debug("streaming stopped successfully")
        } catch (e: Throwable) {
            debug("failed to stop streaming : ${e.message}")
            pipeline = null
        }
    }

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
                        isProcessingFrame = true
                        val frames: FrameSet = pipeline!!.waitForFrames(TIME_WAIT).releaseWith(fr)
                        if (isFrameOK) {
                            mFrameCount--
                            when {
                                mFrameCount > 0 -> {
                                    val colorFrame = frames.first(StreamType.COLOR).releaseWith(fr)
                                    val processFrame = frames.applyFilter(colorizer).releaseWith(fr)
                                    val depthFrame =
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
                hardwareReset()
            }

        }
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