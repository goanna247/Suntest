////////////////////////////////////////////////////////////////////////////////
/// \file   crc.c
/// \brief  8, 16 and 32 bit CRC calculation.
/// \author Martin Louis
/// \date   Created: 2009-11-17
///
/// Provides table driven CRC calculation routines for 8, 16 and 32 bit CRC's
/// with arbitary polygons.

#include "crc.h"

/// \defgroup crc_private_group CRC private.
/// \ingroup crc_group
/// \{




///////////////////////////////////////////////////////////////////////////////
// Configuration

#undef CRC_DEBUG_HEXDUMP			///< Enables hexdump of CRC blocks

#ifdef CRC_DEBUG_HEXDUMP
#include <stdio.h>
#endif



///////////////////////////////////////////////////////////////////////////////
// Private functions

static void crc8_bitwise(crc8_t *crc8, const uint8_t *buf, size_t size);
static void crc16_bitwise(crc16_t *crc16, const uint8_t *buf, size_t size);
static void crc32_bitwise(crc32_t *crc32, const uint8_t *buf, size_t size);



////////////////////////////////////////////////////////////////////////////////
// Initialises an 8 bit CRC state structure by generating an appropriate
// look-up table for required polynomial and storing, initial and final XOR
// values.

int crc8_init(crc8_t *crc8, uint8_t poly, uint8_t initial_value, uint8_t final_xor)
{
    uint8_t buf;
    int i;

    // Sanity check
    if (!crc8)
        return -1;

    // Initialise
    crc8->poly = poly;
    crc8->initial_value = initial_value;
    crc8->final_xor = final_xor;

    // Generate table
    for (i = 0; i < 256; i++)
    {
        buf = (uint8_t)i;
        crc8->crc = 0;
        crc8_bitwise(crc8, &buf, 1);
        crc8->table[i] = crc8->crc;
    }

    return 0;
}



////////////////////////////////////////////////////////////////////////////////
// Begins an 8 bit CRC calculation by setting the CRC accumulator to it's
// initial value.

//int crc8_begin(crc8_t *crc8) {
//    if (!crc8)
//        return -1;
//
//    crc8->crc = crc8->initial_value;
//
//    return 0;
//}

int crc8_begin(crc8_t *crc8) {
    if (!crc8)
        return -1;

    crc8->crc = crc8->initial_value;

    return 0;
}



////////////////////////////////////////////////////////////////////////////////
// Performs an 8 bit CRC calculation of a block of bytes. Does not initialise
// or finalise the CRC accumulator and must therefor be enclosed in a
// crc8_begin() and crc8_end() pair. More than one call to crc8_calc() may
// appear between crc8_begin() and crc16_end() with the end result being a
// correct CRC calculated over the concatenation of all specified blocks.

int crc8_calc(crc8_t *crc8, const uint8_t *buf, size_t size)
{
#ifdef CRC_DEBUG_HEXDUMP
    int count = 0;
#endif

    if (!crc8 || !buf)
        return -1;

    while (size--)
    {
#ifdef CRC_DEBUG_HEXDUMP
        if ((count & 0x0F) == 0)
			printf("%s%04X:  ", count ? "\n" : "", count);
		printf(" %02X", *buf);
		count++;
#endif
        crc8->crc = crc8->table[*buf++ ^ crc8->crc];
    }
#ifdef CRC_DEBUG_HEXDUMP
    if (count) printf("%sCRC=%02X\n", count ? "\n" : "", crc8->crc ^ crc8->final_xor);
#endif

    return 0;
}



////////////////////////////////////////////////////////////////////////////////
// Returns a final 8 bit CRC value obtained by XORing the CRC accumulator with
// the final value. This does not modify the CRC accumulator so subsequent
// calls to crc8_calc() are allowed to continue the calculation with additional
// data.

uint8_t crc8_end(crc8_t *crc8)
{
    return crc8->crc ^ crc8->final_xor;
}



