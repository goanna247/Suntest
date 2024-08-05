//
// Created by goann on 13/06/2024.
//

////////////////////////////////////////////////////////////////////////////////
///
/// \file   ecompass_store.c
/// \brief  Electronic compass memory storage.
/// \author Martin Louis
/// \date   Created: 2012-11-8
///
/// Encodes and decodes elements of an ecompass to and from a memory image.

#include <math.h>
#include <stdint.h>
#include "ecompass.h"
#include "ecompass_store.h"
#include "crc.h"
#include <vector>

/// \ingroup ecompass_store_group
/// \{



////////////////////////////////////////////////////////////////////////////////
// Configuration

#undef DEBUG_HEX_DUMP_STORE			///< Enables hex dumps of memory image to stdout for debugging.
#undef DEBUG_ECOMPASS_STORE			///< Enables output of decoded ecompass parameters to stdout for debugging.

#define CRC_POLY					CRC16_POLY_CCITT	///< CRC polynomial used for the encoded data
#define CRC_INITIAL_VALUE			0x0000				///< Initial value for CRC calculation used for the encoded data.
#define CRC_FINAL_XOR				0x0000				///< Final XOR for CRC calculation used for the encoded data.
#define HEADER_BYTES				2					///< Number of bytes in a block header of encoded data
#define BYTES_PER_DOUBLE			8					///< Number of bytes to store a double precision floating point number
#define TIMESTAMP_BYTES				sizeof(time_t)		///< Number of bytes used to store a timestamp
#define FLOATING_POINT_SIZE_BITS	64					///< Number of total bits in a floating point value
#define FLOATING_POINT_EXP_BITS		11					///< Number of bits used for the exponent of a floating point value.

#if defined(ECOMPASS_DEBUG) || defined(DEBUG_ECOMPASS_STORE) || defined(DEBUG_ECOMPASS_STORE) || defined(ECOMPASS_UNIT_TEST)
#include <stdio.h>
#endif



////////////////////////////////////////////////////////////////////////////////
// Constants

#define BYTES_PER_NUMBER		(((FLOATING_POINT_SIZE_BITS - 1) >> 3) + 1)		///< Number of bytes used to store a floating point number.

#define HDR_ID_CRC				0x00		///< Header ID for a CRC block
#define HDR_ID_TIMESTAMP		0x01		///< Header ID for a timestamp
#define HDR_ID_ACCELEROMETER	0x02		///< Header ID for a accelerometer calibration data block
#define HDR_ID_MAGNETOMETER		0x03		///< Header ID for a magnetometer calibration data block
#define HDR_ID_TEMPERATURE		0x04		///< Header ID for a temperature calibration data block
#define	HDR_ID_SPECIAL			0x05		///< Header ID for a special block

#define SENSOR_FL_HAS_TEMPERATURE				0x01	///< Sensor block flag indicating it includes temperature info.
#define SENSOR_FL_HAS_OFFSET					0x02	///< Sensor block flag indicating it includes a zero order calibration term.
#define SENSOR_FL_HAS_TRANSFORM					0x04	///< Sensor block flag indicating it includes a first order calibration term.
#define SENSOR_FL_HAS_HAS_OFFSET_POLY			0x08	///< Sensor block flag indicating that the zero order calibration term is is a polynomial function of temperature.
#define SENSOR_FL_HAS_OFFSET_IS_PRE_SUBTRACTED	0x10	///< Sensor block flag indicating that the zero order calibration term is pre-subtracted.
#define SENSOR_FL_HAS_MANUAL_ZERO_OFFSETS		0x20	///< Sensor block flag indicating it contains manual zero offset.
//#define SENSOR_FL_HAS_TCOMP_TERMS				0x40
#define SENSOR_FL_MORE_FLAGS					0x80	///< Sensor block flag indicating that more flags follow in additional bytes
#define SENSOR_FL_HAS_CUBIC						0x01	///< Sensor block flag indicating it includes a third order calibration term.
#define SENSOR_FL_BYTE_COUNT					2		///< Number of bytes required for sensor block flags.

#define SCALAR_FL_HAS_POLYNOMIAL				0x01	///< Scalar block flag indicating it has a calibration polynomial.
#define SCALAR_FL_MORE_FLAGS					0x80	///< Scalar block flag indicating that more flags follow in additional bytes (for future expansion)

#define V2_FMT_SCALAR						0		///< Version 2 format field indicating a scalar value
#define V2_FMT_VECTOR3						1		///< Version 2 format field indicating a vector value
#define V2_FMT_MATRIX33						2		///< Version 2 format field indicating a matrix value
#define V2_FMT_FLAGS						3		///< Number of values for version 2 format field
#define V2_TP_SPECIAL_FLAGS					-1		// Not really encoded as TP returned to indicate flags are valid
#define V2_TP_SPECIAL_TEMPERATURE			0
#define V2_TP_SPECIAL_ZERO_ROLL_OFFSET		1
#define V2_TP_SPECIAL_COUNT					2
#define V2_MP_SPECIAL						7
#define V2_MAKE_MAT_HDR(f, t, m)			((((f) << 6) & 0xC0) | (((t) << 3) & 0x38) | ((m) & 0x7))
#define V2_MAKE_FLAGS_HDR(f)				(0xC0 | ((f) & 0x3F))
#define V2_GET_HDR_FMT(h)					(((h) >> 6) & 0x03) ///< Macro to extract matix power term from a version 2 header.
#define V2_GET_HDR_TP(h)					(((h) >> 3) & 0x07)	///< Macro to extract matix temperature power term from a version 2 header.
#define V2_GET_HDR_MP(h)					((h) & 0x07)		///< Macro to extract matix power term from a version 2 header.
#define V2_GET_HDR_FLAGS(h)					((h) & 0x3F)		///< Macro to extract flags from a version 2 header.

#define V2_FLAG_OFFSET_IS_PRE_SUBTRACTED	0x01



////////////////////////////////////////////////////////////////////////////////
// Private function prototypes

static int			ecompass_can_store_to_format_sensor(ecompass_calibration_t *cal, ecompass_store_format_t format);
static int			ecompass_can_store_to_format_scalar(ecompass_scalar_calibration_t *cal, ecompass_store_format_t format);
static uint64_t		ecompass_store_pack(long double value, int total_bits, int exponent_bits);
static long double	ecompass_store_unpack(uint64_t packed, int total_bits, int exponent_bits);

#ifdef ECOMPASS_UNIT_TEST
static size_t		ecompass_test_store_space_required_sensor(ecompass_calibration_t *cal, ecompass_store_format_t format);
static int			ecompass_test_store_put_sensor(ecompass_calibration_t *cal, void *buf, size_t buf_size, ecompass_store_format_t format);
static size_t		ecompass_test_store_get_sensor(ecompass_calibration_t *cal, const void *buf, size_t buf_size, ecompass_store_format_t format);
#endif

#ifdef DEBUG_HEX_DUMP_STORE
static void			debug_hex_dump_store(const char *title, const uint8_t *data, size_t size);
#endif

#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_1) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)
static size_t		ecompass_store_space_required_sensor_v1(ecompass_calibration_t *cal);
static size_t		ecompass_store_space_required_scalar_v1(ecompass_scalar_calibration_t *cal);
#endif
#if defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)
static size_t		ecompass_store_put_v1(ecompass_t *ecompass, void *buf, size_t buf_size);
//int			ecompass_store_put_sensor_v1(ecompass_t *ecompass, ecompass_calibration_t *cal, void *buf, size_t buf_size);
static int			ecompass_store_put_scalar_v1(ecompass_scalar_calibration_t *cal, void *buf, size_t buf_size);
#endif
#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_1)
static size_t		ecompass_store_get_v1(ecompass_t *ecompass, const void *data, size_t size);
static size_t		ecompass_store_get_sensor_v1(ecompass_t *ecompass, ecompass_calibration_t *cal, const void *buf, size_t buf_size);
static size_t		ecompass_store_get_scalar_v1(ecompass_scalar_calibration_t *cal, const void *buf, size_t buf_size);
#endif

#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_2) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)
static size_t		ecompass_store_space_required_sensor_v2(ecompass_calibration_t *cal);
static size_t		ecompass_store_space_required_scalar_v2(ecompass_scalar_calibration_t *cal);
#endif
#if defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)
static size_t		ecompass_store_put_v2(ecompass_t *ecompass, void *buf, size_t buf_size);
static int			ecompass_store_put_sensor_v2(ecompass_calibration_t *cal, void *buf, size_t buf_size);
static int			ecompass_store_put_scalar_v2(ecompass_scalar_calibration_t *cal, void *buf, size_t buf_size);
#endif
#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_2)
static size_t		ecompass_store_get_v2(ecompass_t *ecompass, const void *data, size_t size);
static size_t		ecompass_store_get_sensor_v2(ecompass_calibration_t *cal, const void *buf, size_t buf_size);
static size_t		ecompass_store_get_scalar_v2(ecompass_scalar_calibration_t *cal, const void *buf, size_t buf_size);
static size_t		ecompass_store_get_special_v2(double *special_buf, int *have_special, const void *buf, size_t buf_size);
#endif



////////////////////////////////////////////////////////////////////////////////
// Private data

static crc16_t crc_generator;					///< CRC generator context.
static int	   crc_generator_initialised = 0;	///< Flag that indicates if we have initialised the CRC generator



#pragma mark - Public Functions



////////////////////////////////////////////////////////////////////////////////
// Returns a value indicating wether or not an ecompass can be stored using a
// specified storeage format.
//
// \brief Indicates if an ecompass can be stored to a specified format.
// \param ecompass Pointer to the ecompass.
// \param format The storage format to be used to for the calibration parameters.
// \return Boolean value indicate if the ecompass can be fully stored to
// the specified format.
// \retval 0 The ecomapass can be stored to the specified format.
// \retval non-zero The ecomapass can not be stored to the specified format.

int ecompass_can_store_to_format(ecompass_t *ecompass, ecompass_store_format_t format)
{
    if (!ecompass)
        return 0;

    if (format == ecompassStoreFormatBest)
    {
        if (ecompass_can_store_to_format(ecompass, ecompassStoreFormat1))
            return 1;
        else if (ecompass_can_store_to_format(ecompass, ecompassStoreFormat2))
            return 1;
        return 0;
    }


    return ((ecompass_can_store_to_format_sensor(&ecompass->accelerometer_calibration, format) != 0) &&
            (ecompass_can_store_to_format_sensor(&ecompass->magnetometer_calibration,  format) != 0) &&
            (ecompass_can_store_to_format_scalar(&ecompass->temperature_calibration,   format) != 0));
}



////////////////////////////////////////////////////////////////////////////////
// Calculates and returns the amount of space (in bytes) required to store
// the specified ecompass object using the default format.
//
// \brief Returns storage space required for an ecompass_t.
// \param ecompass Pointer to the ecompass.
// \param format The storage format to be used to for the calibration parameters.
// \return Number of bytes required for storage of \p ecompass.

size_t ecompass_store_space_required(ecompass_t *ecompass)
{
    return ecompass_store_space_required_for_format(ecompass, ECOMPASS_STORE_FORMAT_DEFAULT);
}



////////////////////////////////////////////////////////////////////////////////
// Calculates and returns the amount of space (in bytes) required to store
// the specified ecompass object using the default format.
//
// \brief Returns storage space required for an ecompass_t.
// \param ecompass Pointer to the ecompass.
// \param format The storage format version to be used to for the calibration
// parameters.
// \return Number of bytes required for storage of \p ecompass.

