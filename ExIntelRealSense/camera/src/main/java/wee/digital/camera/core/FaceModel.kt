package wee.digital.camera.core

import android.graphics.Point
import android.graphics.Rect

data class FaceModel(

        var faceRect: Rect,

        var rightEye: Point,

        var leftEye: Point,

        var nose: Point,

        var rightMouth: Point,

        var leftMouth: Point
)