//
// Created by goann on 13/06/2024.
//
#include <stdlib.h>


#ifndef LIBTEST_ECOMPASS_STORE_H
#define LIBTEST_ECOMPASS_STORE_H


////////////////////////////////////////////////////////////////////////////////
///
/// \file   ecompass_store.h
/// \brief  Electronic compass memory storage.
/// \author Martin Louis
/// \date   Created: 2012-11-8
///
/// Encodes and decodes elements of an ecompass to and from a memory image.
///
/// \defgroup ecompass_store_group eCompass memory storage.


/// \ingroup ecompass_store_group
/// \{



////////////////////////////////////////////////////////////////////////////////
// Compile time configuration.

#define ECOMPASS_STORE_USE_V1_IF_POSSIBLE	///< Compile time option to force the code to store in version 1 format if possible for maximum backward compatibility.
#define ECOMPASS_STORE_CAN_READ_FORMAT_1	///< Compile time option enable support for reading data in version 1 format.
#define ECOMPASS_STORE_CAN_WRITE_FORMAT_1	///< Compile time option enable support for writing data in version 1 format.
#define ECOMPASS_STORE_CAN_READ_FORMAT_2	///< Compile time option enable support for reading data in version 2 format.
#define ECOMPASS_STORE_CAN_WRITE_FORMAT_2	///< Compile time option enable support for writing data in version 2 format.

////////////////////////////////////////////////////////////////////////////////
// Constants.

/// Selects a data format version
typedef enum
{
    ecompassStoreFormatBest = 0,	///< Selects the best version to be used for the data
    ecompassStoreFormat1	= 1,	///< Selects format version 1
    ecompassStoreFormat2	= 2,	///< Selects format version 2
} ecompass_store_format_t;

#define ECOMPASS_STORE_FORMAT_DEFAULT	ecompassStoreFormatBest		///< Default format version if none is specified.



////////////////////////////////////////////////////////////////////////////////
/// Returns a value indicating wether or not an ecompass can be stored using a
/// specified storeage format.
///
/// \brief Indicates if an ecompass can be stored to a specified format.
/// \param ecompass Pointer to the ecompass.
/// \param format The storage format to be used to for the calibration parameters.
/// \return Boolean value indicate if the ecompass can be fully stored to
/// the specified format.
/// \retval 0 The ecomapass can be stored to the specified format.
/// \retval non-zero The ecomapass can not be stored to the specified format.

int ecompass_can_store_to_format(ecompass_t *ecompass, ecompass_store_format_t format);



////////////////////////////////////////////////////////////////////////////////
/// Calculates and returns the amount of space (in bytes) required to store
/// the specified ecompass object using the default format.
///
/// \brief Returns storage space required for an ecompass_t.
/// \param ecompass Pointer to the ecompass.
/// \return Number of bytes required for storage of \p ecompass.

size_t ecompass_store_space_required(ecompass_t *ecompass);



////////////////////////////////////////////////////////////////////////////////
/// Calculates and returns the amount of space (in bytes) required to store
/// the specified ecompass object using the default format.
///
/// \brief Returns storage space required for an ecompass_t.
/// \param ecompass Pointer to the ecompass.
/// \param format The storage format version to be used to for the calibration
/// parameters.
/// \return Number of bytes required for storage of \p ecompass.

size_t ecompass_store_space_required_for_format(ecompass_t *ecompass, ecompass_store_format_t format);



////////////////////////////////////////////////////////////////////////////////
/// Stores an ecompass into a byte buffer using default format.
///
/// \brief Stores calibration parameters to binary buffer.
/// \param ecompass Pointer to the ecompass.
/// \param buf Pointer to buffer that \p ecompass will be stored into.
/// \param size Size of buffer that  \p ecompass will be stored into.
/// \return Number of bytes put into the buffer. 0 on error.

int ecompass_store_put(ecompass_t *ecompass, uint8_t *buf, size_t size);

int ecompass_store_put_sensor_v1(ecompass_t *ecompass, ecompass_calibration_t *cal, uint8_t *buf, size_t buf_size);

////////////////////////////////////////////////////////////////////////////////
/// Stores an ecompass into a byte buffer using specified format.
///
/// \brief Stores calibration parameters to binary buffer.
/// \param ecompass Pointer to the ecompass.
/// \param buf Pointer to buffer that \p ecompass will be stored into.
/// \param size Size of buffer that  \p ecompass will be stored into.
/// \param format The format version that will be used to store \p ecompass.
/// \return Number of bytes put into the buffer. 0 on error.

int ecompass_store_put_using_format(ecompass_t *ecompass, uint8_t *buf, size_t size, ecompass_store_format_t format);



////////////////////////////////////////////////////////////////////////////////
/// Retrieves an ecompass from a byte buffer.
/// \see ecompass_store_put() for format details.
///
/// \brief Retrieves calibration parameters from binary storeage.
/// \param ecompass Pointer to the ecompass.
/// \param buf Pointer to buffer that \p ecompass will be retrieved from.
/// \param size Size of buffer that \p ecompass will be retrieved from.
/// \return Number of bytes read from buffer. 0 on error.

size_t ecompass_store_get(ecompass_t *ecompass, const uint8_t *data, size_t size);



#if defined(ECOMPASS_UNIT_TEST)

////////////////////////////////////////////////////////////////////////////////
/// Test packing and unpacking of calibration parameters to buffer. Dumps test
/// results to console.
///
/// \brief Test claibration parameter packing,
/// \return Error count. Number of errors detected
/// \retval 0 No error. All tests passed.

int ecompass_test_store(void);

#endif

#endif //LIBTEST_ECOMPASS_STORE_H
