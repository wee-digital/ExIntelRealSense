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

    private var colorizer: Colorizer? = null
    private var pipeline: Pipeline? = null
    private var pipelineProfile: PipelineProfile? = null

    private var colorBitmap: Bitmap? = null
    private var depthBitmap: Bitmap? = null

    private var isDestroy = false
    private var isFrameOK = false
    var isPauseCamera = false
    private var isSleep = false
    private var isProcessingFrame = false
    private var isStreaming = false

    private var mFrameCount = FRAME_MAX_COUNT

    private var mHandlerThread: HandlerThread? = null
    private var mHandler: Handler? = null
    private val streamRunnable: Runnable = object : Runnable {
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
                            pipeline!!.waitForFrames(TIME_WAIT).releaseWith(fr)
                        if (isFrameOK) {
                            //debug("Run $mFrameCount")
                            mFrameCount--
                            when {
                                mFrameCount > 0 -> {
                                    val colorFrame: Frame =
                                        frames.first(StreamType.COLOR).releaseWith(fr)
                                    val processFrame =
                                        frames.applyFilter(colorizer).releaseWith(fr)
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
        mHandlerThread = HandlerThread("streaming")?.also {
            it.start()
            mHandler = Handler(it.looper)
        }
    }

    fun onCreate() {
        colorizer = Colorizer().apply {
            setValue(Option.COLOR_SCHEME, 0f)
        }
        if (isStreaming) return
        try {
            val config = Config().apply {
                enableStream(StreamType.COLOR, 0, COLOR_WIDTH, COLOR_HEIGHT, StreamFormat.RGB8, FRAME_RATE)
                enableStream(StreamType.DEPTH, 0, DEPTH_WIDTH, DEPTH_HEIGHT, StreamFormat.Z16, FRAME_RATE)
            }
            pipeline = Pipeline()
            pipelineProfile = pipeline?.start(config)?.apply {
                isStreaming = true
                mHandler?.post(streamRunnable)
            }
        } catch (t: Throwable) {
            isStreaming = false
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

    fun hasFace() {
        mFrameCount = FRAME_MAX_COUNT
    }

    @Synchronized
    fun onPause() {
        isStreaming = false
        isDestroy = true
        mHandlerThread?.quitSafely()
        pipeline?.stop()
        pipelineProfile?.close()
    }

    @Throws
    private fun hardwareReset() {
        try {
            pipelineProfile?.device?.hardwareReset()
        } catch (ignore: RuntimeException) {
        }
    }

    interface Listener {

        fun onCameraData(colorBitmap: Bitmap?, depthBitmap: Bitmap?)

        fun onCameraStarted() {}

        fun onCameraMessage(message: String) {}
    }

}