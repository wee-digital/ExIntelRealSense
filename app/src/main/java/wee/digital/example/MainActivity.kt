package wee.digital.example

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import wee.digital.camera.ColorSensor
import wee.digital.camera.DepthSensor
import wee.digital.camera.RealSense
import wee.digital.camera.replaceFragment
import wee.digital.example.ui.AutoEnrollFragment
import wee.digital.example.ui.DetectFragment


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        onClicksInit()
        //cameraView.observe(this)
        RealSense.nReset()
        RealSense.streamMemory { textViewMemory.text = it }
    }

    private fun onClicksInit() {

        viewReset.setOnClickListener {
            RealSense.nReset()
        }
        viewDetect.setOnClickListener {
            replaceFragment(DetectFragment(), R.id.fragmentContainer, true)
        }
        viewAutoEnroll.setOnClickListener {
            replaceFragment(AutoEnrollFragment(), R.id.fragmentContainer, true)
        }


        viewEnroll.setOnClickListener {
        }
        viewMotionEnroll.setOnClickListener {
        }
        viewCapturePortrait.setOnClickListener {
        }


        viewColorPipeStart.setOnClickListener {
            ColorSensor.instance.startPipeline()
        }
        viewColorPipeStop.setOnClickListener {
            ColorSensor.instance.stopPipeline()
        }
        viewColorStreamStart.setOnClickListener {
            ColorSensor.instance.startStream()
        }
        viewColorStreamStop.setOnClickListener {
            ColorSensor.instance.stopStream()
        }
        viewColorCapture.setOnClickListener {
            ColorSensor.instance.capture { ColorSensor.instance.liveData.postValue(it) }
        }


        viewDepthPipeStart.setOnClickListener {
            DepthSensor.instance.startPipeline()
        }
        viewDepthPipeStop.setOnClickListener {
            DepthSensor.instance.stopPipeline()
        }
        viewDepthStreamStart.setOnClickListener {
            DepthSensor.instance.startStream()
        }
        viewDepthStreamStop.setOnClickListener {
            DepthSensor.instance.stopStream()
        }
        viewDepthCapture.setOnClickListener {
            DepthSensor.instance.capture { DepthSensor.instance.liveData.postValue(it) }
        }

    }

}
