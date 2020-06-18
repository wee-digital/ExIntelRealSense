package wee.digital.example.enroll

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.*
import wee.digital.camera.RealSense
import wee.digital.camera.core.RealSenseControl
import wee.digital.camera.detector.FaceDetector
import java.util.concurrent.atomic.AtomicInteger

class EnrollJob(private var uiListener: Listener) :
    RealSenseControl.Listener,
    FaceDetector.OptionListener,
    FaceDetector.StatusListener {

    private val detector: FaceDetector = FaceDetector().also {
        it.optionListener = this
        it.statusListener = this
    }

    private var noneFaceCount = AtomicInteger()

    private var invalidFaceCount = AtomicInteger()

    private var hasDetect: Boolean = false

    fun observe(lifecycleOwner: LifecycleOwner) {
        RealSense.controlLiveData.observe(lifecycleOwner, Observer<RealSenseControl?> {
            it ?: return@Observer
            RealSense.listener = this
            detector.start()
        })

        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun create() {
                startDetect()
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun destroy() {
                hasDetect = false
                RealSense.listener = null
                detector.destroy()
            }
        })
    }

    fun startDetect() {
        noneFaceCount.set(0)
        invalidFaceCount.set(0)
        hasDetect = true
    }

    fun pauseDetect() {
        hasDetect = false
    }


    /**
     * [RealSenseControl.Listener] implement
     */
    override fun onCameraData(colorBitmap: Bitmap?, depthBitmap: Bitmap?) {
        if (colorBitmap == null || depthBitmap == null) {
            return
        }
        if (hasDetect) {
            detector.detectFace(colorBitmap, depthBitmap)
        }
    }


    /**
     * [FaceDetector.OptionListener] implement
     */
    override fun onFaceScore(score: Float): Boolean {
        return score > 0.9
    }

    override fun onFaceRect(left: Int, top: Int, width: Int, height: Int): Boolean {
        return width > 90
    }

    override fun onFaceDegrees(x: Double, y: Double): Boolean {
        return x in -45f..45f && y in -45f..45f
    }


    /**
     * [FaceDetector.StatusListener] implement
     */
    override fun onFacePerformed() {
        RealSense.hasFace()
        invalidFaceCount.set(0)
        noneFaceCount.set(0)
    }

    override fun onFaceLeaved() {
        invalidFaceCount.set(0)
        if (noneFaceCount.incrementAndGet() < 3) return
        mainThread {
            uiListener.onFaceLeaved()
        }
    }

    override fun onFaceChanged() {
        noneFaceCount.set(0)
        invalidFaceCount.set(0)
    }

    override fun onPortraitImage(image: Bitmap, portrait: Bitmap) {
        if (!hasDetect || portrait == null) return
        noneFaceCount.set(0)
        mainThread {
            uiListener.onFaceDetected(image, portrait)
        }
    }


    private fun mainThread(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block()
        else Handler(Looper.getMainLooper()).post { block() }
    }


    /**
     * UI callback
     */
    interface Listener {

        fun onFaceDetected(image: Bitmap, portrait: Bitmap)

        fun onFaceLeaved()

    }

}