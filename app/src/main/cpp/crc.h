//
// Created by goann on 6/06/2024.
//

#ifndef LIBTEST_CRC_H
#define LIBTEST_CRC_H


#if defined(__WIN32__) || defined(WIN32)
#include <windows.h>
#include "win_posix.h"
#else
#include <stddef.h>
#include <stdint.h>
#include <string.h>
#include <iostream>
#endif



////////////////////////////////////////////////////////////////////////////////
// CRC polynomials

#define CRC8_POLY_ATM			0x07		///< 8-bit ATM polynomial:     x8 + x2 + x + 1
#define CRC8_POLY_CCITT			0x8D		///< 8-bit CCITT polynomial:   x8 + x7 + x3 + x2 + 1
#define CRC8_POLY_DALLAS		0x31		///< 8-bit DALLAS polynomial:  x8 + x5 + x4 + 1
#define CRC8_POLY_KOOPMAN		0xD5		///< 8-bit KOOPMAN polynomial: x8 + x7 + x6 + x4 + x2 + 1
#define CRC8_POLY_SAE			0x1D		///< 8-bit SAE polynomial:     x8 + x4 + x3 + x2 + 1

#define CRC16_POLY_IBM			0x8005		///< 16-bit IBM polynomial:     x16 + x15 + x2 + 1
#define CRC16_POLY_CCITT		0x1021		///< 16-bit CCITT polynomial:   x16 + x12 + x5 + 1
#define CRC16_POLY_T10_DIF		0x8BB7		///< 16-bit T10 DIF polynomial: x16 + x15 + x11 + x9 + x8 + x7 + x5 + x4 + x2 + x + 1
#define CRC16_POLY_DNP			0x3D65		///< 16-bit DNP polynomial:	    x16 + x13 + x12 + x11 + x10 + x8 + x6 + x5 + x2 + 1

#define CRC32_POLY_IEEE			0x04C11DB7	///< 32-bit IEEE polynomial: x32 + x26 + x23 + x22 + x16 + x12 + x11 + x10 + x8 + x7 + x5 + x4 + x2 + x + 1
#define CRC32_POLY_32C			0x1EDC6F41	///< 32-bit 32C polynomial:  x32 + x28 + x27 + x26 + x25 + x23 + x22 + x20 + x19 + x18 + x14 + x13 + x11 + x10 + x9 + x8 + x6 + 1
#define CRC32_POLY_32K			0x741B8CD7	///< 32-bit 32K polynomial:  x32 + x30 + x29 + x28 + x26 + x20 + x19 + x17 + x16 + x15 + x11 + x10 + x7 + x6 + x4 + x2 + x + 1
#define CRC32_POLY_32Q			0x814141AB	///< 32-bit 32Q polynomial:  x32 + x31 + x24 + x22 + x16 + x14 + x8 + x7 + x5 + x3 + x + 1



////////////////////////////////////////////////////////////////////////////////
/// Context and state for a 8-bit CRC calculation
///

typedef struct
{
    uint8_t	crc;			///< CRC accumulator.
    uint8_t	poly;			///< CRC polynomial.
    uint8_t	initial_value;	///< Initialisation value for CRC.
    uint8_t final_xor;		///< Value that is XORed with final CRC giving result.
    uint8_t table[256];		///< Lookup table for genrating the CRC
} crc8_t;



////////////////////////////////////////////////////////////////////////////////
/// Context and state for a 8-bit CRC calculation
///
// \ingroup crc_public_group

typedef struct
{
    uint16_t crc;			///< CRC accumulator.
    uint16_t poly;			///< CRC polynomial.
    uint16_t initial_value;	///< Initialisation value for CRC.
    uint16_t final_xor;		///< Value that is XORed with final CRC giving result.
    uint16_t table[256];	///< Lookup table for genrating the CRC
} crc16_t;



////////////////////////////////////////////////////////////////////////////////
/// Context and state for a 8-bit CRC calculation
///

typedef struct
{
    uint32_t crc;			///< CRC accumulator.
    uint32_t poly;			///< CRC polynomial.
    uint32_t initial_value;	///< Initialisation value for CRC.
    uint32_t final_xor;		///< Value that is XORed with final CRC giving result.
    uint32_t table[256];	///< Lookup table for genrating the CRC
} crc32_t;



