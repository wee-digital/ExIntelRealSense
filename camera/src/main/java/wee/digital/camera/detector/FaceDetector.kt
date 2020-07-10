package wee.digital.camera.detector

import android.graphics.Bitmap
import android.graphics.PointF
import android.graphics.Rect
import wee.digital.camera.*

class FaceDetector {

    companion object {
        const val MIN_DISTANCE = 120
        const val MIN_SIZE = 300
    }

    val maskFilter = ModelFilter("face/mask/manifest.json")

    val depthFilter = ModelFilter("face/depth/manifest.json")

    private val mtcnn: MTCNN = MTCNN(RealSense.app.assets)

    private var currentFace: Box? = null

    private var isDetecting: Boolean = false

    var dataListener: DataListener? = null

    var statusListener: StatusListener? = null

    var optionListener: OptionListener = object : OptionListener {}

    var currentColorImage: Bitmap? = null

    fun release() {
        currentFace = null
    }

    fun detectFace(colorBitmap: Bitmap) {
        if (isDetecting) return
        isDetecting = true
        mtcnn.detectFacesAsync(colorBitmap, MIN_SIZE)
                .addOnCanceledListener { isDetecting = false }
                .addOnFailureListener { statusListener?.onFaceLeaved() }
                .addOnCompleteListener { task ->
                    val box: Box? = task.result.largestBox()
                    if (box == null) {
                        statusListener?.onFaceLeaved()
                        isDetecting = false
                    } else {
                        if (!faceChangeProcess(box)) statusListener?.onFaceChanged()
                        currentColorImage = colorBitmap
                        currentFace = box
                        onFaceDetect(box, colorBitmap)
                    }
                }
    }

    fun destroy() {
        depthFilter.destroy()
        maskFilter.destroy()
    }

    /**
     * Detect method 1st: use [OptionListener] filter face properties to continue [onMaskDetect]
     */
    private fun onFaceDetect(box: Box, colorBitmap: Bitmap) {

        if (!optionListener.onFaceScore(box.score)) {
            isDetecting = false
            return
        }

        if (!optionListener.onFaceRect(box.left(), box.top(), box.faceWidth(), box.faceHeight())) {
            isDetecting = false
            return
        }

        statusListener?.onFacePerformed()

        var degreesValid = false
        box.getDegrees { x, y -> degreesValid = optionListener.onFaceDegrees(x, y) }
        if (!degreesValid) {
            isDetecting = false
            return
        }

        val boxRect = box.transformToRect()
        onMaskFilter(boxRect, colorBitmap)
    }


    /**
     * Detect method 2nd: use [maskFilter] crop face color image
     * and get vision label to continue [onDepthDetect]
     */
    private fun onMaskFilter(boxRect: Rect, colorBitmap: Bitmap) {
        if (!maskFilter.isEnable) {
            optionListener.onMaskLabel(ModelFilter.ANY, 100f)
            onDepthFilter(boxRect)
            return
        }
        val faceBitmap = boxRect.cropColorFace(colorBitmap)
        if (faceBitmap != null) {
            onMaskDetect(boxRect, faceBitmap)
        } else {
            isDetecting = false
        }

    }

    private fun onMaskDetect(boxRect: Rect, faceBitmap: Bitmap) {
        dataListener?.onFaceColorImage(faceBitmap)
        maskFilter.processImage(faceBitmap) { text, confidence ->
            if (text != null && optionListener.onMaskLabel(text, confidence)) {
                onDepthFilter(boxRect)
            } else {
                isDetecting = false
            }
        }
    }


    /**
     * Detect method 3rd: use [depthFilter] crop face depth image
     * and get vision label to continue [onGetPortrait]
     */
    private fun onDepthFilter(boxRect: Rect) {
        if (!depthFilter.isEnable) {
            optionListener.onDepthLabel(ModelFilter.ANY, 100f)
            onGetPortrait(boxRect)
            return
        }
        DepthSensor.instance.capture { bitmap ->
            val faceBitmap = boxRect.cropDepthFace(bitmap)
            if (faceBitmap != null) {
                onDepthDetect(boxRect, faceBitmap)
            } else {
                isDetecting = false
            }
        }
    }

    private fun onDepthDetect(boxRect: Rect, faceBitmap: Bitmap) {
        dataListener?.onFaceDepthImage(faceBitmap)
        depthFilter.processImage(faceBitmap) { text, confidence ->
            if (text != null && optionListener.onDepthLabel(text, confidence)) {
                onGetPortrait(boxRect)
            } else {
                isDetecting = false
            }
        }
    }


    /**
     * Detect method 4th: detected face portrait
     */
    private fun onGetPortrait(boxRect: Rect) {
        val bitmap = currentColorImage ?: return
        boxRect.cropPortrait(bitmap)?.also {
            dataListener?.onPortraitImage(it)
        }
        isDetecting = false
    }

    private fun faceChangeProcess(face: Box): Boolean {
        currentFace ?: return false
        val nowRect = face.transformToRect()
        val nowCenterX = nowRect.exactCenterX()
        val nowCenterY = nowRect.exactCenterY()
        val nowCenterPoint = PointF(nowCenterX, nowCenterY)
        val curRect = currentFace!!.transformToRect()
        val curCenterX = curRect.exactCenterX()
        val curCenterY = curRect.exactCenterY()
        val curCenterPoint = PointF(curCenterX, curCenterY)
        val dist = distancePoint(nowCenterPoint, curCenterPoint)
        return dist < MIN_DISTANCE
    }


    /**
     * Callback methods return full size image, face crop image of color image & depth image
     */
    interface DataListener {

        fun onFaceColorImage(bitmap: Bitmap?) {}

        fun onFaceDepthImage(bitmap: Bitmap?) {}

        fun onPortraitImage(bitmap: Bitmap)
    }

    /**
     * Callback methods when detect on a pair of color image & depth image
     */
    interface StatusListener {

        fun onFacePerformed()

        fun onFaceLeaved()

        fun onFaceChanged() {}

    }

    /**
     * Face detector option filter to get a portrait image if all method return true
     */
    interface OptionListener {

        fun onMaskLabel(label: String, confidence: Float): Boolean = label == "face_chip"

        fun onDepthLabel(label: String, confidence: Float): Boolean = label == "real"

        fun onFaceScore(score: Float): Boolean = true

        fun onFaceRect(left: Int, top: Int, width: Int, height: Int): Boolean = true

        fun onFaceDegrees(x: Double, y: Double): Boolean = true

    }

}