package wee.digital.example.camera.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.enroll.*
import wee.digital.camera.ColorSensor
import wee.digital.camera.RealSense
import wee.digital.camera.job.AutoDetectJob
import wee.digital.camera.job.FaceDetectJob
import wee.digital.example.camera.R

class AutoEnrollFragment : Fragment(),
        AutoDetectJob.Listener {

    private val faceAuthJob: AutoDetectJob = AutoDetectJob(this)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.enroll, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ColorSensor.instance.startPipeline()
        ColorSensor.instance.startStream()

        faceAuthJob.observe(this) {
            maskFilter.isEnable = true
            depthFilter.isEnable = true
        }

        ColorSensor.instance.liveData.observe(this, Observer {
            imageViewPreview.setBitmap(it)
        })

    }

    override fun onDestroyView() {
        super.onDestroyView()
        RealSense.nReset()
    }

    /**
     * [FaceDetectJob.Listener] implement
     */
    override fun onFaceDetected(bitmap: Bitmap) {
        imageViewPortrait.setImageBitmap(bitmap)
    }

    override fun onFaceColorImage(bitmap: Bitmap?) {
        imageViewColorFace.setImageBitmap(bitmap)
    }

    override fun onFaceDepthImage(bitmap: Bitmap?) {
        imageViewDepthFace.setImageBitmap(bitmap)
    }

    override fun onFacePerformed() {
        AfkFragment.dismiss(activity)
    }

    override fun onFaceLeaved() {
        imageViewColorFace.setImageBitmap(null)
        imageViewDepthFace.setImageBitmap(null)
        imageViewPortrait.setImageBitmap(null)
        AfkFragment.show(activity)
    }


}