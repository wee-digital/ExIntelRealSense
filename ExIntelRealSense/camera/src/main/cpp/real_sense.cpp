#include <jni.h>
#include <memory.h>
#include <librealsense2/h/rs_internal.h>
#include "include/librealsense2/rs.hpp"
#include "include/librealsense2/h/rs_pipeline.h"
#include "include/librealsense2/rs.h"
#include "include/librealsense2/h/rs_config.h"

rs2::pipeline *pipeline;
jlong pipelineJLong = 0;
jlong pipelineProfileJLong = 0;

extern "C" JNIEXPORT jstring JNICALL
Java_wee_digital_camera_RealSense_nVersion(JNIEnv *env, jobject) {
    return (*env).NewStringUTF(RS2_API_VERSION_STR);
}

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
    pipeline->stop();
    /*rs2_pipeline_profile *profile = reinterpret_cast<rs2_pipeline_profile *>(pipelineProfileJLong);
    rs2_delete_pipeline_profile(profile);
    rs2_pipeline *pipeline = reinterpret_cast<rs2_pipeline *>(pipelineJLong);
    rs2_pipeline_stop(pipeline, NULL);
    rs2_delete_pipeline(pipeline);*/
}

jint colorSize = 1280 * 720 * 3;

extern "C" JNIEXPORT void JNICALL
Java_wee_digital_camera_RealSense_nWaitForFrames(JNIEnv *env, jobject, jbyteArray colorRaw,
                                                 jbyteArray depthRaw) {
    rs2::frameset frameSet = pipeline->wait_for_frames(5000);
    int frameSetCount = frameSet.size();
    for (int i = 0; i < frameSetCount; i++) {
        if (i == 0) {
            rs2::frame frame = frameSet.get_color_frame();
            jsize length = env->GetArrayLength(colorRaw);
            auto rgb_frame_data = frame.get_data();
            env->SetByteArrayRegion(colorRaw, 0, length, static_cast<const jbyte *>(rgb_frame_data));
        }
        //rs2_release_frame(frame);
    }
    //rs2_release_frame(frameSet);
}



