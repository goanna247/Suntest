//
// Created by goann on 13/06/2024.
//
////////////////////////////////////////////////////////////////////////////////
//
//  ols_data.c
//
//  Created by Martin Louis on 20/02/13.
//  Copyright (c) 2013 Martin Louis. All rights reserved.
//

#include <stdlib.h>
#include <string.h>
#include "ols.h"


/// When capacity is expanded it is over allocated by this ammount so that there
/// is enough room for a few subsequent sample points. This prevents re-allocating
/// on evey single addition when points are only submitted a few at a time.
#define ALLOC_EXTRA_SAMPLE_COUNT	16



////////////////////////////////////////////////////////////////////////////////
// Private function prototypes.

static ols_err_t ols_calculate_data_bounding_box(ols_data_t *data, int reset, size_t num, int dim, const double *buf);



////////////////////////////////////////////////////////////////////////////////
// Creates a new OLS data container.
//
// \brief Creates an OLS data container.
// \param dim The dimenstion of the points that will be stored in the OLS data
// container (i.e. number of co-ordinates per point).
// \param capacity Optional capacity in number of sample points. May be 0. The
// data container will always grow in cpacity to accomodate any added points if
// possible.
// \return Pointer to new ols_data_t object or NULL on failure.

ols_data_t *ols_data_create(int dim, size_t capacity)
{
    ols_data_t *data;

    if ((dim < 2) || (dim > 3))
        return NULL;

    if ((data = (ols_data_t *)malloc(sizeof(ols_data_t))) == NULL)
        return NULL;

    data->points = NULL;
    data->dim	 = dim;
    data->count	 = 0;
    data->capacity = 0;
    data->calculate_bb = 0;

    if (capacity > 0)
    {
        if ((data->points = (double *)malloc(sizeof(double) * dim * capacity)) != NULL)
            data->capacity = capacity;
    }

    return data;
}



////////////////////////////////////////////////////////////////////////////////
// Deallocates all resources associated with an OLS data container. After this
// function returns the pointer \p data will be invalid.
//
// \brief Destroys an OLS data container.
// \param data Pointer to the OLS data object which is to be cleared.
// \return Error code
// \retval olsOk No error. Data added successfully.
// \retval olsBadArg \p data was NULL.

ols_err_t ols_data_destroy(ols_data_t *data)
{
    if (!data)
        return olsBadArg;

    ols_data_reset(data);
    free(data);

    return olsOk;
}



////////////////////////////////////////////////////////////////////////////////
// Removes all samples points stored in OLS data but does NOT de-allocate any
// of the associated storeage memory.
//
// \brief Clears OLS data.
// \param data Pointer to the OLS data container which is to be cleared.
// \return Error code
// \retval olsOk No error. Data added successfully.
// \retval olsBadArg \p data was NULL.

ols_err_t ols_data_clear(ols_data_t *data)
{
    if (!data)
        return olsBadArg;

    data->count = 0;

    return olsOk;
}



////////////////////////////////////////////////////////////////////////////////
// Removes all samples points stored in OLS data and de-allocates the
// associated storeage buffer.
//
// \brief Resets OLS data.
// \param data Pointer to the OLS data container which is to be reset.
// \return Error code
// \retval olsOk No error. Data added successfully.
// \retval olsBadArg \p data was NULL.

ols_err_t ols_data_reset(ols_data_t *data)
{
    if (!data)
        return olsBadArg;

    if (data->points)
    {
        free(data->points);
        data->points = NULL;
    }
    data->capacity = 0;
    data->count = 0;

    return olsOk;
}



////////////////////////////////////////////////////////////////////////////////
// Adds a 2D data sample to an OLS data container. Capacity will grow to
// accomodate the new point if necessary.
//
// \brief Add single 2D sample point.
// \param data Pointer to the OLS data container to which the sample point is to
// be added.
// \param p1 First coordinate of the sample point (usually x co-ord).
// \param p2 Second coordinate of the sample point (usually y co-ord).
// \return Error code
// \retval olsOk No error. Data added successfully.
// \retval olsBadArg \p data was NULL.
// \retval olsBadDim Dimension of \p data was not 2.
// \retval olsMemAllocErr Memory allocation failure while trying to expand
/// capcity of \p data for the sample.