size_t ecompass_store_space_required_for_format(ecompass_t *ecompass, ecompass_store_format_t format)
{
    size_t size = 0;

    if (!ecompass)
        return size;

    // If format is zero, chosethe most backward compatible one that the data can be stored to
    if (format == ecompassStoreFormatBest)
    {
        if (ecompass_can_store_to_format(ecompass, ecompassStoreFormat1))
            format = ecompassStoreFormat1;
        else
            format = ecompassStoreFormat2;
    }

#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_1) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)
    if (format == 1)
	{
		// Header consists of 4 bits object type, 12 bits size.
		size += 1;	// Format Version
		if (ecompass->calibration_timestamp_is_valid)
			size += HEADER_BYTES + TIMESTAMP_BYTES;
		size += HEADER_BYTES + ecompass_store_space_required_sensor_v1(&ecompass->accelerometer_calibration);
		size += HEADER_BYTES + ecompass_store_space_required_sensor_v1(&ecompass->magnetometer_calibration);
		size += HEADER_BYTES + ecompass_store_space_required_scalar_v1(&ecompass->temperature_calibration);
		size += HEADER_BYTES + 2;	// CRC 16
	}
#endif

#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_2) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)
    if (format == 2)
	{
		// Header consists of 4 bits object type, 12 bits size.
		size += 1;	// Format Version
		if (ecompass->calibration_timestamp_is_valid)
			size += HEADER_BYTES + TIMESTAMP_BYTES;
		size += HEADER_BYTES + ecompass_store_space_required_sensor_v2(&ecompass->accelerometer_calibration);
		size += HEADER_BYTES + ecompass_store_space_required_sensor_v2(&ecompass->magnetometer_calibration);
		size += HEADER_BYTES + ecompass_store_space_required_scalar_v2(&ecompass->temperature_calibration);
#if defined(ECOMPASS_MANUAL_ZERO_OFFSET_COUNT) && (ECOMPASS_MANUAL_ZERO_OFFSET_COUNT > 0)
		size += HEADER_BYTES + 1 + BYTES_PER_NUMBER;	// Zero roll offset
#endif
		size += HEADER_BYTES + 2;	// CRC 16
	}
#endif

    return size;
}



////////////////////////////////////////////////////////////////////////////////
// Stores an ecompass into a byte buffer using default format.
//
// \brief Stores calibration parameters to binary buffer.
// \param ecompass Pointer to the ecompass.
// \param buf Pointer to buffer that \p ecompass will be stored into.
// \param size Size of buffer that  \p ecompass will be stored into.
// \return Number of bytes put into the buffer. 0 on error.

size_t ecompass_store_put(ecompass_t *ecompass, uint8_t *buf, size_t buf_size)
{

    uint8_t *data = static_cast<uint8_t *>(malloc(265 * sizeof(uint8_t)));
    data = buf;
//            static_cast<uint8_t *>(buf);
//    return data[2];
    return ecompass_store_put_using_format(ecompass, data, buf_size, ECOMPASS_STORE_FORMAT_DEFAULT);
}



////////////////////////////////////////////////////////////////////////////////
// Stores an ecompass into a byte buffer using specified format.
//
// \brief Stores calibration parameters to binary buffer.
// \param ecompass Pointer to the ecompass.
// \param buf Pointer to buffer that \p ecompass will be stored into.
// \param size Size of buffer that  \p ecompass will be stored into.
// \param format The format version that will be used to store \p ecompass.
// \return Number of bytes put into the buffer. 0 on error.

size_t ecompass_store_put_using_format(ecompass_t *ecompass, uint8_t *buf, size_t buf_size, ecompass_store_format_t format)
{
    if (!ecompass)
        return 0;

    // If format is zero, chosethe most backward compatible one that the data can be stored to
    if (format == ecompassStoreFormatBest)
    {
#ifdef ECOMPASS_STORE_USE_V1_IF_POSSIBLE
        if (ecompass_can_store_to_format(ecompass, ecompassStoreFormat1))
			format = ecompassStoreFormat1;
		else
			format = ecompassStoreFormat2;
#else
        format = ecompassStoreFormat2;
#endif
    }

#if defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)
    if (format == 1)
		return ecompass_store_put_v1(ecompass, buf, buf_size);
//        return 2;
#endif

#if defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)
    if (format == 2)
//		return ecompass_store_put_v2(ecompass, buf, buf_size);
        return 3;
#endif

    return 0;
}



////////////////////////////////////////////////////////////////////////////////
/// Retrieves an ecompass from a byte buffer.
/// \see ecompass_store_put() for format details.
///
/// \brief Retrieves calibration parameters from binary storeage.
/// \param ecompass Pointer to the ecompass.
/// \param buf Pointer to buffer that \p ecompass will be retrieved from.
/// \param size Size of buffer that \p ecompass will be retrieved from.
/// \return Number of bytes read from buffer. 0 on error.

size_t ecompass_store_get(ecompass_t *ecompass, const void *data, size_t size)
{
    if (!ecompass || !data || (size < 1))
        return 0;

#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_1)
    if (*((uint8_t *)data) == 1)
		return ecompass_store_get_v1(ecompass, data, size);
#endif

#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_2)
    if (*((uint8_t *)data) == 2)
		return ecompass_store_get_v2(ecompass, data, size);
#endif

    return 0;
}



////////////////////////////////////////////////////////////////////////////////
/// Returns a value indicating wether or not a scalar calibration can be stored
/// using a specified storage format.
///
/// \brief Indicates if an scalar calibration can be stored to a specified format.
/// \param cal Pointer to the scalar calibration.
/// \param format The storage format to be used to for the calibration parameters.
/// \return Boolean value indicate if the scalar calibration can be fully stored
/// to the specified format.
/// \retval 0 The scalar calibration can be stored to the specified format.
/// \retval non-zero The scalar calibration can not be stored to the specified
/// format.

static int ecompass_can_store_to_format_scalar(ecompass_scalar_calibration_t *cal, ecompass_store_format_t format)
{
    // Scalar calibrations can always be stored
    return 1;
}



////////////////////////////////////////////////////////////////////////////////
/// Returns a value indicating wether or not a sensor calibration can be stored
/// using a specified storeage format.
///
/// \brief Indicates if an sensor calibration can be stored to a specified format.
/// \param cal Pointer to the sensor calibration.
/// \param format The storage format to be used to for the calibration parameters.
/// \return Boolean value indicate if the sensor calibration can be fully stored to
/// the specified format.
/// \retval 0 The sensor calibration can be stored to the specified format.
/// \retval non-zero The sensor calibration can not be stored to the specified
/// format.

static int ecompass_can_store_to_format_sensor(ecompass_calibration_t *cal, ecompass_store_format_t format)
{
    int t, p;

    if (format == ecompassStoreFormat1)
    {
        for (t = 0; t <= ECOMPASS_MAX_TEMPERATURE_POWER_TERM; t++)
        {
            for (p = 0; p <= ECOMPASS_MAX_POWER_TERM; p++)
            {
                if (!cal->term[t].matrix_is_used[p])
                    continue;

                // Zero order term allowed as long as it is diagonal
                if (p == 0)
                {
                    if (!cal->term[t].matrix_is_diagonal[p])
                        return 0;
                }

                    // First order term allowed as long as it has no temperature dependent terms
                else if (p == 1)
                {
                    if (t > 0)
                        return 0;
                }

                    // Third order term allowed as long as it diagonal and has no temperature dependent terms
                else if (p == 3)
                {
                    if (!cal->term[t].matrix_is_diagonal[p])
                        return 0;
                    if (t > 0)
                        return 0;
                }

                    // No other power terms allowed
                else
                    return 0;
            }
        }

        return 1;
    }

    else if (format == ecompassStoreFormat2)
        return 1;

    return 0;
}



#pragma mark - General data packing functions



////////////////////////////////////////////////////////////////////////////////
/// Splits a floating point value into it sign, exponent and significand
/// components then recombines them into a binary with arbitary number of bits
/// for the exponent and significand.
///
/// \note Packing of sub-normal values not currently supported. Normal values
/// that can only be represented as subnormals when constained to the specified
/// number of bits for exponent are currently packed as 0.
///
/// \note One bit of precision is lost since frexp() returns a significand with
/// implied most significant bit. When we remove it, we get a zero in the
/// significand's least significant bit.
///
/// \brief Packs a floating point value into a binary form.
/// \param value The value to be packed.
/// \param total_bits total number of bits in packed value. Must be less than
/// or equal to 64.
/// \param exponent_bits the number of bits used in the exponent. Must be at
/// least 2. The numer of bit used for the significand is calculated as
/// (total_bits-exponent_bits-1). \p total_bits and \p exponent_bits must be such
/// that this value is greater than or equal to 2.
/// \return Binary version of specified value. The least significant \p total_bits
/// of the return value represent the packed binary. The most significant bit OF
/// THAT is the sign bit the \p exponent_bits next most significant bits are the
/// biased exponent. The bias is always ((2 ^ (exponent_bits - 1)) - 1) which
/// conforms to IEEE 754 binary32 and binary64 formats (ie bias is 1023 when
/// exponent_bits is 11 for double precision and bias is 127 when exponent_bits
/// is 8 for single precision). As per IEE754, the significand is normalised
/// such that the leading bit is "one" by adjusting the exponent to preserve
/// maximum precision. This bit is implied and not stored in the binary
/// representation. The floating point value is calulated as follows:\n
///
/// value = (-1)^sign + 1.significand * 2^(exponent_bits-exponent_bias)

static uint64_t ecompass_store_pack(long double value, int total_bits, int exponent_bits)
{
    int		 significand_bits;
    uint64_t exponent_mask;

    significand_bits = (total_bits - exponent_bits - 1);

    // Sanity check
    if ((total_bits < 1) || (total_bits > 64) || (exponent_bits < 2) || (significand_bits < 2))
        return 0;

    exponent_mask = ((1ULL << exponent_bits) - 1);

    switch (fpclassify(value))
    {
        case FP_ZERO:
            return signbit(value) ? ((uint64_t)1 << (total_bits - 1)) : 0;

        case FP_INFINITE:
            return ((uint64_t)(signbit(value) ? 1 : 0) << (total_bits - 1)) | (exponent_mask << significand_bits);

        case FP_NAN:
            return (exponent_mask << significand_bits) | (3ULL << (significand_bits - 2));

        case FP_SUBNORMAL:
            // FIXME: Don't support sub normals yet.
            // Sub-normals have a zero exponent and non-zero significand. They
            // represent values smaller than can be represented with a normalised
            // significand since the exponent would be less than the minimum.
            // The significand remains unnormalised with exponent set to minimum.
            // Therefore the most significant bit of significand is no longer implied.
            // subnormal_value = (-1)^sign + 0.significand * 2^(1-exponent_bias)
            // Return 0 for now.
            return signbit(value) ? ((uint64_t)1 << (total_bits - 1)) : 0;

        case FP_NORMAL:
        {
            uint8_t		sign;
            int			exponent;
            uint64_t	significand;
            long double	normalised;

            // Extract sign.
            if (value < 0.0)
            {
                sign = 1;
                value = -value;
            }
            else
                sign = 0;

            // Split significand and exponent. frexp() returns full significand with
            // most significant bit always set. This bit is implied in IEEE 754
            // so we need to get rid if it.
            normalised = fabsl(frexpl(value, &exponent) - 0.5) * 2.0;
            significand = (uint64_t)(normalised * ((1ULL << significand_bits) - 1));

            // Calculate the biased exponent. The dropped bit from the significand
            // explicitly returned by frexp() but implied in binary format) means an
            // additional -1 offest to the exponent. Check range of biased exponent
            // value outside 0 to exponent_mask range cannot be represented so
            // round to
            // +/-ZERO and
            exponent += ((1U << (exponent_bits - 1)) - 1) - 1;

            // Values with exponents lower than we can represent become 0.0
            // Exponent 0 is reserved to represent 0 (already handled) and
            // sub-normals.
            // FIXME: We could un-normalise this to bring exponent back within
            //        range then encode it as a sub-normal.
            if (exponent <= 0)
                return ((uint64_t)sign << (total_bits - 1));

            // Values with exponents higher than we can represent become INFINITY
            // Largest value of exponent reserved to represent NAN so we can't use it.
            if (exponent >= (int)exponent_mask)
                return ((uint64_t)sign << (total_bits - 1)) | (exponent_mask << significand_bits);

            // Combine components and return result.
            return ((uint64_t)sign << (total_bits - 1)) | ((uint64_t)exponent << significand_bits) | significand;
        }
    }

    return 0;
}



