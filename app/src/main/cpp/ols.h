//
// Created by goann on 13/06/2024.
//

#ifndef LIBTEST_OLS_H
#define LIBTEST_OLS_H


////////////////////////////////////////////////////////////////////////////////
//
//  ols.h
//
//  Created by Martin Louis on 20/02/13.
//  Copyright (c) 2013 Martin Louis. All rights reserved.
//


////////////////////////////////////////////////////////////////////////////////
// Constants.

#define OLS_MIM_DIM			2	///< Maximum number of dimensions.
#define OLS_MAX_DIM			3	///< Maximum number of dimensions.



////////////////////////////////////////////////////////////////////////////////
/// Container object used to hold a series of data points used for OLS minimisation.

typedef struct
{
    double *points;					///< Pointer to array of data as array of doubles. size_in_bytes = sizeof(double) * dim * capacity.
    int		dim;					///< Dimension of values in data (2 or 3). Each data point is a sequence of dim consecutive doubles.
    size_t count;					///< Number of points used in data (each point consumes dim doubles in data).
    size_t capacity;				///< Number of points that data is capapble of holding (each point consumes dim doubles data).
    unsigned int calculate_bb:1;	///< Flag to indicate that data bounding box is to be calculated (default = 0).
    double bb_max[OLS_MAX_DIM];		///< Holds the maximum point value along each dimension. Valid only if (count > 0) && calculate_bb.
    double bb_min[OLS_MAX_DIM];		///< Holds the minimum point value along each dimension. Valid only if (count > 0) && calculate_bb.
} ols_data_t;



////////////////////////////////////////////////////////////////////////////////
/// Error codes returned by OLS data functions.

typedef enum
{
    olsOk,				///< No error occured.
    olsBadArg,			///< Invalid argument was passed to a function.
    olsBadDim,			///< The dimenation of the ols_data_t object was inconsistent with specified data.
    olsBadIndex,		///< Index is out of bounds. Does not reference a valid point.
    olsMemAllocErr,		///< Memory allocation error. Could not create object or expand capacity.
} ols_err_t;



////////////////////////////////////////////////////////////////////////////////
/// Creates a new OLS data container.
///
/// \brief Creates an OLS data container.
/// \param dim The dimenstion of the points that will be stored in the OLS data
/// container (i.e. number of co-ordinates per point).
/// \param capacity Optional capacity in number of sample points. May be 0. The
/// data container will always grow in cpacity to accomodate any added points if
/// possible.
/// \return Pointer to new ols_data_t object or NULL on failure.

ols_data_t *ols_data_create(int dim, size_t capacity);



////////////////////////////////////////////////////////////////////////////////
/// Deallocates all resources associated with an OLS data container. After this
/// function returns the pointer \p data will be invalid.
///
/// \brief Destroys an OLS data container.
/// \param data Pointer to the OLS data object which is to be cleared.
/// \return Error code
/// \retval olsOk No error. Data added successfully.
/// \retval olsBadArg \p data was NULL.

ols_err_t ols_data_destroy(ols_data_t *data);



////////////////////////////////////////////////////////////////////////////////
/// Removes all samples points stored in OLS data but does NOT de-allocate any
/// of the associated storeage memory.
///
/// \brief Clears OLS data.
/// \param data Pointer to the OLS data container which is to be cleared.
/// \return Error code
/// \retval olsOk No error. Data added successfully.
/// \retval olsBadArg \p data was NULL.

ols_err_t ols_data_clear(ols_data_t *data);



////////////////////////////////////////////////////////////////////////////////
/// Removes all samples points stored in OLS data and de-allocates the
/// associated storeage buffer.
///
/// \brief Resets OLS data.
/// \param data Pointer to the OLS data container which is to be reset.
/// \return Error code
/// \retval olsOk No error. Data added successfully.
/// \retval olsBadArg \p data was NULL.

ols_err_t ols_data_reset(ols_data_t *data);



////////////////////////////////////////////////////////////////////////////////
/// Adds a 2D data sample to an OLS data container. Capacity will grow to
/// accomodate the new point if necessary.
///
/// \brief Add single 2D sample point.
/// \param data Pointer to the OLS data container to which the sample point is to
/// be added.
/// \param p1 First coordinate of the sample point (usually x co-ord).
/// \param p2 Second coordinate of the sample point (usually y co-ord).
/// \return Error code
/// \retval olsOk No error. Data added successfully.
/// \retval olsBadArg \p data was NULL.
/// \retval olsBadDim Dimension of \p data was not 2.
/// \retval olsMemAllocErr Memory allocation failure while trying to expand
/// capcity of \p data for the sample.

ols_err_t ols_data_add_2d(ols_data_t *data, double p1, double p2);



////////////////////////////////////////////////////////////////////////////////
/// Adds a 3D data sample to OLS data container. Capacity will grow to
/// accomodate the new point if necessary.
///
/// \brief Add single 3D sample point.
/// \param data Pointer to the OLS data container to which the sample point is to
/// be added.
/// \param p1 First coordinate of the sample point (usually x co-ord).
/// \param p2 Second coordinate of the sample point (usually y co-ord).
/// \param p3 Third coordinate of the sample point (usually z co-ord).
/// \return Error code
/// \retval olsOk No error. Data added successfully.
/// \retval olsBadArg \p data was NULL.
/// \retval olsBadDim Dimension of \p data was not 3.
/// \retval olsMemAllocErr Memory allocation failure while trying to expand
/// capcity of \p data for the sample.

ols_err_t ols_data_add_3d(ols_data_t *data, double p1, double p2, double p3);



