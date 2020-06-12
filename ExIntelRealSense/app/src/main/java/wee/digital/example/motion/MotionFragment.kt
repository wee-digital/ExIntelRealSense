package wee.digital.example.motion

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.motion.widget.MotionLayout
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.motion.*
import wee.digital.example.R

class MotionFragment : Fragment(),
    MotionLayout.TransitionListener,
    MotionJob.Listener {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.motion, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        motionLayout.setTransitionListener(this)
    }


    /**
     * [MotionLayout.TransitionListener] implement
     */
    override fun onTransitionTrigger(p0: MotionLayout?, p1: Int, p2: Boolean, p3: Float) {
    }

    override fun onTransitionStarted(p0: MotionLayout?, p1: Int, p2: Int) {
    }

    override fun onTransitionChange(p0: MotionLayout?, p1: Int, p2: Int, p3: Float) {
    }

    override fun onTransitionCompleted(p0: MotionLayout?, p1: Int) {
        if (p1 == R.xml.enroll_scene) {
            println("")
        }
    }

    /**
     * [MotionJob.Listener] implement
     */

    override fun onFaceDetected(raw: ByteArray) {
    }

    override fun onFaceLeaved() {
    }


}