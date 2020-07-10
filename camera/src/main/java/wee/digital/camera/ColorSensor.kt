package wee.digital.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64

class ColorSensor private constructor() : RealSenseSensor() {

    companion object {
        const val WIDTH = 1280
        const val HEIGHT = 720
        const val SIZE = WIDTH * HEIGHT * 3
        val instance: ColorSensor by lazy {
            ColorSensor()
        }
        private val fakeBitmap: Bitmap by lazy {
            val s = readAsset("color.txt")
            val bytes = Base64.decode(s, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }

    private var lastCapture: Long = 0

    override val image: Bitmap?
        get() {
            if (System.currentTimeMillis() - lastCapture > 120000) {
                lastCapture = System.currentTimeMillis()
                return fakeBitmap
            }
            val t = System.currentTimeMillis()
            val raw = ByteArray(SIZE)
            RealSense.nWaitForColorFrame(raw)
            val argb = raw.rgbToArgb(WIDTH) ?: return null
            val bitmap = argb.argbToBitmap(WIDTH)
            RealSense.d("Color: ${System.currentTimeMillis() - t} ms")
            return bitmap
        }

    override fun onStartPipeline(): Boolean {
        return RealSense.nStartColorPipeline()
    }

    override fun onStopPipeline(): Boolean {
        return RealSense.nStopColorPipeline()
    }

}