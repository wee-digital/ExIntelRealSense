package wee.digital.example.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.debug.*
import wee.digital.camera.job.DebugJob
import wee.digital.example.R


class DebugFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.debug, container, false)
        view.setOnTouchListener { _, _ -> true }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        DebugJob(cameraView).startRecord(this)
    }

}