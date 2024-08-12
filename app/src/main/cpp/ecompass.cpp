////////////////////////////////////////////////////////////////////////////////
///
/// \file   ecompass.c
/// \brief  Electronic compass. Calculates orientation from sensor values.
/// \author Martin Louis
/// \date   Created: 2012-11-8
///
/// Calculates orientation as roll pitch and azimuth from accelerometer and magnetometer
/// sensor readings. Is able to calibrate the sensor values if calibration parameters are
/// supplied. Functions in the ecompass_store module encode and decode these parameters
/// to and from memory images. Can optionally calculate variance of the input sensor
/// readings over several samples and provide a measure of stability of these values.

#include <math.h>
#include <stdint.h>
#include <string.h>
#include "crc.h"
#include "ecompass.h"
#include "ecompass_calibrator.h"
#include "ecompass_store.h"
#ifdef ECOMPASS_SENSOR_VARIANCE
#include <sys/time.h>
#include <string>

#endif
#ifdef ECOMPASS_DEBUG_CALCULATION
#include <stdio.h>
#endif

/// \ingroup ecompass_group
/// \{



////////////////////////////////////////////////////////////////////////////////
// Default values

#define DEFAULT_ROLL_STABILISATION_DISABLE	1
#define DEFAULT_ROLL_STABILISATION_FACTOR	0.0
#define DEFAULT_VARIANCE_BY_NUMBER			1
#define DEFAULT_VARIANCE_DURATION			5000000LL		// usec
#define DEFAULT_VARIANCE_NUMBER				5

#include<vector>
#include <jni.h>

std::vector < std::string > strings;
std::vector < std::string > calibrationStrings;

static void customSplit(std::string str, char separator) {
    int startIndex = 0, endIndex = 0;
    for (int i = 0; i <= str.size(); i++) {

        // If we reached the end of the word or the end of the input.
        if (str[i] == separator || i == str.size()) {
            endIndex = i;
            std::string temp;
            temp.append(str, startIndex, endIndex - startIndex);
            strings.push_back(temp);
            startIndex = endIndex + 1;
        }
    }
}

static void customSplitCalibration(std::string str, char separator) {
    int startIndex = 0, endIndex = 0;
    for (int i = 0; i <= str.size(); i++) {

        // If we reached the end of the word or the end of the input.
        if (str[i] == separator || i == str.size()) {
            endIndex = i;
            std::string temp;
            temp.append(str, startIndex, endIndex - startIndex);
            calibrationStrings.push_back(temp);
            startIndex = endIndex + 1;
        }
    }
}

extern "C" {
    std::string jstring2string(JNIEnv *env, jstring jStr) {
        if (!jStr)
            return "";

        const jclass stringClass = env->GetObjectClass(jStr);
        const jmethodID getBytes = env->GetMethodID(stringClass, "getBytes", "(Ljava/lang/String;)[B");
        const jbyteArray stringJbytes = (jbyteArray) env->CallObjectMethod(jStr, getBytes, env->NewStringUTF("UTF-8"));

        size_t length = (size_t) env->GetArrayLength(stringJbytes);
        jbyte* pBytes = env->GetByteArrayElements(stringJbytes, NULL);

        std::string ret = std::string((char *)pBytes, length);
        env->ReleaseByteArrayElements(stringJbytes, pBytes, JNI_ABORT);

        env->DeleteLocalRef(stringJbytes);
        env->DeleteLocalRef(stringClass);
        return ret;
    }

    JNIEXPORT jstring JNICALL Java_com_work_libtest_MainActivity_calibrateAllData(JNIEnv* env, jobject inputData, jstring input_data) {
        std::string calibratedData = calibrateAllData(jstring2string(env, input_data));
        const char* returnedData = calibratedData.c_str();
        return (*env).NewStringUTF(returnedData);
    }
}

/**
 * Roll stabilisation is required in cases where the pitch angle becomes
// vertical (ie. +/- PI) and the roll axis coincides with the gravitation
// vector (down) and so can't be calculated. The first problem this causes is
// that roll is undefined at vertical pitch. The second problem is that NEAR
// vertical pitch the accelerometer components used to calulate roll become
// which causes them to be swamped by sensor noise. A common solution in
// aerospace application is to use a modified the accelelrometer z component
// for roll calculations which adds a small multiple of the x component.
// This ensures that compensation is only seen near vertial pitch where x >> z.
// The effect of this compensation is to drive roll smoothly to zero as pitch
// becomes vertial. Smaller values of the stabilisation factor (the mulitple of
// x added to z) result in smaller errors in roll near vertical (due to less
// compensation) but greater susceptibilty to sensor noise in the accelerometer
// reading.
//
// Azimuth calculations have a similar problem at vertical pitch. The azimuth
// is essentially the horizontal angle from north to the plane in which the
// device is pitching. At vertical pitch there is no such unique plane and
// azimuth can't be calculated (ie pitching down by PI at any azimuth will
// result in the same vertical orientation). A tilt compensated azimuth
// using a stablised roll value results in a well-behaved orientation since
// as roll gets driven to zero, azimuth will be driven to the local negative z
// direction of the device (ie up in the device's frame of reference - where
// roll is zero).
//
// To picture the effect of roll stabilisation and tilt compensated azimuth,
// imagine the device pitching down past vertical. On the other side of
// vertical the azimuth jumps around by PI (180 degrees) ince it is pointing
// in the opposite direction and the roll also changes to reflect the fact that
// the device has now flipped onto its back. Rather than a mathematical
// singularity at vertical the roll smoothly swings through zero and the
// azimuth swings though the device's local up vector and the resulting angles
// will orient the device correctly.
 */

/**
 * GOAL:
 * be able to input calibration matrix, and either the 3 data points from an accelerometer or a magentometer
 * and the library return calibrated (scaled essentially) values.
 */

/**
 *@param data
 * temp, acc_x, acc_y, acc_z, mag_x, mag_y, mag_z
 *
 * function to calibrate all sensor readings, basically an interpreter function.
 *
 * @return all calibrated data in the form:
 * temp,ax,ay,az,mx,my,mz,calibrationSize,calibrationData(seperated by .)
 */
static std::string calibrateAllData(std::string data) {
    double temp;
    double acc_x;
    double acc_y;
    double acc_z;
    double mag_x;
    double mag_y;
    double mag_z;
    int calibrationSize;
    std::string calibrationLongString;

    customSplit(data, ',');
    temp = std::stoi(strings[0]);
    acc_x = std::stoi(strings[1]);
    acc_y = std::stoi(strings[2]);
    acc_z = std::stoi(strings[3]);
    mag_x = std::stoi(strings[4]);
    mag_y = std::stoi(strings[5]);
    mag_z = std::stoi(strings[6]);
    calibrationSize = std::stoi(strings[7]);
    calibrationLongString = strings[8]; //needs to be split by .

//    signed char calByteArray[291] = {1, 16, 4, -81, 2, 111, 89, 32, -100, -84, 1, 63, -16, 53, 96, -100, 84, 90, -27, -65, 62, 5, -72, 27, 29, 27, -124, -65, -110, 20, 103, -84, 67, -29, -38, -65, -115, 90, -116, 62, -95, 103, 109, 63, 98, 11, -21, 119, 119, -19, -20, 63, 31, -18, 81, 16, -65, 81, -86, 63, -16, 59, -5, -91, -10, 23, -61, 2, 63, -120, 114, -125, 0, 24, 122, 62, 0, 0, 0, 0, 0, 0, 0, 0, 63, -118, 103, -57, -56, -9, -85, -61, 0, 0, 0, 0, 0, 0, 0, 0, -65, 105, -121, 26, -31, 74, -1, -12, 0, 0, 0, 0, 0, 0, 0,0, 0, 1, 64, 0, 59, 93, -12, -67, 70, 61, -65, 109, -65, 44, 117, 26, 26, 123, -65, 18, -99, -80, 100, -102, 26, -8, -65, 93, 54, -43, 78, -103, -61, 89, 48, -100, -84, 1, 63, -16, -126, -20, 71, -16, 68, 6, 63, 126, 97, -84, -10, -17, 98, -69, -65, -123, 112, 66, -59, -117, 40, -46, -65,96, 15, 10, 98, -2, -98, -54, -65, -107, 60, -98, -124, 20, -96, 98, 63, 81, -66, -66, -80, -77, 111, -22, 63, -15, 25, -25, -3, -55, -44, -84, 2, 64, 20, -115, -20, -73, -81, 12, -40, 0, 0, 0, 0, 0, 0, 0, 0, 64, 14, -92, -119, -95, -43, 100, 96, 0, 0, 0, 0, 0, 0, 0, 0, 63, -38, -46, -59, -39, 19, -67, 84, 0, 0, 0, 0, 0, 0, 0, 0, 1, 64, 0, 59, 93, -12, -67, 70, 61, 62, -105, 95, -84, 112, 51, -106, -59, 62, -104, -83, -71, -79, 77, 57, 1, 62, -120, 5, -45, 11, 86, 103};

//    signed char calByteArray[310] = ;
    std::vector<uint8_t> calByteArray = {1, 0, 8, 105, 4, 175, 2, 111, 89, 20, 16, 1, 91, 1, 16, 4, 175, 2, 111, 89, 32, 156, 172, 1, 63, 240, 53, 96, 156, 84, 90, 229, 191, 62, 5, 184, 27, 29, 27, 132, 191, 146, 20, 103, 172, 67, 227, 218, 191, 141, 90, 140, 62, 161, 103, 109, 63, 98, 11, 235, 119, 119, 237, 236, 63, 31, 238, 81, 16, 191, 81, 170, 63, 240, 59, 251, 165, 246, 23, 195, 2, 63, 136, 114, 131, 0, 24, 122, 62, 0, 0, 0, 0, 0, 0, 0, 0, 63, 138, 103, 199, 200, 247, 171, 195, 0, 0, 0, 0, 0, 0, 0, 0, 191, 105, 135, 26, 225, 74, 255, 244, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 64, 0, 59, 93, 244, 189, 70, 61, 191, 109, 191, 44, 117, 26, 26, 123, 191, 18, 157, 176, 100, 154, 26, 248, 191, 93, 54, 213, 78, 153, 195, 89, 48, 156, 172, 1, 63, 240, 130, 236, 71, 240, 68, 6, 63, 126, 97, 172, 246, 239, 98, 187, 191, 133, 112, 66, 197, 139, 40, 210, 191, 96, 15, 10, 98, 254, 158, 202, 191, 149, 60, 158, 132, 20, 160, 98, 63, 81, 190, 190, 176, 179, 111, 234, 63, 241, 25, 231, 253, 201, 212, 172, 2, 64, 20, 141, 236, 183, 175, 12, 216, 0, 0, 0, 0, 0, 0, 0, 0, 64, 14, 164, 137, 161, 213, 100, 96, 0, 0, 0, 0, 0, 0, 0, 0, 63, 218, 210, 197, 217, 19, 189, 84};
//            {1, 0, 8, 105, 4, -81, 2, 111, 89, 20, 16, 1, 91, 1, 16, 4, -81, 2, 111, 89, 32, -100, -84, 1, 63, -16, 53, 96, -100, 84, 90, -27, -65, 62, 5, -72, 27, 29, 27, -124, -65, -110, 20, 103, -84, 67, -29, -38, -65, -115, 90, -116, 62, -95, 103, 109, 63, 98, 11, -21, 119, 119, -19, -20, 63, 31, -18, 81, 16, -65, 81, -86, 63, -16, 59, -5, -91, -10, 23, -61, 2, 63, -120, 114, -125, 0, 24, 122, 62, 0, 0, 0, 0, 0, 0, 0, 0, 63, -118, 103, -57, -56, -9, -85, -61, 0, 0, 0, 0, 0, 0, 0, 0, -65, 105, -121, 26, -31, 74, -1, -12, 0, 0, 0, 0, 0, 0, 0,0, 0, 1, 64, 0, 59, 93, -12, -67, 70, 61, -65, 109, -65, 44, 117, 26, 26, 123, -65, 18, -99, -80, 100, -102, 26, -8, -65, 93, 54, -43, 78, -103, -61, 89, 48, -100, -84, 1, 63, -16, -126, -20, 71, -16, 68, 6, 63, 126, 97, -84, -10, -17, 98, -69, -65, -123, 112, 66, -59, -117, 40, -46, -65,96, 15, 10, 98, -2, -98, -54, -65, -107, 60, -98, -124, 20, -96, 98, 63, 81, -66, -66, -80, -77, 111, -22, 63, -15, 25, -25, -3, -55, -44, -84, 2, 64, 20, -115, -20, -73, -81, 12, -40, 0, 0, 0, 0, 0, 0, 0, 0, 64, 14, -92, -119, -95, -43, 100, 96, 0, 0, 0, 0, 0, 0, 0, 0, 63, -38, -46, -59, -39, 19, -67, 84, 0, 0, 0, 0, 0, 0, 0, 0, 1, 64, 0, 59, 93, -12, -67, 70, 61, 62, -105, 95, -84, 112, 51, -106, -59, 62, -104, -83, -71, -79, 77, 57, 1, 62, -120, 5, -45, 11, 86, 103};

//    void *calibration_data = calByteArray;

    customSplitCalibration(calibrationLongString, '.');
    std::string allCalData;
    int byteArraySize = 0;
    for (int i = 0; i < calibrationSize*2; i++) {
        allCalData = allCalData + std::to_string(calByteArray[i]);
        byteArraySize++;
    }

    ecompass_t *testCompass = new ecompass_t();
    ecompass_init(testCompass);
    ecompass_set_name(testCompass, "ANNA");
//    ecompass_store_get(&testCompass, calByteArray, 291);
    const char *dataValue = "Hello, eCompass!THING THING";
    size_t size = std::strlen(dataValue) + 1;
//    size_t calibration_being_got_value = ecompass_store_get(testCompass, dataValue, size);
//    size_t calibration_being_got_value = ecompass_store_get(testCompass, calByteArray, 291);
//    size_t calibration_being_set_value = ecompass_store_put(testCompass, calByteArray, 291);

    /**
     * Calibration attempt following CalibrationGLKViewController
     */

//    ecompass_calibrator_t *calibrator;
//    ecompass_calibrator_config_t _calibratorConfig;
//    ecompass_sensor_t accSensor;
//
//    if (calibrator == NULL) {
//        calibrator = ecompass_calibrator_create();
//    }
//
//    ecompass_calibrator_configure(calibrator, &_calibratorConfig);
//    calibrator->config.sensor = accSensor;

    /**
     * END Calibration attempt
     */


    /**
     * Custom probe calibration function based on IOS app
     * idk if that is properly working it is doing a bunch of bit manipulation which doesnt look right
     */
//    std::vector<uint8_t> ouputFromFunction = buildCalibrationData(testCompass, reinterpret_cast<uint8_t *>(calByteArray), 291);
//    std::string outputFromFunctionCHANGED = buildCalibrationData(testCompass, reinterpret_cast<uint8_t *>(calByteArray), 291);

//    void* buffer = &calByteArray;
    uint8_t *dataBuffer = calByteArray.data();
//    uint8_t startValueOne = calByteArray[4];
    uint8_t startValueOne = dataBuffer[2];

//    int storeReturn = ecompass_store_put(testCompass, dataBuffer, 264);
    int storeReturn = ecompass_store_get(testCompass, dataBuffer, 264);

//    ecompass_store_put_sensor_v1(testCompass, &testCompass->accelerometer_calibration, &calByteArray, 291);

    /**
     * Setting calibration manually - works, except does keen values when a value is inputted
     * to be calibrated.
     */
//    ecompass_calibration_t accCalibration;
//    ecompass_sensor_t accelerometer;
//    ecompass_calibration_init(&accCalibration, accelerometer);
//    accCalibration.concrete.temperature = 19;
//    for (int i = 0; i < 5; i++) {
//        for (int j = 0; j < 3; j++) {
//            for (int k = 0; k < 3; k++) {
//                accCalibration.concrete.parameter[i][j][k] = i+k-j;//[5][3][3] matrix
//            }
//        }
//    }
//    for (int i = 0; i < 5; i++) {
//        for (int j = 0; j < 3; j++) {
//            for (int k = 0; k < 3; k++) {
//                accCalibration.term->parameter[i][j][k] = i+k-j;//[5][3][3] matrix
//            }
//        }
//    }
//    for (int i = 0; i < 5; i++) {
//        accCalibration.concrete.matrix_is_used[i] = 1; //uint8_t [5] matrix
//    }
//    for (int i = 0; i < 5; i++) {
//        accCalibration.concrete.matrix_is_diagonal[i] = 1; //uint8_t [5] matrix
//    }
//    ecompass_calibration_set_accelerometer(testCompass, &accCalibration);

//    ecompass_calibrator_t newCalibrator;
//    ecompass_calibrator_calculate(newCalibrator, 0);
    /**
     * END
     */

    ecompass_accelerometer_reading(testCompass, 1, 2, 3, 19, 1);

//    testCompass->accelerometer.x = 4;
//    testCompass->accelerometer.y = 3;
//    testCompass->accelerometer.z = 2;

//    testCompass->accelerometer.uncalibrated_x = 4;
//    testCompass->accelerometer.uncalibrated_y = 3;
//    testCompass->accelerometer.uncalibrated_z = 2;
//    testCompass->accelerometer.is_calibrated = 0;
//    testCompass->accelerometer.is_valid = 1;


//    double calibratedXValue = testCompass->accelerometer.x;
//    double calibratedYValue = testCompass->accelerometer.y;
//    double calibratedZValue = testCompass->accelerometer.z;
//    int calFlag = testCompass->accelerometer.is_calibrated;
//    int validFlag = testCompass->accelerometer.is_valid;
//    int timestampValid = testCompass->calibration_timestamp_is_valid;
//
//    std::time_t calTimestamp = testCompass->calibration_timestamp;
//    std::time_t now = calTimestamp;
//    std::tm * ptm = std::localtime(&now);
//    char buffer[32];
//    std::strftime(buffer, 32, "%a, %d.%m.%Y %H:%M:%S", ptm);
//    std::string timeCalStamp = buffer;
//    std::string what = testCompass->name;

    /**
     * Get all concrete parameters from accelerometer calibration data
     */
    std::string caliDataFromAcc = "";
    for (int i = 0; i < 5; i++) {
        for (int j = 0; j < 3; j++) {
            for (int k = 0; k < 3; k++) {
//                caliDataFromAcc = caliDataFromAcc + "," + std::to_string(testCompass->accelerometer_calibration.concrete.temperature);//term[i].temperature);
                caliDataFromAcc = caliDataFromAcc + "," + std::to_string(testCompass->accelerometer_calibration.term[0].parameter[i][j][k]);

            }
            caliDataFromAcc = caliDataFromAcc + "\n";
        }
        caliDataFromAcc = caliDataFromAcc + "|";
    }

//    int caliDataFromAcc = testCompass->accelerometer_calibration.term[0].parameter[3][0][0];
    /**
     * END
     */

//    std::string all_calibration_data = ", Calibration first value = \n" + caliDataFromAcc;
//    std::string calibration_timestamp_valid = ", Timestamp is valid: " + std::to_string(timestampValid);
//
//    std::string acc_valid = "Accelerometer value is valid: " + std::to_string(validFlag);
//    std::string cal_valid = " , calibration is valid : " + std::to_string(calFlag);

    std::string acc_ux = std::to_string(testCompass->accelerometer.uncalibrated_x);
    std::string acc_uy = std::to_string(testCompass->accelerometer.uncalibrated_y);
    std::string acc_uz = std::to_string(testCompass->accelerometer.uncalibrated_z);

    std::string acc_cx = std::to_string(testCompass->accelerometer.x);
    std::string acc_cy = std::to_string(testCompass->accelerometer.y);
    std::string acc_cz = std::to_string(testCompass->accelerometer.z);

    //1 is empty, 0 has been assigned parameters
//    std::string cal_is_empty = std::to_string(ecompass_calibration_is_empty(&testCompass->accelerometer_calibration));
//
//    std::string test = std::to_string(testCompass->accelerometer_calibration.concrete.parameter[0][0][0]);

//    return outputFromFunctionCHANGED;
//    return cal_is_empty + ", Calibration Data: " + all_calibration_data + "\n" + acc_ux + ":" + acc_cx + "|" + acc_uy + ":" + acc_cy + "|" + acc_uz + ":" + acc_cz + "|";
    std::string nameString = testCompass->name;
    return nameString + ", \nCALIBRATION: " + caliDataFromAcc + ", Start value 1: " + std::to_string(startValueOne) + ", Storage return: " + std::to_string(storeReturn) + " FINISH\n" + acc_ux + ":" + acc_cx + "|" + acc_uy + ":" + acc_cy + "|" + acc_uz + ":" + acc_cz + "|";
}
static crc8_t crc_generator;
#define CRC_OLD_POLY			CRC8_POLY_CCITT
#define CRC_OLD_INITIAL_VALUE	0x0000
#define CRC_OLD_FINAL_XOR		0x0000
#define CBID_HEADER				0x01
#define CBID_CALIBRATION		0x10

