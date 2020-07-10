package wee.digital.camera.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.widget_camera_view.view.*
import wee.digital.camera.ColorSensor
import wee.digital.camera.DepthSensor
import wee.digital.camera.R
import java.io.ByteArrayOutputStream

class CameraView : ConstraintLayout {

    constructor(context: Context) : super(context) {
        onViewInit(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        onViewInit(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
            defStyleAttr
    ) {
        onViewInit(context)
    }

    private fun onViewInit(context: Context) {
        LayoutInflater.from(context).inflate(R.layout.widget_camera_view, this)
    }

    fun observe(lifecycleOwner: LifecycleOwner) {
        ColorSensor.instance.liveData.observe(lifecycleOwner, Observer<Bitmap?> {
            imageViewColor.setBitmap(it)
        })
        DepthSensor.instance.liveData.observe(lifecycleOwner, Observer<Bitmap?> {
            imageViewDepth.setImageBitmap(it)
        })
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

    fun targetFace(left: Int, top: Int = 0, width: Int = 0, height: Int = 0) {
        if (left < 0) {
            imageViewCensored.visibility = View.INVISIBLE
            return
        }
        imageViewCensored.visibility = View.VISIBLE
        val set = ConstraintSet()
        set.clone(viewPreview)

        set.connect(imageViewCensored.id, ConstraintSet.START,
                ConstraintSet.PARENT_ID, ConstraintSet.START,
                viewPreview.width * left / ColorSensor.WIDTH)

        set.connect(imageViewCensored.id, ConstraintSet.END,
                ConstraintSet.PARENT_ID, ConstraintSet.END,
                viewPreview.width - (viewPreview.width * (left + width) / ColorSensor.WIDTH))

        set.connect(imageViewCensored.id, ConstraintSet.TOP,
                ConstraintSet.PARENT_ID, ConstraintSet.TOP,
                viewPreview.height * top / ColorSensor.HEIGHT
        )
        set.connect(imageViewCensored.id, ConstraintSet.BOTTOM,
                ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM,
                viewPreview.height - (viewPreview.height * (top + height) / ColorSensor.HEIGHT)
        )
        set.applyTo(viewPreview)
    }

}