////////////////////////////////////////////////////////////////////////////////
/// Unpacks the sign, exponent and significand components from a binary
/// representation created by ecompass_pack() and returns the corresponding
/// floating point value.
/// \see ecompass_pack()
///
/// \note Unpacking of sub-normal values not currently supported. Normal values
/// that can only be represented as subnormals when constained to the specified
/// number of bits for exponent are currently packed as 0.
///
/// \brief Unpacks a floating point value from a binary form.
/// \param packed The packed value to be unpacked.
/// \param total_bits total number of bits in packed value. Must be less than
/// or equal to 64.
/// \param exponent_bits the number of bits used in the exponent. Must be at
/// least 2. The numer of bit used for the significand is calculated as
/// (total_bits-exponent_bits-1). \p total_bits and \p exponent_bits must be such
/// that this value is greater than or equal to 2.
/// \return Floating point value corresponding to packed value. See ecompass_pack()
/// fo format details.

static long double ecompass_store_unpack(uint64_t packed, int total_bits, int exponent_bits)
{
    int		 exponent, shift;
    uint64_t significand, exponent_mask;
    int		 significand_bits = (total_bits - exponent_bits - 1);
    long double value;

    // Sanity check
    if ((total_bits < 1) || (total_bits > 64) || (exponent_bits < 2) || (significand_bits < 2))
        return 0.0;

    exponent_mask = ((uint64_t)1 << exponent_bits) - 1;
    exponent    = (int)((packed >> significand_bits) & exponent_mask);
    significand = packed & (((uint64_t)1 << significand_bits) - 1);

    // Special case: zero (positive and negative) and sub-normals
    if (exponent == 0)
    {
        if (significand == 0)
        {
            // Zero
            value = 0.0;
            if (packed & ((uint64_t)1 << (total_bits - 1)))
                value = copysignl(value, -1.0);
        }
        else
        {
            // FIXME: Sub-normals not supported yet. Return zero.
            value = 0.0;
            if (packed & ((uint64_t)1 << (total_bits - 1)))
                value = copysignl(value, -1.0);

        }
        return value;
    }

    // Special case: infinity and NAN
    if (exponent == exponent_mask)
    {
        if (significand == 0)
            value = (packed & ((uint64_t)1 << (total_bits - 1))) ? -INFINITY : INFINITY;
        else
            value = nanl("");
        return value;
    }

    value = 1.0 + ((long double)significand / (long double)(1ULL << significand_bits));
    shift = exponent - ((1 << (exponent_bits - 1)) - 1);
    while(shift > 0) { value *= 2.0; shift--; }
    while(shift < 0) { value *= 0.5; shift++; }
    if (packed & ((uint64_t)1 << (total_bits - 1)))
        value = -value;

    return value;
}



#pragma mark - Debug functions



#ifdef DEBUG_HEX_DUMP_STORE

////////////////////////////////////////////////////////////////////////////////
// Needs documentation

static void debug_hex_dump_store(const char *title, const uint8_t *data, size_t size)
{
	int i;

	if (title)
		printf("%s\n", title);

	if (data && size > 0)
	{
		for (i = 0; i < size; i++)
		{
			if ((i & 0x0F) == 0x00)
				printf("%04X:  ", i);
			printf(" %02X", data[i]);
			if ((i & 0x0F) == 0x0F)
				printf("\n");
		}
		if ((i & 0x0F) != 0x00)
			printf("\n");
	}
}

#endif



#pragma mark - Test functions



#if defined(ECOMPASS_UNIT_TEST)

////////////////////////////////////////////////////////////////////////////////
// Needs documentation

static size_t ecompass_test_store_space_required_sensor(ecompass_calibration_parameters_t *params, ecompass_store_format_t format)
{
	if (!params)
		return 0;

#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_1) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)
	if (format == ecompassStoreFormat1)
		return ecompass_store_space_required_sensor_v1(params);
#endif
#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_2) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)
	if (format == ecompassStoreFormat2)
		return ecompass_store_space_required_sensor_v2(params);
#endif

	return 0;
}

#endif // defined(ECOMPASS_UNIT_TEST)



#if defined(ECOMPASS_UNIT_TEST)

////////////////////////////////////////////////////////////////////////////////
// Needs documentation

static int ecompass_test_store_put_sensor(ecompass_calibration_parameters_t *params, void *buf, size_t buf_size, ecompass_store_format_t format)
{
	if (!params)
		return 0;

#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_1) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)
	if (format == ecompassStoreFormat1)
		return ecompass_store_put_sensor_v1(params, buf, buf_size);
#endif

#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_2) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)
	if (format == ecompassStoreFormat2)
		return ecompass_store_put_sensor_v2(params, buf, buf_size);
#endif

	return 0;
}

#endif // defined(ECOMPASS_UNIT_TEST)



#if defined(ECOMPASS_UNIT_TEST)

static size_t ecompass_test_store_get_sensor(ecompass_calibration_t *cal, const void *buf, size_t buf_size, ecompass_store_format_t format)
{
	if (!params)
		return 0;

#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_1) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)
	if (format == ecompassStoreFormat1)
		return ecompass_store_get_sensor_v1(cal, buf, buf_size);
#endif
#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_2) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)
	if (format == ecompassStoreFormat2)
		return ecompass_store_get_sensor_v2(cal, buf, buf_size);
#endif

	return 0;
}

#endif // defined(ECOMPASS_UNIT_TEST)



#if defined(ECOMPASS_UNIT_TEST)

////////////////////////////////////////////////////////////////////////////////
// Test packing and unpacking of calibration parameters to buffer.
//
// \brief Test claibration parameter packing,
// \return Error count. Number of errors detected
// \retval 0 No error. All tests passed.

int ecompass_test_store(void)
{
	uint64_t packed;
	long double unpacked;
	int errs = 0;
	int i;

	#define DOUBLE_TEST_PAIRS_COUNT	12
	static const struct
	{
		uint64_t binary;
		double	 value;
	} double_test_pairs[DOUBLE_TEST_PAIRS_COUNT] =
	{
		{ 0x3fd5555555555555LL, 1.0/3.0 },
		{ 0x3ff0000000000000LL, 1.0 },
		{ 0x3ff0000000000001LL, 1.0000000000000002 },			// Smallest number > 1.0
		{ 0x3ff0000000000002LL, 1.0000000000000004 },
		{ 0x4000000000000000LL, 2.0 },
		{ 0xc000000000000000LL, -2.0 },
		// { 0x0000000000000001LL, 5e-324 },						// Min positive sub-normal
		// { 0x000fffffffffffffLL, 2.2250738585072009e-308 },		// Max positive sub-normal
		{ 0x0010000000000000LL, 2.2250738585072014e-308 },		// Min positive normal
		{ 0x7fefffffffffffffLL, 1.7976931348623157e308 },		// Max positive normal
		{ 0x0000000000000000LL, 0.0 },							// Positive zero
		{ 0x8000000000000000LL, -0.0 },							// Negative zero
		{ 0x7ff0000000000000LL, INFINITY },						// Positive infinity
		{ 0xfff0000000000000LL, -INFINITY }						// Negative infinity
	};


	#define SINGLE_TEST_PAIRS_COUNT	10
	static const struct
	{
		uint32_t binary;
		float	 value;
	} single_test_pairs[SINGLE_TEST_PAIRS_COUNT] =
	{
		{ 0x3eaaaaab, 1.0/3.0 },
		{ 0x3ec00000, 0.375 },
		{ 0xc1c80000, -25.0 },
		{ 0x3f800000, 1.0 },
		{ 0xc0000000, -2.0 },
		{ 0x7f7fffff, 3.4028234e38 },					// Max positive normal
		{ 0x00000000, 0.0 },
		{ 0x80000000, -0.0 },
		{ 0x7f800000, INFINITY },
		{ 0xff800000, -INFINITY }
	};

	for (i = 0; i < DOUBLE_TEST_PAIRS_COUNT; i++)
	{
		packed = ecompass_store_pack(double_test_pairs[i].value, 64, 11);

		// See if value matches. Ignore the least significant bit in the significand
		// since we loose one bit of precision during packing. See note in ecompass_pack_double().
		if (((double_test_pairs[i].binary & 0xFFF0000000000000L) != (packed & 0xFFF0000000000000L)) ||
			(labs((long)(double_test_pairs[i].binary & 0x000FFFFFFFFFFFFFL) - (long)(packed & 0x000FFFFFFFFFFFFFL) > 1L)))
		{
			errs++;
			printf("ECOMPASS PACK TEST FAILED:   %g -> %016llX (expected %016llX)\n", double_test_pairs[i].value, packed, double_test_pairs[i].binary);
		}
	}


	for (i = 0; i < DOUBLE_TEST_PAIRS_COUNT; i++)
	{
		unpacked = ecompass_store_unpack(double_test_pairs[i].binary, 64, 11);

		if (double_test_pairs[i].value != (double)unpacked)
		{
			errs++;
			printf("ECOMPASS UNPACK TEST FAILED: %016llX -> %Lg (expected %g)\n", double_test_pairs[i].binary, unpacked, double_test_pairs[i].value);
		}
	}

	for (i = 0; i < SINGLE_TEST_PAIRS_COUNT; i++)
	{
		packed = ecompass_store_pack(single_test_pairs[i].value, 32, 8);

		// See if value matches. Ignore the least significant bit in the significand
		// since we loose one bit of precision during packing. See note in ecompass_pack_double().
		if (((single_test_pairs[i].binary & 0xFF800000UL) != (packed & 0xFF800000UL)) ||
			(labs((long)(single_test_pairs[i].binary & 0x007FFFFFL) - (long)(packed & 0x007FFFFFL) > 1L)))
		{
			errs++;
			printf("ECOMPASS PACK TEST FAILED:   %g -> %08llX (expected %08X)\n", single_test_pairs[i].value, packed, single_test_pairs[i].binary);
		}
	}

	for (i = 0; i < SINGLE_TEST_PAIRS_COUNT; i++)
	{
		unpacked = ecompass_store_unpack(single_test_pairs[i].binary, 32, 8);

		if (single_test_pairs[i].value != unpacked)
		{
			errs++;
			printf("ECOMPASS UNPACK TEST FAILED: %08X -> %Lg (expected %g)\n", single_test_pairs[i].binary, unpacked, single_test_pairs[i].value);
		}
	}

	#define FORMAT_COUNT 2

	for (i = 0; i < FORMAT_COUNT; i++)
	{
		#define BUF_SIZE	512

		static const ecompass_store_format_t format[FORMAT_COUNT] = {ecompassStoreFormat1,ecompassStoreFormat2};
		ecompass_calibration_parameters_t param_in, param_out;
		uint8_t buf[BUF_SIZE];
		size_t  count;
		int j;
		double max_err;

		ecompass_calibration_parameters_init(&param_in);
		ecompass_calibration_parameters_init(&param_out);

		param_in.transform[0] = 0.0;
		param_in.transform[1] = 0.1;
		param_in.transform[2] = 0.2;
		param_in.transform[3] = 1.0;
		param_in.transform[4] = 1.1;
		param_in.transform[5] = 1.2;
		param_in.transform[6] = 2.0;
		param_in.transform[7] = 2.1;
		param_in.transform[8] = 2.2;
		param_in.transform_is_valid = 1;
		param_in.offset[0] = 3.0;
		param_in.offset[1] = 3.1;
		param_in.offset[2] = 3.2;
		param_in.offset_is_valid = 1;
		param_in.cubic[0] = 4.0;
		param_in.cubic[1] = 4.1;
		param_in.cubic[2] = 4.2;
		param_in.cubic_is_valid = 1;
		param_in.temperature = 25.4;
		param_in.temperature_is_valid = 1;

		max_err = 1e-15; // Use 1e-5 for single precision and 1e-15 for double precision

		if ((count = ecompass_test_store_put_sensor(&param_in, buf, BUF_SIZE, format[i])) == 0)
		{
			printf("ECOMPASS PACK TEST FAILED:   Packing parameters wrote 0 bytes in format %d.\n", (int)format[i]);
			continue;
		}

		ecompass_test_store_get_sensor(&param_out, buf, BUF_SIZE, format[i]);

		if (param_in.transform_is_valid != param_out.transform_is_valid)
		{
			errs++;
			printf("ECOMPASS PACK TEST FAILED:   Calibration transform_is_valid = %d (expected %d)\n", param_out.transform_is_valid, param_in.transform_is_valid);
		}
		if (param_in.offset_is_valid != param_out.offset_is_valid)
		{
			errs++;
			printf("ECOMPASS PACK TEST FAILED:   Calibration offset_is_valid = %d (expected %d)\n", param_out.offset_is_valid, param_in.offset_is_valid);
		}
		if (param_in.cubic_is_valid != param_out.cubic_is_valid)
		{
			errs++;
			printf("ECOMPASS PACK TEST FAILED:   Calibration cubic_is_valid = %d (expected %d)\n", param_out.cubic_is_valid, param_in.cubic_is_valid);
		}

		for (j = 0; j < 9; j++)
		{
			if (fabs(param_out.transform[j] - param_in.transform[j]) >= max_err)
			{
				errs++;
				printf("ECOMPASS PACK TEST FAILED:   gain[%d] = %g (expected %g)   err = %g\n", j, param_out.transform[j], param_in.transform[j], fabs(param_out.transform[j]-param_in.transform[j]));
			}
		}

		for (j = 0; j < 3; j++)
		{
			if (fabs(param_out.offset[j] - param_in.offset[j]) >= max_err)
			{
				errs++;
				printf("ECOMPASS PACK TEST FAILED:   offset[%d] = %g (expected %g)   err = %g\n", j, param_out.offset[j], param_in.offset[j], fabs(param_out.offset[j]-param_in.offset[j]));
			}
		}

		for (j = 0; j < 3; j++)
		{
			if (fabs(param_out.cubic[j] - param_in.cubic[j]) >= max_err)
			{
				errs++;
				printf("ECOMPASS PACK TEST FAILED:   cubic[%d] = %g (expected %g)   err = %g\n", j, param_out.cubic[j], param_in.cubic[j], fabs(param_out.cubic[j]-param_in.cubic[j]));
			}
		}

		if (param_in.temperature_is_valid && !param_out.temperature_is_valid)
		{
			errs++;
			printf("ECOMPASS PACK TEST FAILED:   Decoded temperature is not valid but it should be.\n");
		}
		else if (!param_in.temperature_is_valid && param_out.temperature_is_valid)
		{
			errs++;
			printf("ECOMPASS PACK TEST FAILED:   Decoded temperature is valid but it shouldn't be.\n");
		}
		else if (param_in.temperature_is_valid && param_out.temperature_is_valid && (fabs(param_out.temperature - param_in.temperature) >= max_err))
		{
			errs++;
			printf("ECOMPASS PACK TEST FAILED:   temperature = %g (expected %g)   err = %g\n", param_out.temperature, param_in.temperature,
				   fabs(param_out.temperature - param_in.temperature));
		}
	}

	if (errs == 0)
		printf("ECOMPASS STORE TEST: No errors detected.\n");
	return errs;
}