////////////////////////////////////////////////////////////////////////////////
/// Initialises an 8 bit CRC state structure by generating an appropriate
/// look-up table for required polynomial and storing, initial and final XOR
/// values.
///
/// \brief Initialises 8 bit crc state.
/// \param crc8 Pointer to a CRC table/state structure to be initialised. This
/// is then used as a "context" for subsequent CRC calculations.
/// \param poly CRC polygon with which to initialise the table of \p crc8. Bit n
/// represents the polynomial's n'th co-efficient with co-eff 8 (always 1)
/// implied.
/// \param initial_value This is the initial value for the CRC accumulator
/// to be used when beginning a CRC calculation.
/// \param final_xor This value is XOR'ed with the CRC accumulator at the end of
/// a calculation to generate the final value.
/// \return Error code.
/// \retval 0 No error. \p crc8 was initialised correctly.
/// \retval -1 Bad parameter. \p crc8 was NULL.

int	crc8_init(crc8_t *crc8, uint8_t poly, uint8_t initial_value, uint8_t final_xor);



////////////////////////////////////////////////////////////////////////////////
/// Begins an 8 bit CRC calculation by setting the CRC accumulator to it's
/// initial value.
///
/// \brief Begins an 8 bit CRC calculation.
/// \param crc8 Pointer to a CRC state structure whose accumulator is to be set
/// to it's initial value. This must have been initialised previously by
/// crc8_init().
/// \return Error code.
/// \retval 0 No error.
/// \retval -1 Bad parameter. \p crc8 was NULL.


int	crc8_begin(_jstring *inputArray);
//int	crc8_begin(crc8_t *crc8);



////////////////////////////////////////////////////////////////////////////////
/// Performs an 8 bit CRC calculation of a block of bytes. Does not initialise
/// or finalise the CRC accumulator and must therefor be enclosed in a
/// crc8_begin() and crc8_end() pair. More than one call to crc8_calc() may
/// appear between crc8_begin() and crc16_end() with the end result being a
/// correct CRC calculated over the concatenation of all specified blocks.
///
/// \brief Performs an 8 bit CRC calculation on a block of data.
/// \param crc8 Pointer to a CRC state structure whose accumulator is to be set
/// to it's initial value. This must have been initialised previously by
/// crc8_init().
/// \param buf Pointer to a block of bytes for the CRC calculation.
/// \param size Number of bytes in block pointed to by \p buf.
/// \return Error code.
/// \retval 0 No error.
/// \retval -1 Bad parameter. \p crc8 or \p buf was NULL.

int crc8_calc(crc8_t *crc8, const uint8_t *buf, size_t size);



////////////////////////////////////////////////////////////////////////////////
/// Returns a final 8 bit CRC value obtained by XORing the CRC accumulator with
/// the final value. This does not modify the CRC accumulator so subsequent
/// calls to crc8_calc() are allowed to continue the calculation with additional
/// data.
///
/// \brief Returns the result of an 8 bit CRC calculation.
/// \param crc8 Pointer to a CRC state structure whose accumulator is to be set
/// to it's initial value. This must have been initialised previously by
/// crc8_init().
/// \return Final CRC value.

uint8_t  crc8_end(crc8_t *crc8);



////////////////////////////////////////////////////////////////////////////////
/// Initialises a 16 bit CRC state structure by generating an appropriate look-up
/// table for required polynomial and storing, initial and final XOR values.
///
/// \brief Initialises 16 bit crc state.
/// \param crc16 Pointer to a CRC table/state structure to be initialised. This
/// is then used as a "context" for subsequent CRC calculations.
/// \param poly CRC polygon with which to initialise the table of \p crc16. Bit
/// n represents the polynomial's n'th co-efficient with co-eff 16 (always 1)
/// implied.
/// \param initial_value This is the initial value for the CRC accumulator
/// to be used when beginning a CRC calculation.
/// \param final_xor This value is XOR'ed with the CRC accumulator at the end of
/// a calculation to generate the final value.
/// \return Error code.
/// \retval 0 No error. \p crc16 was initialised correctly.
/// \retval -1 Bad parameter. \p crc16 was NULL.

int	crc16_init(crc16_t *crc16, uint16_t poly, uint16_t initial_value, uint16_t final_xor);



////////////////////////////////////////////////////////////////////////////////
/// Begins a 16 bit CRC calculation by setting the CRC accumulator to it's
/// initial value.
///
/// \brief Begins a 16 bit CRC calculation.
/// \param crc16 Pointer to a CRC state structure whose accumulator is to be set
/// to it's initial value. This must have been initialised previously by
/// crc16_init().
/// \return Error code.
/// \retval 0 No error.
/// \retval -1 Bad parameter. \p crc16 was NULL.

int crc16_begin(crc16_t *crc16);



