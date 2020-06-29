#include <jni.h>
#include <memory.h>
#include <librealsense2/h/rs_internal.h>
#include "include/librealsense2/rs.hpp"
#include "include/librealsense2/h/rs_pipeline.h"
#include "include/librealsense2/rs.h"
#include "include/librealsense2/h/rs_config.h"

rs2::pipeline *pipeline;

extern "C" JNIEXPORT void JNICALL
Java_wee_digital_camera_RealSense_nStart(JNIEnv *env, jobject) {
    rs2::config *config = new rs2::config();
    config->enable_stream(rs2_stream::RS2_STREAM_COLOR, 0,
                          1280, 720,
                          rs2_format::RS2_FORMAT_RGB8,
                          10);
    /*config->enable_stream(rs2_stream::RS2_STREAM_DEPTH, 0,
                          640, 480,
                          rs2_format::RS2_FORMAT_Z16,
                          10);*/
    pipeline = new rs2::pipeline();
    pipeline->start(*config);
}

extern "C" JNIEXPORT void JNICALL
Java_wee_digital_camera_RealSense_nStop(JNIEnv *env, jobject) {
    if (NULL != pipeline) {
        pipeline->stop();
    }
}

extern "C" JNIEXPORT void JNICALL
Java_wee_digital_camera_RealSense_nWaitForFrames(JNIEnv *env, jobject, jbyteArray raw) {
    if (NULL == pipeline) {
        return;
    }
    rs2::frameset frameSet = pipeline->wait_for_frames(5000);
    if (NULL == frameSet) {
        return;
    }
    int frameSetCount = frameSet.size();
    if (frameSetCount == 0) {
        return;
    }
    rs2::frame frame = frameSet.get_color_frame();
    if (NULL == frame) {
        return;
    }
    jsize length = env->GetArrayLength(raw);
    auto rgb_frame_data = frame.get_data();
    env->SetByteArrayRegion(raw, 0, length, static_cast<const jbyte *>(rgb_frame_data));

}