std::string buildCalibrationData(ecompass_t *ecompass, uint8_t* inputBuffer, size_t inputBufferlen) {
    static bool crc_generator_initialised = false;
//    static CRCGenerator crc_generator;

    if (!crc_generator_initialised) {
        crc8_init(&crc_generator, CRC_OLD_POLY, CRC_OLD_INITIAL_VALUE, CRC_OLD_FINAL_XOR);
        crc_generator_initialised = true;
    }

    size_t matricesLen = ecompass_store_space_required(ecompass);
    size_t bufferLen = 6 + sizeof(time_t) + 1 + 3 + matricesLen;
    size_t len = 0;

    std::vector<uint8_t> buffer(bufferLen);

    buffer[len++] = CBID_HEADER;
    buffer[len++] = 0;
    buffer[len++] = 4 + sizeof(time_t);
    buffer[len++] = static_cast<uint8_t>((bufferLen >> 8) & 0xFF);
    buffer[len++] = static_cast<uint8_t>(bufferLen & 0xFF);
    buffer[len++] = static_cast<uint8_t>(sizeof(time_t));

    time_t temp = ecompass->calibration_timestamp;
    for (size_t i = 0; i < sizeof(time_t); i++, temp >>= 8) {
        buffer[len++] = static_cast<uint8_t>(temp & 0xFF);
    }

    int something = crc8_begin(&crc_generator);
    crc8_calc(&crc_generator, buffer.data(), len);
    buffer[len++] = crc8_end(&crc_generator);

    buffer[len++] = CBID_CALIBRATION;
    buffer[len++] = static_cast<uint8_t>((matricesLen >> 8) & 0xFF);
    buffer[len++] = static_cast<uint8_t>(matricesLen & 0xFF);

    if ((matricesLen = ecompass_store_put(ecompass, &buffer[len], bufferLen - len)) == 0) {
        return ""; // Return an empty vector if storing failed
    }

    len += matricesLen;
    buffer.resize(len);

    return std::to_string(buffer[len]); //RETURNING FULL
}

//std::vector<uint8_t> buildCalibrationData(ecompass_t *ecompass, uint8_t* inputBuffer, size_t inputBufferlen) {
//    static bool crc_generator_initialised = false;
////    static CRCGenerator crc_generator;
//
//    if (!crc_generator_initialised) {
//        crc8_init(&crc_generator, CRC_OLD_POLY, CRC_OLD_INITIAL_VALUE, CRC_OLD_FINAL_XOR);
//        crc_generator_initialised = true;
//    }
//
//    size_t matricesLen = ecompass_store_space_required(ecompass);
//    size_t bufferLen = 6 + sizeof(time_t) + 1 + 3 + matricesLen;
//    size_t len = 0;
//
//    std::vector<uint8_t> buffer(bufferLen);
//
//    buffer[len++] = CBID_HEADER;
//    buffer[len++] = 0;
//    buffer[len++] = 4 + sizeof(time_t);
//    buffer[len++] = static_cast<uint8_t>((bufferLen >> 8) & 0xFF);
//    buffer[len++] = static_cast<uint8_t>(bufferLen & 0xFF);
//    buffer[len++] = static_cast<uint8_t>(sizeof(time_t));
//
//    time_t temp = ecompass->calibration_timestamp;
//    for (size_t i = 0; i < sizeof(time_t); i++, temp >>= 8) {
//        buffer[len++] = static_cast<uint8_t>(temp & 0xFF);
//    }
//
//    int something = crc8_begin(&crc_generator);
//    crc8_calc(&crc_generator, buffer.data(), len);
//    buffer[len++] = crc8_end(&crc_generator);
//
//    buffer[len++] = CBID_CALIBRATION;
//    buffer[len++] = static_cast<uint8_t>((matricesLen >> 8) & 0xFF);
//    buffer[len++] = static_cast<uint8_t>(matricesLen & 0xFF);
//
//    if ((matricesLen = ecompass_store_put(ecompass, &buffer[len], bufferLen - len)) == 0) {
//        return std::vector<uint8_t>(); // Return an empty vector if storing failed
//    }
//
//    len += matricesLen;
//    buffer.resize(len);
//
//    return buffer;
//}

////////////////////////////////////////////////////////////////////////////////
// Private function prototypes

static void		ecompass_calculate_orientation(ecompass_t *ecompass);
//static void ecompass_calibrated_show_orientation(ecompass_t *ecompass) {
//    if (ecompass->name != NULL) {
//
//    }
//}

// Unused funrction
//static double	calculate_poly_term(double coeff, double x, int power);

#if defined(ECOMPASS_DEBUG)
static void		ecompass_print_calibration_coeffs(const ecompass_calibration_t *cal, const char *title, int concrete);
static void		ecompass_print_calibration_coeff(const ecompass_calibration_t *cal, int concrete, int power, int row, int col);
#endif



////////////////////////////////////////////////////////////////////////////////
/// A "default" ecompass context used when functions that require a context are
/// given NULL as a parameter.

ecompass_t ecompass_shared_instance;
int		   ecompass_shared_instance_initialised = 0;



////////////////////////////////////////////////////////////////////////////////
// Public variables

/// Flag indicating that roll_for_display ranges from [-180:180] instead of [0:360]
/// Global value that applies to all ecompasses.
unsigned int ecompass_roll_for_display_range_is_symmetric = 0;



////////////////////////////////////////////////////////////////////////////////
// Initialises sensor reading to default values.
//
// \brief Initialises sensor reading.
// \param reading Pointer to the reading to initialise.

void ecompass_sensor_reading_init(ecompass_sensor_reading_t *reading)
{
    // Sanity check
    if (!reading)
        return;

    reading->x = 0.0;
    reading->y = 0.0;
    reading->z = 0.0;
    reading->t = ECOMPASS_INVALID_TEMPERATURE;
    reading->uncalibrated_x = 0.0;
    reading->uncalibrated_y = 0.0;
    reading->uncalibrated_z = 0.0;
    reading->is_valid = 0;
    reading->is_calibrated = 0;
    reading->t_is_valid = 0;

}



////////////////////////////////////////////////////////////////////////////////
// Initialises scalar reading to default values.
//
// \brief Initialises sensor reading.
// \param reading Pointer to the reading to initialise.

void ecompass_scalar_reading_init(ecompass_scalar_reading_t *reading)
{
    reading->value = 0.0;
    reading->uncalibrated_value = 0.0;
    reading->is_valid = 0;
}



////////////////////////////////////////////////////////////////////////////////
// Initialises orientation to default values.
//
// \brief Initialises sensor reading.
// \param orientation Pointer to the orientation to initialise.

void ecompass_orientation_init(ecompass_orientation_t *orientation)
{
    if (!orientation)
        return;

    orientation->roll_is_valid		= 0;
    orientation->pitch_is_valid		= 0;
    orientation->azimuth_is_valid	= 0;
}



////////////////////////////////////////////////////////////////////////////////
// Initialises sensor reading to default values and populates it with an
// initial uncalibrated value.
//
// \brief Initialises sensor reading with uncalibrated reading.
// \param reading Pointer to the reading to initialise.
// \param x X component of uncalibrated reading.
// \param y Y component of uncalibrated reading.
// \param z Z component of uncalibrated reading.

void ecompass_sensor_reading_init_with_uncalibrated(ecompass_sensor_reading_t *reading, double x, double y, double z)
{
    // Sanity check
    if (!reading)
        return;

    ecompass_sensor_reading_init(reading);
    reading->uncalibrated_x = x;
    reading->uncalibrated_y = y;
    reading->uncalibrated_z = z;
    reading->is_valid = 1;
}