////////////////////////////////////////////////////////////////////////////////
/// Adds one or more data samples to an OLS data container. Capacity will grow to
/// accomodate the new points if necessary.
///
/// \brief Add sample point from pointer to array.
/// \param data Pointer to the OLS data container to which the sample point is to
/// be added.
/// \param num Number of sample points in \p buf.
/// \param dim Dimension of sample points in buff (i.e. the number of
/// co-ordinates per point).
/// \param buf Pointer to buffer containing the data for the points. \p buf is
/// assumed to be holding \p num x \p dim doubles stored as a sequence of \p num
/// sample points with each sample point being a sequence of \p dim doubles.
/// \return Error code
/// \retval olsOk No error. Data added successfully.
/// \retval olsBadArg \p data was NULL.
/// \retval olsBadDim Dimension of \p data was not match the dimension of the
/// supplied data (i.e. \p dim).
/// \retval olsMemAllocErr Memory allocation failure while trying to expand
/// capcity of \p data for the samples.

ols_err_t ols_data_add(ols_data_t *data, size_t num, int dim, const double *buf);



////////////////////////////////////////////////////////////////////////////////
/// Fetches a 2D data sample from an OLS data container as specified by an index.
///
/// \brief Fetch single 2D sample.
/// \param data Pointer to the OLS data container to which the sample point is to
/// be added.
/// \param p1 pointer to a double that receives a copy of the sample point's
/// first co-ordinate (usually x co-ord).
/// \param p2 pointer to a double that receives a copy of the sample point's
/// second co-ordinate (usually y co-ord).
/// \return Error code
/// \retval olsOk No error. Sample point coordinated fetched successfully.
/// \retval olsBadArg \p data was NULL.
/// \retval olsBadDim Dimension of \p data was not 2.
/// \retval olsBadIndex The point at the requested index does not exist.

ols_err_t ols_data_get_2d(ols_data_t *data, int index, double *p1, double *p2);



////////////////////////////////////////////////////////////////////////////////
/// Fetches a 3D data sample from an OLS data container as specified by an index.
///
/// \brief Fetch single 3D sample.
/// \param data Pointer to the OLS data container to which the sample point is to
/// be added.
/// \param p1 pointer to a double that receives a copy of the sample point's
/// first co-ordinate (usually x co-ord).
/// \param p2 pointer to a double that receives a copy of the sample point's
/// second co-ordinate (usually y co-ord).
/// \param p3 pointer to a double that receives a copy of the sample point's
/// third co-ordinate (usually z co-ord).
/// \return Error code
/// \retval olsOk No error. Sample point coordinated fetched successfully.
/// \retval olsBadArg \p data was NULL.
/// \retval olsBadDim Dimension of \p data was not 3.
/// \retval olsBadIndex The point at the requested index does not exist.

ols_err_t ols_data_get_3d(ols_data_t *data, int index, double *p1, double *p2, double *p3);



////////////////////////////////////////////////////////////////////////////////
/// Fetches one or more data samples from an OLS data container.
///
/// \brief Fetch sample point from pointer to array.
/// \param data Pointer to the OLS data container to which the sample point is to
/// be added.
/// \param index Index of first sample to fetch.
/// \param num Number of sample points to fetch \p buf.
/// \param dim Dimension of sample points to fetch (i.e. the number of
/// co-ordinates per point).
/// \param buf Pointer to buffer that will receive the data for the points.
/// \p buf is assumed to have space for \p num x \p dim doubles and data will be
/// copied there as a sequence of \p num sample points with each sample point
/// being a sequence of \p dim doubles representing the coordinates.
/// \return Error code
/// \retval olsOk No error. Data added successfully.
/// \retval olsBadArg \p data or \p buf was NULL.
/// \retval olsBadDim Dimension of \p data does not match the dimension of the
/// supplied data (i.e. \p dim).
/// \retval olsBadIndex One or more points in the range of \p index to (\p index
/// + \p num - 1) do not exist. In this case, nothing is copied to \p buf.

ols_err_t ols_data_copy(ols_data_t *data, int index, size_t num, int dim, double *buf);



////////////////////////////////////////////////////////////////////////////////
/// Returns a pointer to internal storage for sample data identified by an
/// index.
///
/// \brief Gets pointer to sample point data.
/// \note Use with caution. The the pointer returned by this function may be
/// invalidated by any call that adds sample points to \p data.
/// \param data Pointer to the OLS data container.
/// \param index Index of required sample.
/// \p buf is assumed to have space for \p num x \p dim doubles and data will be
/// copied there as a sequence of \p num sample points with each sample point
/// being a sequence of \p dim doubles representing the coordinates.
/// \return Pointer to an array of (data->count - index) * data->dim doubles
/// representing a sequence of (data->count - index) sample points starting from
/// the specified index, each consisting of data->dim co-ordinates.
/// Returns NULL if no such point (i.e if index < 0 or >= data->count).

double *ols_data_get_ptr(ols_data_t *data, int index);



////////////////////////////////////////////////////////////////////////////////
/// Finds a sphere that best fits the specified data. Returns the centre and
/// radius.
///
/// \brief Spherically constrained Ordinary Least Squares solver.
/// \param data Pointer to the OLS data container.
/// \param centre Pointer to array of doubles that will receive a copy of the
/// sphere's centre. Should be big enough to hold at least data->dim doubles.
/// May be NULL in which case the shpere is solved but no centre is returned.
/// \param radius Pointer to a single double that will receive a copy of the
/// sphere's radius. May be NULL in which case the shpere is solved but no
/// radius is returned.
/// \return Error code
/// \retval olsOk No error. Data added successfully.
/// \retval olsBadArg \p data is NULL.
/// \retval olsBadDim Dimension of \p data was not allowed.

ols_err_t ols_sphere_solve(ols_data_t *data, double *centre, double *radius);






#endif //LIBTEST_OLS_H
