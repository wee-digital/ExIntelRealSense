package wee.digital.camera

import android.app.Application
import android.graphics.*
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.intel.realsense.librealsense.RsContext
import com.intel.realsense.librealsense.UsbUtilities
import wee.digital.camera.core.Box
import wee.digital.camera.core.FaceModel
import wee.digital.camera.core.RealSenseControl
import java.io.ByteArrayOutputStream
import java.util.*
import kotlin.math.*

class Camera {

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

        val instance: Camera by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            RsContext.init(mApp)
            UsbUtilities.grantUsbPermissionIfNeeded(mApp)
            Camera()
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

    private fun convertPoint(oldPoint: Point?, faceRect: Rect, plusW: Int, plusH: Int): Point {
        oldPoint ?: return Point(0, 0)
        val old_X = oldPoint.x
        val old_Y = oldPoint.y
        val new_X = old_X - faceRect.left + plusW / 2
        val new_Y = old_Y - faceRect.top + plusH / 2
        return Point(new_X, new_Y)
    }

    private fun getDataFace(face: Box, plusW: Int, plusH: Int): FaceModel {
        val newRect = Rect(0 + plusW / 2, 0 + plusH / 2, face.width() + plusW / 2, face.height() + plusH / 2)
        val listNewPoint = arrayListOf<Point>()
        for (point in face.landmark) {
            val newP = convertPoint(point, face.transformToRect(), plusW, plusH)
            listNewPoint.add(newP)
        }
        return FaceModel(
                newRect,
                listNewPoint[1],
                listNewPoint[0],
                listNewPoint[2],
                listNewPoint[4],
                listNewPoint[3]
        )
    }

    private fun getDataFace(face: Box): FaceModel {
        val listNewPoint = arrayListOf<Point>()
        for (point in face.landmark) {
            listNewPoint.add(point)
        }
        return FaceModel(
                face.transformToRect(),
                listNewPoint[1],
                listNewPoint[0],
                listNewPoint[2],
                listNewPoint[4],
                listNewPoint[3])
    }

    fun checkFaceZone(face: Box): Boolean {
        val left = face.box[0]
        val right = face.box[2]
        val top = face.box[1]
        val bot = face.box[3]
        val x = (left + right) * 0.5f
        val y = (top + bot) * 0.5f
        return x in 275f..355f && y in 225f..300f
    }

}


/**
 * Box
 */
fun Vector<Box>?.largestBox(): Box? {
    if (this.isNullOrEmpty()) return null
    var largestFace: Box? = null
    this.forEach {
        if (largestFace == null) {
            largestFace = it
        } else if (largestFace!!.width() < it.width()) {
            largestFace = it
        }
    }
    return largestFace
}

fun Box.faceWidth(): Int {
    val right = this.box[2]
    val left = this.box[0]
    return right - left
}

fun Box.faceHeight(): Int {
    val bottom = this.box[3]
    val top = this.box[1]
    return bottom - top
}

fun Box.getDegrees(block: (Double, Double) -> Unit) {
    val rect = this.landmark
    val leftEye = rect[0]
    val rightEye = rect[1]
    val nose = rect[2]
    val rightMouth = rect[4]
    val leftMouth = rect[3]
    val x = getFaceDegreeX(leftEye, rightEye, nose, leftMouth, rightMouth)
    val y = getFaceDegreeY(leftEye, rightEye, nose, leftMouth, rightMouth)
    block(x, y)
}

fun Rect.getRectCrop(bitmap: Bitmap): Rect {
    val top = if (top < 0) 0 else top
    val left = if (left < 0) 0 else left
    val right = if (right > bitmap.width) bitmap.width else right
    val bottom = if (bottom > bitmap.height) bitmap.height else bottom
    return Rect(left, top, right, bottom)
}


/**
 * Crop face portrait image
 * @this: box.transformToRect
 */