////////////////////////////////////////////////////////////////////////////////
/// Initialises calibration coeffs by setting them all to zero and invalidating
/// them.
///
/// \brief Initialise calibration coefficients.
/// \param coeffs Pointer to the coefficients to initialise.

void ecompass_calibration_coeffs_init(ecompass_calibration_coeffs_t *coeffs)
{
    int p, r, c;

    // Sanity check
    if (!coeffs)
        return;

    coeffs->temperature = ECOMPASS_INVALID_TEMPERATURE;
    coeffs->is_valid	= 0;

    for (p = 0; p <= ECOMPASS_MAX_POWER_TERM; p++)
    {
        coeffs->matrix_is_used[p]	  = 0;
        coeffs->matrix_is_diagonal[p] = 0;
        for (r = 0; r < 3; r++)
            for (c = 0; c < 3; c++)
                coeffs->parameter[p][r][c] = 0.0;
    }
}



////////////////////////////////////////////////////////////////////////////////
// Initialises calibration parameters by setting the model to
// ecompassCalibrationModelNone, the offset vector to zero, and the gain to the
// identity matrix. This results in parameters that have no effect in the input.
//
// \brief Initialise calibration parameters.
// \param cal Pointer to the parameters to initialise.
// \param sensor The sensor that the calibration belongs to.

void ecompass_calibration_init(ecompass_calibration_t *cal, ecompass_sensor_t sensor)
{
    int i;

    // Sanity check
    if (!cal)
        return;

    cal->sensor = sensor;
    cal->temperature_threshold = 600; //0.001
    cal->offset_is_pre_subtracted = 0;
    ecompass_calibration_coeffs_init(&cal->concrete);
    for (i = 0; i <= ECOMPASS_MAX_TEMPERATURE_POWER_TERM; i++)
        ecompass_calibration_coeffs_init(&cal->term[i]);
}



////////////////////////////////////////////////////////////////////////////////
/// Calibration parameters are empty if they have not been assigned any parameters.
///
/// \brief Idicated if cxalibration parameters are empty.
/// \param cal Pointer to the parameters to test.
/// \return Boolean indicating if \p cal is uninitialised.
/// \retval 1 \p is empty (uninitialised)
/// \retval 0 \p has been assigned parameters.

int ecompass_calibration_is_empty(ecompass_calibration_t *cal)
{
    int t;

    if (!cal)
        return 1;

    for (t = 0; t <= ECOMPASS_MAX_TEMPERATURE_POWER_TERM; t++)
    {
        int p;

        for (p = 0; p <= ECOMPASS_MAX_POWER_TERM; p++)
            if (cal->term[t].matrix_is_used[p])
                return 0;
    }

    return 1;
}



////////////////////////////////////////////////////////////////////////////////
// Scalar calibration parameters are empty if they have not been assigned any parameters.
//
// \brief Inicates if scalar calibration parameters are empty.
// \param cal Pointer to the parameters to test.
// \return Bollean indicating if \p param is uninitialised.
// \retval 1 \p is empty (uninitialised)
// \retval 0 \p has been assigned parameters.

int ecompass_scalar_calibration_is_empty(ecompass_scalar_calibration_t *cal)
{
    if (!cal)
        return 1;

    if (!cal->is_valid)
        return 1;

    return 0;
}



////////////////////////////////////////////////////////////////////////////////
// Initialises scalar calibration parameters. This results in parameters that
// have no effect in the input.
//
// \brief Initialise calibration parameters.
// \param cal Pointer to the parameters to initialise.

void ecompass_scalar_calibration_init(ecompass_scalar_calibration_t *cal, ecompass_sensor_t sensor)
{
    int i;

    // Sanity check
    if (!cal)
        return;

    cal->sensor = sensor;

    // All terms zero except the first power coefficient. This gives us
    // poly(x) = x by default.
    for (i = 0; i <= ECOMPASS_MAX_SCALAR_POWER_TERM; i++)
        cal->term[i] = (i == 1) ? 1.0 : 0.0;

    cal->is_valid = 0;
}





////////////////////////////////////////////////////////////////////////////////
// Applies calibration parameters a ecompass_sensor_reading_t structure.
// Assumes that the uncalibrated x, y, and z components have been populated and
// the is_valid flag MUST be set. Will populate the calibrated x, y, z components
// and sets the is_calibrated flag if appropriate. Will also set the temperature
// fields if \p termperature is valid.
//
// \brief Calculate a calibrated value.
// \param reading Pointer to ecompass_sensor_reading_t structure with uncalibrated
// x, y, z fields poulated. The is_valid MUST be set.
// \param temperature The temperature that the unclibrated point was taken at
// If ECOMPASS_TEMPERATURE_IS_VALID(\p temperature) then tempeature compensation
// will be attempted.
// \param cal Calibration parameters to use.

void ecompass_calibrate(ecompass_sensor_reading_t *reading, float temperature, ecompass_calibration_t *cal)
{
    double x, y, z;
    unsigned int is_calibrated = 0;
    int p;

    // Sanity check
    if (!reading)
        return;

    x = reading->uncalibrated_x;
    y = reading->uncalibrated_y;
    z = reading->uncalibrated_z;

    // Invalidate the concrete coeffs if the temperature differs too much
    if (!ECOMPASS_TEMPERATURE_IS_VALID(cal->concrete.temperature) || (fabs(temperature - cal->concrete.temperature) > cal->temperature_threshold))
        cal->concrete.is_valid = 0;

    if (!cal->concrete.is_valid)
    {
        int r, c, t;
        int was_diagonal[ECOMPASS_MAX_POWER_TERM + 1];

        ecompass_calibration_coeffs_init(&cal->concrete);

        for (p = 0; p <= ECOMPASS_MAX_POWER_TERM; p++)
            was_diagonal[p] = 1;

        for (t = 0; t <= ECOMPASS_MAX_TEMPERATURE_POWER_TERM; t++)
        {
            // Make sure the termerature term is usable.
            if (!cal->term[t].is_valid)
                continue;

            // Can't calculate non-zero powers of temperature if the temperature is invalid
            if ((t > 0) && !ECOMPASS_TEMPERATURE_IS_VALID(temperature))
                continue;

            for (p = 0; p <= ECOMPASS_MAX_POWER_TERM; p++)
            {
                if (!cal->term[t].matrix_is_used[p])
                    continue;

                if (!cal->term[t].matrix_is_diagonal[p])
                    was_diagonal[p] = 0;

                for (r = 0; r < 3; r++)
                {
                    for (c = 0; c < 3; c++)
                    {
                        // Add temperature term for power t
                        switch (t)
                        {
                            case 0:  cal->concrete.parameter[p][r][c] += cal->term[t].parameter[p][r][c]; break;
                            case 1:  cal->concrete.parameter[p][r][c] += cal->term[t].parameter[p][r][c] * temperature; break;
                            case 2:  cal->concrete.parameter[p][r][c] += cal->term[t].parameter[p][r][c] * temperature * temperature; break;
                            case 3:  cal->concrete.parameter[p][r][c] += cal->term[t].parameter[p][r][c] * temperature * temperature * temperature; break;
                            default: cal->concrete.parameter[p][r][c] += cal->term[t].parameter[p][r][c] * pow(temperature, (double)t); break;
                        }
                    }
                }

                // Flag at least one valid entry.
                cal->concrete.matrix_is_used[p] = 1;
                cal->concrete.is_valid = 1;
            }
        }

        for (p = 0; p <= ECOMPASS_MAX_POWER_TERM; p++)
            if (cal->concrete.matrix_is_used[p] && was_diagonal[p])
                cal->concrete.matrix_is_diagonal[p] = 1;

        cal->concrete.temperature = temperature;

#ifdef ECOMPASS_DEBUG_CALCULATION
        printf("Generated concrete calibration at temperature %g.\n", cal->concrete.temperature);
		ecompass_print_calibration(cal, NULL, 1);
#endif
    }

    // Special case if offset is presubtracted.
    if (cal->concrete.is_valid && cal->offset_is_pre_subtracted)
    {
        // This only works if there are no terms greater than 1
        for (p = 2; p <= ECOMPASS_MAX_POWER_TERM; p++)
            if (cal->concrete.matrix_is_used[p])
                break;

        if (p > ECOMPASS_MAX_POWER_TERM)
        {
            if (cal->concrete.matrix_is_used[0])
            {
                x -= cal->concrete.parameter[0][0][0];
                y -= cal->concrete.parameter[0][1][1];
                z -= cal->concrete.parameter[0][2][2];
                is_calibrated = 1;
            }

            if (cal->concrete.matrix_is_used[1])
            {
                double x2, y2, z2;

                x2 = (x * cal->concrete.parameter[1][0][0]) + (y * cal->concrete.parameter[1][0][1]) + (z * cal->concrete.parameter[1][0][2]);
                y2 = (x * cal->concrete.parameter[1][1][0]) + (y * cal->concrete.parameter[1][1][1]) + (z * cal->concrete.parameter[1][1][2]);
                z2 = (x * cal->concrete.parameter[1][2][0]) + (y * cal->concrete.parameter[1][2][1]) + (z * cal->concrete.parameter[1][2][2]);
                x = x2;
                y = y2;
                z = z2;
                is_calibrated = 1;
            }
        }
    }

    else if (cal->concrete.is_valid)
    {
        double x_pow, y_pow, z_pow;

        x = y = z = 0.0;

        // Calibrate as standard polynomial.
        for (p = 0; p <= ECOMPASS_MAX_POWER_TERM; p++)
        {
            if (!cal->concrete.matrix_is_used[p])
                continue;

            switch (p)
            {
                case 0:
                    x_pow = 1.0;
                    y_pow = 1.0;
                    z_pow = 1.0;
                    break;

                case 1:
                    x_pow = reading->uncalibrated_x;
                    y_pow = reading->uncalibrated_y;
                    z_pow = reading->uncalibrated_z;
                    break;

                case 2:
                    x_pow = reading->uncalibrated_x * reading->uncalibrated_x;
                    y_pow = reading->uncalibrated_y * reading->uncalibrated_y;
                    z_pow = reading->uncalibrated_z * reading->uncalibrated_z;
                    break;

                case 3:
                    x_pow = reading->uncalibrated_x * reading->uncalibrated_x * reading->uncalibrated_x;
                    y_pow = reading->uncalibrated_y * reading->uncalibrated_y * reading->uncalibrated_y;
                    z_pow = reading->uncalibrated_z * reading->uncalibrated_z * reading->uncalibrated_z;
                    break;

                default:
                    x_pow = pow(reading->uncalibrated_x, (double)p);
                    y_pow = pow(reading->uncalibrated_y, (double)p);
                    z_pow = pow(reading->uncalibrated_z, (double)p);
                    break;
            }

            if (cal->concrete.matrix_is_diagonal[p])
            {
                x += x_pow * cal->concrete.parameter[p][0][0];
                y += y_pow * cal->concrete.parameter[p][1][1];
                z += z_pow * cal->concrete.parameter[p][2][2];
                is_calibrated = 1;
            }
            else
            {
                double x2, y2, z2;

                x2 = (x_pow * cal->concrete.parameter[p][0][0]) + (y_pow * cal->concrete.parameter[p][0][1]) + (z_pow * cal->concrete.parameter[p][0][2]);
                y2 = (x_pow * cal->concrete.parameter[p][1][0]) + (y_pow * cal->concrete.parameter[p][1][1]) + (z_pow * cal->concrete.parameter[p][1][2]);
                z2 = (x_pow * cal->concrete.parameter[p][2][0]) + (y_pow * cal->concrete.parameter[p][2][1]) + (z_pow * cal->concrete.parameter[p][2][2]);
                x += x2;
                y += y2;
                z += z2;
                is_calibrated = 1;
            }
        }
    }

    if (ECOMPASS_TEMPERATURE_IS_VALID(temperature))
    {
        reading->t = temperature;
        reading->t_is_valid = 1;
    }
    else
    {
        reading->t = ECOMPASS_INVALID_TEMPERATURE;
        reading->t_is_valid = 0;
    }

    reading->x = x;
    reading->y = y;
    reading->z = z;
    reading->is_calibrated = is_calibrated;
}



////////////////////////////////////////////////////////////////////////////////
// Applies calibration parameters a ecompass_scalar_reading_t structure.
// Assumes that the uncalibrated value has been populated and the is_valid flag
// MUST be set. Will populate the calibrated value and set the is_calibrated
// flag if appropriate.
//
// \brief Calculate a calibrated scalar value.
// \param reading Pointer to ecompass_scalar_reading_t structure with
// uncalibrated_value field poulated. The is_valid MUST be set.
// \param cal Calibration parameters to use.

void ecompass_calibrate_scalar(ecompass_scalar_reading_t *reading, const ecompass_scalar_calibration_t *cal)
{
    // Sanity check
    if (!reading)
        return;

    if (cal && cal->is_valid)
    {
#if (ECOMPASS_MAX_SCALAR_POWER_TERM == 0)
        // When ther is only one term the first power coefficient is implicitly one.
        reading->value = cal->poly[0] + reading->uncalibrated_value;
        reading->is_calibrated = 1;
#elif (ECOMPASS_MAX_SCALAR_POWER_TERM == 1)
        reading->value = cal->term[0] + cal->term[1] * reading->uncalibrated_value;
		reading->is_calibrated = 1;
#else
#error "Unimplimented offset calculation for this number of polynomial terms".
#endif
    }
    else
    {
        reading->value = reading->uncalibrated_value;
        reading->is_calibrated = 0;
    }
}



////////////////////////////////////////////////////////////////////////////////
/// Calculates a polynomial term using the coefficient, the independent variable
/// value and the power (i.e. coeff * x^power). Avoids use of pow90 function for
/// small powers.
///
/// \brief Calculate polynomial term
/// \param coeff Co-efficient.
/// \param x Value for independent variable.
/// \param power The term's power.
/// \return \p coeff * \p x ^ \p power

// Unused funrction
//static double calculate_poly_term(double coeff, double x, int power)
//{
//	switch (power)
//	{
//		case 0:  return coeff;
//		case 1:  return coeff * x;
//		case 2:  return coeff * x * x;
//		case 3:  return coeff * x * x * x;
//		default: return coeff * pow(x, (double)power);
//	}
//}