#endif // defined(ECOMPASS_UNIT_TEST)



#pragma mark - Format version 1 functions



#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_1) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)

////////////////////////////////////////////////////////////////////////////////
// Needs documentation.

static size_t ecompass_store_space_required_sensor_v1(ecompass_calibration_t *cal)
{
	size_t size = 0;
	int p;

	// Sanity checks
	if (!cal)
		return size;

	// Can only store calibration if offset and cubic terms are diagonals.
	if (cal->term[0].matrix_is_used[0] && !cal->term[0].matrix_is_diagonal[0])
		return size;
	if (cal->term[0].matrix_is_used[3] && !cal->term[0].matrix_is_diagonal[3])
		return size;
	// Can't store if using snd order term or anything higher than third order.
	if (cal->term[0].matrix_is_used[2])
		return size;
	for (p = 4; p <= ECOMPASS_MAX_POWER_TERM; p++)
		if (cal->term[0].matrix_is_used[p])
			return size;

	// Flags n bytes ending at the first with MSB clear.
	// eg 0xxxxxxx or 1xxxxxxx 1xxxxxxx 0xxxxxxx
	size = 1;	// 7 flag bits.
	// Extra flag bytes.
	if (cal->term[0].matrix_is_used[3])
		size++;

	if (ECOMPASS_TEMPERATURE_IS_VALID(cal->term[0].temperature))
		size += BYTES_PER_DOUBLE;

//	if (cal->offset_is_valid)
//		size += 3 * BYTES_PER_DOUBLE;

	if (cal->term[0].matrix_is_used[1])
		size += 9 * BYTES_PER_DOUBLE;

	if (cal->term[0].matrix_is_used[0])
		size += 1 + 3 * (ECOMPASS_MAX_TEMPERATURE_POWER_TERM + 1) * BYTES_PER_DOUBLE;

#if defined(ECOMPASS_MANUAL_ZERO_OFFSET_COUNT) && (ECOMPASS_MANUAL_ZERO_OFFSET_COUNT > 0)
	size += 1 + ECOMPASS_MANUAL_ZERO_OFFSET_COUNT * BYTES_PER_DOUBLE;
#endif

	if (cal->term[0].matrix_is_used[3])
		size += 3 * BYTES_PER_DOUBLE;

	return size;
}

#endif // defined(ECOMPASS_STORE_CAN_READ_FORMAT_1) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)



#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_1) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)

////////////////////////////////////////////////////////////////////////////////
// Needs documentation.

static size_t ecompass_store_space_required_scalar_v1(ecompass_scalar_calibration_t *cal)
{
	size_t size = 0;

	if (!cal)
		return size;

	// Flags n bytes ending at the first with MSB clear.
	// eg 0xxxxxxx or 1xxxxxxx 1xxxxxxx 0xxxxxxx
	size = 1;	// 7 flag bits.

	if (cal->is_valid)
		size += 1 + (ECOMPASS_MAX_SCALAR_POWER_TERM + 1) * BYTES_PER_DOUBLE;

	return size;
}

#endif // defined(ECOMPASS_STORE_CAN_READ_FORMAT_1) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)



#if defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)

////////////////////////////////////////////////////////////////////////////////
// Stores an ecompass into a byte buffer using format version 1.
//
// \brief Stores calibration parameters to binary buffer.
// \param ecompass Pointer to the ecompass.
// \param buf Pointer to buffer that \p ecompass will be stored into.
// \param size Size of buffer that  \p ecompass will be stored into.
// \return Number of bytes put into the buffer. 0 on error.

static size_t ecompass_store_put_v1(ecompass_t *ecompass, void *buf, size_t buf_size)
{
	#define BLOCK_START		{ block_ptr = ptr; ptr += HEADER_BYTES; store_size += HEADER_BYTES; buf_size -= HEADER_BYTES; }
	#define BLOCK_END(i,s)	{ block_ptr[0] = (((i) << 4) & 0xF0) | ((s) >> 8) & 0x0F; block_ptr[1] = (s) & 0xFF; }

    uint8_t *ptr;
    ptr = static_cast<uint8_t *>(malloc(256 * sizeof(uint8_t)));
    uint8_t *block_ptr;
	uint16_t crc;
	size_t   store_size = 0;
	int      block_size;

	if (!ecompass)
		return 0;

	if (buf_size < ecompass_store_space_required(ecompass))
		return 0;


	ptr = static_cast<uint8_t *>(buf);


	// Add version
	*ptr++ = 1;
	store_size++;
	buf_size--;
//    return 4; //BACKSPACE
//    return ptr[2];

	// Add time stamp if it is valid
	if (ecompass->calibration_timestamp_is_valid && (buf_size >= (HEADER_BYTES + TIMESTAMP_BYTES)))
	{
		int i;
		time_t temp;

		BLOCK_START;

		for (i = 0, temp = ecompass->calibration_timestamp; i < TIMESTAMP_BYTES; i++, temp >>= 8)
		{
			*ptr++ = (uint8_t)(temp & 0xFF);
			store_size++;
			buf_size--;
		}

		BLOCK_END(HDR_ID_TIMESTAMP, TIMESTAMP_BYTES);
	}

	BLOCK_START;
	if ((block_size = ecompass_store_put_sensor_v1(ecompass, &ecompass->accelerometer_calibration, ptr, buf_size)) < 0)
		return 0;
	ptr += block_size;
	store_size += block_size;
	buf_size -= block_size;
	BLOCK_END(HDR_ID_ACCELEROMETER, block_size);

	BLOCK_START;
	if ((block_size = ecompass_store_put_sensor_v1(ecompass, &ecompass->magnetometer_calibration, ptr, buf_size)) < 0)
		return 0;
	ptr += block_size;
	store_size += block_size;
	buf_size -= block_size;
	BLOCK_END(HDR_ID_MAGNETOMETER, block_size);

	BLOCK_START;
	if ((block_size = ecompass_store_put_scalar_v1(&ecompass->temperature_calibration, ptr, buf_size)) < 0)
		return 0;
	ptr += block_size;
	store_size += block_size;
	buf_size -= block_size;
	BLOCK_END(HDR_ID_TEMPERATURE, block_size);

	if (buf_size < 2)
		return 0;

	// Calculate CRC
	if (!crc_generator_initialised)
	{
		crc16_init(&crc_generator, CRC_POLY, CRC_INITIAL_VALUE, CRC_FINAL_XOR);
		crc_generator_initialised = 1;
	}

	// Store CRC. Build the block header first then drop CRC in last.
	BLOCK_START;
	store_size += 2;
	//buf_size -= 2; // Dead store
	ptr += 2;
	BLOCK_END(HDR_ID_CRC, 2);
	crc16_begin(&crc_generator);
	uint8_t *buf2 = (uint8_t *)buf;
	crc16_calc(&crc_generator, buf2, store_size-2);
	crc = crc16_end(&crc_generator);
	ptr[-2] = (uint8_t)((crc >> 8) & 0xFF);
	ptr[-1] = (uint8_t)(crc & 0xFF);

#ifdef DEBUG_HEX_DUMP_STORE
	debug_hex_dump_store("PUT:", buf, store_size);
#endif

#ifdef DEBUG_ECOMPASS_STORE
	if (ecompass->calibration_timestamp_is_valid)
		printf("Stored eCompass: Timestamp = %ld\n", ecompass->calibration_timestamp);
	else
		printf("Stored eCompass: Timestamp = INVALID\n");
	ecompass_print(ecompass);
#endif

	return store_size;

	#undef BLOCK_START
	#undef BLOCK_END
}

#endif // defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)



#if defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)

////////////////////////////////////////////////////////////////////////////////
// Needs documentation.

