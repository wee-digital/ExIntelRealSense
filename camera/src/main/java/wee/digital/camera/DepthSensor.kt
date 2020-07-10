package wee.digital.camera

import android.graphics.Bitmap

class DepthSensor private constructor() : RealSenseSensor() {

    companion object {
        const val WIDTH = 640
        const val HEIGHT = 480
        const val SIZE = WIDTH * HEIGHT * 3
        val instance: DepthSensor by lazy {
            DepthSensor()
        }
    }

    override val image: Bitmap?
        get() {
            val t = System.currentTimeMillis()
            val raw = ByteArray(SIZE)
            RealSense.nWaitForDepthFrame(raw)
            val argb = raw.rgbToArgb(WIDTH) ?: return null
            val bitmap = argb.argbToBitmap(WIDTH)
            RealSense.d("Depth: ${System.currentTimeMillis() - t} ms")
            return bitmap
        }

    override fun onStartPipeline(): Boolean {
        return RealSense.nStartDepthPipeline()
    }

    override fun onStopPipeline(): Boolean {
        return RealSense.nStopDepthPipeline()
    }


}