////////////////////////////////////////////////////////////////////////////////
// Initialises the specified ecompass context. If NULL is passes, the globally
// shared context is initialised. Not that the globally shared context is
// automatially initialised so this only needs to be done if you want to
// reset it to defaults.
//
// \brief Initialises an ecompass context to default state.
// \param ecompass Pointer to the ecompass context. Can be NULL in which case
// the global shared context is initialised.

void ecompass_init(ecompass_t *ecompass)
{
    // Use shared instance if no context specified
    if (!ecompass)
    {
        if (!ecompass_shared_instance_initialised)
        {
            ecompass_init(&ecompass_shared_instance);
            ecompass_shared_instance_initialised = 1;
        }
        ecompass = &ecompass_shared_instance;
    }

    ecompass->name[0] = 0;

    ecompass_sensor_reading_init(&ecompass->accelerometer);
    ecompass_sensor_reading_init(&ecompass->magnetometer);
    ecompass_scalar_reading_init(&ecompass->temperature);
    ecompass_orientation_init(&ecompass->orientation);
    ecompass->roll_stabilisation_disable	= DEFAULT_ROLL_STABILISATION_DISABLE;
    ecompass->roll_stabilisation_factor		= DEFAULT_ROLL_STABILISATION_FACTOR;
    ecompass->allow_uncompensated_azimuth   = 0;
    ecompass->calibration_timestamp_is_valid = 0;
    ecompass->calibration_timestamp = (time_t)-1;

    ecompass_calibration_init(&ecompass->accelerometer_calibration, ecompassSensorAccelerometer);
    ecompass_calibration_init(&ecompass->magnetometer_calibration, ecompassSensorMagnetometer);
    ecompass_scalar_calibration_init(&ecompass->temperature_calibration, ecompassSensorTemperature);

#if defined(ECOMPASS_MANUAL_ZERO_OFFSET_COUNT) && (ECOMPASS_MANUAL_ZERO_OFFSET_COUNT > 0)
    {
		int i;
		for (i = 0; i < ECOMPASS_MANUAL_ZERO_OFFSET_COUNT; i++)
			ecompass->manual_zero_offset[i] = 0.0;
	}
#endif

#ifdef ECOMPASS_SENSOR_VARIANCE
    ecompass->variance_put = 0;
	ecompass->variance_get = 0;
	ecompass->variance_count = 0;
	ecompass->variance_needs_update  = 1;
	ecompass->variance_use_number	 = DEFAULT_VARIANCE_BY_NUMBER;
	ecompass->variance_duration_usec = DEFAULT_VARIANCE_DURATION;
	ecompass->variance_number		 = DEFAULT_VARIANCE_NUMBER;
#endif

}



////////////////////////////////////////////////////////////////////////////////
/// Sets the nanme for an ecomapss. The name is not really significant. It is
/// only used when printing diagnostics.
///
/// \brief Sets the name for an ecompass.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context is initialised.
/// \param name Name for eCompass.

void ecompass_set_name(ecompass_t *ecompass, const char *name)
{
    // Use shared instance if no context specified
    if (!ecompass)
    {
        if (!ecompass_shared_instance_initialised)
        {
            ecompass_init(&ecompass_shared_instance);
            ecompass_shared_instance_initialised = 1;
        }
        ecompass = &ecompass_shared_instance;
    }

    if (name)
    {
        strncpy(ecompass->name, name, ECOMPASS_MAX_NAME_LEN+1);
        ecompass->name[ECOMPASS_MAX_NAME_LEN] = '\0';
    }
    else
        ecompass->name[0] = '\0';
}



////////////////////////////////////////////////////////////////////////////////
/// Invalidates the reading values of an ecompass. This includes the accelerometer
/// magnetometer, temperature and orientation values.
///
/// \brief Invalidate reading.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context is invalidated.

void ecompass_invalidate_reading(ecompass_t *ecompass)
{
    ecompass_sensor_reading_init(&ecompass->accelerometer);
    ecompass_sensor_reading_init(&ecompass->magnetometer);
    ecompass_scalar_reading_init(&ecompass->temperature);
    ecompass->orientation.roll_is_valid		= 0;
    ecompass->orientation.pitch_is_valid	= 0;
    ecompass->orientation.azimuth_is_valid	= 0;
}



////////////////////////////////////////////////////////////////////////////////
// Sets all of the manual zero offsets to a value of zero.
//
// \brief Clear manual zero offsets.
// \param ecompass Pointer to the ecompass context. Can be NULL in which case
// the global shared context is set to zero.

void ecompass_clear_manual_zero_offsets(ecompass_t *ecompass)
{
#if defined(ECOMPASS_MANUAL_ZERO_OFFSET_COUNT) && (ECOMPASS_MANUAL_ZERO_OFFSET_COUNT > 0)
    int i;

	// Use shared instance if no context specified
	if (!ecompass)
	{
		if (!ecompass_shared_instance_initialised)
		{
			ecompass_init(&ecompass_shared_instance);
			ecompass_shared_instance_initialised = 1;
		}
		ecompass = &ecompass_shared_instance;
	}

	for (i = 0; i < ECOMPASS_MANUAL_ZERO_OFFSET_COUNT; i++)
		ecompass->manual_zero_offset[i] = 0.0;
#endif
}




////////////////////////////////////////////////////////////////////////////////
// Roll stabilisation is required in cases where the pitch angle becomes
// vertical (ie. +/- PI) and the roll axis coincides with the gravitation
// vector (down) and so can't be calculated. The first problem this causes is
// that roll is undefined at vertical pitch. The second problem is that NEAR
// vertical pitch the accelerometer components used to calulate roll become
// which causes them to be swamped by sensor noise. A common solution in
// aerospace application is to use a modified the accelelrometer z component
// for roll calculations which adds a small multiple of the x component.
// This ensures that compensation is only seen near vertial pitch where x >> z.
// The effect of this compensation is to drive roll smoothly to zero as pitch
// becomes vertial. Smaller values of the stabilisation factor (the mulitple of
// x added to z) result in smaller errors in roll near vertical (due to less
// compensation) but greater susceptibilty to sensor noise in the accelerometer
// reading.
//
// Azimuth calculations have a similar problem at vertical pitch. The azimuth
// is essentially the horizontal angle from north to the plane in which the
// device is pitching. At vertical pitch there is no such unique plane and
// azimuth can't be calculated (ie pitching down by PI at any azimuth will
// result in the same vertical orientation). A tilt compensated azimuth
// using a stablised roll value results in a well-behaved orientation since
// as roll gets driven to zero, azimuth will be driven to the local negative z
// direction of the device (ie up in the device's frame of reference - where
// roll is zero).
//
// To picture the effect of roll stabilisation and tilt compensated azimuth,
// imagine the device pitching down past vertical. On the other side of
// vertical the azimuth jumps around by PI (180 degrees) ince it is pointing
// in the opposite direction and the roll also changes to reflect the fact that
// the device has now flipped onto its back. Rather than a mathematical
// singularity at vertical the roll smoothly swings through zero and the
// azimuth swings though the device's local up vector and the resulting angles
// will orient the device correctly.
//
// \brief Disables roll stabilisation for orientation calculations.
// \param ecompass Pointer to the ecompass context. Can be NULL in which case
// the global shared context will be used.
// \param disable Desired disabled state for roll stabilisation. 1 = disabled,
// 0 = enabled. Default state is enabled.

void ecompass_roll_stabilisation_disable(ecompass_t *ecompass, int disable)
{
    // Use shared instance if no context specified
    if (!ecompass)
    {
        if (!ecompass_shared_instance_initialised)
        {
            ecompass_init(&ecompass_shared_instance);
            ecompass_shared_instance_initialised = 1;
        }
        ecompass = &ecompass_shared_instance;
    }

    if (ecompass->roll_stabilisation_disable == disable)
        return;

    ecompass->roll_stabilisation_disable = disable;

    ecompass_calculate_orientation(ecompass);
}



////////////////////////////////////////////////////////////////////////////////
// Sets the factor used by roll stabilisation if enabled. This factor is the
// multiplier for the x accelerometer component that is added to the z component
// for stabilisation. A value of zero effectively disables any compensation
// (but use ecompass_roll_stabilisation_enable() for this as it is more
// efficient). The value should be non-negative and as small as possible for
// increased accuracy far from vertical but big enough to suppress sensor noise
// (and therefore increase accuracy near vertical). Default value is 0.01 which
// should be suitable for most situations.
// See ecompass_roll_stabilisation_enable() for detailed discussion.
//
// \brief Specified roll stabilisation factor.
// \param ecompass Pointer to the ecompass context. Can be NULL in which case
// the global shared context will be used.
// \param factor Roll stabilisation factor. default value is 0.01.

void ecompass_roll_stabilisation_factor(ecompass_t *ecompass, double factor)
{
    // Use shared instance if no context specified
    if (!ecompass)
    {
        if (!ecompass_shared_instance_initialised)
        {
            ecompass_init(&ecompass_shared_instance);
            ecompass_shared_instance_initialised = 1;
        }
        ecompass = &ecompass_shared_instance;
    }

    ecompass->roll_stabilisation_factor = factor;

    ecompass_calculate_orientation(ecompass);
}



////////////////////////////////////////////////////////////////////////////////
// Normally azimuth is calculated from tilt compensated magnetometer
// readings whereby the magnetometer vertor is rotated to compensate for
// device tilt using acceleromter pitch and roll measurements rather than
// using the x, y components and ignoring z (accurate only near horizontal)
// The compensated calculation is more accurate but requires a valid
// accelerometer reading. Normally if no such reading is availble the
// aziumuth is calulated using the uncompensated approach but is marked as
// invalid. When allow_uncompensated_azimuth enabled, the uncompensated
// result is marked as valid. This should always be disabled unless
// there is no accelerometer or for diagnostic purposes. Default is 0.
//
// \brief Allow or disallow uncompensated (for device tilt) azimuth calculation
// to be considered valid.
// \param ecompass Pointer to the ecompass context. Can be NULL in which case
// the global shared context will be used.
// \param allow Desired allowability of uncompensated azimuth values.
// 0 = disallow, 1 = allow.

void allow_uncompensated_azimuth(ecompass_t *ecompass, int allow)
{
    // Use shared instance if no context specified
    if (!ecompass)
    {
        if (!ecompass_shared_instance_initialised)
        {
            ecompass_init(&ecompass_shared_instance);
            ecompass_shared_instance_initialised = 1;
        }
        ecompass = &ecompass_shared_instance;
    }

    // No action if no change
    if (ecompass->allow_uncompensated_azimuth == allow)
        return;

    ecompass->allow_uncompensated_azimuth = allow;

    ecompass_calculate_orientation(ecompass);
}



////////////////////////////////////////////////////////////////////////////////
// \brief Invalidate the current accelerometer reading.
// \param ecompass Pointer to the ecompass context. Can be NULL in which case
// the global shared context will be used.

void ecompass_accelerometer_invalidate(ecompass_t *ecompass)
{
    // Use shared instance if no context specified
    if (!ecompass)
    {
        if (!ecompass_shared_instance_initialised)
        {
            ecompass_init(&ecompass_shared_instance);
            ecompass_shared_instance_initialised = 1;
        }
        ecompass = &ecompass_shared_instance;
    }

    ecompass->accelerometer.is_valid = 0;
    ecompass_calculate_orientation(ecompass);
}



////////////////////////////////////////////////////////////////////////////////
// Sets the current accelerometer reading and recalculates orientation. The
// current (most recently set) magnetometer reading will be used for the
// calculation. If the \p accelerometer_is_pre_calibrated parameter is set,
// the \p x, \p y, \p z components are assumed to be in g's (gravitaional
// acceleration units). If not, calibration is attempted and it is assumed that
// the components will be transformed to g's by the calibration parameters.
//
// \brief Calculate orientation using new accelerometer reading.
// \param ecompass Pointer to the ecompass context. Can be NULL in which case
// the global shared context will be used.
// \param x Accelerometer x component reading in g's.
// \param y Accelerometer y component reading in g's.
// \param z Accelerometer z component reading in g's.
// \param temperature Temperature at which the reading were taken.
// \param accelerometer_is_pre_calibrated Flag indicating that the \p x, \p y
// and \p z components are pre calibrated. If set, calibration will not be
// applied but the accelerometer vector will still be marked as calibrated.

void ecompass_accelerometer_reading(ecompass_t *ecompass, double x, double y, double z, float t, int is_pre_calibrated)
{
    // Use shared instance if no context specified
    if (!ecompass)
    {
        if (!ecompass_shared_instance_initialised)
        {
            ecompass_init(&ecompass_shared_instance);
            ecompass_shared_instance_initialised = 1;
        }
        ecompass = &ecompass_shared_instance;
    }

    ecompass_sensor_reading_init_with_uncalibrated(&ecompass->accelerometer, x, y, z);

    if (ECOMPASS_TEMPERATURE_IS_VALID(t))
    {
        ecompass->accelerometer.t = t;
        ecompass->accelerometer.t_is_valid = 1;
    }
    else
        ecompass->accelerometer.t_is_valid = 0;

    if (is_pre_calibrated)
        ecompass->accelerometer.is_calibrated = 1;
    else
        ecompass_calibrate(&ecompass->accelerometer, t, &ecompass->accelerometer_calibration);

    ecompass_calculate_orientation(ecompass);

#ifdef ECOMPASS_SENSOR_VARIANCE
    {
		struct timeval timestamp;

		if (gettimeofday(&timestamp, NULL) == 0)
		{
			ecompass->variance_buffer[ecompass->variance_put].timestamp = ((uint64_t)timestamp.tv_sec * 1000000LL) + (uint64_t)timestamp.tv_usec;
			ecompass->variance_buffer[ecompass->variance_put].acc_x = ecompass->accelerometer.x;
			ecompass->variance_buffer[ecompass->variance_put].acc_y = ecompass->accelerometer.y;
			ecompass->variance_buffer[ecompass->variance_put].acc_z = ecompass->accelerometer.z;
			ecompass->variance_buffer[ecompass->variance_put].acc_is_valid = 1;
			ecompass->variance_buffer[ecompass->variance_put].mag_is_valid = 0;
			ecompass->variance_count++;
			if (++ecompass->variance_put >= ECOMPASS_SENSOR_VARIANCE_MAX_SAMPLES)
				ecompass->variance_put = 0;
			if (ecompass->variance_put == ecompass->variance_get)
			{
				ecompass->variance_count--;
				if (++ecompass->variance_get >= ECOMPASS_SENSOR_VARIANCE_MAX_SAMPLES)
					ecompass->variance_get = 0;
			}
			ecompass->variance_needs_update = 1;
		}
	}
#endif
}