ols_err_t ols_data_add_2d(ols_data_t *data, double p1, double p2)
{
    double p[2];

    p[0] = p1;
    p[1] = p2;

    return ols_data_add(data, 1, 2, p);
}



////////////////////////////////////////////////////////////////////////////////
// Adds a 3D data sample to OLS data container. Capacity will grow to
// accomodate the new point if necessary.
//
// \brief Add single 3D sample point.
// \param data Pointer to the OLS data container to which the sample point is to
// be added.
// \param p1 First coordinate of the sample point (usually x co-ord).
// \param p2 Second coordinate of the sample point (usually y co-ord).
// \param p3 Third coordinate of the sample point (usually z co-ord).
// \return Error code
// \retval olsOk No error. Data added successfully.
// \retval olsBadArg \p data was NULL.
// \retval olsBadDim Dimension of \p data was not 3.
// \retval olsMemAllocErr Memory allocation failure while trying to expand
// capcity of \p data for the sample.

ols_err_t ols_data_add_3d(ols_data_t *data, double p1, double p2, double p3)
{
    double p[3];

    p[0] = p1;
    p[1] = p2;
    p[2] = p3;

    return ols_data_add(data, 1, 3, p);
}



////////////////////////////////////////////////////////////////////////////////
// Adds one or more data samples to an OLS data container. Capacity will grow to
// accomodate the new points if necessary.
//
// \brief Add sample point from pointer to array.
// \param data Pointer to the OLS data container to which the sample point is to
// be added.
// \param num Number of sample points in \p buf.
// \param dim Dimension of sample points in buff (i.e. the number of
// co-ordinates per point).
// \param buf Pointer to buffer containing the data for the points. \p buf is
// assumed to be holding \p num x \p dim doubles stored as a sequence of \p num
// sample points with each sample point being a sequence of \p dim doubles.
// \return Error code
// \retval olsOk No error. Data added successfully.
// \retval olsBadArg \p data was NULL.
// \retval olsBadDim Dimension of \p data was not match the dimension of the
// supplied data (i.e. \p dim).
// \retval olsMemAllocErr Memory allocation failure while trying to expand
// capcity of \p data for the samples.

ols_err_t ols_data_add(ols_data_t *data, size_t num, int dim, const double *buf)
{
    size_t required_capacity;

    if (!data)
        return olsBadArg;

    if (dim != data->dim)
        return olsBadDim;

    // See if we have enough capacity for the specified number of sample points.
    required_capacity = data->count + num;

    if (data->capacity < required_capacity)
    {
        double *ptr;

        // We can either expand capacity to exactly match our requirements or
        // to allocate a little extra so that there is room for subsequent data.
        // If data is added only a few points at a time then we want to
        // over-allocate which ensures there is room for later points and we
        // don't keep reallocating every time (slows things down). However, if
        // only a few large blocks of data are added, we are better off
        // allocating the exact amount of space. Consequently we allocate the
        // extra space if the amount of added points is small.
        if (num < ALLOC_EXTRA_SAMPLE_COUNT)
            required_capacity += ALLOC_EXTRA_SAMPLE_COUNT;

        if ((ptr = (double *)realloc(data->points, sizeof(double) * dim * required_capacity)) == NULL)
            return olsMemAllocErr;

        data->points   = ptr;
        data->capacity = required_capacity;
    }

    // This should not happen and is just a safety net
    if ((data->count + num) > data->capacity)
        return olsMemAllocErr;

    // If bounding box calculation is enabled, expand it to include the new points.
    if (data->calculate_bb)
        ols_calculate_data_bounding_box(data, (data->count == 0) ? 1 : 0, num, dim, buf);

    // Copy the points into the data container.
    if (data->points && buf)
        memcpy(&data->points[dim * data->count], buf, sizeof(double) * dim * num);
    data->count += num;

    return olsOk;
}



////////////////////////////////////////////////////////////////////////////////
// Fetches a 2D data sample from an OLS data container as specified by an index.
//
// \brief Fetch single 2D sample.
// \param data Pointer to the OLS data container to which the sample point is to
// be added.
// \param p1 pointer to a double that receives a copy of the sample point's
// first co-ordinate (usually x co-ord).
// \param p2 pointer to a double that receives a copy of the sample point's
// second co-ordinate (usually y co-ord).
// \return Error code
// \retval olsOk No error. Sample point coordinated fetched successfully.
// \retval olsBadArg \p data was NULL.
// \retval olsBadDim Dimension of \p data was not 2.
// \retval olsBadIndex The point at the requested index does not exist.

