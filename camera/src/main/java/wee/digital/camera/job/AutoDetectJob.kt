package wee.digital.camera.job

import android.graphics.Bitmap
import androidx.lifecycle.*
import wee.digital.camera.ColorSensor
import wee.digital.camera.DepthSensor
import wee.digital.camera.detector.FaceDetector
import wee.digital.camera.uiThread
import java.util.concurrent.atomic.AtomicInteger

/**
 * [FaceDetector.DataListener], [FaceDetector.OptionListener], [FaceDetector.StatusListener]
 * face enroll job callback wrapper on UI
 */
class AutoDetectJob(private var uiListener: Listener) :
        FaceDetector.DataListener,
        FaceDetector.OptionListener,
        FaceDetector.StatusListener {

    private val noneFaceCount = AtomicInteger()

    private var hasDetect: Boolean = false

    private val detector: FaceDetector = FaceDetector().also {
        it.dataListener = this
        it.optionListener = this
        it.statusListener = this
    }

    private val colorDetector = Observer<Bitmap?> {
        it?.apply {
            detector.detectFace(it)
        }
    }

    fun observe(lifecycleOwner: LifecycleOwner, block: FaceDetector.() -> Unit = {}) {
        detector.block()
        startDetect()
        lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            fun destroy() {
                pauseDetect()
            }
        })
    }

    fun startDetect() {
        noneFaceCount.set(0)
        hasDetect = true
        ColorSensor.instance.liveData.observeForever(colorDetector)
    }

    fun pauseDetect() {
        hasDetect = false
        ColorSensor.instance.liveData.removeObserver(colorDetector)
        detector.destroy()
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

    override fun onMaskLabel(label: String, confidence: Float): Boolean {
        if (super.onMaskLabel(label, confidence)) {
            return true
        }
        onFaceLeaved()
        return false
    }

    override fun onDepthLabel(label: String, confidence: Float): Boolean {
        if (super.onDepthLabel(label, confidence)) {
            return true
        }
        onFaceLeaved()
        return false
    }


    /**
     * [FaceDetector.StatusListener] implement
     */
    override fun onFacePerformed() {
        DepthSensor.instance.startPipeline()
        noneFaceCount.set(0)
        uiThread {
            uiListener.onFacePerformed()
        }
    }

    override fun onFaceLeaved() {
        if (noneFaceCount.incrementAndGet() < 10) return
        uiThread {
            uiListener.onFaceLeaved()
        }
        if (noneFaceCount.incrementAndGet() > 200) {
            DepthSensor.instance.stopPipeline()
        }

    }

    override fun onFaceChanged() {
        noneFaceCount.set(0)
    }


    /**
     * [FaceDetector.DataListener] implement
     */

    override fun onPortraitImage(bitmap: Bitmap) {
        if (!hasDetect) return
        noneFaceCount.set(0)
        uiThread {
            uiListener.onFaceDetected(bitmap)
        }
    }

    override fun onFaceColorImage(bitmap: Bitmap?) {
        uiThread {
            uiListener.onFaceColorImage(bitmap)
        }
    }

    override fun onFaceDepthImage(bitmap: Bitmap?) {
        uiThread {
            uiListener.onFaceDepthImage(bitmap)
        }
    }

    /**
     * UI callback
     */
    interface Listener {

        fun onFaceDetected(bitmap: Bitmap)

        fun onFaceColorImage(bitmap: Bitmap?)

        fun onFaceDepthImage(bitmap: Bitmap?)

        fun onFacePerformed()

        fun onFaceLeaved()

    }

}