////////////////////////////////////////////////////////////////////////////////
// \brief Invalidate the current magnetometer reading.
// \param ecompass Pointer to the ecompass context. Can be NULL in which case
// the global shared context will be used.

void ecompass_magnetometer_invalidate(ecompass_t *ecompass)
{
    // Use shared instance if no context specified
    if (!ecompass)
    {
        if (!ecompass_shared_instance_initialised)
        {
            ecompass_init(&ecompass_shared_instance);
            ecompass_shared_instance_initialised = 1;
        }
        ecompass = &ecompass_shared_instance;
    }

    ecompass->magnetometer.is_valid = 0;
    ecompass_calculate_orientation(ecompass);
}



////////////////////////////////////////////////////////////////////////////////
// Sets the current magnetometer reading and recalculates orientation. The
// current (most recently set) accelerometer reading will be used for the
// calculation. If the \p is_pre_calibrated parameter is set, the \p x, \p y,
// \p z components are assumed to be in micro Teslas. If not, calibration is
// attempted and it is assumed that the components will be transformed to micro
// Teslas by the calibration parameters.
//
// \brief Calculate orientation using new magnetometer reading.
// \param ecompass Pointer to the ecompass context. Can be NULL in which case
// the global shared context will be used.
// \param x Magnetometer x component reading in g's.
// \param y Magnetometer y component reading in g's.
// \param z Magnetometer z component reading in g's.
// \param t Temperature at which the reading were taken.
// \param is_pre_calibrated Flag indicating that the \p x, \p y
// and \p z components are pre calibrated. If set, calibration will not be
// applied but the magnetometer vector will still be marked as calibrated.

void ecompass_magnetometer_reading(ecompass_t *ecompass, double x, double y, double z, float t, int is_pre_calibrated)
{
    // Use shared instance if no context specified
    if (!ecompass)
    {
        if (!ecompass_shared_instance_initialised)
        {
            ecompass_init(&ecompass_shared_instance);
            ecompass_shared_instance_initialised = 1;
        }
        ecompass = &ecompass_shared_instance;
    }

    ecompass_sensor_reading_init_with_uncalibrated(&ecompass->magnetometer, x, y, z);

    if (ECOMPASS_TEMPERATURE_IS_VALID(t))
    {
        ecompass->magnetometer.t = t;
        ecompass->magnetometer.t_is_valid = 1;
    }
    else
        ecompass->magnetometer.t_is_valid = 0;

    if (is_pre_calibrated)
        ecompass->magnetometer.is_calibrated = 1;
    else
        ecompass_calibrate(&ecompass->magnetometer, t, &ecompass->magnetometer_calibration);

    ecompass_calculate_orientation(ecompass);

#ifdef ECOMPASS_SENSOR_VARIANCE
    {
		struct timeval timestamp;

		if (gettimeofday(&timestamp, NULL) == 0)
		{
			ecompass->variance_buffer[ecompass->variance_put].timestamp = ((uint64_t)timestamp.tv_sec * 1000000LL) + (uint64_t)timestamp.tv_usec;
			ecompass->variance_buffer[ecompass->variance_put].acc_is_valid = 0;
			ecompass->variance_buffer[ecompass->variance_put].mag_x = ecompass->magnetometer.x;
			ecompass->variance_buffer[ecompass->variance_put].mag_y = ecompass->magnetometer.y;
			ecompass->variance_buffer[ecompass->variance_put].mag_z = ecompass->magnetometer.z;
			ecompass->variance_buffer[ecompass->variance_put].mag_is_valid = 1;
			ecompass->variance_count++;
			if (++ecompass->variance_put >= ECOMPASS_SENSOR_VARIANCE_MAX_SAMPLES)
				ecompass->variance_put = 0;
			if (ecompass->variance_put == ecompass->variance_get)
			{
				ecompass->variance_count--;
				if (++ecompass->variance_get >= ECOMPASS_SENSOR_VARIANCE_MAX_SAMPLES)
					ecompass->variance_get = 0;
			}
			ecompass->variance_needs_update = 1;
		}
	}
#endif
}



////////////////////////////////////////////////////////////////////////////////
// \brief Invalidate both the current accelerometer and magnetometer readings.
// \param ecompass Pointer to the ecompass context. Can be NULL in which case
// the global shared context will be used.

void ecompass_accelerometer_and_magnetometer_invalidate(ecompass_t *ecompass)
{
    // Use shared instance if no context specified
    if (!ecompass)
    {
        if (!ecompass_shared_instance_initialised)
        {
            ecompass_init(&ecompass_shared_instance);
            ecompass_shared_instance_initialised = 1;
        }
        ecompass = &ecompass_shared_instance;
    }

    ecompass->accelerometer.is_valid = 0;
    ecompass->magnetometer.is_valid  = 0;
    ecompass_calculate_orientation(ecompass);
}



////////////////////////////////////////////////////////////////////////////////
// Sets the current accelerometer and magnetometer reading and recalculates
// orientation. If the \p accelerometer_is_pre_calibrated parameter is set,
// the \p ax, \p ay, \p az components are assumed to be in g's (gravitaional
// acceleration units). If not, calibration is attempted and it is assumed that
// the components will be transformed to g's by the calibration parameters.
// If the \p is_pre_calibrated parameter is set, the \p mx, \p my, \p mz
// components are assumed to be in micro Teslas. If not, calibration is
// attempted and it is assumed that the components will be transformed to micro
// Teslas by the calibration parameters.
//
// \brief Calculate orientation using new accelerometer and magnetometer readings.
// \param ecompass Pointer to the ecompass context. Can be NULL in which case
// the global shared context will be used.
// \param ax Accelerometer x component reading in g's.
// \param ay Accelerometer y component reading in g's.
// \param az Accelerometer z component reading in g's.
// \param at Accelerometer temperature.
// \param mx Magnetometer x component reading in micro Teslas.
// \param my Magnetometer y component reading in micro Teslas.
// \param mz Magnetometer z component reading in micro Teslas.
// \param at Magnetometer temperature.
// \param accelerometer_is_pre_calibrated Flag indicating that the \p ax, \p ay
// and \p az components are pre calibrated. If set, calibration will not be
// applied but the accelerometer vector will still be marked as calibrated.
// \param magnetometer_is_pre_calibrated Flag indicating that the \p mx, \p my
// and \p mz components are pre calibrated. If set, calibration will not be
// applied but the magnetometer vector will still be marked as calibrated.

void ecompass_accelerometer_and_magnetometer_reading(ecompass_t *ecompass,
                                                     double ax, double ay, double az, float at,
                                                     double mx, double my, double mz, float mt,
                                                     int accelerometer_is_pre_calibrated, int magnetometer_is_pre_calibrated)
{
    // Use shared instance if no context specified
    if (!ecompass)
    {
        if (!ecompass_shared_instance_initialised)
        {
            ecompass_init(&ecompass_shared_instance);
            ecompass_shared_instance_initialised = 1;
        }
        ecompass = &ecompass_shared_instance;
    }

    ecompass_sensor_reading_init_with_uncalibrated(&ecompass->accelerometer, ax, ay, az);
    ecompass_sensor_reading_init_with_uncalibrated(&ecompass->magnetometer,  mx, my, mz);

    if (ECOMPASS_TEMPERATURE_IS_VALID(at))
    {
        ecompass->accelerometer.t = at;
        ecompass->accelerometer.t_is_valid = 1;
    }
    else
        ecompass->accelerometer.t_is_valid = 0;

    if (ECOMPASS_TEMPERATURE_IS_VALID(mt))
    {
        ecompass->magnetometer.t  = mt;
        ecompass->magnetometer.t_is_valid  = 1;
    }
    else
        ecompass->magnetometer.t_is_valid  = 0;

    if (accelerometer_is_pre_calibrated)
        ecompass->accelerometer.is_calibrated = 1;
    else
        ecompass_calibrate(&ecompass->accelerometer, at, &ecompass->accelerometer_calibration);

    if (magnetometer_is_pre_calibrated)
        ecompass->magnetometer.is_calibrated = 1;
    else
        ecompass_calibrate(&ecompass->magnetometer, mt, &ecompass->magnetometer_calibration);

    ecompass_calculate_orientation(ecompass);

#ifdef ECOMPASS_SENSOR_VARIANCE
    {
		struct timeval timestamp;

		if (gettimeofday(&timestamp, NULL) == 0)
		{
			ecompass->variance_buffer[ecompass->variance_put].timestamp = ((uint64_t)timestamp.tv_sec * 1000000LL) + (uint64_t)timestamp.tv_usec;
			ecompass->variance_buffer[ecompass->variance_put].acc_x = ecompass->accelerometer.x;
			ecompass->variance_buffer[ecompass->variance_put].acc_y = ecompass->accelerometer.y;
			ecompass->variance_buffer[ecompass->variance_put].acc_z = ecompass->accelerometer.z;
			ecompass->variance_buffer[ecompass->variance_put].acc_is_valid = 1;
			ecompass->variance_buffer[ecompass->variance_put].mag_x = ecompass->magnetometer.x;
			ecompass->variance_buffer[ecompass->variance_put].mag_y = ecompass->magnetometer.y;
			ecompass->variance_buffer[ecompass->variance_put].mag_z = ecompass->magnetometer.z;
			ecompass->variance_buffer[ecompass->variance_put].mag_is_valid = 1;
			ecompass->variance_count++;
			if (++ecompass->variance_put >= ECOMPASS_SENSOR_VARIANCE_MAX_SAMPLES)
				ecompass->variance_put = 0;
			if (ecompass->variance_put == ecompass->variance_get)
			{
				ecompass->variance_count--;
				if (++ecompass->variance_get >= ECOMPASS_SENSOR_VARIANCE_MAX_SAMPLES)
					ecompass->variance_get = 0;
			}
			ecompass->variance_needs_update = 1;
		}
	}
#endif
}



////////////////////////////////////////////////////////////////////////////////
// \brief Invalidate the current temperature reading.
// \param ecompass Pointer to the ecompass context. Can be NULL in which case
// the global shared context will be used.

void ecompass_temperature_invalidate(ecompass_t *ecompass)
{
    // Use shared instance if no context specified
    if (!ecompass)
    {
        if (!ecompass_shared_instance_initialised)
        {
            ecompass_init(&ecompass_shared_instance);
            ecompass_shared_instance_initialised = 1;
        }
        ecompass = &ecompass_shared_instance;
    }

    ecompass_scalar_reading_init(&ecompass->temperature);
}



////////////////////////////////////////////////////////////////////////////////
// Sets the current overall temperature of the ecompass reading.
//
// \brief Set ecompass temperature.
// \param ecompass Pointer to the ecompass context. Can be NULL in which case
// the global shared context will be used.
// \param t Temperature in degrees celcius. Use constant ECOMPASS_INVALID_TEMPERATURE
// to represent unknown temperature.

void ecompass_temperature_reading(ecompass_t *ecompass, float t)
{
    // Use shared instance if no context specified
    if (!ecompass)
    {
        if (!ecompass_shared_instance_initialised)
        {
            ecompass_init(&ecompass_shared_instance);
            ecompass_shared_instance_initialised = 1;
        }
        ecompass = &ecompass_shared_instance;
    }

    ecompass->temperature.uncalibrated_value = t;
    ecompass->temperature.is_valid = 1;
    ecompass_calibrate_scalar(&ecompass->temperature, &ecompass->temperature_calibration);
}



////////////////////////////////////////////////////////////////////////////////
/// Calculate the device orientation based on the current state of the specified
/// ecompass context (or the global shared context if NULL specified). The
/// validity flags for the orientation's roll pitch and azimuth are set
/// according to the results of the calculation. A valid accelerometer reading
/// is required to calulate roll and pitch. Valid accelerometer and magnetometer
/// readings are required to calculate azimuth unless allow_uncompensated_azimuth
/// is set in which case only a valid magnetometer reading is required.
///
/// \brief Calculate orientation.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context will be used.

static void ecompass_calculate_orientation(ecompass_t *ecompass)
{
    // Use shared instance if no context specified
    if (!ecompass)
    {
        if (!ecompass_shared_instance_initialised)
        {
            ecompass_init(&ecompass_shared_instance);
            ecompass_shared_instance_initialised = 1;
        }
        ecompass = &ecompass_shared_instance;
    }

    // Initialise to invalid orientation
    ecompass->orientation.azimuth_is_valid = 0;
    ecompass->orientation.pitch_is_valid   = 0;
    ecompass->orientation.roll_is_valid	   = 0;

    // Calculate pitch and roll if we have a valid accelerometer reading
    if (ecompass->accelerometer.is_valid)
    {
        double x, y, z, len;

        x = ecompass->accelerometer.x;
        y = ecompass->accelerometer.y;
        z = ecompass->accelerometer.z;
        len = sqrt(x * x + y * y + z * z);
        if (len > 0.0)
        {
            // Normalise accelerometer reading
            x /= len;
            y /= len;
            z /= len;

            // Calculate pitch.
            ecompass->orientation.pitch = atan2(-x, sqrt(y * y + z * z));
            ecompass->orientation.pitch_is_valid = 1;

            // Calculate roll.
            if (ecompass->roll_stabilisation_disable)
            {
                ecompass->orientation.roll  = atan2( y,  z);
            }
            else
            {
                ecompass->orientation.roll  = atan2( y, sqrt(z * z + ecompass->roll_stabilisation_factor * x * x));
                if (z < 0.0)
                    ecompass->orientation.roll = ((ecompass->orientation.roll >= 0.0) ? M_PI : -M_PI) - ecompass->orientation.roll;
            }
            ecompass->orientation.roll_is_valid = 1;
        }
    }

    if (ecompass->magnetometer.is_valid)
    {
        double x, y;

        // Get horizontal magnetometer components. If we have, pitch and roll values
        // then we use them to compensate for tilt
        if (ecompass->orientation.pitch_is_valid && ecompass->orientation.roll_is_valid)
        {
            x = (ecompass->magnetometer.x) * cos(ecompass->orientation.pitch) +
                (ecompass->magnetometer.y) * sin(ecompass->orientation.pitch) * sin(ecompass->orientation.roll) +
                (ecompass->magnetometer.z) * sin(ecompass->orientation.pitch) * cos(ecompass->orientation.roll);
            y = (ecompass->magnetometer.y) * cos(ecompass->orientation.roll) -
                (ecompass->magnetometer.z) * sin(ecompass->orientation.roll);
            ecompass->orientation.azimuth_is_valid = 1;
        }
        else
        {
            x = ecompass->magnetometer.x;
            y = ecompass->magnetometer.y;
            if (ecompass->allow_uncompensated_azimuth)
                ecompass->orientation.azimuth_is_valid = 1;
        }

        // Calculate azimuth from horizontal magnetometer components.
        ecompass->orientation.azimuth = atan2(-y, x);

        // Limit azimuth to [0,360) not (-180,180]
        if (ecompass->orientation.azimuth <  0.0)		   ecompass->orientation.azimuth += 2.0 * M_PI;
        if (ecompass->orientation.azimuth >= (2.0 * M_PI)) ecompass->orientation.azimuth -= 2.0 * M_PI;
    }

#if defined(ECOMPASS_MANUAL_ZERO_OFFSET_COUNT) && (ECOMPASS_MANUAL_ZERO_OFFSET_COUNT > 0)
    ecompass->orientation.roll_for_display = ecompass->orientation.roll - ecompass->manual_zero_offset[ECOMPASS_MANUAL_ZERO_ROLL_OFFSET];
	if ((ecompass->orientation.roll_for_display < 0.0) && !ecompass_roll_for_display_range_is_symmetric)
		ecompass->orientation.roll_for_display += 2.0 * M_PI;
#endif
}