ols_err_t ols_data_get_2d(ols_data_t *data, int index, double *p1, double *p2)
{
    double *ptr;

    if (!data)
        return olsBadArg;
    if (data->dim != 2)
        return olsBadDim;

    if ((ptr = ols_data_get_ptr(data, index)) == NULL)
        return olsBadIndex;

    if (p1) *p1 = ptr[0];
    if (p2) *p2 = ptr[1];

    return olsOk;
}



////////////////////////////////////////////////////////////////////////////////
// Fetches a 3D data sample from an OLS data container as specified by an index.
//
// \brief Fetch single 3D sample.
// \param data Pointer to the OLS data container to which the sample point is to
// be added.
// \param p1 pointer to a double that receives a copy of the sample point's
// first co-ordinate (usually x co-ord).
// \param p2 pointer to a double that receives a copy of the sample point's
// second co-ordinate (usually y co-ord).
// \param p3 pointer to a double that receives a copy of the sample point's
// third co-ordinate (usually z co-ord).
// \return Error code
// \retval olsOk No error. Sample point coordinated fetched successfully.
// \retval olsBadArg \p data was NULL.
// \retval olsBadDim Dimension of \p data was not 3.
// \retval olsBadIndex The point at the requested index does not exist.

ols_err_t ols_data_get_3d(ols_data_t *data, int index, double *p1, double *p2, double *p3)
{
    double *ptr;

    if (!data)
        return olsBadArg;
    if (data->dim != 3)
        return olsBadDim;

    if ((ptr = ols_data_get_ptr(data, index)) == NULL)
        return olsBadIndex;

    if (p1) *p1 = ptr[0];
    if (p2) *p2 = ptr[1];
    if (p3) *p3 = ptr[2];

    return olsOk;
}



////////////////////////////////////////////////////////////////////////////////
// Fetches one or more data samples from an OLS data container.
//
// \brief Fetch sample point from pointer to array.
// \param data Pointer to the OLS data container to which the sample point is to
// be added.
// \param index Index of first sample to fetch.
// \param num Number of sample points to fetch \p buf.
// \param dim Dimension of sample points to fetch (i.e. the number of
// co-ordinates per point).
// \param buf Pointer to buffer that will receive the data for the points.
// \p buf is assumed to have space for \p num x \p dim doubles and data will be
// copied there as a sequence of \p num sample points with each sample point
// being a sequence of \p dim doubles representing the coordinates.
// \return Error code
// \retval olsOk No error. Data added successfully.
// \retval olsBadArg \p data was NULL.
// \retval olsBadDim Dimension of \p data was not match the dimension of the
// supplied data (i.e. \p dim).
// \retval olsBadIndex One or more points in the range of \p index to (\p index
// + \p num - 1) do not exist. In this case, nothing is copied to \p buf.

ols_err_t ols_data_copy(ols_data_t *data, int index, size_t num, int dim, double *buf)
{
    if (!data || !buf)
        return olsBadArg;
    if (dim != data->dim)
        return olsBadDim;
    if (num <= 0)
        return olsOk;

    if ((index < 0) || ((index + num) > data->count))
        return olsBadIndex;

    memcpy(buf, &data->points[index * dim], sizeof(double) * dim * num);

    return olsOk;
}



////////////////////////////////////////////////////////////////////////////////
// Returns a pointer to internal storage for sample data identified by an
// index.
//
// \brief Gets pointer to sample point data.
// \note Use with caution. The the pointer returned by this function may be
// invalidated by any call that adds sample points to \p data.
// \param data Pointer to the OLS data container.
// \param index Index of required sample.
// \p buf is assumed to have space for \p num x \p dim doubles and data will be
// copied there as a sequence of \p num sample points with each sample point
// being a sequence of \p dim doubles representing the coordinates.
// \return Pointer to an array of (data->count - index) * data->dim doubles
// representing a sequence of (data->count - index) sample points starting from
// the specified index, each consisting of data->dim co-ordinates.
// Returns NULL if no such point (i.e if index < 0 or >= data->count).

double *ols_data_get_ptr(ols_data_t *data, int index)
{
    if (!data || !data->points || (index < 0) || (index >= data->count))
        return NULL;

    return &data->points[index * data->dim];
}



