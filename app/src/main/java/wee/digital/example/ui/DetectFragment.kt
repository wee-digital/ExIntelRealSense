package wee.digital.example.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.detect.*
import wee.digital.example.R


class DetectFragment : Fragment() {

    override fun onCreateView(li: LayoutInflater, vg: ViewGroup?, b: Bundle?): View? {
        val view = li.inflate(R.layout.detect, vg, false)
        view.setOnTouchListener { _, _ -> true }
        return view
    }

    override fun onViewCreated(v: View, b: Bundle?) {
        detectorView.observe(this)
    }

}