////////////////////////////////////////////////////////////////////////////////
// Copies the accelerometer calibration parameters of an ecompass_t to a
// parameters structure. If the ecompass pointer is NULL, the parameters are
// set to defaults. If the parameters are NULL, no action is taken.
//
// \brief Get accelerometer calibration parameters for an ecompass
// \param ecompass Pointer to ecompass. If NULL, \p param is set to defaults.
// \param cal pointer to structure to which the parameters are copied.

void ecompass_calibration_get_accelerometer(const ecompass_t *ecompass, ecompass_calibration_t *cal)
{
    // Sanity chack
    if (!cal)
        return;

    if (ecompass)
        *cal = ecompass->accelerometer_calibration;
    else
        ecompass_calibration_init(cal, ecompassSensorAccelerometer);
}



////////////////////////////////////////////////////////////////////////////////
// Copies the accelerometer calibration parameters to an ecompass_t from a
// parameters structure. If the parameters pointer is NULL, the ecompass
// parameters are set to defaults. If the ecompass is NULL, no action is taken.
//
// \brief Set accelerometer calibration parameters for an ecompass
// \param ecompass Pointer to ecompass.
// \param cal pointer to structure from which the parameters are copied.
// If NULL, calibration parameters for \p ecompass are set to defaults.

void ecompass_calibration_set_accelerometer(ecompass_t *ecompass, const ecompass_calibration_t *cal)
{
    // Sanity chack
    if (!ecompass)
        return;

    if (cal)
    {
        ecompass->accelerometer_calibration = *cal;
        ecompass->accelerometer_calibration.sensor = ecompassSensorAccelerometer;
    }
    else
        ecompass_calibration_init(&ecompass->accelerometer_calibration, ecompassSensorAccelerometer);
}



////////////////////////////////////////////////////////////////////////////////
// Copies the magnetometer calibration parameters of an ecompass_t to a
// parameters structure. If the ecompass pointer is NULL, the parameters are
// set to defaults. If the parameters are NULL, no action is taken.
//
// \brief Get magnetometer calibration parameters for an ecompass
// \param ecompass Pointer to ecompass. If NULL, \p param is set to defaults.
// \param cal pointer to structure to which the parameters are copied.

void ecompass_calibration_get_magnetometer(const ecompass_t *ecompass, ecompass_calibration_t *cal)
{
    // Sanity chack
    if (!cal)
        return;

    if (ecompass)
        *cal = ecompass->magnetometer_calibration;
    else
        ecompass_calibration_init(cal, ecompassSensorMagnetometer);
}



////////////////////////////////////////////////////////////////////////////////
// Copies the magnetometer calibration parameters to an ecompass_t from a
// parameters structure. If the parameters pointer is NULL, the ecompass
// parameters are set to defaults. If the ecompass is NULL, no action is taken.
//
// \brief Set magnetometer calibration parameters for an ecompass
// \param ecompass Pointer to ecompass.
// \param cal pointer to structure from which the parameters are copied.
// If NULL, calibration parameters for \p ecompass are set to defaults.

void ecompass_calibration_set_magnetometer(ecompass_t *ecompass, const ecompass_calibration_t *cal)
{
    // Sanity chack
    if (!ecompass)
        return;

    if (cal)
    {
        ecompass->magnetometer_calibration = *cal;
        ecompass->magnetometer_calibration.sensor = ecompassSensorMagnetometer;
    }
    else
        ecompass_calibration_init(&ecompass->magnetometer_calibration, ecompassSensorMagnetometer);
}



////////////////////////////////////////////////////////////////////////////////
// Copies the temperature calibration parameters of an ecompass_t to a
// parameters structure. If the ecompass pointer is NULL, the parameters are
// set to defaults. If the parameters are NULL, no action is taken.
//
// \brief Get temperature calibration parameters for an ecompass
// \param cal Pointer to ecompass. If NULL, \p param is set to defaults.
// \param param pointer to structure to which the parameters are copied.

void ecompass_calibration_get_temperature(const ecompass_t *ecompass, ecompass_scalar_calibration_t *cal)
{
    // Sanity chack
    if (!cal)
        return;

    if (ecompass)
        *cal = ecompass->temperature_calibration;
    else
        ecompass_scalar_calibration_init(cal, ecompassSensorTemperature);
}



////////////////////////////////////////////////////////////////////////////////
// Copies the temperature calibration parameters to an ecompass_t from a
// parameters structure. If the parameters pointer is NULL, the ecompass
// parameters are set to defaults. If the ecompass is NULL, no action is taken.
//
// \brief Set temperature calibration parameters for an ecompass
// \param ecompass Pointer to ecompass.
// \param cal pointer to structure from which the parameters are copied.
// If NULL, calibration parameters for \p ecompass are set to defaults.

void ecompass_calibration_set_temperature(ecompass_t *ecompass, const ecompass_scalar_calibration_t *cal)
{
    // Sanity chack
    if (!ecompass)
        return;

    if (cal)
    {
        ecompass->temperature_calibration = *cal;
        ecompass->temperature_calibration.sensor = ecompassSensorTemperature;
    }
    else
        ecompass_scalar_calibration_init(&ecompass->temperature_calibration, ecompassSensorTemperature);
}



////////////////////////////////////////////////////////////////////////////////
// Sets the calibration timestamp of an ecompass to the specified value.
//
// \brief Set calibration timestamp.
// \param ecompass Pointer to ecompass.
// \param timestamp Time to which the timestamp should be set.

void ecompass_calibration_timestamp_set(ecompass_t *ecompass, time_t timestamp)
{
    if (ecompass)
    {
        ecompass->calibration_timestamp = timestamp;
        ecompass->calibration_timestamp_is_valid = (ecompass->calibration_timestamp == (time_t)-1) ? 0 : 1;
    }
}



////////////////////////////////////////////////////////////////////////////////
// Sets the calibration timestamp of an ecompass to the current time.
//
// \brief Set calibration timestamp to current time.
// \param ecompass Pointer to ecompass.

void ecompass_calibration_timestamp_now(ecompass_t *ecompass)
{
    if (ecompass)
    {
        ecompass->calibration_timestamp = time(NULL);
        ecompass->calibration_timestamp_is_valid = (ecompass->calibration_timestamp == (time_t)-1) ? 0 : 1;
    }
}




////////////////////////////////////////////////////////////////////////////////
// Invalidates the calibration timestamp of an ecompass. The ecompass
// essentially has no timestamp of last calibration.
//
// \brief Invalidate calibration timestamp.
// \param ecompass Pointer to ecompass.

void ecompass_calibration_timestamp_invalidate(ecompass_t *ecompass)
{
    if (ecompass)
    {
        ecompass->calibration_timestamp = (time_t)-1;
        ecompass->calibration_timestamp_is_valid = 0;
    }
}



////////////////////////////////////////////////////////////////////////////////
// Sets a manual zero offset value.
//
// \brief Set manual zero offset.
// \param ecompass Pointer to ecompass.
// \param which The offset is to be set. Values may be ECOMPASS_MANUAL_ZERO_ACC_ROLL
// \param how How to set the value. May be ECOMPASS_MANUAL_ZERO_CLEAR,
// ECOMPASS_MANUAL_ZERO_SET_AUTO, ECOMPASS_MANUAL_ZERO_SET_VALUE, ECOMPASS_MANUAL_ZERO_SET_VALUE_DEGREES
// or ECOMPASS_MANUAL_ZERO_SET_VALUE_RADIANS.
// \param value The value to be set when \p how is ECOMPASS_MANUAL_ZERO_SET_VALUE,
// ECOMPASS_MANUAL_ZERO_SET_VALUE_DEGREES or ECOMPASS_MANUAL_ZERO_SET_VALUE_RADIANS.
// Ignored otherwise.
// \return Error code
// \retval 0 No error.
// \retval -1 Value was unchanged.
// \retval -2 Invalid parameters

int ecompass_set_zero_offset(ecompass_t *ecompass, int which, int how, double value)
{
    if (!ecompass)
        return -2;

#if defined(ECOMPASS_MANUAL_ZERO_OFFSET_COUNT) && (ECOMPASS_MANUAL_ZERO_OFFSET_COUNT > 0)
    switch (which)
	{
		case ECOMPASS_MANUAL_ZERO_ROLL_OFFSET:
			if (how == ECOMPASS_MANUAL_ZERO_CLEAR)
				value = 0.0;
			else if (how == ECOMPASS_MANUAL_ZERO_SET_AUTO)
				value = ecompass->orientation.roll_is_valid ? ecompass->orientation.roll : 0.0;
			else if ((how == ECOMPASS_MANUAL_ZERO_SET_VALUE) || (how == ECOMPASS_MANUAL_ZERO_SET_VALUE_RADIANS))
				{ /* Nothing - leave value as is */ }
			else if (how == ECOMPASS_MANUAL_ZERO_SET_VALUE_DEGREES)
				value = value * M_PI / 180.0;
			else
				return -2;

			if (ecompass->manual_zero_offset[which] != value)
			{
				ecompass->manual_zero_offset[which] = value;
				ecompass_calibration_timestamp_now(ecompass);
				return 0;
			}
			return -1;
	}
#endif

    return -2;
}



////////////////////////////////////////////////////////////////////////////////
/// Gets a manual zero offset value.
///
/// \brief Set manual zero offset.
/// \param ecompass Pointer to ecompass.
/// \param which The offset is to be gotten. Values may be ECOMPASS_MANUAL_ZERO_ACC_ROLL

double ecompass_get_zero_offset(ecompass_t *ecompass, int which)
{
    if (!ecompass)
        return 0.0;

#if defined(ECOMPASS_MANUAL_ZERO_OFFSET_COUNT) && (ECOMPASS_MANUAL_ZERO_OFFSET_COUNT > 0)
    if ((which >= 0) && (which < ECOMPASS_MANUAL_ZERO_OFFSET_COUNT))
		return ecompass->manual_zero_offset[which];
#endif

    return 0.0;
}



#ifdef ECOMPASS_SENSOR_VARIANCE

////////////////////////////////////////////////////////////////////////////////
// Returns an noise metric (variance or maximum deviation from mean) of recent
// accelerometer or magnetometer sensor measurements.
//
// \brief Noise metric for recent measurements.
// \param ecompass Pointer to ecompass.
// \param which Selects the noise metric to be returned.
// \param is_stable If not null, this pointer receives the flag
// indicating stablity of the selected varience metric.
// \return The selected noise metric
// \retval <0 On error

