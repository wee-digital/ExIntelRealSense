package wee.digital.camera.job

import android.graphics.Bitmap
import androidx.lifecycle.*
import wee.digital.camera.ColorSensor
import wee.digital.camera.detector.FaceDetector
import wee.digital.camera.uiThread
import java.util.concurrent.atomic.AtomicInteger

/**
 * [FaceDetector.DataListener], [FaceDetector.OptionListener], [FaceDetector.StatusListener]
 * face enroll job callback wrapper on UI
 */
class FaceDetectJob(private var uiListener: Listener) :
        FaceDetector.DataListener,
        FaceDetector.OptionListener,
        FaceDetector.StatusListener {

    private val noneFaceCount = AtomicInteger()

    private val invalidFaceCount = AtomicInteger()

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

    fun observe(lifecycleOwner: LifecycleOwner) {
        detector.release()
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
        invalidFaceCount.set(0)
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
        onFaceInvalid()
        return false
    }

    override fun onDepthLabel(label: String, confidence: Float): Boolean {
        if (super.onDepthLabel(label, confidence)) {
            return true
        }
        onFaceInvalid()
        return false
    }


    /**
     * [FaceDetector.StatusListener] implement
     */
    override fun onFacePerformed() {
        invalidFaceCount.set(0)
        noneFaceCount.set(0)
    }

    override fun onFaceLeaved() {
        invalidFaceCount.set(0)
        if (noneFaceCount.incrementAndGet() < 10) return
        uiThread {
            uiListener.onFaceLeaved()
        }
    }

    override fun onFaceChanged() {
        noneFaceCount.set(0)
        invalidFaceCount.set(0)
    }

    override fun onPortraitImage(bitmap: Bitmap) {
        if (!hasDetect) return
        noneFaceCount.set(0)
        uiThread {
            uiListener.onFaceDetected(bitmap)
        }
    }


    /**
     *
     */
    private fun onFaceInvalid() {
        noneFaceCount.set(0)
        if (!hasDetect) return
        if (invalidFaceCount.incrementAndGet() < 10) return
        uiThread {
            uiListener.onFaceInvalid()
        }
    }


    /**
     * UI callback
     */
    interface Listener {

        fun onFaceDetected(bitmap: Bitmap)

        fun onFaceLeaved()

        fun onFaceInvalid(message: String? = null)

    }

}