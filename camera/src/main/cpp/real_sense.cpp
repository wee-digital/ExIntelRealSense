#include <jni.h>
#include <memory.h>
#include <librealsense2/h/rs_internal.h>
#include "include/librealsense2/rs.hpp"
#include "include/librealsense2/h/rs_pipeline.h"
#include "include/librealsense2/rs.h"
#include "include/librealsense2/h/rs_config.h"


unsigned int FRAME_RATE = 10, FRAME_TIMEOUT = 5000;
rs2::error *e = 0;
rs2::context *context;

void writeFrameData(JNIEnv *env, rs2::frame frame, jbyteArray raw) {
    if (NULL == frame) return;
    try {
        jsize length = env->GetArrayLength(raw);
        auto frameData = frame.get_data();
        env->SetByteArrayRegion(raw, 0, length, static_cast<const jbyte *>(frameData));
    } catch (rs2::error e) {
        printf("");
    }
}

void handleError(rs2::error e) {
    auto type = e.get_type();
    auto s = rs2_exception_type_to_string(type);
    printf("%s", s);
}

extern "C" JNIEXPORT void JNICALL
Java_wee_digital_camera_RealSense_nReset(JNIEnv *env, jobject) {
    if (NULL == context) return;
    rs2::device_list deviceList = context->query_devices(RS2_PRODUCT_LINE_ANY);
    context->query_all_sensors().clear();
    int deviceCount = deviceList.size();
    if (deviceCount == 0) return;
    rs2::device device = deviceList[0];
    device.hardware_reset();
}


/**
 * Color pipe
 */
rs2::pipeline *colorPipe = NULL;
rs2::pipeline_profile colorProfile;
rs2_stream COLOR_STREAM_TYPE = rs2_stream::RS2_STREAM_COLOR;
rs2_format COLOR_STREAM_FMT = rs2_format::RS2_FORMAT_RGB8;
unsigned int COLOR_WIDTH = 1280, COLOR_HEIGHT = 720, COLOR_INDEX = 0;

void resetColorSensor() {
    if (NULL == context) return;
    rs2::device_list deviceList = context->query_devices(RS2_PRODUCT_LINE_ANY);
    rs2::device device = deviceList[0];
    int deviceCount = deviceList.size();
    if (deviceCount == 0) return;
    rs2::sensor colorSensor = device.first<rs2::color_sensor>();
    colorSensor.stop();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_wee_digital_camera_RealSense_nStartColorPipeline(JNIEnv *env, jobject) {
    try {
        if (NULL == context) {
            context = new rs2::context();
        }
        if (NULL == colorPipe) {
            colorPipe = new rs2::pipeline(*context);
        }
        rs2::config *config = new rs2::config();
        config->enable_stream(COLOR_STREAM_TYPE, COLOR_INDEX,
                              COLOR_WIDTH,
                              COLOR_HEIGHT,
                              COLOR_STREAM_FMT, FRAME_RATE);
        colorProfile = colorPipe->start(*config);
        return true;
    } catch (rs2::error e) {
        handleError(e);
        return false;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_wee_digital_camera_RealSense_nStopColorPipeline(JNIEnv *env, jobject) {
    if (NULL == colorPipe) return false;
    try {
        colorPipe->stop();
    } catch (rs2::error e) {
        handleError(e);
    }
    return false;
}

extern "C" JNIEXPORT void JNICALL
Java_wee_digital_camera_RealSense_nWaitForColorFrame(JNIEnv *env, jobject, jbyteArray raw) {
    if (NULL == colorPipe) return;
    try {
        rs2::frameset frameSet = colorPipe->wait_for_frames(FRAME_TIMEOUT);
        if (NULL == frameSet) return;
        int frameSetCount = frameSet.size();
        if (frameSetCount == 0) return;
        rs2::frame frame = frameSet.first(COLOR_STREAM_TYPE, COLOR_STREAM_FMT);
        writeFrameData(env, frame, raw);
    } catch (rs2::error e) {
        printf("");
    }
}


/**
 * Depth pipe
 */
rs2::pipeline *depthPipe = NULL;
rs2::pipeline_profile depthProfile;
rs2::colorizer colorizer;
rs2_stream DEPTH_STREAM_TYPE = rs2_stream::RS2_STREAM_DEPTH;
rs2_format DEPTH_STREAM_FMT = rs2_format::RS2_FORMAT_Z16;
unsigned int DEPTH_WIDTH = 640, DEPTH_HEIGHT = 480, DEPTH_INDEX = 0;
bool isWaitForDepthFrame = false;

extern "C" JNIEXPORT jboolean JNICALL
Java_wee_digital_camera_RealSense_nStartDepthPipeline(JNIEnv *env, jobject) {
    try {
        if (NULL == context) {
            context = new rs2::context();
        }
        if (NULL == depthPipe) {
            colorizer = rs2::colorizer();
            colorizer.set_option(rs2_option::RS2_OPTION_COLOR_SCHEME, 0);
            depthPipe = new rs2::pipeline(*context);
        }
        rs2::config *config = new rs2::config();
        config->enable_stream(DEPTH_STREAM_TYPE, DEPTH_INDEX,
                              DEPTH_WIDTH,
                              DEPTH_HEIGHT,
                              DEPTH_STREAM_FMT, FRAME_RATE);
        depthProfile = depthPipe->start(*config);
        isWaitForDepthFrame = false;
        return true;
    } catch (rs2::error e) {
        handleError(e);
        return false;
    }
}

extern "C" JNIEXPORT jboolean JNICALL
Java_wee_digital_camera_RealSense_nStopDepthPipeline(JNIEnv *env, jobject) {
    if (NULL == depthPipe) return false;
    try {
        isWaitForDepthFrame = true;
        depthPipe->stop();
        return false;
    } catch (rs2::error e) {
        handleError(e);
        return false;
    }
}

extern "C" JNIEXPORT void JNICALL
Java_wee_digital_camera_RealSense_nWaitForDepthFrame(JNIEnv *env, jobject, jbyteArray raw) {

    if (NULL == depthPipe || isWaitForDepthFrame) return;
    try {
        isWaitForDepthFrame = true;
        rs2::frameset frameSet = depthPipe->wait_for_frames(FRAME_TIMEOUT);
        if (NULL == frameSet) {
            return;
        }
        int frameSetCount = frameSet.size();
        if (frameSetCount == 0) {
            return;
        }
        rs2::frame frame = frameSet.apply_filter(colorizer);
        writeFrameData(env, frame, raw);
        isWaitForDepthFrame = false;
    } catch (rs2::error e) {
        handleError(e);
        isWaitForDepthFrame = false;
    }


}
