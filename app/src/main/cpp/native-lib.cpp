#include <jni.h>
#include <string>

#include "matrix.h"
#include "ecompass.h"

//extern "C" JNIEXPORT jstring JNICALL
//Java_com_work_libtest_MainActivity_stringFromJNI(
//        JNIEnv* env,
//        jobject /* this */) {
//    std::string hello = "Hello from C++";
//    return env->NewStringUTF(hello.c_str());
//}
//
//extern "C" JNIEXPORT jstring JNICALL
//Java_com_work_libtest_MainActivity_calibrateProbeValues(
//        JNIEnv* env,
//        jstring inputData) {
//    std::string returnString = calibrateAllData();
//    return env->NewStringUTF(returnString.c_str());
//}