double ecompass_noise_metric(ecompass_t *ecompass, ecompass_noise_metric_t which, int *is_stable)
{
	struct timeval timestamp;
	uint64_t oldest;

	if (!ecompass)
		return -1.0;

	if (ecompass->variance_get == ecompass->variance_put)
		return -1.0;

	if (ecompass->variance_use_number)
	{
		// Consume entries more than required count.
		while (ecompass->variance_count > ecompass->variance_number)
		{
			ecompass->variance_count--;
			if (++ecompass->variance_get >= ECOMPASS_SENSOR_VARIANCE_MAX_SAMPLES)
				ecompass->variance_get = 0;
		}
	}
	else
	{
		// Consume entries older than required duration.
		if (gettimeofday(&timestamp, NULL) == 0)
		{
			oldest = ((uint64_t)timestamp.tv_sec * 1000000LL) + (uint64_t)timestamp.tv_usec - ecompass->variance_duration_usec;
			while (ecompass->variance_get != ecompass->variance_put)
			{
				if (ecompass->variance_buffer[ecompass->variance_get].timestamp >= oldest)
					break;

				ecompass->variance_count--;
				if (++ecompass->variance_get >= ECOMPASS_SENSOR_VARIANCE_MAX_SAMPLES)
					ecompass->variance_get = 0;
			}
		}
	}

	if (ecompass->variance_needs_update)
	{
//		double acc_mean[3], mag_mean[3];
		int	   acc_mean_is_valid, mag_mean_is_valid;
		int    acc_count = 0,  mag_count = 0;
		int    i;
		int    debug_count = 0;
#ifdef ECOMPASS_SENSOR_VARIANCE_PER_AXIS
		int    axis;
#endif

		ecompass->variance_needs_update = 0;

		ecompass->mean_accelerometer_axis[0] = ecompass->mean_accelerometer_axis[1] = ecompass->mean_accelerometer_axis[2] = 0.0;
		ecompass->mean_magnetometer_axis[0] = ecompass->mean_magnetometer_axis[1] = ecompass->mean_magnetometer_axis[2] = 0.0;
		acc_mean_is_valid = mag_mean_is_valid = 0;

		for (i = ecompass->variance_get; i != ecompass->variance_put; i = (i < (ECOMPASS_SENSOR_VARIANCE_MAX_SAMPLES - 1)) ? (i + 1) : 0)
		{
			debug_count++;
			if (ecompass->variance_buffer[i].acc_is_valid)
			{
				ecompass->mean_accelerometer_axis[0] += ecompass->variance_buffer[i].acc_x;
				ecompass->mean_accelerometer_axis[1] += ecompass->variance_buffer[i].acc_y;
				ecompass->mean_accelerometer_axis[2] += ecompass->variance_buffer[i].acc_z;
				acc_count++;
			}
			if (ecompass->variance_buffer[i].mag_is_valid)
			{
				ecompass->mean_magnetometer_axis[0] += ecompass->variance_buffer[i].mag_x;
				ecompass->mean_magnetometer_axis[1] += ecompass->variance_buffer[i].mag_y;
				ecompass->mean_magnetometer_axis[2] += ecompass->variance_buffer[i].mag_z;
				mag_count++;
			}
		}

		if (acc_count > 0)
		{
			ecompass->mean_accelerometer_axis[0] /= (double)acc_count;
			ecompass->mean_accelerometer_axis[1] /= (double)acc_count;
			ecompass->mean_accelerometer_axis[2] /= (double)acc_count;
			acc_mean_is_valid = 1;
		}
		if (mag_count > 0)
		{
			ecompass->mean_magnetometer_axis[0] /= (double)mag_count;
			ecompass->mean_magnetometer_axis[1] /= (double)mag_count;
			ecompass->mean_magnetometer_axis[2] /= (double)mag_count;
			mag_mean_is_valid = 1;
		}

		acc_count = 0;
		mag_count = 0;
		ecompass->variance_accelerometer = 0.0;
		ecompass->variance_magnetometer  = 0.0;
		ecompass->max_deviation_accelerometer = 0.0;
		ecompass->max_deviation_magnetometer  = 0.0;
#ifdef ECOMPASS_SENSOR_VARIANCE_PER_AXIS
		for (axis = 0; axis < 3; axis++)
		{
			ecompass->variance_accelerometer_axis[axis] = 0.0;
			ecompass->variance_magnetometer_axis[axis]  = 0.0;
			ecompass->max_deviation_magnetometer_axis[axis] = 0.0;
			ecompass->max_deviation_accelerometer_axis[axis] = 0.0;
		}
#endif

		ecompass->mean_accelerometer = sqrt(ecompass->mean_accelerometer_axis[0] * ecompass->mean_accelerometer_axis[0] +
											ecompass->mean_accelerometer_axis[1] * ecompass->mean_accelerometer_axis[1] +
											ecompass->mean_accelerometer_axis[2] * ecompass->mean_accelerometer_axis[2]);
		ecompass->mean_magnetometer  = sqrt(ecompass->mean_magnetometer_axis[0]  * ecompass->mean_magnetometer_axis[0]  +
											ecompass->mean_magnetometer_axis[1]  * ecompass->mean_magnetometer_axis[1]  +
											ecompass->mean_magnetometer_axis[2]  * ecompass->mean_magnetometer_axis[2]);

		for (i = ecompass->variance_get; i != ecompass->variance_put; i = (i < (ECOMPASS_SENSOR_VARIANCE_MAX_SAMPLES - 1)) ? (i + 1) : 0)
		{
			double diff[3], dsqr;

			if (ecompass->variance_buffer[i].acc_is_valid && acc_mean_is_valid)
			{
				diff[0] = ecompass->variance_buffer[i].acc_x - ecompass->mean_accelerometer_axis[0];
				diff[1] = ecompass->variance_buffer[i].acc_y - ecompass->mean_accelerometer_axis[1];
				diff[2] = ecompass->variance_buffer[i].acc_z - ecompass->mean_accelerometer_axis[2];
				dsqr = diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
				ecompass->variance_accelerometer += dsqr;
				if (dsqr > ecompass->max_deviation_accelerometer)
					ecompass->max_deviation_accelerometer = dsqr;
#ifdef ECOMPASS_SENSOR_VARIANCE_PER_AXIS
				for (axis = 0; axis < 3; axis++)
				{
					ecompass->variance_accelerometer_axis[axis] += diff[axis];
					if (diff[axis] > ecompass->max_deviation_accelerometer_axis[axis])
						ecompass->max_deviation_accelerometer_axis[axis] = diff[axis];
				}
#endif
				acc_count++;
			}
			if (ecompass->variance_buffer[i].mag_is_valid && mag_mean_is_valid)
			{
				diff[0] = ecompass->variance_buffer[i].mag_x - ecompass->mean_magnetometer_axis[0];
				diff[1] = ecompass->variance_buffer[i].mag_y - ecompass->mean_magnetometer_axis[1];
				diff[2] = ecompass->variance_buffer[i].mag_z - ecompass->mean_magnetometer_axis[2];
				dsqr = diff[0] * diff[0] + diff[1] * diff[1] + diff[2] * diff[2];
				ecompass->variance_magnetometer += dsqr;
				if (dsqr > ecompass->max_deviation_magnetometer)
				{
					ecompass->max_deviation_magnetometer = dsqr;
				}

#ifdef ECOMPASS_SENSOR_VARIANCE_PER_AXIS
				for (axis = 0; axis < 3; axis++)
				{
					ecompass->variance_magnetometer_axis[axis] += diff[axis];
					if (diff[axis] > ecompass->max_deviation_magnetometer_axis[axis])
					{
						ecompass->max_deviation_magnetometer_axis[axis] = diff[axis];
					}
				}
#endif
				mag_count++;
			}
		}

		if (acc_count > 0)
		{
			ecompass->max_deviation_accelerometer = sqrt(ecompass->max_deviation_accelerometer);
			ecompass->variance_accelerometer /= (double)acc_count;
			if (ecompass->variance_use_number && (acc_count >= ecompass->variance_number) && (ecompass->max_deviation_accelerometer <= ECOMPASS_STABLE_ACC_DEVIATION))
				ecompass->accelerometer_is_stable = 1;
			else if (!ecompass->variance_use_number && (ecompass->max_deviation_accelerometer <= ECOMPASS_STABLE_ACC_DEVIATION))
				ecompass->accelerometer_is_stable = 1;
			else
				ecompass->accelerometer_is_stable = 0;

#ifdef ECOMPASS_SENSOR_VARIANCE_PER_AXIS
			for (axis = 0; axis < 3; axis++)
			{
				ecompass->variance_accelerometer_axis[axis] /= (double)acc_count;
				if (ecompass->variance_use_number && (acc_count >= ecompass->variance_number) && (ecompass->max_deviation_accelerometer_axis[axis] <= ECOMPASS_STABLE_ACC_DEVIATION))
					ecompass->accelerometer_is_stable_axis[axis] = 1;
				else if (!ecompass->variance_use_number && (ecompass->max_deviation_accelerometer_axis[axis] <= ECOMPASS_STABLE_ACC_DEVIATION))
					ecompass->accelerometer_is_stable_axis[axis] = 1;
				else
					ecompass->accelerometer_is_stable_axis[axis] = 0;
			}
#endif
		}
		else
		{
			ecompass->variance_accelerometer = -1.0;
			ecompass->max_deviation_accelerometer = -1.0;
			ecompass->accelerometer_is_stable = 0;
#ifdef ECOMPASS_SENSOR_VARIANCE_PER_AXIS
			for (axis = 0; axis < 3; axis++)
			{
				ecompass->variance_accelerometer_axis[axis] = -1.0;
				ecompass->max_deviation_accelerometer_axis[axis] = -1.0;
			}
#endif
		}

		if (mag_count > 0)
		{
			ecompass->max_deviation_magnetometer = sqrt(ecompass->max_deviation_magnetometer);
			ecompass->variance_magnetometer /= (double)mag_count;
			if (ecompass->variance_use_number && (acc_count >= ecompass->variance_number) && (ecompass->max_deviation_magnetometer <= ECOMPASS_STABLE_MAG_DEVIATION))
				ecompass->magnetometer_is_stable = 1;
			else if (!ecompass->variance_use_number && (ecompass->max_deviation_magnetometer <= ECOMPASS_STABLE_MAG_DEVIATION))
				ecompass->magnetometer_is_stable = 1;
			else
				ecompass->magnetometer_is_stable = 0;

#ifdef ECOMPASS_SENSOR_VARIANCE_PER_AXIS
			for (axis = 0; axis < 3; axis++)
			{
				ecompass->variance_magnetometer_axis[axis] /= (double)mag_count;
				if (ecompass->variance_use_number && (acc_count >= ecompass->variance_number) && (ecompass->max_deviation_magnetometer_axis[axis] <= ECOMPASS_STABLE_MAG_DEVIATION))
					ecompass->magnetometer_is_stable_axis[axis] = 1;
				else if (!ecompass->variance_use_number && (ecompass->max_deviation_magnetometer_axis[axis] <= ECOMPASS_STABLE_MAG_DEVIATION))
					ecompass->magnetometer_is_stable_axis[axis] = 1;
				else
					ecompass->magnetometer_is_stable_axis[axis] = 0;
			}
#endif
		}
		else
		{
			ecompass->variance_magnetometer = -1.0;
			ecompass->max_deviation_magnetometer = -1.0;
			ecompass->magnetometer_is_stable = 0;
#ifdef ECOMPASS_SENSOR_VARIANCE_PER_AXIS
			for (axis = 0; axis < 3; axis++)
			{
				ecompass->variance_magnetometer_axis[axis] = -1.0;
				ecompass->max_deviation_magnetometer_axis[axis] = -1.0;
			}
#endif
		}
	}

	switch (which)
	{
		case ecompassNoiseAccelerometerMean:
			if (is_stable) *is_stable = (int)ecompass->accelerometer_is_stable;
			return ecompass->mean_accelerometer;

		case ecompassNoiseAccelerometerVariance:
			if (is_stable) *is_stable = (int)ecompass->accelerometer_is_stable;
			return ecompass->variance_accelerometer;

		case ecompassNoiseAccelerometerMaxDeviation:
			if (is_stable) *is_stable = (int)ecompass->accelerometer_is_stable;
			return ecompass->max_deviation_accelerometer;

		case ecompassNoiseMagnetometerMean:
			if (is_stable) *is_stable = (int)ecompass->magnetometer_is_stable;
			return ecompass->mean_magnetometer;

		case ecompassNoiseMagnetometerVariance:
			if (is_stable) *is_stable = (int)ecompass->magnetometer_is_stable;
			return ecompass->variance_magnetometer;

		case ecompassNoiseMagnetometerMaxDeviation:
			if (is_stable) *is_stable = (int)ecompass->magnetometer_is_stable;
			return ecompass->max_deviation_magnetometer;

#ifdef ECOMPASS_SENSOR_VARIANCE_PER_AXIS

		case ecompassNoiseAccelerometerXMean:
			if (is_stable) *is_stable = (int)ecompass->accelerometer_is_stable_axis[0];
			return ecompass->mean_accelerometer_axis[0];

		case ecompassNoiseAccelerometerXVariance:
			if (is_stable) *is_stable = (int)ecompass->accelerometer_is_stable_axis[0];
			return ecompass->variance_accelerometer_axis[0];

		case ecompassNoiseAccelerometerXMaxDeviation:
			if (is_stable) *is_stable = (int)ecompass->accelerometer_is_stable_axis[0];
			return ecompass->max_deviation_accelerometer_axis[0];

		case ecompassNoiseMagnetometerXMean:
			if (is_stable) *is_stable = (int)ecompass->magnetometer_is_stable_axis[0];
			return ecompass->mean_magnetometer_axis[0];

		case ecompassNoiseMagnetometerXVariance:
			if (is_stable) *is_stable = (int)ecompass->magnetometer_is_stable_axis[0];
			return ecompass->variance_magnetometer_axis[0];

		case ecompassNoiseMagnetometerXMaxDeviation:
			if (is_stable) *is_stable = (int)ecompass->magnetometer_is_stable_axis[0];
			return ecompass->max_deviation_magnetometer_axis[0];

		case ecompassNoiseAccelerometerYMean:
			if (is_stable) *is_stable = (int)ecompass->accelerometer_is_stable_axis[1];
			return ecompass->mean_accelerometer_axis[1];

		case ecompassNoiseAccelerometerYVariance:
			if (is_stable) *is_stable = (int)ecompass->accelerometer_is_stable_axis[1];
			return ecompass->variance_accelerometer_axis[1];

		case ecompassNoiseAccelerometerYMaxDeviation:
			if (is_stable) *is_stable = (int)ecompass->accelerometer_is_stable_axis[1];
			return ecompass->max_deviation_accelerometer_axis[1];

		case ecompassNoiseMagnetometerYMean:
			if (is_stable) *is_stable = (int)ecompass->magnetometer_is_stable_axis[1];
			return ecompass->mean_magnetometer_axis[1];

		case ecompassNoiseMagnetometerYVariance:
			if (is_stable) *is_stable = (int)ecompass->magnetometer_is_stable_axis[1];
			return ecompass->variance_magnetometer_axis[1];

		case ecompassNoiseMagnetometerYMaxDeviation:
			if (is_stable) *is_stable = (int)ecompass->magnetometer_is_stable_axis[1];
			return ecompass->max_deviation_magnetometer_axis[1];

		case ecompassNoiseAccelerometerZMean:
			if (is_stable) *is_stable = (int)ecompass->accelerometer_is_stable_axis[2];
			return ecompass->mean_accelerometer_axis[2];

		case ecompassNoiseAccelerometerZVariance:
			if (is_stable) *is_stable = (int)ecompass->accelerometer_is_stable_axis[2];
			return ecompass->variance_accelerometer_axis[2];

		case ecompassNoiseAccelerometerZMaxDeviation:
			if (is_stable) *is_stable = (int)ecompass->accelerometer_is_stable_axis[2];
			return ecompass->max_deviation_accelerometer_axis[2];

		case ecompassNoiseMagnetometerZMean:
			if (is_stable) *is_stable = (int)ecompass->magnetometer_is_stable_axis[2];
			return ecompass->mean_magnetometer_axis[2];

		case ecompassNoiseMagnetometerZVariance:
			if (is_stable) *is_stable = (int)ecompass->magnetometer_is_stable_axis[2];
			return ecompass->variance_magnetometer_axis[2];

		case ecompassNoiseMagnetometerZMaxDeviation:
			if (is_stable) *is_stable = (int)ecompass->magnetometer_is_stable_axis[2];
			return ecompass->max_deviation_magnetometer_axis[2];
#endif
	}

	return -1.0;
}