////////////////////////////////////////////////////////////////////////////////
/// Performs an 8 bit CRC calculation of a block of bytes much like crc8_calc()
/// except that it uses a bitwise (slow) algorithm rather than a look-up table.
/// This function is used to generate the lookup table itself.
///
/// \brief Performs an 8 bit CRC calculation on a block of data without using
/// a look-up table.
/// \param crc8 Pointer to a CRC state structure whose accumulator is to be set
/// to it's initial value. This must have been initialised previously by
/// crc8_init().
/// \param buf Pointer to a block of bytes for the CRC calculation.
/// \param size Number of bytes in block pointed to by \p buf.

static void crc8_bitwise(crc8_t *crc8, const uint8_t *buf, size_t size)
{
    int bit;

    while (size--)
    {
        crc8->crc ^= *buf;
        for (bit = 0; bit < 8; bit++)
        {
            if (crc8->crc & 0x80)
                crc8->crc = (crc8->crc << 1) ^ crc8->poly;
            else
                crc8->crc = crc8->crc << 1;
        }
        buf++;
    }
}



////////////////////////////////////////////////////////////////////////////////
// Initialises a 16 bit CRC state structure by generating an appropriate
// look-up table for required polynomial and storing, initial and final XOR
// values.

int crc16_init(crc16_t *crc16, uint16_t poly, uint16_t initial_value, uint16_t final_xor)
{
    uint8_t buf;
    int i;

    // Sanity check
    if (!crc16)
        return -1;

    // Initialise
    crc16->poly = poly;
    crc16->initial_value = initial_value;
    crc16->final_xor = final_xor;

    // Generate table
    for (i = 0; i < 256; i++)
    {
        buf = (uint8_t)i;
        crc16->crc = 0;
        crc16_bitwise(crc16, &buf, 1);
        crc16->table[i] = crc16->crc;
    }

    return 0;
}



////////////////////////////////////////////////////////////////////////////////
// Begins a 16 bit CRC calculation by setting the CRC accumulator to it's
// initial value.

int crc16_begin(crc16_t *crc16)
{
    if (!crc16)
        return -1;

    crc16->crc = crc16->initial_value;

    return 0;
}



////////////////////////////////////////////////////////////////////////////////
// Performs a 16 bit CRC calculation of a block of bytes. Does not initialise
// or finalise the CRC accumulator and must therefor be enclosed in a
// crc16_begin() and crc16_end() pair. More than one call to crc16_calc() may
// appear between crc16_begin() and crc16_end() with the end result being a
// correct CRC calculated over the concatenation of all specified blocks.

int crc16_calc(crc16_t *crc16, const uint8_t *buf, size_t size)
{
#ifdef CRC_DEBUG_HEXDUMP
    int count = 0;
#endif

    if (!crc16 || !buf)
        return -1;

    while (size--)
    {
#ifdef CRC_DEBUG_HEXDUMP
        if ((count & 0x0F) == 0)
			printf("%s%04X:  ", count ? "\n" : "", count);
		printf(" %02X", *buf);
		count++;
#endif
        crc16->crc = (crc16->crc << 8) ^ crc16->table[*buf++ ^ (crc16->crc >> 8)];
    }
#ifdef CRC_DEBUG_HEXDUMP
    if (count) printf("%sCRC=%04X\n", count ? "\n" : "", crc16->crc ^ crc16->final_xor);
#endif

    return 0;
}



////////////////////////////////////////////////////////////////////////////////
// Returns a final 16 bit CRC value obtained by XORing the CRC accumulator with
// the final value. This does not modify the CRC accumulator so subsequent
// calls to crc16_calc() are allowed to continue the calculation with additional
// data.

uint16_t crc16_end(crc16_t *crc16)
{
    return crc16->crc ^ crc16->final_xor;
}



////////////////////////////////////////////////////////////////////////////////
/// Performs a 16 bit CRC calculation of a block of bytes much like crc16_calc()
/// except that it uses a bitwise (slow) algorithm rather than a look-up table.
/// This function is used to generate the lookup table itself.
///
/// \brief Performs a 16 bit CRC calculation on a block of data without using
/// a look-up table.
/// \param crc16 Pointer to a CRC state structure whose accumulator is to be set
/// to it's initial value. This must have been initialised previously by
/// crc16_init().
/// \param buf Pointer to a block of bytes for the CRC calculation.
/// \param size Number of bytes in block pointed to by \p buf.

