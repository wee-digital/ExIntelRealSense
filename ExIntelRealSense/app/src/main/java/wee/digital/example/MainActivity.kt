package wee.digital.example

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import wee.digital.camera.addFragment
import wee.digital.example.debug.DebugFragment
import wee.digital.example.enroll.EnrollFragment


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        viewDebug.setOnClickListener {
            addFragment(DebugFragment(), R.id.fragmentContainer, true)
        }
        viewEnroll.setOnClickListener {
            addFragment(EnrollFragment(), R.id.fragmentContainer, true)
        }
        viewAutoEnroll.setOnClickListener {
            Toast.makeText(this, "chua code xong", Toast.LENGTH_SHORT).show()
        }
        viewMotionEnroll.setOnClickListener {
            Toast.makeText(this, "chua code xong", Toast.LENGTH_SHORT).show()
        }
        viewCapturePortrait.setOnClickListener {
            Toast.makeText(this, "chua code xong", Toast.LENGTH_SHORT).show()
        }
    }
}
