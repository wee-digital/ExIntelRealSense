#include <jni.h>
#include <memory.h>
#include <librealsense2/h/rs_internal.h>
#include "include/librealsense2/rs.hpp"
#include "include/librealsense2/h/rs_pipeline.h"
#include "include/librealsense2/rs.h"
#include "include/librealsense2/h/rs_config.h"


unsigned int FRAME_RATE = 10, FRAME_TIMEOUT = 5000;
rs2::error *e = 0;


/**
 * Color pipe
 */
rs2::pipeline *colorPipe = NULL;
rs2::pipeline_profile colorProfile;
rs2_stream COLOR_STREAM_TYPE = rs2_stream::RS2_STREAM_COLOR;
rs2_format COLOR_STREAM_FMT = rs2_format::RS2_FORMAT_RGB8;
unsigned int COLOR_WIDTH = 1280, COLOR_HEIGHT = 720;

extern "C" JNIEXPORT void JNICALL
Java_wee_digital_camera_ColorSense_nStartPipeline(JNIEnv *env, jobject) {
    if (NULL != colorPipe) {
        return;
    }
    rs2::config *config = new rs2::config();
    config->enable_stream(COLOR_STREAM_TYPE, 0,
                          COLOR_WIDTH,
                          COLOR_HEIGHT,
                          COLOR_STREAM_FMT, FRAME_RATE);
    colorPipe = new rs2::pipeline();
    colorProfile = colorPipe->start(*config);
    printf("color pipe started");
}

extern "C" JNIEXPORT void JNICALL
Java_wee_digital_camera_ColorSense_nStopPipeline(JNIEnv *env, jobject) {
    if (NULL != colorPipe) {
        colorPipe->stop();
        colorPipe = NULL;
        printf("color pipe stopped");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_wee_digital_camera_ColorSense_nWaitForFrame(JNIEnv *env, jobject, jbyteArray raw) {
    if (NULL == colorPipe) {
        return;
    }
    rs2::frameset frameSet = colorPipe->wait_for_frames(FRAME_TIMEOUT);
    if (NULL == frameSet) {
        return;
    }
    int frameSetCount = frameSet.size();
    if (frameSetCount == 0) {
        return;
    }
    rs2::frame frame = frameSet.first(COLOR_STREAM_TYPE, COLOR_STREAM_FMT);
    if (NULL == frame) {
        return;
    }
    jsize length = env->GetArrayLength(raw);
    auto frameData = frame.get_data();
    env->SetByteArrayRegion(raw, 0, length, static_cast<const jbyte *>(frameData));
}

extern "C" JNIEXPORT void JNICALL
Java_wee_digital_camera_ColorSense_nTryWaitForFrames(JNIEnv *env, jobject, jbyteArray raw) {
    if (NULL == colorPipe) {
        return;
    }
    rs2::frameset *frameSet = new rs2::frameset();
    bool frameAvailable = colorPipe->try_wait_for_frames(frameSet);
    if (!frameAvailable) {
        return;
    }
    int frameSetCount = frameSet->size();
    if (frameSetCount == 0) {
        return;
    }
    rs2::frame frame = frameSet->first(COLOR_STREAM_TYPE, COLOR_STREAM_FMT);
    if (NULL == frame) {
        return;
    }
    jsize length = env->GetArrayLength(raw);
    auto rgb_frame_data = frame.get_data();
    env->SetByteArrayRegion(raw, 0, length, static_cast<const jbyte *>(rgb_frame_data));
}

extern "C" JNIEXPORT void JNICALL
Java_wee_digital_camera_ColorSense_nPollForFrames(JNIEnv *env, jobject, jbyteArray raw) {
    if (NULL == colorPipe) {
        return;
    }
    rs2::frameset *frameSet = new rs2::frameset();
    bool frameAvailable = colorPipe->poll_for_frames(frameSet);
    if (!frameAvailable) {
        return;
    }
    int frameSetCount = frameSet->size();
    if (frameSetCount == 0) {
        return;
    }
    rs2::frame frame = frameSet->first(COLOR_STREAM_TYPE, COLOR_STREAM_FMT);
    if (NULL == frame) {
        return;
    }
    jsize length = env->GetArrayLength(raw);
    auto rgb_frame_data = frame.get_data();
    env->SetByteArrayRegion(raw, 0, length, static_cast<const jbyte *>(rgb_frame_data));
}


/**
 * Depth pipe
 */
rs2::pipeline *depthPipe = NULL;
rs2::pipeline_profile depthProfile;
rs2::colorizer colorizer;
rs2_stream DEPTH_STREAM_TYPE = rs2_stream::RS2_STREAM_DEPTH;
rs2_format DEPTH_STREAM_FMT = rs2_format::RS2_FORMAT_Z16;
unsigned int DEPTH_WIDTH = 640, DEPTH_HEIGHT = 480;


extern "C" JNIEXPORT void JNICALL
Java_wee_digital_camera_DepthSense_nStartPipeline(JNIEnv *env, jobject) {
    if (NULL != depthPipe) {
        return;
    }
    colorizer = rs2::colorizer();
    colorizer.set_option(rs2_option::RS2_OPTION_COLOR_SCHEME, 0);
    rs2::config *config = new rs2::config();
    config->enable_stream(DEPTH_STREAM_TYPE, 0,
                          DEPTH_WIDTH,
                          DEPTH_HEIGHT,
                          DEPTH_STREAM_FMT, FRAME_RATE);
    depthPipe = new rs2::pipeline();
    depthProfile = depthPipe->start(*config);
    printf("depth pipe started");
}

extern "C" JNIEXPORT void JNICALL
Java_wee_digital_camera_DepthSense_nStopPipeline(JNIEnv *env, jobject) {
    if (NULL != depthPipe) {
        depthPipe->stop();
        depthPipe = NULL;
        printf("depth pipe stopped");
    }
}

extern "C" JNIEXPORT void JNICALL
Java_wee_digital_camera_DepthSense_nWaitForFrame(JNIEnv *env, jobject, jbyteArray raw) {
    if (NULL == depthPipe) {
        return;
    }
    rs2::device device = depthProfile.get_device();
    rs2::frameset frameSet = depthPipe->wait_for_frames(FRAME_TIMEOUT);
    if (NULL == frameSet) {
        return;
    }
    int frameSetCount = frameSet.size();
    if (frameSetCount == 0) {
        return;
    }
    rs2::frame frame = frameSet.apply_filter(colorizer);
    if (NULL == frame) {
        return;
    }
    jsize length = env->GetArrayLength(raw);
    auto frameData = frame.get_data();
    env->SetByteArrayRegion(raw, 0, length, static_cast<const jbyte *>(frameData));
}