static void crc16_bitwise(crc16_t *crc16, const uint8_t *buf, size_t size)
{
    int bit;

    while (size--)
    {
        crc16->crc ^= (uint16_t)*buf << 8;
        for (bit = 0; bit < 8; bit++)
        {
            if (crc16->crc & 0x8000)
                crc16->crc = (crc16->crc << 1) ^ crc16->poly;
            else
                crc16->crc = crc16->crc << 1;
        }
        buf++;
    }
}



////////////////////////////////////////////////////////////////////////////////
// Initialises a 32 bit CRC state structure by generating an appropriate
// look-up table for required polynomial and storing, initial and final XOR
// values.

int crc32_init(crc32_t *crc32, uint32_t poly, uint32_t initial_value, uint32_t final_xor)
{
    uint8_t buf;
    int i;

    // Sanity check
    if (!crc32)
        return -1;

    // Initialise
    crc32->poly = poly;
    crc32->initial_value = initial_value;
    crc32->final_xor = final_xor;

    // Generate table
    for (i = 0; i < 256; i++)
    {
        buf = (uint8_t)i;
        crc32->crc = 0;
        crc32_bitwise(crc32, &buf, 1);
        crc32->table[i] = crc32->crc;
    }

    return 0;
}



////////////////////////////////////////////////////////////////////////////////
// Begins a 32 bit CRC calculation by setting the CRC accumulator to it's
// initial value.

int crc32_begin(crc32_t *crc32)
{
    if (!crc32)
        return -1;

    crc32->crc = crc32->initial_value;

    return 0;
}



////////////////////////////////////////////////////////////////////////////////
// Performs a 32 bit CRC calculation of a block of bytes. Does not initialise
// or finalise the CRC accumulator and must therefor be enclosed in a
// crc32_begin() and crc32_end() pair. More than one call to crc32_calc() may
// appear between crc32_begin() and crc32_end() with the end result being a
// correct CRC calculated over the concatenation of all specified blocks.

int crc32_calc(crc32_t *crc32, const uint8_t *buf, size_t size)
{
#ifdef CRC_DEBUG_HEXDUMP
    int count = 0;
#endif

    if (!crc32 || !buf)
        return -1;

    while (size--)
    {
#ifdef CRC_DEBUG_HEXDUMP
        if ((count & 0x0F) == 0)
			printf("%s%04X:  ", count ? "\n" : "", count);
		printf(" %02X", *buf);
		count++;
#endif
        crc32->crc = (crc32->crc << 8) ^ crc32->table[*buf++ ^ (crc32->crc >> 24)];
    }
#ifdef CRC_DEBUG_HEXDUMP
    if (count) printf("%sCRC=%08X\n", count ? "\n" : "", crc32->crc ^ crc32->final_xor);
#endif

    return 0;
}



////////////////////////////////////////////////////////////////////////////////
// Returns a final 32 bit CRC value obtained by XORing the CRC accumulator with
// the final value. This does not modify the CRC accumulator so subsequent
// calls to crc32_calc() are allowed to continue the calculation with additional
// data.

uint32_t crc32_end(crc32_t *crc32)
{
    return crc32->crc ^ crc32->final_xor;
}



////////////////////////////////////////////////////////////////////////////////
/// Performs a 32 bit CRC calculation of a block of bytes much like crc32_calc()
/// except that it uses a bitwise (slow) algorithm rather than a look-up table.
/// This function is used to generate the lookup table itself.
///
/// \brief Performs a 32 bit CRC calculation on a block of data without using
/// a look-up table.
/// \param crc32 Pointer to a CRC state structure whose accumulator is to be set
/// to it's initial value. This must have been initialised previously by
/// crc32_init().
/// \param buf Pointer to a block of bytes for the CRC calculation.
/// \param size Number of bytes in block pointed to by \p buf.

static void crc32_bitwise(crc32_t *crc32, const uint8_t *buf, size_t size)
{
    int bit;

    while (size--)
    {
        crc32->crc ^= (uint32_t)*buf << 24;
        for (bit = 0; bit < 8; bit++)
        {
            if (crc32->crc & 0x80000000)
                crc32->crc = (crc32->crc << 1) ^ crc32->poly;
            else
                crc32->crc = crc32->crc << 1;
        }
        buf++;
    }
}



/// \} End of crc_private_group