int ecompass_store_put_sensor_v1(ecompass_t *ecompass, ecompass_calibration_t *cal, void *buf, size_t buf_size)
{
	uint8_t	flags[SENSOR_FL_BYTE_COUNT] = {0};
	int i;
	uint8_t *ptr = static_cast<uint8_t *>(buf);
	int store_size = 0;
	uint64_t packed;
	int		 bs, shift;
	int r, c;

	// Can only store calibration if offset and cubic terms are diagonals.
	if (cal->term[0].matrix_is_used[0] && !cal->term[0].matrix_is_diagonal[0])
		return -1;
	if (cal->term[0].matrix_is_used[3] && !cal->term[0].matrix_is_diagonal[3])
		return -1;
	// Can't store if using snd order term or anything higher than third order.
	if (cal->term[0].matrix_is_used[2])
		return -1;
	for (i = 4; i <= ECOMPASS_MAX_POWER_TERM; i++)
		if (cal->term[0].matrix_is_used[i])
			return -1;

	if (ECOMPASS_TEMPERATURE_IS_VALID(cal->term[0].temperature))
		flags[0] |= SENSOR_FL_HAS_TEMPERATURE;

	if (cal->term[0].matrix_is_used[0])
		flags[0] |= SENSOR_FL_HAS_HAS_OFFSET_POLY;
	if (cal->term[0].matrix_is_used[1])
		flags[0] |= SENSOR_FL_HAS_TRANSFORM;

	if (cal->offset_is_pre_subtracted)
		flags[0] |= SENSOR_FL_HAS_OFFSET_IS_PRE_SUBTRACTED;
#if defined(ECOMPASS_MANUAL_ZERO_OFFSET_COUNT) && (ECOMPASS_MANUAL_ZERO_OFFSET_COUNT > 0)
	flags[0] |= SENSOR_FL_HAS_MANUAL_ZERO_OFFSETS;
#endif

	if (cal->term[0].matrix_is_used[3])
	{
		flags[0] |= SENSOR_FL_MORE_FLAGS;
		flags[1] |= SENSOR_FL_HAS_CUBIC;
	}

	i = 0;
	do
	{
		if (buf_size < 1)
			return -1;
		*ptr++ = flags[i];
		store_size++;
		buf_size--;
		i++;
	} while (flags[i-1] & SENSOR_FL_MORE_FLAGS);

	if (ECOMPASS_TEMPERATURE_IS_VALID(cal->term[0].temperature))
	{
		if (buf_size < BYTES_PER_NUMBER)
			return -1;

		packed = ecompass_store_pack(cal->term[0].temperature, FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
		for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
			*ptr++ = (packed >> shift) & 0xFF;

		store_size += BYTES_PER_NUMBER;
		buf_size   -= BYTES_PER_NUMBER;
	}

	if (cal->term[0].matrix_is_used[1])
	{
		bs = 9 * BYTES_PER_NUMBER;
		if (buf_size < bs)
			return -1;

		for (r = 0; r < 3; r++)
		{
			for (c = 0; c < 3; c++)
			{
				packed = ecompass_store_pack(cal->term[0].parameter[1][r][c], FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
				for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
					*ptr++ = (packed >> shift) & 0xFF;
			}
		}

		store_size += bs;
		buf_size   -= bs;
	}

	if (cal->term[0].matrix_is_used[0])
	{
		int t;

		bs = 3 * (ECOMPASS_MAX_TEMPERATURE_POWER_TERM + 1) * BYTES_PER_NUMBER;
		if (buf_size < (1 + bs))
			return -1;

		*ptr++ = (ECOMPASS_MAX_TEMPERATURE_POWER_TERM + 1);
		store_size++;
		buf_size++;

		for (i = 0; i < 3; i++)
		{
			for (t = 0; t <= ECOMPASS_MAX_TEMPERATURE_POWER_TERM; t++)
			{
				packed = ecompass_store_pack(cal->term[t].parameter[0][i][i], FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
				for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
					*ptr++ = (packed >> shift) & 0xFF;
			}
		}

		store_size += bs;
		buf_size   -= bs;
	}

#if defined(ECOMPASS_MANUAL_ZERO_OFFSET_COUNT) && (ECOMPASS_MANUAL_ZERO_OFFSET_COUNT > 0)
	bs = ECOMPASS_MANUAL_ZERO_OFFSET_COUNT * BYTES_PER_NUMBER;
	if (buf_size < (1 + bs))
		return -1;

	*ptr++ = ECOMPASS_MANUAL_ZERO_OFFSET_COUNT;
	store_size++;
	buf_size++;

	for (i = 0; i < ECOMPASS_MANUAL_ZERO_OFFSET_COUNT; i++)
	{
		packed = ecompass_store_pack(ecompass->manual_zero_offset[i], FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
		for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
			*ptr++ = (packed >> shift) & 0xFF;
	}

	store_size += bs;
	buf_size   -= bs;
#endif

	if (cal->term[0].matrix_is_used[3])
	{
		bs = 3 * BYTES_PER_NUMBER;
		if (buf_size < bs)
			return -1;

		for (i = 0; i < 3; i++)
		{
			packed = ecompass_store_pack(cal->term[0].parameter[3][i][i], FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
			for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
				*ptr++ = (packed >> shift) & 0xFF;
		}

		store_size += bs;
		// buf_size   -= bs; Dead store
	}

	return store_size;
}

#endif // defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)



#if defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)

////////////////////////////////////////////////////////////////////////////////
// Needs documentation.

static int ecompass_store_put_scalar_v1(ecompass_scalar_calibration_t *cal, void *buf, size_t buf_size)
{
	uint8_t	flags[SENSOR_FL_BYTE_COUNT] = {0};
	int i;
	uint8_t *ptr = static_cast<uint8_t *>(buf);
	int store_size = 0;
	uint64_t packed;
	int		 bs, shift;

	if (cal->is_valid)
		flags[0] |= SCALAR_FL_HAS_POLYNOMIAL;

	if (buf_size < 1)
		return -1;
	*ptr++ = flags[0];
	store_size++;
	buf_size--;

	if (cal->is_valid)
	{
		bs = (ECOMPASS_MAX_SCALAR_POWER_TERM + 1) * BYTES_PER_NUMBER;
		if (buf_size < (1 + bs))
			return -1;

		*ptr++ = (ECOMPASS_MAX_SCALAR_POWER_TERM + 1);
		store_size++;
		buf_size++;

		for (i = 0; i <= ECOMPASS_MAX_SCALAR_POWER_TERM; i++)
		{
			packed = ecompass_store_pack(cal->term[i], FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
			for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
				*ptr++ = (packed >> shift) & 0xFF;
		}

		store_size += bs;
		// buf_size   -= bs; // Dead store
	}

	return store_size;
}

#endif // defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_1)



#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_1)

////////////////////////////////////////////////////////////////////////////////
// Retrieves an ecompass from a byte buffer.
// \see ecompass_store_put() for format details.
//
// \brief Retrieves calibration parameters from binary storeage.
// \param ecompass Pointer to the ecompass.
// \param buf Pointer to buffer that \p ecompass will be retrieved from.
// \param size Size of buffer that \p ecompass will be retrieved from.
// \return Number of bytes read from buffer. 0 on error.

static size_t ecompass_store_get_v1(ecompass_t *ecompass, const void *data, size_t size)
{
	const uint8_t *ptr;
	uint8_t  block_id, version;
	uint16_t block_len, crc_calc, crc_data;
	size_t read_size, byte_count;
	int i;
	time_t time_stamp;

	ecompass_t ecompass_buf;
	uint8_t have_acc_cal = 0, have_mag_cal = 0, have_temp_cal = 0, have_time_stamp = 0;

	ecompass_init(&ecompass_buf);
	ptr = (const uint8_t *)data;
	read_size = 0;

	// GET version number
	if (size < 1)
		return 0;

	version = *ptr++;
	size--;
	read_size++;
	if (version != 1)
		return 0;

#ifdef DEBUG_HEX_DUMP_STORE
	debug_hex_dump_store("GET:", data, size + 1);
#endif

	for (;;)
	{
		if (size < 2)
			break;

		block_id  = (*ptr >> 4) & 0x0F;
		block_len = (((uint16_t)ptr[0] << 8) | (uint16_t)ptr[1]) & 0x0FFF;
		ptr += 2;
		size -= 2;
		read_size += 2;

		if (size < block_len)
			break;

		size -= block_len;
		read_size += block_len;

		switch (block_id)
		{
			case HDR_ID_CRC:
				if (block_len != 2)
					return 0;

				// Calculate CRC
				if (!crc_generator_initialised)
				{
					crc16_init(&crc_generator, CRC_POLY, CRC_INITIAL_VALUE, CRC_FINAL_XOR);
					crc_generator_initialised = 1;
				}

				crc16_begin(&crc_generator);
//				uint8_t *data2 = (uint8_t *)data;
				crc16_calc(&crc_generator, (uint8_t *)data, read_size - 2);
				crc_calc = crc16_end(&crc_generator);

				crc_data  = (uint16_t)ptr[0] << 8;
				crc_data |= (uint16_t)ptr[1];

				if (crc_calc != crc_data)
					return 0;

				if (ecompass)
				{
					if (have_acc_cal)
					{
						ecompass->accelerometer_calibration = ecompass_buf.accelerometer_calibration;
						ecompass->accelerometer_calibration.sensor = ecompassSensorAccelerometer;
					}
					if (have_mag_cal)
					{
						ecompass->magnetometer_calibration = ecompass_buf.magnetometer_calibration;
						ecompass->magnetometer_calibration.sensor = ecompassSensorMagnetometer;
					}
					if (have_temp_cal)
					{
						ecompass->temperature_calibration = ecompass_buf.temperature_calibration;
						ecompass->temperature_calibration.sensor = ecompassSensorTemperature;
					}
					if (have_time_stamp)
					{
						ecompass->calibration_timestamp = time_stamp;
						ecompass->calibration_timestamp_is_valid = (ecompass->calibration_timestamp == (time_t)-1) ? 0 : 1;
					}
//#if defined(ECOMPASS_MANUAL_ZERO_OFFSET_COUNT) && (ECOMPASS_MANUAL_ZERO_OFFSET_COUNT > 0)
//					for (i = 0; i < ECOMPASS_MANUAL_ZERO_OFFSET_COUNT; i++)
//						ecompass->manual_zero_offset[i] = ecompass_buf.manual_zero_offset[i];
//#endif
				}
				return read_size;
			case HDR_ID_TIMESTAMP:
				if (block_len > 0)
				{
					int all_bits_set = 1;

					time_stamp = 0;
					for (i = 0; i < block_len; i++)
					{
						if (ptr[i] != 0xFF) all_bits_set = 0;
						time_stamp |= (time_t)(ptr[i]) << (i << 3);
					}

					// Special case: All bits set means (time_t)-1.
					if (all_bits_set)
						time_stamp = (time_t)-1;
				}
				else
					time_stamp = (time_t)-1;
				have_time_stamp	= 1;
				break;

			case HDR_ID_ACCELEROMETER:
				byte_count = ecompass_store_get_sensor_v1(&ecompass_buf, &ecompass_buf.accelerometer_calibration, ptr, block_len);
				if (byte_count == (size_t)block_len)
					have_acc_cal = 1;
				break;

			case HDR_ID_MAGNETOMETER:
				byte_count = ecompass_store_get_sensor_v1(&ecompass_buf, &ecompass_buf.magnetometer_calibration, ptr, block_len);
				if (byte_count == (size_t)block_len)
					have_mag_cal = 1;
				break;

			case HDR_ID_TEMPERATURE:
				byte_count = ecompass_store_get_scalar_v1(&ecompass_buf.temperature_calibration, ptr, block_len);
				if (byte_count == (size_t)block_len)
					have_temp_cal = 1;
				break;
		}

		ptr += block_len;
	}

	return 0;
}

#endif // defined(ECOMPASS_STORE_CAN_READ_FORMAT_1)



#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_1)

////////////////////////////////////////////////////////////////////////////////
// Needs documentation.

static size_t ecompass_store_get_sensor_v1(ecompass_t *ecompass, ecompass_calibration_t *cal, const void *buf, size_t buf_size)
{
	uint8_t	*ptr, more;
	uint8_t	 flags[SENSOR_FL_BYTE_COUNT] = {0};
	size_t   read_size = 0;
	uint64_t packed;
	int      i, shift, bs;

	if (!cal || !buf || (buf_size < 1))
		return read_size;

	ptr = (uint8_t *)buf;

	// Parse the flags. The flags act as a table of contents for the following
	// data (plus a few other things). Grab the flags we know about and skip
	// those we don't. Flags are a sequence of n bytes ending at the first with
	// MSB clear. eg 0xxxxxxx or 1xxxxxxx 1xxxxxxx 0xxxxxxx
	// This allows future expansion without breaking backward compatibilty.
	for (more = 1, i = 0; more && (buf_size > 0); i++)
	{
		if (i < SENSOR_FL_BYTE_COUNT)
			flags[i] = *ptr;
		more = *ptr & SENSOR_FL_MORE_FLAGS;
		ptr++;
		buf_size--;
		read_size++;
	}

	ecompass_calibration_init(cal, cal->sensor);

	if (flags[0] & SENSOR_FL_HAS_TEMPERATURE)
	{
		if (buf_size < BYTES_PER_NUMBER)
			return 0;

		packed = 0;
		for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
			packed |= (uint64_t)*ptr++ << shift;
		cal->term[0].temperature = ecompass_store_unpack(packed, FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
		read_size += BYTES_PER_NUMBER;
		buf_size  -= BYTES_PER_NUMBER;
	}

	if (flags[0] & SENSOR_FL_HAS_OFFSET)
	{
		bs = 3 * BYTES_PER_NUMBER;
		if (buf_size < bs)
			return 0;

		for (i = 0; i < 3; i++)
		{
			packed = 0;
			for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
				packed |= (uint64_t)*ptr++ << shift;
			cal->term[0].parameter[0][i][i] = ecompass_store_unpack(packed, FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
		}

		read_size += bs;
		buf_size   -= bs;

		cal->term[0].matrix_is_used[0]	   = 1;
		cal->term[0].matrix_is_diagonal[0] = 1;
		cal->term[0].is_valid = 1;
	}

	if (flags[0] & SENSOR_FL_HAS_TRANSFORM)
	{
		int r, c;

		bs = 9 * BYTES_PER_NUMBER;
		if (buf_size < bs)
			return 0;

		for (r = 0; r < 3; r++)
		{
			for (c = 0; c < 3; c++)
			{
				packed = 0;
				for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
					packed |= (uint64_t)*ptr++ << shift;
				cal->term[0].parameter[1][r][c] = ecompass_store_unpack(packed, FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
			}
		}

		read_size += bs;
		buf_size   -= bs;
		cal->term[0].matrix_is_used[1]	   = 1;
		cal->term[0].matrix_is_diagonal[1] = 0;
		cal->term[0].is_valid = 1;
	}

	if (flags[0] & SENSOR_FL_HAS_HAS_OFFSET_POLY)
	{
		uint8_t terms;
		int r, t;

		// Read number of terms and load in accordingly
		if (buf_size < 1)
			return 0;
		terms = *ptr++;
		read_size++;
		buf_size--;

		bs = 3 * terms * BYTES_PER_NUMBER;
		if (buf_size < bs)
			return 0;

		for (r = t = i = 0; i <  3 * terms; i++)
		{
			packed = 0;
			for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
				packed |= (uint64_t)*ptr++ << shift;

			if ((t <= ECOMPASS_MAX_TEMPERATURE_POWER_TERM) && (r < 3))
			{
				cal->term[t].parameter[0][r][r]    = ecompass_store_unpack(packed, FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
				cal->term[t].matrix_is_used[0]	   = 1;
				cal->term[t].matrix_is_diagonal[0] = 1;
				cal->term[t].is_valid = 1;
			}

			if (++t >= terms)
			{
				t = 0;
				r++;
			}
		}

		read_size += bs;
		buf_size   -= bs;
	}

	if (flags[0] & SENSOR_FL_HAS_OFFSET_IS_PRE_SUBTRACTED)
		cal->offset_is_pre_subtracted = 1;

	if (flags[0] & SENSOR_FL_HAS_MANUAL_ZERO_OFFSETS)
	{
		uint8_t terms;

		bs = ECOMPASS_MANUAL_ZERO_OFFSET_COUNT * BYTES_PER_NUMBER;
		if (buf_size < (1 + bs))
			return 0;

		// Read number of terms and load in accordingly
		terms = *ptr++;
		read_size++;
		buf_size--;

		for (i = 0; i < terms; i++)
		{
			packed = 0;
			for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
				packed |= (uint64_t)*ptr++ << shift;

#if defined(ECOMPASS_MANUAL_ZERO_OFFSET_COUNT) && (ECOMPASS_MANUAL_ZERO_OFFSET_COUNT > 0)
			if (i >= ECOMPASS_MANUAL_ZERO_OFFSET_COUNT)
				continue;

			ecompass->manual_zero_offset[i] = ecompass_store_unpack(packed, FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
#endif
		}

		read_size += bs;
		buf_size   -= bs;
	}

	if (flags[1] & SENSOR_FL_HAS_CUBIC)
	{
		bs = 3 * BYTES_PER_NUMBER;
		if (buf_size < bs)
			return 0;

		for (i = 0; i < 3; i++)
		{
			packed = 0;
			for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
				packed |= (uint64_t)*ptr++ << shift;
			cal->term[0].parameter[3][i][i] = ecompass_store_unpack(packed, FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
		}

		read_size += bs;
		// buf_size   -= bs;	Dead store
		cal->term[0].matrix_is_used[3]	   = 1;
		cal->term[0].matrix_is_diagonal[3] = 1;
		cal->term[0].is_valid = 1;
	}

	return read_size;

}

#endif // defined(ECOMPASS_STORE_CAN_READ_FORMAT_1)




#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_1)

////////////////////////////////////////////////////////////////////////////////
// Needs documentation.

static size_t ecompass_store_get_scalar_v1(ecompass_scalar_calibration_t *cal, const void *buf, size_t buf_size)
{
	uint8_t	*ptr, more;
	uint8_t  flags[SENSOR_FL_BYTE_COUNT] = {0};
	size_t   read_size = 0;
	uint64_t packed;
	int      i, shift, bs;

	if (!cal || !buf || (buf_size < 1))
		return read_size;

	ptr = (uint8_t *)buf;

	// Parse the flags. The flags act as a table of contents for the following
	// data (plus a few other things). Grab the flags we know about and skip
	// those we don't. Flags are a sequence of n bytes ending at the first with
	// MSB clear. eg 0xxxxxxx or 1xxxxxxx 1xxxxxxx 0xxxxxxx
	// This allows future expansion without breaking backward compatibilty.
	for (more = 1, i = 0; more && (buf_size > 0); i++)
	{
		if (i < SENSOR_FL_BYTE_COUNT)
			flags[i] = *ptr;
		more = *ptr & SENSOR_FL_MORE_FLAGS;
		ptr++;
		buf_size--;
		read_size++;
	}

	ecompass_scalar_calibration_init(cal, cal->sensor);

	if (flags[0] & SCALAR_FL_HAS_POLYNOMIAL)
	{
		uint8_t terms;

		bs = (ECOMPASS_MAX_SCALAR_POWER_TERM + 1) * BYTES_PER_NUMBER;
		if (buf_size < (1 + bs))
			return 0;

		// Read number of terms and load in accordingly
		terms = *ptr++;
		read_size++;
		buf_size--;

		for (i = 0; i < terms; i++)
		{
			packed = 0;
			for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
				packed |= (uint64_t)*ptr++ << shift;

			if (i <= ECOMPASS_MAX_SCALAR_POWER_TERM)
				cal->term[i] = ecompass_store_unpack(packed, FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
		}

		read_size += bs;
		// buf_size   -= bs;  Dead Store
		cal->is_valid = 1;
	}

	return read_size;
}

#endif // defined(ECOMPASS_STORE_CAN_READ_FORMAT_1)



#pragma mark - Format version 2 functions

////////////////////////////////////////////////////////////////////////////////
// Version 2 format
// PACKET format:
//    1 byte PACKET_FORMAT = 2
//    2 byte BLOCK_HEADER
//    n byte BLOCK_PAYLOAD (n is the PAYLOAD_SIZE field in BLOCK_HEADER)
//
// BLOCK_HEADER format:
//    1 byte Upper 4 bits are BLOCK_ID, lower 4 bits form bits 8:11 of PAYLOAD_SIZE
//    1 byte Forms bits 0:9 of PAYLOAD_SIZE
//
// BLOCK_ID (which defines the BLOCK_PAYLOAD format) is one of:
//    HDR_ID_CRC:			BLOCK_PAYLOAD is 2 byte CCITT-16 CRC stored in big endian format.
//    HDR_ID_TIMESTAMP:		BLOCK_PAYLOAD is an ? byte ??? stored in little endian format.
//    HDR_ID_ACCELEROMETER: BLOCK_PAYLOAD is a sequence of SENSOR_ELEMENT objects
//    HDR_ID_MAGNETOMETER		0x03
//    HDR_ID_TEMPERATURE		0x04
//    HDR_ID_SPECIAL			0x05
//
// SENSOR_ELEMENT format:
//    1     byte ELEMENT_FORMAT (determines number of ELEMENT_VALUEs that follow.
//    n x 8 byte ELEMENT_VALUE each stored in IEEE754 binary64 big-endian format
//
// ELEMENT_FORMAT format:
//   Bits 7:6 Value type 0: Scalar (1 ELEMENT_VALUE follows ELEMENT_FORMAT)
//                       1: Vector or diagonal matrix (3 ELEMENT_VALUEs follow ELEMENT_FORMAT)
//                       2: Matrix (9 ELEMENT_VALUEs follow ELEMENT_FORMAT)
//                       3: Flags (NO ELEMENT_VALUEs follow ELEMENT_FORMAT)
//
// If ELEMENT_FORMAT is not 3 then the remaining bits are interpreted as follows.
//   Bits 5:3 TEMPERATURE_POWER (0-7) for the folowing ELEMENT_VALUE(s) if MEASUREMENT_POWER is not 7
//   Bits 2:0 MEASUREMENT_POWER (0-6) for the folowing ELEMENT_VALUE(s)
//            MEASUREMENT_POWER 7 signifies a special ELEMENT_VALUE. In this case the TEMPERATURE_POWER
//            identifies the special value:
//                V2_TP_TEMPERATURE:      ELEMENT_VALUE is a scalar indicating the calibration
//                                          temperature of the sensor.
//                V2_TP_ZERO_ROLL_OFFSET	ELEMENT_VALUE is a scalar indicating the zero roll offset
//                                          for the ecompass (found in HDR_ID_SPECIAL block type)
//
// If ELEMENT_FORMAT is 3 (flags) then the remaining bits are interpreted as follows.
//   Bit 0   Zero power term is pre-subtracted if set.




#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_2) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)

////////////////////////////////////////////////////////////////////////////////
// Needs documentation.

static size_t ecompass_store_space_required_sensor_v2(ecompass_calibration_t *cal)
{
	size_t size = 0;
	int p, t;
	uint8_t flags = 0;

	// Sanity checks
	if (!cal)
		return size;

	if (cal->offset_is_pre_subtracted)
		flags |= V2_FLAG_OFFSET_IS_PRE_SUBTRACTED;

	if (flags != 0)
		size += 1;

	if (ECOMPASS_TEMPERATURE_IS_VALID(cal->term[0].temperature))
		size += 1 + BYTES_PER_DOUBLE;

	for (p = 0; p <= ECOMPASS_MAX_POWER_TERM; p++)
	{
		for (t = 0; t <= ECOMPASS_MAX_TEMPERATURE_POWER_TERM; t++)
		{
			if (!cal->term[t].is_valid || !cal->term[t].matrix_is_used[p])
				continue;
			if (cal->term[t].matrix_is_diagonal[p])
				size += 1 + (BYTES_PER_DOUBLE * 3);
			else
				size += 1 + (BYTES_PER_DOUBLE * 9);
		}
	}

	return size;
}

#endif // defined(ECOMPASS_STORE_CAN_READ_FORMAT_2) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)



#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_2) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)

////////////////////////////////////////////////////////////////////////////////
// Needs documentation.

static size_t ecompass_store_space_required_scalar_v2(ecompass_scalar_calibration_t *cal)
{
	size_t size = 0;
	uint8_t flags = 0;

	// Sanity checks
	if (!cal)
		return size;

	// No flags currently used. Code is here to make life easier in case they are.
	if (flags != 0)
		size += 1;

	if (cal->is_valid)
		size += (ECOMPASS_MAX_SCALAR_POWER_TERM + 1) * (1 + BYTES_PER_DOUBLE);

	return size;
}

#endif // defined(ECOMPASS_STORE_CAN_READ_FORMAT_2) || defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)



#if defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)

////////////////////////////////////////////////////////////////////////////////
// Stores an ecompass into a byte buffer using format version 1.
//
// \brief Stores calibration parameters to binary buffer.
// \param ecompass Pointer to the ecompass.
// \param buf Pointer to buffer that \p ecompass will be stored into.
// \param size Size of buffer that  \p ecompass will be stored into.
// \return Number of bytes put into the buffer. 0 on error.

static size_t ecompass_store_put_v2(ecompass_t *ecompass, void *buf, size_t buf_size)
{
#define BLOCK_START		{ block_ptr = ptr; ptr += HEADER_BYTES; store_size += HEADER_BYTES; buf_size -= HEADER_BYTES; }
#define BLOCK_END(i,s)	{ block_ptr[0] = (((i) << 4) & 0xF0) | ((s) >> 8) & 0x0F; block_ptr[1] = (s) & 0xFF; }

	uint8_t *ptr, *block_ptr;
	uint16_t crc;
	size_t   bs, store_size = 0;
	int      block_size;

	if (!ecompass)
		return 0;

	if (buf_size < ecompass_store_space_required(ecompass))
		return 0;

	ptr = static_cast<uint8_t *>(buf);

	// Add version
	*ptr++ = 2;
	store_size++;
	buf_size--;

	// Add time stamp if it is valid
	if (ecompass->calibration_timestamp_is_valid && (buf_size >= (HEADER_BYTES + TIMESTAMP_BYTES)))
	{
		int i;
		time_t temp;

		BLOCK_START;

		for (i = 0, temp = ecompass->calibration_timestamp; i < TIMESTAMP_BYTES; i++, temp >>= 8)
		{
			*ptr++ = (uint8_t)(temp & 0xFF);
			store_size++;
			buf_size--;
		}

		BLOCK_END(HDR_ID_TIMESTAMP, TIMESTAMP_BYTES);
	}

	BLOCK_START;
	if ((block_size = ecompass_store_put_sensor_v2(&ecompass->accelerometer_calibration, ptr, buf_size)) < 0)
		return 0;
	ptr += block_size;
	store_size += block_size;
	buf_size -= block_size;
	BLOCK_END(HDR_ID_ACCELEROMETER, block_size);

	BLOCK_START;
	if ((block_size = ecompass_store_put_sensor_v2(&ecompass->magnetometer_calibration, ptr, buf_size)) < 0)
		return 0;
	ptr += block_size;
	store_size += block_size;
	buf_size -= block_size;
	BLOCK_END(HDR_ID_MAGNETOMETER, block_size);

	BLOCK_START;
	if ((block_size = ecompass_store_put_scalar_v2(&ecompass->temperature_calibration, ptr, buf_size)) < 0)
		return 0;
	ptr += block_size;
	store_size += block_size;
	buf_size -= block_size;
	BLOCK_END(HDR_ID_TEMPERATURE, block_size);

	// Add zero roll offset
#if defined(ECOMPASS_MANUAL_ZERO_OFFSET_COUNT) && (ECOMPASS_MANUAL_ZERO_OFFSET_COUNT > 0)
	bs = 1 + BYTES_PER_NUMBER;
	if (buf_size >= (HEADER_BYTES + bs))
	{
		uint64_t packed;
		int		 shift;

		BLOCK_START;

		*ptr++ = V2_MAKE_MAT_HDR(V2_FMT_SCALAR, V2_TP_SPECIAL_ZERO_ROLL_OFFSET, V2_MP_SPECIAL);

		packed = ecompass_store_pack(ecompass->manual_zero_offset[ECOMPASS_MANUAL_ZERO_ROLL_OFFSET], FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
		for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
			*ptr++ = (packed >> shift) & 0xFF;

		store_size += bs;
		buf_size   -= bs;

		BLOCK_END(HDR_ID_SPECIAL, bs);
	}
#endif

	if (buf_size < (HEADER_BYTES + 2))
		return 0;

	// Calculate CRC
	if (!crc_generator_initialised)
	{
		crc16_init(&crc_generator, CRC_POLY, CRC_INITIAL_VALUE, CRC_FINAL_XOR);
		crc_generator_initialised = 1;
	}

	// Store CRC. Build the block header first then drop CRC in last.
	BLOCK_START;
	store_size += 2;
	//buf_size -= 2; // Dead store
	ptr += 2;
	BLOCK_END(HDR_ID_CRC, 2);
	crc16_begin(&crc_generator);
	crc16_calc(&crc_generator, (uint8_t *)buf, store_size-2);
	crc = crc16_end(&crc_generator);
	ptr[-2] = (uint8_t)((crc >> 8) & 0xFF);
	ptr[-1] = (uint8_t)(crc & 0xFF);

#ifdef DEBUG_HEX_DUMP_STORE
	debug_hex_dump_store("PUT:", buf, store_size);
#endif

#ifdef DEBUG_ECOMPASS_STORE
	if (ecompass->calibration_timestamp_is_valid)
		printf("Stored eCompass: Timestamp = %ld\n", ecompass->calibration_timestamp);
	else
		printf("Stored eCompass: Timestamp = INVALID\n");
	ecompass_print(ecompass);
#endif

	return store_size;

#undef BLOCK_START
#undef BLOCK_END
}

#endif // defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)



#if defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)

////////////////////////////////////////////////////////////////////////////////
// Needs documentation.

static int ecompass_store_put_sensor_v2(ecompass_calibration_t *cal, void *buf, size_t buf_size)
{
	uint8_t *ptr = static_cast<uint8_t *>(buf);
	int		 store_size = 0;
	uint64_t packed;
	int		 bs, shift;
	int		 p, t, r, c;
	uint8_t  flags = 0;


	if (cal->offset_is_pre_subtracted)
		flags |= V2_FLAG_OFFSET_IS_PRE_SUBTRACTED;

	if (flags)
	{
		if (buf_size < (1 + BYTES_PER_NUMBER))
			return -1;
		*ptr++ = V2_MAKE_FLAGS_HDR(flags);
		store_size++;
		buf_size--;
	}

	if (ECOMPASS_TEMPERATURE_IS_VALID(cal->term[0].temperature))
	{
		if (buf_size < (1 + BYTES_PER_NUMBER))
			return -1;

		*ptr++ = V2_MAKE_MAT_HDR(V2_FMT_SCALAR, V2_TP_SPECIAL_TEMPERATURE, V2_MP_SPECIAL);
		store_size++;
		buf_size--;

		packed = ecompass_store_pack(cal->term[0].temperature, FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
		for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
			*ptr++ = (packed >> shift) & 0xFF;

		store_size += BYTES_PER_NUMBER;
		buf_size   -= BYTES_PER_NUMBER;
	}

	for (p = 0; p <= ECOMPASS_MAX_POWER_TERM; p++)
	{
		for (t = 0; t <= ECOMPASS_MAX_TEMPERATURE_POWER_TERM; t++)
		{
			if (!cal->term[t].is_valid || !cal->term[t].matrix_is_used[p])
				continue;

			if (cal->term[t].matrix_is_diagonal[p])
			{
				bs = 1 + (3 * BYTES_PER_NUMBER);
				if (buf_size < bs)
					return -1;

				*ptr++ = V2_MAKE_MAT_HDR(V2_FMT_VECTOR3, t, p);

				for (r = 0; r < 3; r++)
				{
					packed = ecompass_store_pack(cal->term[t].parameter[p][r][r], FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
					for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
						*ptr++ = (packed >> shift) & 0xFF;
				}

				store_size += bs;
				buf_size   -= bs;
			}
			else
			{
				bs = 1 + (9 * BYTES_PER_NUMBER);
				if (buf_size < bs)
					return -1;

				*ptr++ = V2_MAKE_MAT_HDR(V2_FMT_MATRIX33, t, p);

				for (r = 0; r < 3; r++)
				{
					for (c = 0; c < 3; c++)
					{
						packed = ecompass_store_pack(cal->term[t].parameter[p][r][c], FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
						for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
							*ptr++ = (packed >> shift) & 0xFF;
					}
				}

				store_size += bs;
				buf_size   -= bs;
			}
		}
	}

	return store_size;
}

#endif // defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)



#if defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)

////////////////////////////////////////////////////////////////////////////////
// Needs documentation.

static int ecompass_store_put_scalar_v2(ecompass_scalar_calibration_t *cal, void *buf, size_t buf_size)
{
	int		 t;
	uint8_t *ptr = static_cast<uint8_t *>(buf);
	int		 store_size = 0;
	uint64_t packed;
	int		 bs, shift;
	uint8_t  flags = 0;


	// No flags currently used. Code is here to make life easier in case they are.
	if (flags)
	{
		if (buf_size < (1 + BYTES_PER_NUMBER))
			return -1;
		*ptr++ = V2_MAKE_FLAGS_HDR(flags);
		store_size++;
		buf_size--;
	}

	for (t = 0; t <= ECOMPASS_MAX_SCALAR_POWER_TERM; t++)
	{
		if (!cal->is_valid)
			continue;

		bs = 1 + BYTES_PER_NUMBER;
		if (buf_size < bs)
			return -1;

		*ptr++ = V2_MAKE_MAT_HDR(V2_FMT_SCALAR, t, 0);

		packed = ecompass_store_pack(cal->term[t], FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
		for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
			*ptr++ = (packed >> shift) & 0xFF;

		store_size += bs;
		buf_size   -= bs;
	}

	return store_size;
}

#endif // defined(ECOMPASS_STORE_CAN_WRITE_FORMAT_2)



#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_2)

////////////////////////////////////////////////////////////////////////////////
// Retrieves an ecompass from a byte buffer.
// \see ecompass_store_put() for format details.
//
// \brief Retrieves calibration parameters from binary storeage.
// \param ecompass Pointer to the ecompass.
// \param buf Pointer to buffer that \p ecompass will be retrieved from.
// \param size Size of buffer that \p ecompass will be retrieved from.
// \return Number of bytes read from buffer. 0 on error.

static size_t ecompass_store_get_v2(ecompass_t *ecompass, const void *data, size_t size)
{
	uint8_t *data2 = (uint8_t *)data;
	const uint8_t *ptr;
	uint8_t  block_id, version;
	uint16_t block_len, crc_calc, crc_data;
	size_t read_size, byte_count;
	int i;
	time_t time_stamp;
	ecompass_calibration_t acc_cal;
	ecompass_calibration_t mag_cal;
	ecompass_scalar_calibration_t temp_cal;
	uint8_t have_acc_cal = 0, have_mag_cal = 0, have_temp_cal = 0, have_time_stamp = 0;
	double  special_values[V2_TP_SPECIAL_COUNT];
	int		have_special[V2_TP_SPECIAL_COUNT];

	ecompass_calibration_init(&acc_cal, ecompassSensorAccelerometer);
	ecompass_calibration_init(&mag_cal, ecompassSensorMagnetometer);
	ecompass_scalar_calibration_init(&temp_cal, ecompassSensorTemperature);
	ptr = (const uint8_t *)data;
	read_size = 0;

	for (i = 0; i < V2_TP_SPECIAL_COUNT; i++)
		have_special[i] = 0;

	// GET version number
	if (size < 1)
		return 0;

	version = *ptr++;
	size--;
	read_size++;
	if (version != 2)
		return 0;

#ifdef DEBUG_HEX_DUMP_STORE
	debug_hex_dump_store("GET:", data, size + 1);
#endif

	for (;;)
	{
		if (size < 2)
			break;

		block_id  = (*ptr >> 4) & 0x0F;
		block_len = (((uint16_t)ptr[0] << 8) | (uint16_t)ptr[1]) & 0x0FFF;
		ptr += 2;
		size -= 2;
		read_size += 2;

		if (size < block_len)
			break;

		size -= block_len;
		read_size += block_len;

		switch (block_id)
		{
			case HDR_ID_CRC:
				if (block_len != 2)
					return 0;

				// Calculate CRC
				if (!crc_generator_initialised)
				{
					crc16_init(&crc_generator, CRC_POLY, CRC_INITIAL_VALUE, CRC_FINAL_XOR);
					crc_generator_initialised = 1;
				}

				crc16_begin(&crc_generator);
				crc16_calc(&crc_generator, data2, read_size - 2);
				crc_calc = crc16_end(&crc_generator);

				crc_data  = (uint16_t)ptr[0] << 8;
				crc_data |= (uint16_t)ptr[1];

				if (crc_calc != crc_data)
					return 0;

				if (ecompass)
				{
					if (have_acc_cal)
					{
						ecompass->accelerometer_calibration = acc_cal;
						ecompass->accelerometer_calibration.sensor = ecompassSensorAccelerometer;
					}
					if (have_mag_cal)
					{
						ecompass->magnetometer_calibration = mag_cal;
						ecompass->magnetometer_calibration.sensor = ecompassSensorMagnetometer;
					}
					if (have_temp_cal)
					{
						ecompass->temperature_calibration = temp_cal;
						ecompass->temperature_calibration.sensor = ecompassSensorTemperature;
					}
					if (have_time_stamp)
					{
						ecompass->calibration_timestamp = time_stamp;
						ecompass->calibration_timestamp_is_valid = (ecompass->calibration_timestamp == (time_t)-1) ? 0 : 1;
					}

#if defined(ECOMPASS_MANUAL_ZERO_OFFSET_COUNT) && (ECOMPASS_MANUAL_ZERO_OFFSET_COUNT > 0)
					if (have_special[V2_TP_SPECIAL_ZERO_ROLL_OFFSET])
						ecompass->manual_zero_offset[ECOMPASS_MANUAL_ZERO_ROLL_OFFSET] = special_values[V2_TP_SPECIAL_ZERO_ROLL_OFFSET];
#endif

				}

#ifdef DEBUG_ECOMPASS_STORE
				if (ecompass->calibration_timestamp_is_valid)
					printf("Retrieved eCompass: Timestamp = %ld\n", ecompass->calibration_timestamp);
				else
					printf("Retrieved eCompass: Timestamp = INVALID\n");
				ecompass_print(ecompass);
#endif
				return read_size;

			case HDR_ID_TIMESTAMP:
				if (block_len > 0)
				{
					int all_bits_set = 1;

					time_stamp = 0;
					for (i = 0; i < block_len; i++)
					{
						if (ptr[i] != 0xFF) all_bits_set = 0;
						time_stamp |= (time_t)(ptr[i]) << (i << 3);
					}

					// Special case: All bits set means (time_t)-1.
					if (all_bits_set)
						time_stamp = (time_t)-1;
				}
				else
					time_stamp = (time_t)-1;
				have_time_stamp	= 1;
				break;

			case HDR_ID_ACCELEROMETER:
				byte_count = ecompass_store_get_sensor_v2(&acc_cal, ptr, block_len);
				if (byte_count == (size_t)block_len)
					have_acc_cal = 1;
				break;

			case HDR_ID_MAGNETOMETER:
				byte_count = ecompass_store_get_sensor_v2(&mag_cal, ptr, block_len);
				if (byte_count == (size_t)block_len)
					have_mag_cal = 1;
				break;

			case HDR_ID_TEMPERATURE:
				byte_count = ecompass_store_get_scalar_v2(&temp_cal, ptr, block_len);
				if (byte_count == (size_t)block_len)
					have_temp_cal = 1;
				break;

			case HDR_ID_SPECIAL:
				ecompass_store_get_special_v2(special_values, have_special, ptr, block_len);
				break;
		}

		ptr += block_len;
	}

	return 0;
}

#endif // defined(ECOMPASS_STORE_CAN_READ_FORMAT_2)



#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_2)

////////////////////////////////////////////////////////////////////////////////
// Needs documentation.

static size_t ecompass_store_get_value_v2(double *mat_buf, uint8_t *flag_buf, const void *buf, size_t buf_size, int *mp_idx, int *tp_idx, int *is_diagonal)
{
	uint8_t	*ptr;
	size_t   read_size = 0;
	uint64_t packed;
	int		 shift, r, c;
	double   value;

	if (buf_size < 1)
		return 0;

	ptr = (uint8_t *)buf;

	int format = V2_GET_HDR_FMT(*ptr);
	if (format != V2_FMT_FLAGS)
	{
		if (tp_idx) *tp_idx = V2_GET_HDR_TP(*ptr);
		if (mp_idx) *mp_idx = V2_GET_HDR_MP(*ptr);
	}
	else
	{
		// Put clags in flags buffer and idicate to caller setting special (pseudo) MP and TP
		if (flag_buf) *flag_buf = V2_GET_HDR_FLAGS(*ptr);
		if (tp_idx)	  *tp_idx = V2_TP_SPECIAL_FLAGS;
		if (mp_idx)   *mp_idx = V2_MP_SPECIAL;

	}

	ptr++;
	read_size++;
	buf_size--;

	if (is_diagonal) *is_diagonal = 0;

	switch (format)
	{
		case V2_FMT_FLAGS:
			break;

		case V2_FMT_SCALAR:
			if (buf_size < BYTES_PER_NUMBER)
				return 0;

			packed = 0;
			for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
				packed |= (uint64_t)*ptr++ << shift;
			value = ecompass_store_unpack(packed, FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
			read_size += BYTES_PER_NUMBER;
			// buf_size  -= BYTES_PER_NUMBER;	Dead store

			if (mat_buf)
			{
				mat_buf[8] = mat_buf[4] = mat_buf[0] = value;
				mat_buf[1] = mat_buf[2] = mat_buf[3] = 0.0;
				mat_buf[5] = mat_buf[6] = mat_buf[7] = 0.0;
			}
			if (is_diagonal) *is_diagonal = 1;
			break;

		case V2_FMT_VECTOR3:
			if (buf_size < (3 * BYTES_PER_NUMBER))
				return 0;

			for (r = 0; r < 3; r++)
			{
				packed = 0;
				for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
					packed |= (uint64_t)*ptr++ << shift;
				value = ecompass_store_unpack(packed, FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
				read_size += BYTES_PER_NUMBER;
				buf_size  -= BYTES_PER_NUMBER;

				if (mat_buf)
					mat_buf[(r * 3) + r] = value;
			}
			if (mat_buf)
			{
				mat_buf[1] = mat_buf[2] = mat_buf[3] = 0.0;
				mat_buf[5] = mat_buf[6] = mat_buf[7] = 0.0;
			}
			if (is_diagonal) *is_diagonal = 1;
			break;

		case V2_FMT_MATRIX33:
			if (buf_size < (9 * BYTES_PER_NUMBER))
				return 0;

			for (r = 0; r < 3; r++)
			{
				for (c = 0; c < 3; c++)
				{
					packed = 0;
					for (shift = ((BYTES_PER_NUMBER - 1) << 3); shift >= 0; shift -= 8)
						packed |= (uint64_t)*ptr++ << shift;
					value = ecompass_store_unpack(packed, FLOATING_POINT_SIZE_BITS, FLOATING_POINT_EXP_BITS);
					read_size += BYTES_PER_NUMBER;
					buf_size  -= BYTES_PER_NUMBER;

					if (mat_buf)
						mat_buf[(r * 3) + c] = value;
				}
			}
			break;

		default:
			return 0;
	}

	return read_size;
}

#endif // defined(ECOMPASS_STORE_CAN_READ_FORMAT_2)



#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_2)

////////////////////////////////////////////////////////////////////////////////
// Needs documentation.

static size_t ecompass_store_get_sensor_v2(ecompass_calibration_t *cal, const void *buf, size_t buf_size)
{
	uint8_t	*ptr;
	size_t   len, read_size = 0;
	int      mp, tp, is_diagonal;
	uint8_t  flags = 0;
	double   mat_buf[9];

	if (!cal || !buf || (buf_size < 1))
		return read_size;

	ptr = (uint8_t *)buf;

	ecompass_calibration_init(cal, cal->sensor);

	while (buf_size > 0)
	{
		if ((len = ecompass_store_get_value_v2(mat_buf, &flags, ptr, buf_size, &mp, &tp, &is_diagonal)) == 0)
			return 0;

		ptr		  += len;
		read_size += len;
		buf_size  -= len;

		if (mp == V2_MP_SPECIAL)
		{
			switch (tp)
			{
				case V2_TP_SPECIAL_FLAGS:
					cal->offset_is_pre_subtracted = (flags & V2_FLAG_OFFSET_IS_PRE_SUBTRACTED) ? 1 : 0;
					break;

				case V2_TP_SPECIAL_TEMPERATURE:
					if (tp <= ECOMPASS_MAX_TEMPERATURE_POWER_TERM)
						cal->term[tp].temperature = mat_buf[0];
					break;

				case V2_TP_SPECIAL_ZERO_ROLL_OFFSET:	break; // Should not get this here. only in special block
			}
		}

		else if ((mp <= ECOMPASS_MAX_POWER_TERM) && (tp <= ECOMPASS_MAX_TEMPERATURE_POWER_TERM))
		{
			cal->term[tp].parameter[mp][0][0] = mat_buf[0];
			cal->term[tp].parameter[mp][0][1] = mat_buf[1];
			cal->term[tp].parameter[mp][0][2] = mat_buf[2];
			cal->term[tp].parameter[mp][1][0] = mat_buf[3];
			cal->term[tp].parameter[mp][1][1] = mat_buf[4];
			cal->term[tp].parameter[mp][1][2] = mat_buf[5];
			cal->term[tp].parameter[mp][2][0] = mat_buf[6];
			cal->term[tp].parameter[mp][2][1] = mat_buf[7];
			cal->term[tp].parameter[mp][2][2] = mat_buf[8];
			cal->term[tp].matrix_is_used[mp] = 1;
			cal->term[tp].matrix_is_diagonal[mp] = is_diagonal;
			cal->term[tp].is_valid = 1;
		}
	}

	return read_size;
}

#endif // defined(ECOMPASS_STORE_CAN_READ_FORMAT_2)



#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_2)

////////////////////////////////////////////////////////////////////////////////
// Needs documentation.

static size_t ecompass_store_get_scalar_v2(ecompass_scalar_calibration_t *cal, const void *buf, size_t buf_size)
{
	uint8_t	*ptr;
	size_t   len, read_size = 0;
	int      mp, tp, is_diagonal;
	uint8_t  flags = 0;
	double   mat_buf[9];

	if (!cal || !buf || (buf_size < 1))
		return read_size;

	ptr = (uint8_t *)buf;

	ecompass_scalar_calibration_init(cal, cal->sensor);

	while (buf_size > 0)
	{
		if ((len = ecompass_store_get_value_v2(mat_buf, &flags, ptr, buf_size, &mp, &tp, &is_diagonal)) == 0)
			return 0;

		ptr		  += len;
		read_size += len;
		buf_size  -= len;

		if (mp == V2_MP_SPECIAL)
		{
			switch (tp)
			{
				case V2_TP_SPECIAL_FLAGS:			 break;		// No flags currently stored in scalar sensors.
				case V2_TP_SPECIAL_TEMPERATURE:		 break; 	// Scalar sensors do not have this
				case V2_TP_SPECIAL_ZERO_ROLL_OFFSET: break;		// Should not appear in a scalar sensor block
			}
		}

		else if ((mp == 0) && (tp <= ECOMPASS_MAX_SCALAR_POWER_TERM))
		{
			cal->term[tp] = mat_buf[0];
			cal->is_valid = 1;
		}
	}

	return read_size;
}

#endif // defined(ECOMPASS_STORE_CAN_READ_FORMAT_2)

#if defined(ECOMPASS_STORE_CAN_READ_FORMAT_2)

////////////////////////////////////////////////////////////////////////////////
// Needs documentation.

static size_t ecompass_store_get_special_v2(double *special_buf, int *have_special, const void *buf, size_t buf_size)
{
	uint8_t	*ptr;
	size_t   len, read_size = 0;
	int      mp, tp, is_diagonal;
	uint8_t  flags = 0;
	double   mat_buf[9];

	if (!buf || (buf_size < 1))
		return read_size;

	ptr = (uint8_t *)buf;

	while (buf_size > 0)
	{
		if ((len = ecompass_store_get_value_v2(mat_buf, &flags, ptr, buf_size, &mp, &tp, &is_diagonal)) == 0)
			return 0;

		ptr		  += len;
		read_size += len;
		buf_size  -= len;

		if (mp == V2_MP_SPECIAL)
		{
			switch (tp)
			{
				case V2_TP_SPECIAL_FLAGS:			 break;		// No flags currently stored in special block.
				case V2_TP_SPECIAL_TEMPERATURE:		 break; 	// Special block does not have this
				case V2_TP_SPECIAL_ZERO_ROLL_OFFSET:
					if (special_buf)
						special_buf[V2_TP_SPECIAL_ZERO_ROLL_OFFSET] = mat_buf[0];
					if (have_special)
						have_special[V2_TP_SPECIAL_ZERO_ROLL_OFFSET] = 1;
					break;
			}
		}

		// Special block shold only contain elements with special MP (they don't apply on a per-ensor basis).
	}

	return read_size;
}

#endif // defined(ECOMPASS_STORE_CAN_READ_FORMAT_2)


/// \} End of ecompass_store_group