////////////////////////////////////////////////////////////////////////////////
/// Performs a 16 bit CRC calculation of a block of bytes. Does not initialise
/// or finalise the CRC accumulator and must therefor be enclosed in a
/// crc16_begin() and crc16_end() pair. More than one call to crc16_calc() may
/// appear between crc16_begin() and crc16_end() with the end result being a
/// correct CRC calculated over the concatenation of all specified blocks.
///
/// \brief Performs a 16 bit CRC calculation on a block of data.
/// \param crc16 Pointer to a CRC state structure whose accumulator is to be set
/// to it's initial value. This must have been initialised previously by
/// crc16_init().
/// \param buf Pointer to a block of bytes for the CRC calculation.
/// \param size Number of bytes in block pointed to by \p buf.
/// \return Error code.
/// \retval 0 No error.
/// \retval -1 Bad parameter. \p crc16 or \p buf was NULL.

int	crc16_calc(crc16_t *crc16, const uint8_t *buf, size_t size);



////////////////////////////////////////////////////////////////////////////////
/// Returns a final 16 bit CRC value obtained by XORing the CRC accumulator with
/// the final value. This does not modify the CRC accumulator so subsequent
/// calls to crc16_calc() are allowed to continue the calculation with additional
/// data.
///
/// \brief Returns the result of a 16 bit CRC calculation.
/// \param crc16 Pointer to a CRC state structure whose accumulator is to be set
/// to it's initial value. This must have been initialised previously by
/// crc16_init().
/// \return Final CRC value.

uint16_t crc16_end(crc16_t *crc16);



////////////////////////////////////////////////////////////////////////////////
/// Initialises a 32 bit CRC state structure by generating an appropriate
/// look-up table for required polynomial and storing, initial and final XOR
/// values.
///
/// \brief Initialises 32 bit crc state.
/// \param crc32 Pointer to a CRC table/state structure to be initialised. This
/// is then used as a "context" for subsequent CRC calculations.
/// \param poly CRC polygon with which to initialise the table of \p crc32. Bit
/// n represents the polynomial's n'th co-efficient with co-eff 32 (always 1)
/// implied.
/// \param initial_value This is the initial value for the CRC accumulator
/// to be used when beginning a CRC calculation.
/// \param final_xor This value is XOR'ed with the CRC accumulator at the end of
/// a calculation to generate the final value.
/// \return Error code.
/// \retval 0 No error. \p crc32 was initialised correctly.
/// \retval -1 Bad parameter. \p crc32 was NULL.

int	crc32_init(crc32_t *crc32, uint32_t poly, uint32_t initial_value, uint32_t final_xor);



////////////////////////////////////////////////////////////////////////////////
/// Begins a 32 bit CRC calculation by setting the CRC accumulator to it's
/// initial value.
///
/// \brief Begins a 32 bit CRC calculation.
/// \param crc32 Pointer to a CRC state structure whose accumulator is to be set
/// to it's initial value. This must have been initialised previously by
/// crc32_init().
/// \return Error code.
/// \retval 0 No error.
/// \retval -1 Bad parameter. \p crc32 was NULL.

int	crc32_begin(crc32_t *crc32);



////////////////////////////////////////////////////////////////////////////////
/// Performs a 32 bit CRC calculation of a block of bytes. Does not initialise
/// or finalise the CRC accumulator and must therefor be enclosed in a
/// crc32_begin() and crc32_end() pair. More than one call to crc32_calc() may
/// appear between crc32_begin() and crc32_end() with the end result being a
/// correct CRC calculated over the concatenation of all specified blocks.
///
/// \brief Performs a 32 bit CRC calculation on a block of data.
/// \param crc32 Pointer to a CRC state structure whose accumulator is to be set
/// to it's initial value. This must have been initialised previously by
/// crc32_init().
/// \param buf Pointer to a block of bytes for the CRC calculation.
/// \param size Number of bytes in block pointed to by \p buf.
/// \return Error code.
/// \retval 0 No error.
/// \retval -1 Bad parameter. \p crc32 or \p buf was NULL.

int	crc32_calc(crc32_t *crc32, const uint8_t *buf, size_t size);



////////////////////////////////////////////////////////////////////////////////
/// Returns a final 32 bit CRC value obtained by XORing the CRC accumulator with
/// the final value. This does not modify the CRC accumulator so subsequent
/// calls to crc32_calc() are allowed to continue the calculation with additional
/// data.
///
/// \brief Returns the result of a 32 bit CRC calculation.
/// \param crc32 Pointer to a CRC state structure whose accumulator is to be set
/// to it's initial value. This must have been initialised previously by
/// crc32_init().
/// \return Final CRC value.

uint32_t crc32_end(crc32_t *crc32);

#endif //LIBTEST_CRC_H