fun Rect.cropPortrait(bitmap: Bitmap): Bitmap? {

    val plusH = height() * 0.15
    val plusW = width() * 0.15
    val height = height() + plusH.roundToInt()
    val width = width() + plusW.roundToInt()

    var top = top - (plusH / 2).roundToInt()
    var left = left - (plusW / 2).roundToInt()
    if (top < 0) top = 0
    if (left < 0) left = 0

    val newRect = Rect(left, top, left + width, top + height)
    val rectCrop = newRect.getRectCrop(bitmap)
    val copiedBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
    return try {
        val crop = Bitmap.createBitmap(
                copiedBitmap,
                rectCrop.left,
                rectCrop.top,
                rectCrop.width(),
                rectCrop.height()
        )
        crop
    } catch (t: Throwable) {
        null
    }
}


/**
 * Crop face color image
 * @this: box.transformToRect
 */
fun Rect.cropColorFace(bitmap: Bitmap): Bitmap? {
    val rect = this.getRectCrop(bitmap)
    val copiedBitmap = bitmap.copy(Bitmap.Config.RGB_565, true)
    return try {
        Bitmap.createBitmap(copiedBitmap, rect.left, rect.top, rect.width(), rect.height())
    } catch (ex: Exception) {
        Log.e("cropFace", "${ex.message}")
        null
    }
}


/**
 * Crop face depth image
 * @this: box.transformToRect
 */
fun Rect.cropDepthFace(bitmap: Bitmap): Bitmap? {
    val rect = this.getFace1280x720()
    var top = rect.top
    if (top < 0) {
        top = 0
    }
    var left = rect.left
    if (left < 0) {
        left = 0
    }
    val height = rect.height()
    val width = rect.width()
    var x = left
    var y = top
    if (x < 0) x = 0
    if (y < 0) y = 0

    return try {
        val cropBitmap = Bitmap.createBitmap(bitmap, x, y, width, height)
        cropBitmap
    } catch (ex: Exception) {
        null
    }
}

fun Rect.getFace1920x1080(): Rect {
    val leftCorner = 1
    val topCorner = 35
    val scale = 0.36
    val x = this.exactCenterX() * scale + leftCorner / scale
    val y = this.exactCenterY() * scale + topCorner
    val width = this.width() * scale
    val height = this.height() * scale
    val left = x - width / 2
    val top = y - height / 2
    val right = x + width / 2
    val bottom = y + height / 2
    return Rect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())
}

fun Rect.getFace1280x720(): Rect {
    val leftCorner = -13
    val topCorner = 29
    val scale = 0.58
    val x = this.exactCenterX() * scale + leftCorner / scale
    val y = this.exactCenterY() * scale + topCorner
    val width = this.width() * scale
    val height = this.height() * scale
    val left = x - width / 2
    val top = y - height / 2
    val right = x + width / 2
    val bottom = y + height / 2
    return Rect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())
}

fun Rect.getFace640x320(): Rect {
    val leftCorner = 77
    val topCorner = 60
    val scale = 0.76
    val x = this.exactCenterX() * scale + leftCorner / scale
    val y = this.exactCenterY() * scale + topCorner
    val width = this.width() * scale
    val height = this.height() * scale
    val left = x - width / 2
    val top = y - height / 2
    val right = x + width / 2
    val bottom = y + height / 2
    return Rect(left.roundToInt(), top.roundToInt(), right.roundToInt(), bottom.roundToInt())
}


/**
 * Utils
 */
private fun getFaceDegreeY(pointEyeLeft: Point, pointEyeRight: Point, pointNose: Point, pointMouthLeft: Point, pointMouthRight: Point): Double {
    val pointCenterEye = getCenterPoint(pointEyeLeft, pointEyeRight)
    val pointCenterMouth = getCenterPoint(pointMouthLeft, pointMouthRight)
    val pointCenterY = getCenterPoint(pointCenterEye, pointCenterMouth)
    val rY = distancePoint(pointCenterEye, pointCenterY)
    val disOMY = distancePoint(Point(pointCenterY.x, pointNose.y), pointCenterY)
    val angleDataY = disOMY / rY
    val angleY = acos(angleDataY.toDouble())
    return if (pointNose.y < pointCenterY.y) (90 - angleY * (180 / Math.PI).toFloat()) else -(90 - angleY * (180 / Math.PI).toFloat())
}

