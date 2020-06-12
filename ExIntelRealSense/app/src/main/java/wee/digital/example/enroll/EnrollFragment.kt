package wee.digital.example.enroll

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import kotlinx.android.synthetic.main.enroll.*
import wee.digital.camera.RealSenseLiveData
import wee.digital.camera.replaceFragment
import wee.digital.example.R
import wee.digital.example.member.PortraitFragment

class EnrollFragment : Fragment(),
        EnrollJob.Listener {

    private val faceAuthJob: EnrollJob = EnrollJob(this)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.enroll, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onStartFaceDetect()
    }

    override fun onDestroyView() {
        disposeEnrollRequest()
        super.onDestroyView()
    }

    /**
     * [EnrollJob.Listener] implement
     */
    override fun onFaceDetected(bitmap: Bitmap) {
        onEnrollRequest(bitmap)
    }

    override fun onFaceLeaved() {
        disposeEnrollRequest()
    }

    /**
     * util
     */
    private fun onStartFaceDetect() {
        faceAuthJob.observe(this)
        RealSenseLiveData.instance.observe(this, Observer {
            imageViewPreview?.setBitmap(it)
        })
    }





    /**
     * An emulator network enroll request delayed 5 seconds
     * Replace with your presenter or view model or controller
     */
    private var isOnRequest: Boolean = false

    private val enrollHandler: Handler = Handler()

    private var enrollRunnable: Runnable? = null

    private fun onEnrollRequest(bitmap: Bitmap) {
        if (isOnRequest) return
        isOnRequest = true
        enrollRunnable = Runnable {
            faceAuthJob.pauseDetect()
            isOnRequest = false
            activity?.replaceFragment(
                PortraitFragment.newInstance(bitmap),
                R.id.fragmentContainer,
                true
            )
        }
        progressBar?.visibility = View.VISIBLE
        enrollHandler.postDelayed(enrollRunnable, 5000)
    }

    private fun disposeEnrollRequest()  {
        isOnRequest = false
        progressBar?.visibility = View.INVISIBLE
        enrollRunnable?.also {
            enrollHandler.removeCallbacks(it)
        }
    }

}