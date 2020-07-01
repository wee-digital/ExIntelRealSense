package wee.digital.example.detect

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.face_detect.*
import wee.digital.camera.job.DebugDetectJob
import wee.digital.example.R


class FaceDetectFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.face_detect, container, false)
        view.setOnTouchListener { _, _ -> true }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        cameraView.observe(this)

        DebugDetectJob(detectorView).observe(this)

        detectorView.faceRectListener = { left, top, width, height ->
            cameraView.targetFace(left, top, width, height)
        }
    }

}