private fun getFaceDegreeX(pointEyeLeft: Point, pointEyeRight: Point, pointNose: Point, pointMouthLeft: Point, pointMouthRight: Point): Double {
    val pointCenterEyeMouthLeft = getCenterPoint(pointEyeLeft, pointMouthLeft)
    val pointCenterEyeMouthRight = getCenterPoint(pointEyeRight, pointMouthRight)
    val pointCenterX = getCenterPoint(pointCenterEyeMouthLeft, pointCenterEyeMouthRight)
    val rX = distancePoint(pointCenterEyeMouthLeft, pointCenterX)
    val disOMX = distancePoint(Point(pointNose.x, pointCenterEyeMouthLeft.y), pointCenterX)
    val angleDataX = disOMX / rX
    val angleX = acos(angleDataX.toDouble())
    return if (pointNose.x > pointCenterX.x) (90 - angleX * (180 / Math.PI).toFloat()) else -(90 - angleX * (180 / Math.PI).toFloat())
}

private fun getCenterPoint(point1: Point, point2: Point): Point {
    val disX = abs((point1.x - point2.x)) / 2
    val x = if (point1.x > point2.x) {
        point2.x + disX
    } else {
        point1.x + disX
    }
    val disY = abs((point1.y - point2.y)) / 2
    val y = if (point1.y > point2.y) {
        point2.y + disY
    } else {
        point1.y + disY
    }
    return Point(x, y)
}

fun distancePoint(a: Point, b: Point): Float {
    return sqrt((a.x.toDouble() - b.x.toDouble()).pow(2.0) + (a.y.toDouble() - b.y.toDouble()).pow(2.0)).toFloat()
}

fun distancePoint(p1: PointF, p2: PointF): Float {
    val dist = sqrt(
            (p2.x - p1.x).toDouble().pow(2)
                    + (p2.y - p1.y).toDouble().pow(2)
    )
    return dist.toFloat()
}

/**
 * Image utils
 */
fun com.intel.realsense.librealsense.Frame?.getBitmap(size: Int, width: Int, height: Int): Bitmap? {
    this ?: return null
    return try {
        val data = ByteArray(size)
        this.getData(data)
        val argb = data.rgb8ToArgb(width, height) ?: return null
        val bmp = Bitmap.createBitmap(argb, width, height, Bitmap.Config.RGB_565)
        return bmp.flipHorizontal()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    } catch (e: Throwable) {
        e.printStackTrace()
        null
    }
}
/*
fun com.intel.realsense.librealsense.Frame?.getBitmap(width: Int, height: Int): Bitmap? {
    this ?: return null
    return try {
        val data = ByteArray(width * height)
        this.getData(data)
        val argb = data.rgb8ToArgb(width, height) ?: return null
        val bmp = Bitmap.createBitmap(argb, width, height, Bitmap.Config.RGB_565)
        return bmp.flipHorizontal()
    } catch (e: OutOfMemoryError) {
        null
    } catch (e: Exception) {
        null
    }
}
*/

fun Bitmap.flipHorizontal(): Bitmap? {
    return try {
        val matrix = Matrix()
        matrix.preScale(-1.0f, 1.0f)
        val fliped = Bitmap.createBitmap(this, 0, 0, this.width, this.height, matrix, true)
        this.recycle()
        fliped
    } catch (e: Exception) {
        null
    }
}

fun Bitmap?.toBytes(): ByteArray {
    this ?: return ByteArray(1)
    return try {
        val stream = ByteArrayOutputStream()
        this.copy(Bitmap.Config.RGB_565, true)?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val byteArray = stream.toByteArray()
        stream.close()
        byteArray
    } catch (e: Exception) {
        ByteArray(1)
    }

}

fun ByteArray.rgb8ToArgb(width: Int, height: Int): IntArray? {
    try {
        val frameSize = width * height
        val rgb = IntArray(frameSize)
        var index = 0
        for (j in 0 until height) {
            for (i in 0 until width) {
                val B = this[3 * index].toInt()
                val G = this[3 * index + 1].toInt()
                val R = this[3 * index + 2].toInt()
                rgb[index] = (R and 0xff) or (G and 0xff shl 8) or (B and 0xff shl 16)
                rgb[index] = (R and 0xff) or (G and 0xff shl 8) or (B and 0xff shl 16)
                index++
            }
        }
        return rgb
    } catch (e: Exception) {
        return null
    }

}