////////////////////////////////////////////////////////////////////////////////
// Controls wether noinse metrics are calculated of a most recent number of
// samples or the the samples within a specified duration.
//
// \brief Forces errors to be calculated of specified number of samples rather
// than specified time period.
// \param ecompass Pointer to ecompass.
// \param enable Boolean value that enables calcualtion over a specified number
// if set or, a specified duration if not.

void ecompass_noise_metric_enable_number(ecompass_t *ecompass, int enable)
{
	if (!ecompass)
		return;

	ecompass->variance_use_number = enable;
	ecompass->variance_needs_update = 1;
}



////////////////////////////////////////////////////////////////////////////////
// Noise metrics are calculated over the most recent sensor values defined by
// time period \p duration_sec (i.e. those no older than \p duration_sec).
// This is only used if ecompass_noise_metric_enable_number() is not set.
//
// \brief Set time duration over which noise metrics are calculated.
// \param ecompass Pointer to ecompass.
// \param duration_sec Duration in seconds for noise metric calulation.

void ecompass_noise_metric_duration(ecompass_t *ecompass, double duration_sec)
{
	if (!ecompass)
		return;

	ecompass->variance_duration_usec = (uint64_t)(duration_sec * 1e6);
	ecompass->variance_needs_update = 1;
}



////////////////////////////////////////////////////////////////////////////////
// Noise metrics are calculated over the most recent number of sensor values
// defined by \p number. This is only used if
// ecompass_noise_metric_enable_number() is set.
//
// \brief Set number of measurements over which noise metrics are calculated.
// \param ecompass Pointer to ecompass.
// \param duration_sec Number of measurements.

void ecompass_noise_metric_number(ecompass_t *ecompass, int number)
{
	if (!ecompass)
		return;

	ecompass->variance_number = number;
	ecompass->variance_needs_update = 1;
}

std::string ecompass_show(const ecompass_t *ecompass)
{
    std::string returningString = "";
    if (!ecompass)
        return "";

    if (ecompass->name[0] != '\0')
        returningString = returningString + "Ecompass name: " + ecompass->name;
    else
//        returningString = returningString + "Unknown eCompass\n";
//        return returningString;

    returningString = returningString + "\n NEW1" + ecompass_show_calibration(&ecompass->accelerometer_calibration, "  Acc", 0);
    returningString = returningString + "\n NEW2" + ecompass_show_calibration(&ecompass->magnetometer_calibration,  "  Mag", 0);

    return returningString;
}

std::string ecompass_show_calibration(const ecompass_calibration_t *cal, const char *title, int concrete)
{
    std::string returningString = "";
    if (!cal)
        return returningString;

    if (title == NULL)
    {
        switch (cal->sensor)
        {
            case ecompassSensorUnknown:			title = "???"; break;
            case ecompassSensorAccelerometer:	title = "ACC"; break;
            case ecompassSensorMagnetometer:	title = "MAG"; break;
            case ecompassSensorTemperature:		title = "TMP"; break;
        }
    }

    returningString = returningString + ecompass_show_calibration_coeffs(cal, title, concrete);
    return returningString;
}



////////////////////////////////////////////////////////////////////////////////
/// Prints out specific coefficients for calibration. Normally prints out each
/// coefficient as a termperature compensated polynomial (in T) but can also
/// print the concrete calibration currently in use.
///
/// \brief Prints calibration parameters.
/// \param coeffs The coefficients that are to be printed.
/// \param title Title used for the print.
/// \param max_terms Maximum number of polynomial terms for each coefficient. If
/// this is greater than 1, assumes that \p coeffs points to an array of
/// ecompass_calibration_coeffs_t which contains power coefficients.
/// when set rather than the termperature polynomial.

static std::string ecompass_show_calibration_coeffs(const ecompass_calibration_t *cal, const char *title, int concrete)
{
    std::string returningString = "";
    int title_len, power, row;

    title_len = title ? (int)strlen(title) : 0;

    if (!cal || (concrete && !cal->concrete.is_valid) || (!concrete && !cal->term[0].is_valid))
    {
//        printf("%*s   EMPTY\n", title_len, title ? title : "");
        returningString = title_len + title;
        return returningString;
    }

    for (row = 0; row < 3; row++)
    {
//        printf("%*s", title_len, (title && (row == 0)) ? title : "");
        returningString = returningString + title;
        for (power = 0; power <= ECOMPASS_MAX_POWER_TERM; power++)
        {
            int is_used     = concrete ? cal->concrete.matrix_is_used[power]     : cal->term[0].matrix_is_used[power];
            int is_diagonal = concrete ? cal->concrete.matrix_is_diagonal[power] : cal->term[0].matrix_is_diagonal[power];

            if (!is_used)
                continue;

            std::string addDiag = " ";

            switch (row)
            {
                case 0:
//                    printf("  %d|", power);
                    returningString = returningString + std::to_string(power);
                    break;
                case 1:
//                    printf("  %c|", is_diagonal ? '\\' : ' ');
                    addDiag = is_diagonal ? '\\' : ' ';
                    returningString = returningString + addDiag;
                    break;
                case 2:
//                    printf("  %c|", (power == 0) ? ((cal->offset_is_pre_subtracted) ? '-' : '+') : ' ');
                    addDiag = (power == 0) ? ((cal->offset_is_pre_subtracted) ? '-' : '+') : ' ';
                    returningString = returningString + addDiag;
                    break;
                default:
//                    printf("   |");
                    returningString = returningString + "    |";
                    break;
            }

            if (is_diagonal)
            {
                returningString = returningString + " ";
//                printf(" ");
                returningString = returningString + ecompass_show_calibration_coeff(cal, concrete, power, row, row);
            }
            else
            {
//                printf(" ");
                returningString = returningString + " ";
                returningString = returningString + ecompass_show_calibration_coeff(cal, concrete, power, row, 0);
//                printf(" ");
                returningString = returningString + " ";
                returningString = returningString + ecompass_show_calibration_coeff(cal, concrete, power, row, 1);
//                printf(" ");
                returningString = returningString + " ";
                returningString = returningString + ecompass_show_calibration_coeff(cal, concrete, power, row, 2);
            }
//            printf(" |");
            returningString = returningString + " |";
        }

        float temperature = concrete ? cal->concrete.temperature : cal->term[0].temperature;
        if ((row == 0) && ECOMPASS_TEMPERATURE_IS_VALID(temperature))
            returningString = returningString + std::to_string(temperature);

//        printf("  T=%g", temperature);
//        printf("\n");
        returningString = returningString + "\n";
    }
    return returningString + "New3";
}



////////////////////////////////////////////////////////////////////////////////
/// Prints out a single calibration coefficient as a poly nomial in T
/// (temperature).
///
/// \brief Prints calibration coefficient.
/// \param coeffs Pointer to coefficients structure.
/// \param power Selects the matrix to print by its power term.
/// \param row Row index of the term to print from the selected matrix.
/// \param col Column index of the term to print from the selected matrix.

static std::string ecompass_show_calibration_coeff(const ecompass_calibration_t *cal, int concrete, int power, int row, int col)
{
    std::string returningString = "";
    int t, count = 0, temp_power = 0;
    double x;

    if (!cal)
        return "";

    if ((power < 0) || (power >= (ECOMPASS_MAX_POWER_TERM + 1)) || (row < 0) || (row >= 3) || (col < 0) || (col >= 3))
    {
//        printf("-------");
        returningString = returningString + "-------";
        return returningString;
    }

    for (t = 0; t <= ECOMPASS_MAX_TEMPERATURE_POWER_TERM; t++)
    {
        if (concrete)
            x = cal->concrete.parameter[power][row][col];
        else
            x = cal->term[t].parameter[power][row][col];

        if (x != 0.0)
        {
            if (x < 0.0)
                returningString = returningString + "-";
//                printf("-");
            else if (temp_power > 0)
                returningString = returningString + "+";
//                printf("+");
            else
                returningString = returningString + " ";
//                printf(" ");

            if (fabs(x) >= 10.0)
                returningString = returningString + std::to_string(fabs(x));
//                printf("%7.4f", fabs(x));
            else
                returningString = returningString + std::to_string(fabs(x));
//                printf("%7.5f", fabs(x));
            if (temp_power >= 2)
                returningString = returningString + std::to_string(temp_power);
//                printf("*T^%d", temp_power);
            else if (temp_power == 1)
                returningString = returningString + "*T";
//                printf("*T");
            count++;
        }
        temp_power++;

        if (concrete)
            break;
    }

    if (count == 0)
        returningString = returningString + "0";
//        printf(" %7.5f", 0.0);
}



#endif



#if defined(ECOMPASS_DEBUG)

#include <stdio.h>
#include <string.h>



////////////////////////////////////////////////////////////////////////////////
// Prints out calibration parameters for all the sensors of an ecompass.
//
// \brief Prints calibration parameters for an ecompass.
// \param ecompass The ecompass whose parameters are to be printed.

void ecompass_print(const ecompass_t *ecompass)
{
	if (!ecompass)
		return;

	if (ecompass->name[0] != '\0')
		printf("eCompass %s\n", ecompass->name);
	else
		printf("Unknown eCompass\n");
	ecompass_print_calibration(&ecompass->accelerometer_calibration, "  Acc", 0);
	ecompass_print_calibration(&ecompass->magnetometer_calibration,  "  Mag", 0);
}



////////////////////////////////////////////////////////////////////////////////
// Prints out calibration. Normally prints out each coefficient as a
// termperature compensated polynomial (in T) but can also print the concrete
// calibration currently in use.
//
// \brief Prints calibration parameters.
// \param cal The parameters that are to be printed.
// \param title Title used for the print.
// \param concrete Flag that selects the current concrete calibration coeffs
// when set rather than the termperature polynomial.

void ecompass_print_calibration(const ecompass_calibration_t *cal, const char *title, int concrete)
{
	if (!cal)
		return;

	if (title == NULL)
	{
		switch (cal->sensor)
		{
			case ecompassSensorUnknown:			title = "???"; break;
			case ecompassSensorAccelerometer:	title = "ACC"; break;
			case ecompassSensorMagnetometer:	title = "MAG"; break;
			case ecompassSensorTemperature:		title = "TMP"; break;
		}
	}

	ecompass_print_calibration_coeffs(cal, title, concrete);
}



////////////////////////////////////////////////////////////////////////////////
/// Prints out specific coefficients for calibration. Normally prints out each
/// coefficient as a termperature compensated polynomial (in T) but can also
/// print the concrete calibration currently in use.
///
/// \brief Prints calibration parameters.
/// \param coeffs The coefficients that are to be printed.
/// \param title Title used for the print.
/// \param max_terms Maximum number of polynomial terms for each coefficient. If
/// this is greater than 1, assumes that \p coeffs points to an array of
/// ecompass_calibration_coeffs_t which contains power coefficients.
/// when set rather than the termperature polynomial.

static void ecompass_print_calibration_coeffs(const ecompass_calibration_t *cal, const char *title, int concrete)
{
	int title_len, power, row;

	title_len = title ? (int)strlen(title) : 0;

	if (!cal || (concrete && !cal->concrete.is_valid) || (!concrete && !cal->term[0].is_valid))
	{
		printf("%*s   EMPTY\n", title_len, title ? title : "");
		return;
	}

	for (row = 0; row < 3; row++)
	{
		printf("%*s", title_len, (title && (row == 0)) ? title : "");

		for (power = 0; power <= ECOMPASS_MAX_POWER_TERM; power++)
		{
			int is_used     = concrete ? cal->concrete.matrix_is_used[power]     : cal->term[0].matrix_is_used[power];
			int is_diagonal = concrete ? cal->concrete.matrix_is_diagonal[power] : cal->term[0].matrix_is_diagonal[power];

			if (!is_used)
				continue;

			switch (row)
			{
				case 0:  printf("  %d|", power); break;
				case 1:  printf("  %c|", is_diagonal ? '\\' : ' '); break;
				case 2:  printf("  %c|", (power == 0) ? ((cal->offset_is_pre_subtracted) ? '-' : '+') : ' '); break;
				default: printf("   |"); break;
			}

			if (is_diagonal)
			{
				printf(" ");
				ecompass_print_calibration_coeff(cal, concrete, power, row, row);
			}
			else
			{
				printf(" ");
				ecompass_print_calibration_coeff(cal, concrete, power, row, 0);
				printf(" ");
				ecompass_print_calibration_coeff(cal, concrete, power, row, 1);
				printf(" ");
				ecompass_print_calibration_coeff(cal, concrete, power, row, 2);
			}
			printf(" |");
		}

		float temperature = concrete ? cal->concrete.temperature : cal->term[0].temperature;
		if ((row == 0) && ECOMPASS_TEMPERATURE_IS_VALID(temperature))
			printf("  T=%g", temperature);
		printf("\n");
	}
}



////////////////////////////////////////////////////////////////////////////////
/// Prints out a single calibration coefficient as a poly nomial in T
/// (temperature).
///
/// \brief Prints calibration coefficient.
/// \param coeffs Pointer to coefficients structure.
/// \param power Selects the matrix to print by its power term.
/// \param row Row index of the term to print from the selected matrix.
/// \param col Column index of the term to print from the selected matrix.

static void ecompass_print_calibration_coeff(const ecompass_calibration_t *cal, int concrete, int power, int row, int col)
{
	int t, count = 0, temp_power = 0;
	double x;

	if (!cal)
		return;

	if ((power < 0) || (power >= (ECOMPASS_MAX_POWER_TERM + 1)) || (row < 0) || (row >= 3) || (col < 0) || (col >= 3))
	{
		printf("-------");
		return;
	}

	for (t = 0; t <= ECOMPASS_MAX_TEMPERATURE_POWER_TERM; t++)
	{
		if (concrete)
			x = cal->concrete.parameter[power][row][col];
		else
			x = cal->term[t].parameter[power][row][col];

		if (x != 0.0)
		{
			if (x < 0.0)
				printf("-");
			else if (temp_power > 0)
				printf("+");
			else
				printf(" ");

			if (fabs(x) >= 10.0)
				printf("%7.4f", fabs(x));
			else
				printf("%7.5f", fabs(x));
			if (temp_power >= 2)
				printf("*T^%d", temp_power);
			else if (temp_power == 1)
				printf("*T");
			count++;
		}
		temp_power++;

		if (concrete)
			break;
	}

	if (count == 0)
		printf(" %7.5f", 0.0);
}



#endif // defined(ECOMPASS_DEBUG)


/// \} End of ecompass_group


