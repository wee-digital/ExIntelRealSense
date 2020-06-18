package wee.digital.camera.widget

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import kotlinx.android.synthetic.main.widget_camera_view.view.*
import wee.digital.camera.R
import wee.digital.camera.core.RealSenseControl
import wee.digital.camera.job.DebugJob

class CameraView : ConstraintLayout, DebugJob.UiListener {

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


    /**
     * [DebugJob.UiListener] implement
     */
    override fun onFaceScore(score: Float): Boolean {
        textViewScore.text = score.toString()
        return true
    }

    override fun onFaceRect(left: Int, top: Int, width: Int, height: Int): Boolean {
        textViewLeft.text = left.toString()
        textViewTop.text = top.toString()
        textViewWidth.text = width.toString()
        textViewHeight.text = height.toString()
        censoredFace(left, top, width, height)
        return true
    }

    override fun onFaceDegrees(x: Double, y: Double): Boolean {
        textViewDegrees.text = "x %.2f,  y %.2f".format(x, y)
        return true
    }

    override fun onMaskLabel(label: String, confidence: Float): Boolean {
        textViewMaskLabel.text = label
        return true
    }

    override fun onDepthLabel(label: String, confidence: Float): Boolean {
        textViewDepthLabel.text = label
        return true
    }

    override fun onFullColorImage(bitmap: Bitmap) {
        imageViewPreview.setBitmap(bitmap)
    }

    override fun onFullDepthImage(bitmap: Bitmap) {
        imageViewDepth.setImageBitmap(bitmap)
    }

    override fun onFaceColorImage(bitmap: Bitmap?) {
        imageViewCropColor.setImageBitmap(bitmap)
    }

    override fun onFaceDepthImage(bitmap: Bitmap?) {
        imageViewCropDepth.setImageBitmap(bitmap)
    }

    override fun onPortraitImage(image: Bitmap, portrait: Bitmap) {
        imageViewPortrait?.setImageBitmap(portrait)
    }

    override fun onFaceLeaved() {
        imageViewCensored.visibility = View.INVISIBLE
        textViewFaceStatus.text = "Leaved"
    }

    override fun onFaceChanged() {
        textViewFaceStatus.text = "Changed"
    }

    override fun onFacePerformed() {
        imageViewCensored.visibility = View.VISIBLE
        textViewFaceStatus.text = "Performed"
    }

    private fun censoredFace(left: Int, top: Int, width: Int, height: Int) {

        val set = ConstraintSet()
        set.clone(viewPreview)
        set.connect(
            imageViewCensored.id,
            ConstraintSet.START,
            ConstraintSet.PARENT_ID,
            ConstraintSet.START,
            viewPreview.width * left / RealSenseControl.COLOR_WIDTH
        )
        set.connect(
            imageViewCensored.id,
            ConstraintSet.END,
            ConstraintSet.PARENT_ID,
            ConstraintSet.END,
            viewPreview.width - (viewPreview.width * (left + width) / RealSenseControl.COLOR_WIDTH)
        )

        set.connect(
            imageViewCensored.id,
            ConstraintSet.TOP,
            ConstraintSet.PARENT_ID,
            ConstraintSet.TOP,
            viewPreview.height * top / RealSenseControl.COLOR_HEIGHT
        )
        set.connect(
            imageViewCensored.id,
            ConstraintSet.BOTTOM,
            ConstraintSet.PARENT_ID,
            ConstraintSet.BOTTOM,
            viewPreview.height - (viewPreview.height * (top + height) / RealSenseControl.COLOR_HEIGHT)
        )
        set.applyTo(viewPreview)
    }


}