////////////////////////////////////////////////////////////////////////////////
// Finds a sphere that best fits the specified data. Returns the centre and
// radius.
//
// \brief Spherically constrained Ordinary Least Squares solver.
// \param data Pointer to the OLS data container.
// \param centre Pointer to array of doubles that will receive a copy of the
// sphere's centre. Should be big enough to hold at least data->dim doubles.
// May be NULL in which case the shpere is solved but no centre is returned.
// \param radius Pointer to a single double that will receive a copy of the
// sphere's radius. May be NULL in which case the shpere is solved but no
// radius is returned.
// \return Error code
// \retval olsOk No error. Data added successfully.
// \retval olsBadArg \p data is NULL.
// \retval olsBadDim Dimension of \p data was not allowed.

ols_err_t ols_sphere_solve(ols_data_t *data, double *centre, double *radius)
{
    if (!data)
        return olsBadArg;

    if ((data->dim < OLS_MIM_DIM) || (data->dim > OLS_MAX_DIM))
        return olsBadDim;

    // FIXME: THIS IS A NAIVE 3 PARAMETER CALIBRATION ALGORITHM THAT REMOVES BIAS
    //        AND IS NOT ACCURATE. IT IS USED ONLY FOR TESTING UNTIL THE CORRECT
    //        ALGORITHM CAN BE PUT INTO PLACE.

    // Calculate mean for each co-ordinate and use this as
    if (data->count > 0)
    {
        double sum_x = 0.0, sum_y = 0.0, sum_z = 0.0;
        int i, idx;

        for (i = idx= 0; i < data->count; i++, idx += data->dim)
        {
            sum_x += data->points[idx];
            sum_y += data->points[idx + 1];
            if (data->dim >= 3)
                sum_z += data->points[idx + 2];
        }

        if (centre)
        {
            centre[0] = sum_x / (double)data->count;
            centre[1] = sum_y / (double)data->count;
            centre[2] = sum_z / (double)data->count;
        }
    }

    return olsOk;
}



////////////////////////////////////////////////////////////////////////////////
/// Calculates the bounding box for a data container by expanding its exsiting
/// bounding box to include the points supplied. If \p reset is set, then
/// the data container's bounding box will be reset and only the supplied points
/// (use this when adding the first point(s) to a container).
///
/// \brief Calculate bounding box for sample points.
/// \param data Pointer to the OLS data container for which the bounding box is
/// to be calculated. Results will be in data->bb_min[] and data->bb_max[].
/// \param num Number of sample points in \p buf.
/// \param dim Dimension of sample points in buff (i.e. the number of
/// co-ordinates per point).
/// \param buf Pointer to buffer containing the data for the points used for the
/// calculation. It may be a pointer to within data->points itself (eg to expand
/// the BB to include some newly added points) or may point to a buffer of points
/// about to be added to data->points. \p buf is assumed to be holding \p num x
/// \p dim doubles stored as a sequence of \p num sample points with each sample
/// point being a sequence of \p dim doubles.
/// \return Error code
/// \retval olsOk No error. Sample point coordinated fetched successfully.
/// \retval olsBadArg \p data or \p buf was NULL.
/// \retval olsBadDim Dimension of \p data was not within tha allowed range.

static ols_err_t ols_calculate_data_bounding_box(ols_data_t *data, int reset, size_t num, int dim, const double *buf)
{
    int i, d;
    const double *ptr;

    if (!data || !buf)
        return olsBadArg;

    if ((data->dim < OLS_MIM_DIM) || (data->dim > OLS_MAX_DIM))
        return olsBadDim;

    ptr = buf;
    for (i = 0; i < num; i++, ptr += dim)
    {
        if (reset)
        {
            for (d = 0; d < dim; d++)
                data->bb_min[d] = data->bb_max[d] = ptr[d];
            for (; d < OLS_MAX_DIM; d++)
                data->bb_min[d] = data->bb_max[d] = 0.0;
            reset = 0;
            continue;
        }

        for (d = 0; d < dim; d++)
        {
            if (ptr[d] > data->bb_max[d]) data->bb_max[d] = ptr[d];
            if (ptr[d] < data->bb_min[d]) data->bb_min[d] = ptr[d];
        }
    }

    return olsOk;
}


