//
// Created by goann on 13/06/2024.
//

////////////////////////////////////////////////////////////////////////////////
//
//  ecompass_calibrator.c
//
//  Created by Martin Louis on 8/11/12.
//  Copyright (c) 2012 Martin Louis. All rights reserved.
//

#include <stdlib.h>
#include <math.h>
#include <stdio.h>
#include "ecompass_calibrator.h"
#include "matrix.h"


////////////////////////////////////////////////////////////////////////////////
// Configuration

#undef DEBUG_SHOW_STABILISER
#define ORIENTATION_TURNS_POSITIVE_ONLY		// Negative turns in orientation string are normalised to positive.



////////////////////////////////////////////////////////////////////////////////
// Constants

#define	LEAST_SQUARES_PRE_SUBTRACTED_OFFSET	0
#define DATA_CHUNK_SIZE						256			///< Number of data samples to allocat at a time
#define ACCELEROMETER_REDUNDANCY_THRESHOLD	0.5			///< Samples within this distance of each other are considered redundant
#define REDUNDANCY_RADIUS_FACTOR			0.5			///< The multiple of seperation distance of readings used for redundancy radius
#define SQRT3								1.732050807568877293527446341505872367


////////////////////////////////////////////////////////////////////////////////
// Macros

#define ACCELEROMETER_REDUNDANCY_THRESHOLD_SQUARED	(ACCELEROMETER_REDUNDANCY_THRESHOLD * ACCELEROMETER_REDUNDANCY_THRESHOLD)
#define WOULD_USE_STABILISER(c)	(((c)->sensor == ecompassSensorAccelerometer) || ((c)->sensor == ecompassSensorMagnetometer))
#define USES_STABILISER(c)		((((c)->config.sensor == ecompassSensorAccelerometer) || ((c)->config.sensor == ecompassSensorMagnetometer)) && \
								 !(c)->config.stabiliser.disable)


////////////////////////////////////////////////////////////////////////////////
// Private types

typedef enum
{
    stabiliserBecomeStable,			///< Stabiliser has detected a transition from unstable to stable.
    stabiliserStillStable,			///< Reading is stable and was previously stable.
    stabiliserBecomeUnstable,		///< Stabiliser has detected a transition from stable to unstable.
    stabiliserStillUnstable			///< Reading is unstable and was previously unstable.
} stabiliser_result_t;

/// Least squares solution type
typedef enum
{
    leastSquaresSimple,				///< Zero and first order term.
    leastSquaresCubicDiagonal,		///< Zero, first and third (diagonal) order term.
    leastSquaresQuadCubicDiagonal,	///< Zero, first second (diagonal) and third (diagonal) order term.
    leastSquaresCubic,				///< Zero, first and third order term.
    leastSquaresQuadCubic,			///< Zero, first second and third order term.
    leastSquaresUpToFifthDiagonal,	///< Test configuration. Terms vary depending on test.
    leastSquaresQuadCubicPlusFourthFifthDiagonal ///< Test configuration. Terms vary depending on test.
} least_squares_type_t;


////////////////////////////////////////////////////////////////////////////////
// Private function prototypes

static stabiliser_result_t			ecompass_calibrator_test_stability(ecompass_calibrator_t *calibrator, ecompass_calibrator_data_t *data);
static int							ecompass_calibrator_data_is_redundant(ecompass_calibrator_t *calibrator, ecompass_calibrator_data_t *data);
static ecompass_calibrator_submit_t ecompass_calibrator_calculate(ecompass_calibrator_t *calibrator, int has_ended);
static ecompass_calibrator_submit_t	least_squares_arbitrary(ecompass_calibrator_t *calibrator);
static ecompass_calibrator_submit_t least_squares_init(ecompass_calibrator_t *calibrator, int *count_available, int *count_required, ecompass_calibration_points_t *sample_points);
static matrix_t					   *least_squares_create_measurement_matrix(ecompass_calibrator_t *calibrator, int count, least_squares_type_t type, int axis);
static ecompass_calibrator_submit_t least_squares(ecompass_calibrator_t *calibrator, least_squares_type_t type, int pre_subtracted_offset);
static void							calculate_magnitude_error(ecompass_calibrator_t *calibrator);
static double						reading_seperation_distance(ecompass_calibration_points_t readings);



////////////////////////////////////////////////////////////////////////////////
// Creates an ecompass calibrator and initialises it.
//
// \brief Create an ecompass calibrator.
// \return Pointer to new ecompas calibrator.

ecompass_calibrator_t *ecompass_calibrator_create(void)
{
    ecompass_calibrator_t *calibrator;

    // Allocate structure
    if ((calibrator = (ecompass_calibrator_t *)malloc(sizeof(ecompass_calibrator_t))) != NULL)
    {
        // Initialise context
        calibrator->data					= NULL;
        calibrator->capacity				= 0;
        calibrator->count					= 0;
        calibrator->error					= ecompassCalibratorOk;
        calibrator->calibration_in_progress = 0;
        calibrator->calibration_complete	= 0;
        calibrator->needs_recalculation		= 0;
        calibrator->error_magnitude_rms		= ECOMPASS_INVALID_ERROR;
        calibrator->error_magnitude_max		= ECOMPASS_INVALID_ERROR;


        // Initialise stabiliser
        calibrator->stabiliser.data		= NULL;
        calibrator->stabiliser.capacity = 0;
        calibrator->stabiliser.count	= 0;
        calibrator->stabiliser.put		= 0;
        calibrator->stabiliser.get		= 0;
        calibrator->redundancy_limit_squared = -1.0;

        // Set congiguration options to defaults.
        ecompass_calibrator_configure_defaults(&calibrator->config);

        // Initialise calibration parameters.
        ecompass_calibration_init(&calibrator->calibration, ecompassSensorUnknown);
    }

    return calibrator;
}



////////////////////////////////////////////////////////////////////////////////
// Destroys an ecompass calibrator and releases all related resources. On
// return the tructure pointed to by \p calibrator will no longer be valid.
//
// \brief Destroy an ecompass calibrator.
// \param calibrator Pointer to the calibrator to destroy.

void ecompass_calibrator_destroy(ecompass_calibrator_t *calibrator)
{
    // Sanity check
    if (!calibrator)
        return;

    // Abort any calibration currently in progress and release sample data held
    // by calibrator.
    ecompass_calibrator_abort(calibrator);
    ecompass_calibrator_clear(calibrator);

    // Release context
    free(calibrator);
}



////////////////////////////////////////////////////////////////////////////////
// Sets a structure of calibrator configuration options to their default values.
// Such a structure still neeeds to be applied to calibrator using
// ecompass_calibrator_configure() to take effect.
//
// \brief Set calibrator configuration options to default values.
// \param config A structure to be initialised with default values.
// \return Error code.
// \retval ecompassCalibratorOk Configuration parameters were successfully applied.
// \retval ecompassCalibratorErrBadArg \p config was NULL.

ecompass_calibrator_err_t ecompass_calibrator_configure_defaults(ecompass_calibrator_config_t *config)
{
    // Sanity check
    if (!config)
        return ecompassCalibratorErrBadArg;

    config->sensor						= ecompassSensorUnknown;
    config->accelerometer_sample_points	= ecompassPointsSixAxisAligned;
    config->magnetometer_sample_points	= ecompassPointsSixAxisAligned;
    config->sample_limit				= 0;
    config->stabiliser.disable			= 0;
    config->stabiliser.sample_count		= 10;
    config->stabiliser.maximum_spread	= 0.01;
    config->stabiliser.hysteresis		= 1.0;
    config->magnetic_field.magnitude	=  58.2778;
    config->magnetic_field.inclination	= -66.27;
//	config->temperature_compensated     = 0;

    return ecompassCalibratorOk;
}



////////////////////////////////////////////////////////////////////////////////
// Sets configuration options for a calibrator from a structure passed by the.
// application. The calibrator copies this data into itself so the application
// may destory or reuse the structure after this function returns. If NULL
// is specified for \p config, the calibrator's options will be set to default.
// Note that the default sensor config is unknown so this still needs to be
// explicitly set for the calibrator to be functional. Configuring a calibrator
// will fail if there is any calibration in progress at the time. It must be
// aborted (or ended) first.
//
// The application may set a blank ecompass_calibrator_config_t structure to
// defaults using ecompass_calibrator_configure_defaults().
//
// \brief Configure a calibrator.
// \param calibrator Calibrator that will get its mode changed.
// \param config A structure containing the configuration options to be applied
// A value of NULL will set the target \p calibrator optione do default.
// \return Error code.
// \retval ecompassCalibratorOk Configuration parameters were successfully set.
// \retval ecompassCalibratorErrBadArg \p calibrator was NULL.
// \retval ecompassCalibratorErrInProgress Configuration cannot be applied
// because a calibration is currently in progress.

ecompass_calibrator_err_t ecompass_calibrator_configure(ecompass_calibrator_t *calibrator, ecompass_calibrator_config_t *config)
{
    // Sanity check
    if (!calibrator)
        return ecompassCalibratorErrBadArg;

    if (!calibrator->calibration_in_progress)
    {
        // Copy the config or set to defaults if NULL specified.
        calibrator->error = ecompassCalibratorOk;
        if (config)
            calibrator->config = *config;
        else
            ecompass_calibrator_configure_defaults(&calibrator->config);
    }
    else
        calibrator->error = ecompassCalibratorErrInProgress;

    return calibrator->error;
}



////////////////////////////////////////////////////////////////////////////////
/// Returns the reading type that the calibrator is configured for. This
/// determines the calibration procedure and depends in the sensor currently
/// being calibrated.
///
/// \brief Readings type (ecompass_calibration_readings_t) that the calibrator
/// is configured to use.
/// \param calibrator The calibrator.
/// \return The reading type that the calibrator is configured to use.

ecompass_calibration_points_t ecompass_calibrator_reading_type(ecompass_calibrator_t *calibrator)
{
    if (!calibrator)
        return ecompassPointsArbitrary;

    if (calibrator->config.sensor == ecompassSensorAccelerometer)
        return calibrator->config.accelerometer_sample_points;

    if (calibrator->config.sensor == ecompassSensorMagnetometer)
        return calibrator->config.magnetometer_sample_points;

    return ecompassPointsArbitrary;
}



////////////////////////////////////////////////////////////////////////////////
// Starts a new calibration operation. Will abort any calibration currently in
// progress and clear its buffered samples.
//
// \brief Begins new calibration.
// \param calibrator Pointer to the calibrator.
// \return Error code.
// \retval ecompassCalibratorOk Calibration was successfully started.
// \retval ecompassCalibratorErrBadArg \p calibrator was NULL.
// \retval ecompassCalibratorErrMemory Internal memory allocation failure.

ecompass_calibrator_err_t ecompass_calibrator_begin(ecompass_calibrator_t *calibrator)
{
    // Sanity check
    if (!calibrator)
        return ecompassCalibratorErrBadArg;

    // Abort any calibration in progress and clear any held data
    ecompass_calibrator_abort(calibrator);
    ecompass_calibrator_clear(calibrator);
    ecompass_calibration_init(&calibrator->calibration, ecompassSensorUnknown);

    // Initialise calibration data.
    calibrator->count					= 0;
    calibrator->error					= ecompassCalibratorOk;
    calibrator->needs_recalculation		= 0;
    calibrator->calibration_complete	= 0;
    calibrator->calibration_in_progress = 1;
    calibrator->stabiliser.is_stable	= 0;
    calibrator->error_magnitude_rms		= ECOMPASS_INVALID_ERROR;
    calibrator->error_magnitude_max		= ECOMPASS_INVALID_ERROR;

    // Prepare the redundancy limit. Only applies when calibration to reading aligned
    // along known optimal orientations (ie not arbitrary readings). A negative
    // redundancy limit disabled redundancy checking.
    calibrator->redundancy_limit_squared = -1.0;
    switch (calibrator->config.sensor)
    {
        case ecompassSensorUnknown:
        case ecompassSensorTemperature:
            break;

        case ecompassSensorAccelerometer:
            if (calibrator->config.accelerometer_sample_points != ecompassPointsArbitrary)
            {
                if ((calibrator->redundancy_limit_squared = reading_seperation_distance(calibrator->config.accelerometer_sample_points)) < 0.0)
                    break;
                calibrator->redundancy_limit_squared *= REDUNDANCY_RADIUS_FACTOR;
                calibrator->redundancy_limit_squared  = calibrator->redundancy_limit_squared * calibrator->redundancy_limit_squared;
            }
            break;

        case ecompassSensorMagnetometer:
            if (calibrator->config.magnetometer_sample_points != ecompassPointsArbitrary)
            {
                if ((calibrator->redundancy_limit_squared = reading_seperation_distance(calibrator->config.magnetometer_sample_points)) < 0.0)
                    break;
                calibrator->redundancy_limit_squared *= REDUNDANCY_RADIUS_FACTOR;
                calibrator->redundancy_limit_squared  = calibrator->redundancy_limit_squared * calibrator->redundancy_limit_squared;
            }
            break;
    }

    // Prepare stabiliser if it is needed.
    if (USES_STABILISER(calibrator))
    {
        int count = calibrator->config.stabiliser.sample_count;
        if (count < 2)
            count = 2;

        if ((calibrator->stabiliser.data = (ecompass_calibrator_data_t *)malloc(sizeof(ecompass_calibrator_data_t) * count)) != NULL)
        {
            calibrator->stabiliser.capacity  = count;
            calibrator->stabiliser.count	 = 0;
            calibrator->stabiliser.put		 = 0;
            calibrator->stabiliser.get		 = 0;
            calibrator->stabiliser.is_stable = 0;
            calibrator->stabiliser.maximum_spread = calibrator->config.stabiliser.maximum_spread;
            calibrator->stabiliser.hysteresis     = calibrator->config.stabiliser.hysteresis;

            // Start with is_stable set this way, the first submitted point won't be accepted
            // until after the device is moved (needs to transition from unstable to stable to be accepted).
            calibrator->stabiliser.is_stable = 1;
        }
        else
            calibrator->error = ecompassCalibratorErrMemory;
    }

    return calibrator->error;
}



////////////////////////////////////////////////////////////////////////////////
// Finalises a calibration that is currently in progres. Samples gathered so
// far are retained as a courtesy to the application in case it needs them
// (eg to plot samples). They will be cleared when the next calibration begins
// or when ecompass_calibrator_clear() is called.
//
// \brief Ends a calibration.
// \param calibrator Pointer to the calibrator.
// \return Error code.
// \retval ecompassCalibratorOk Calibration was successfully ended and
// calibration results are valid.
// \retval ecompassCalibratorErrBadArg \p calibrator was NULL.
// \retval ecompassCalibratorErrNotEnoughData Calibration has ended but there
// were not enough valid sensor readings to calculate any valid calibration
// parameters.
// \retval ecompassCalibratorErrBadConfig Calibration has ended but no valid
// calibration parameters could be calculated because the calibrator is
// incorrectly configured.

ecompass_calibrator_err_t ecompass_calibrator_end(ecompass_calibrator_t *calibrator)
{
    // Sanity check
    if (!calibrator)
        return ecompassCalibratorErrBadArg;

    // Finalise calculation. If the calculation accepts the current data without
    // ending the calibration, then there wan't enough data.
    switch (ecompass_calibrator_calculate(calibrator, 1))
    {
        case ecompassCalibratorSampleAccepted:
        case ecompassCalibratorSampleUnstable:
        case ecompassCalibratorSampleWaitingOrientation:
        case ecompassCalibratorSampleUnsuitable:
        case ecompassCalibratorSampleLimitExceeded:
            // If not reading anarbitrary number of points and we haven't ended the
            // calibration by supplying the correct number, then we didn't have enough data
            if (ecompass_calibrator_reading_type(calibrator) != ecompassPointsArbitrary)
                calibrator->error = ecompassCalibratorErrNotEnoughData;
            else
                calibrator->error = ecompassCalibratorOk;
            break;

        case ecompassCalibratorSampleEnded:
            calibrator->error = ecompassCalibratorOk;
            break;

        case ecompassCalibratorSampleFailed:
            // Leave error as set by ecompass_calibrator_calculate()
            break;
    }
    calibrator->calibration_in_progress = 0;
    calibrator->needs_recalculation		= 0;

    // Release unneeded resources
    if (calibrator->stabiliser.data)
        free(calibrator->stabiliser.data);
    calibrator->stabiliser.data		 = NULL;
    calibrator->stabiliser.capacity  = 0;
    calibrator->stabiliser.count	 = 0;
    calibrator->stabiliser.put		 = 0;
    calibrator->stabiliser.get		 = 0;
    calibrator->stabiliser.is_stable = 0;

    return calibrator->error;
}



////////////////////////////////////////////////////////////////////////////////
// Aborts a calibration currently in progress. Samples gathered so far are
// retained as a courtesy to the application in case it needs them (eg to plot
// samples) but the calibrator will not finalise the current calibration.
// They will be cleared when the next calibration begins or when
// ecompass_calibrator_clear() is called.
//
// \brief Aborts a calibration.
// \param calibrator Pointer to the calibrator.
// \return Error code.
// \retval ecompassCalibratorOk Calibration was successfully aborted.
// \retval ecompassCalibratorErrBadArg \p calibrator was NULL.

ecompass_calibrator_err_t ecompass_calibrator_abort(ecompass_calibrator_t *calibrator)
{
    // Sanity check
    if (!calibrator)
        return ecompassCalibratorErrBadArg;

    // No more calibration in progress
    calibrator->error = ecompassCalibratorOk;
    calibrator->calibration_in_progress = 0;
    calibrator->needs_recalculation		= 0;

    // Release unneeded resources
    if (calibrator->stabiliser.data)
        free(calibrator->stabiliser.data);
    calibrator->stabiliser.data		 = NULL;
    calibrator->stabiliser.capacity  = 0;
    calibrator->stabiliser.count	 = 0;
    calibrator->stabiliser.put		 = 0;
    calibrator->stabiliser.get		 = 0;
    calibrator->stabiliser.is_stable = 0;

    return calibrator->error;
}



////////////////////////////////////////////////////////////////////////////////
// The calibrator may use a stabiliser for certain modes (specificaly when auto
// calibrating the acceleromter) in order to determine if the data is usable.
// This function may be helpful to determine if the stabiliser needs configuring
// for a calibration or to provide operator feedback that the data has become
// stable and accepted so they can move the device to a new orientation.
//
// \brief Indicates if a calibrator uses stabiliser in current mode.
// \param calibrator Pointer to calibrator.
// \return Boolean indication that \p calibrator uses a stabiliser.
// \retval 0 The calibrator does NOT use a stabiliser.
// \retval 1 The calibrator does use a stabiliser.

int ecompass_calibrator_uses_stabiliser(ecompass_calibrator_t *calibrator)
{
    // Sanity check
    if (!calibrator)
        return 0;

    calibrator->error = ecompassCalibratorOk;
    return USES_STABILISER(calibrator) ? 1 : 0;
}



////////////////////////////////////////////////////////////////////////////////
// Returns a boolean indicating wither a particular configuration would use
// a stabiliser if it were enabled.
//
// \brief Indicates if a calibrator config would stabiliser if allowed to.
// \param config Pointer to calibrator configuration to test.
// \return Boolean indication that \p calibrator uses a stabiliser.
// \retval 0 The calibrator config would NOT use a stabiliser.
// \retval 1 The calibrator config would use a stabiliser.

int ecompass_calibrator_would_use_stabiliser(ecompass_calibrator_config_t *config)
{
    // Sanity check
    if (!config)
        return 0;

    return WOULD_USE_STABILISER(config) ? 1 : 0;
}



////////////////////////////////////////////////////////////////////////////////
// Resets the stabiliser by clearing buffered data and setting into a specified
// state. Can be used to "un-pause" a calibration and forget about
//
// \brief Reset stabiliser.
// \param calibrator The calibrator whose stabiliser is to be reset.
// \param is_stable The state that the calibrator should be set to. If set to
// zero, the stabiliser will be unstable and will wait for the measurements to
// stablise before accepting a value. If set to non-zero, the stabiliser will be
// stable and wait for the measurements to move FIRST and then re-stabilise
// before accepting a value.
// \return Error code.
// \retval ecompassCalibratorOk The calibrator's stabiliser has been reset/
// \retval ecompassCalibratorErrBadArg \p calibrator was NULL.

int ecompass_calibrator_reset_stabiliser(ecompass_calibrator_t *calibrator, int is_stable)
{
    if (!calibrator)
        return ecompassCalibratorErrBadArg;

    calibrator->stabiliser.is_stable = is_stable;
    calibrator->stabiliser.count	 = 0;
    calibrator->stabiliser.put		 = 0;
    calibrator->stabiliser.get		 = 0;

    return ecompassCalibratorOk;
}



////////////////////////////////////////////////////////////////////////////////
// Clears any currntly held data samples used by the calibrator. The sampled
// data are held in order to calculate the calibration constants. Even though
// they not needed once the calibration is complete, they are retained as a
// courtesy to the application in case it needs them (eg to plot samples).
// Calling this function will release all of these points. Clearing data
// will fail if a calibration is currently in progress. It must be ended or
// aborted first.
//
// \brief Release buffered calibration samples.
// \param calibrator Pointer to the calibrator.
// \return Error code.
// \retval ecompassCalibratorOk Calibration data cleared.
// \retval ecompassCalibratorErrBadArg \p calibrator was NULL.
// \retval ecompassCalibratorErrInProgress Could not clear calibration data
// because a calibration is currently in progress.

ecompass_calibrator_err_t ecompass_calibrator_clear(ecompass_calibrator_t *calibrator)
{
    // Sanity check
    if (!calibrator)
        return ecompassCalibratorErrBadArg;

    if (!calibrator->calibration_in_progress)
    {
        // Clear buffered samples.
        if (calibrator->data)
            free(calibrator->data);
        calibrator->data	 = NULL;
        calibrator->capacity = 0;
        calibrator->count	 = 0;
        calibrator->error	 = ecompassCalibratorOk;
    }
    else
        calibrator->error = ecompassCalibratorErrInProgress;

    return calibrator->error;
}



////////////////////////////////////////////////////////////////////////////////
// Submits a data sample to calibrator. The calibrator will pre-process the
// sample (filtering and testing for stability if required) and adds the sample
// to the sample set and recalulating calibration parameters if appropriate.
// The result member of \p calibrator to indicate how it went.
//
// \brief Submits a data sample to calibrator.
// \param calibrator Pointer to the calibrator.
// \param x The x axis component of data sample.
// \param y The y axis component of data sample.
// \param z The z axis component of data sample.
// \return Indication of whether the data was accepted and if not, why not.
// See ecompass_calibrator_submit_t for details.

ecompass_calibrator_submit_t ecompass_calibrator_submit_data(ecompass_calibrator_t *calibrator, double x, double y, double z)
{
    ecompass_calibrator_data_t data;

    // Sanity check
    if (!calibrator)
        return ecompassCalibratorSampleFailed;

    calibrator->error = ecompassCalibratorOk;
    calibrator->submitted_data_changed_parameters = 0;
    calibrator->last_orientation_is_valid = 0;

    if (!calibrator->calibration_in_progress)
    {
        calibrator->error = ecompassCalibratorErrNotInProgress;
        return ecompassCalibratorSampleFailed;
    }

    if (calibrator->config.sensor == ecompassSensorUnknown)
    {
        calibrator->error = ecompassCalibratorErrBadConfig;
        return ecompassCalibratorSampleFailed;
    }

    if ((calibrator->config.sensor == ecompassSensorMagnetometer) && (fabs(calibrator->config.magnetic_field.magnitude) > 0.0) &&
        (calibrator->config.magnetometer_sample_points != ecompassPointsArbitrary))
    {
        x /= calibrator->config.magnetic_field.magnitude;
        y /= calibrator->config.magnetic_field.magnitude;
        z /= calibrator->config.magnetic_field.magnitude;
    }

    data.x = x;
    data.y = y;
    data.z = z;

    // Test for stabilisation of accelerometer data
    if (USES_STABILISER(calibrator))
    {
        switch (ecompass_calibrator_test_stability(calibrator, &data))
        {
            case stabiliserBecomeStable:
                calibrator->stabiliser.is_stable = 1;
                break;

            case stabiliserStillStable:
                return ecompassCalibratorSampleWaitingOrientation;

            case stabiliserBecomeUnstable:
                calibrator->stabiliser.is_stable = 0;
                return ecompassCalibratorSampleUnstable;

            case stabiliserStillUnstable:
                return ecompassCalibratorSampleUnstable;
        }
    }

    if (ecompass_calibrator_data_is_redundant(calibrator, &data))
        return ecompassCalibratorSampleUnsuitable;

    // See if we have hit the sample limit
    if ((calibrator->config.sample_limit > 0) && (calibrator->count >= calibrator->config.sample_limit))
        return ecompassCalibratorSampleLimitExceeded;

    // Make room for the new sample if we need to.
    if ((calibrator->data == NULL) || (calibrator->count >= calibrator->capacity))
    {
        ecompass_calibrator_data_t *ptr;

        if ((ptr = static_cast<ecompass_calibrator_data_t *>(realloc(calibrator->data,
                                                                     (calibrator->capacity +
                                                                      DATA_CHUNK_SIZE) *
                                                                     sizeof(ecompass_calibrator_data_t)))) != NULL)
        {
            calibrator->data = ptr;
            calibrator->capacity += DATA_CHUNK_SIZE;
        }
    }

    // Add the sample
    if (calibrator->data && (calibrator->count < calibrator->capacity))
    {
        calibrator->data[calibrator->count].x = x;
        calibrator->data[calibrator->count].y = y;
        calibrator->data[calibrator->count].z = z;
        calibrator->count++;
        calibrator->needs_recalculation = 1;
    }

    // Recalculate calibration parameters and set the result.
    return ecompass_calibrator_calculate(calibrator, 0);
}



////////////////////////////////////////////////////////////////////////////////
// Undoes last submitted measurement by removing it.
//
// \brief Removes last submitted sample point.
// \param calibrator Pointer to the calibrator.
// \return Submission status. See ecompass_calibrator_submit_t for details.
// Returns ecompassCalibratorSampleFailed if \p calbrator was NULL or it had
// no measurements to undo otherwise it returns value is the same as
// reasons as ecompass_calibrator_calculate().

ecompass_calibrator_submit_t ecompass_calibrator_undo_last(ecompass_calibrator_t *calibrator)
{
    if (!calibrator)
        return ecompassCalibratorSampleFailed;

    if (calibrator->count <= 0)
        return ecompassCalibratorSampleFailed;

    calibrator->error = ecompassCalibratorOk;
    calibrator->submitted_data_changed_parameters = 0;
    calibrator->last_orientation_is_valid = 0;
    calibrator->count--;
    calibrator->needs_recalculation = 1;

    // Recalculate calibration parameters and set the result.
    return ecompass_calibrator_calculate(calibrator, 0);
}



////////////////////////////////////////////////////////////////////////////////
// Returns the number of data points the calibrator is curretly holding.
//
// \brief Data count for calibrator.
// \param calibrator Pointer to the calibrator.
// \return Number of data points currently held by \p calibrator.

int ecompass_calibrator_data_count(ecompass_calibrator_t *calibrator)
{
    return calibrator ? calibrator->count : 0;
}



////////////////////////////////////////////////////////////////////////////////
/// Returns a flag indicating that the calibrator is at its configured data
/// limit. The next measurment to be submitted will be ignored and return with
/// ecompassCalibratorSampleLimitExceeded.
///
/// \brief Returns a flag indicating that calibrator is at full capacity.
/// \param calibrator Pointer to the calibrator.
/// \return Flag indicating that calibrator is at full capacity.
/// \retval 0 The calibrator can accept further calibration data.
/// \retval 1 The calibrator is at maximum capacity and will not accept further
/// calibration data.

int ecompass_calibrator_will_exceed_data_limit(ecompass_calibrator_t *calibrator)
{
    if (!calibrator)
        return 1;
    if (calibrator->config.sample_limit <= 0)
        return 0;
    if (calibrator->count < calibrator->config.sample_limit)
        return 0;

    return 1;
}



////////////////////////////////////////////////////////////////////////////////
/// Tests the stability of a sampled data point by analysing the spread of the
/// most recent samples from their mean. Used to determine if acceleromter
/// samples have become stable (if the device is moving) and can be used for
/// calibration. When determined to be stable, the data is replaced with the
/// mean stabilsed value.
///
/// \brief Tests data sample for stability.
/// \param calibrator Pointer to the calibrator.
/// \param data Pointer to sampled data. May be modified.
/// \return Data stability.
/// \retval stabiliserBecomeStable Stabiliser has detected a transition from unstable
/// to stable.
/// \retval stabiliserStillStable Reading is stable and was previously stable.
/// \retval stabiliserBecomeUnstable Stabiliser has detected a transition from stable
/// to unstable.
/// \retval stabiliserStillUnstable Reading is unstable and was previously unstable.

static stabiliser_result_t ecompass_calibrator_test_stability(ecompass_calibrator_t *calibrator, ecompass_calibrator_data_t *data)
{
    int i;
    double spread;
    ecompass_calibrator_data_t mean;
    ecompass_calibrator_data_t min;
    ecompass_calibrator_data_t max;

    // Sanity check.
    if (!calibrator->stabiliser.data || (calibrator->stabiliser.capacity < 1))
        return calibrator->stabiliser.is_stable ? stabiliserBecomeUnstable : stabiliserStillUnstable;

    // Add sample
    calibrator->stabiliser.data[calibrator->stabiliser.put].x = data->x;
    calibrator->stabiliser.data[calibrator->stabiliser.put].y = data->y;
    calibrator->stabiliser.data[calibrator->stabiliser.put].z = data->z;
    if (++calibrator->stabiliser.put == calibrator->stabiliser.capacity)
        calibrator->stabiliser.put = 0;
    if (calibrator->stabiliser.put == calibrator->stabiliser.get)
        if (++calibrator->stabiliser.get == calibrator->stabiliser.capacity)
            calibrator->stabiliser.get = 0;
    if (calibrator->stabiliser.count < calibrator->stabiliser.capacity)
        calibrator->stabiliser.count++;

    // Cant be stable if we don't have enough samples. Stay in initialised state
    // until we have enough info to make a definitive assessment.
    if (calibrator->stabiliser.count < calibrator->stabiliser.capacity)
        return calibrator->stabiliser.is_stable ? stabiliserStillStable : stabiliserStillUnstable;

    // Analyse data. Order doesn not matter and we know we have a full buffer so
    // we'll just scan through it rather than going from get to put.
    // Use the simplest measure of spread which is the maximum distance of each
    // x/y/z component from the mean.
    mean.x = mean.y = mean.z = 0;
    min.x = min.y = min.z = 0;
    max.x = max.y = max.z = 0;
    for (i = 0; i < calibrator->stabiliser.count; i++)
    {
        mean.x += calibrator->stabiliser.data[i].x;
        mean.y += calibrator->stabiliser.data[i].y;
        mean.z += calibrator->stabiliser.data[i].z;

        if (i > 0)
        {
            if (min.x > calibrator->stabiliser.data[i].x) min.x = calibrator->stabiliser.data[i].x;
            if (min.y > calibrator->stabiliser.data[i].y) min.y = calibrator->stabiliser.data[i].y;
            if (min.z > calibrator->stabiliser.data[i].z) min.z = calibrator->stabiliser.data[i].z;
            if (max.x < calibrator->stabiliser.data[i].x) max.x = calibrator->stabiliser.data[i].x;
            if (max.y < calibrator->stabiliser.data[i].y) max.y = calibrator->stabiliser.data[i].y;
            if (max.z < calibrator->stabiliser.data[i].z) max.z = calibrator->stabiliser.data[i].z;
        }
        else
        {
            min.x = calibrator->stabiliser.data[i].x;
            min.y = calibrator->stabiliser.data[i].y;
            min.z = calibrator->stabiliser.data[i].z;
            max.x = calibrator->stabiliser.data[i].x;
            max.y = calibrator->stabiliser.data[i].y;
            max.z = calibrator->stabiliser.data[i].z;
        }
    }
    mean.x /= calibrator->stabiliser.count;
    mean.y /= calibrator->stabiliser.count;
    mean.z /= calibrator->stabiliser.count;

    spread = fabs(min.x - mean.x);
    if (fabs(min.y - mean.y) > spread) spread = fabs(min.y - mean.y);
    if (fabs(min.z - mean.z) > spread) spread = fabs(min.z - mean.z);
    if (fabs(max.x - mean.x) > spread) spread = fabs(max.x - mean.x);
    if (fabs(max.y - mean.y) > spread) spread = fabs(max.y - mean.y);
    if (fabs(max.z - mean.z) > spread) spread = fabs(max.z - mean.z);

#ifdef DEBUG_SHOW_STABILISER
    printf("eCompass stabilser: Reading [%.8f, %.8f, %.8f], mean [%.8f, %.8f, %.8f], spread %.8f, target %.8f (%s).\n",
		   data->x, data->y, data->z, mean.x, mean.y, mean.z, spread, calibrator->stabiliser.maximum_spread,
		   (spread <= calibrator->stabiliser.maximum_spread) ? "stable" : "NOT stable");
#endif
    // See if inside limit
    if (spread <= calibrator->stabiliser.maximum_spread)
    {
        data->x = mean.x;
        data->y = mean.y;
        data->z = mean.z;
        return calibrator->stabiliser.is_stable ? stabiliserStillStable : stabiliserBecomeStable;
    }

    // See if outside limit (with hysteresis)
    if (spread > (calibrator->stabiliser.maximum_spread * (1.0 + calibrator->stabiliser.hysteresis)))
    {
        return calibrator->stabiliser.is_stable ? stabiliserBecomeUnstable : stabiliserStillUnstable;
    }

    // Spread is inside hysteresis band. State stays as it was.
    return calibrator->stabiliser.is_stable ? stabiliserStillStable : stabiliserStillUnstable;
}



////////////////////////////////////////////////////////////////////////////////
/// Tests the data for redundancy. Depending on what is being calibrated and the
/// calibration mode, the calibrator may expect a very specific set of sample
/// points. This function will test a newly accepted value to determine if it
/// matches an existing one.
///
/// \brief Tests data sample for redundancy.
/// \param calibrator Pointer to the calibrator.
/// \param data Pointer to sampled data. May be modified.
/// \return Boolean indication of stability.
/// \retval 0 data sample is NOT redundant.
/// \retval 1 data sample is redundant.

static int ecompass_calibrator_data_is_redundant(ecompass_calibrator_t *calibrator, ecompass_calibrator_data_t *data)
{
    int i;

    // All samples are redundant for an unknown sensor type
    if (calibrator->config.sensor == ecompassSensorUnknown)
        return 1;

    // Negative redundancy limit means no redundancy chack and all points are usable
    if (calibrator->redundancy_limit_squared < 0.0)
        return 0;

    // See if this sample lies within the specified radius certain distance of existing samples
    // are redundant.
    for (i = 0; i < calibrator->count; i++)
    {

        double dx, dy, dz;
        dx = data->x - calibrator->data[i].x;
        dy = data->y - calibrator->data[i].y;
        dz = data->z - calibrator->data[i].z;
        if ((dx * dx + dy * dy + dz * dz) < calibrator->redundancy_limit_squared)
        {
            // Avoid flagging an error for intentionally duplicated measurements in some sequences.
            if (ecompassPointsThirtySixOrthodromeCubicDiagonal)
            {
                if ((calibrator->count == 15) && (i ==  3)) continue;
                if ((calibrator->count == 21) && (i ==  9)) continue;
                if ((calibrator->count == 24) && (i ==  0)) continue;
                if ((calibrator->count == 27) && (i == 12)) continue;
                if ((calibrator->count == 30) && (i ==  6)) continue;
                if ((calibrator->count == 33) && (i == 18)) continue;
            }
            // printf("Measurement %d is same as measurement %d\n", calibrator->count, i);
            return 1;
        }
    }

    return 0;
}



////////////////////////////////////////////////////////////////////////////////
/// Recalulates calibration constants if necessary and sets the results field
/// of the calibrator appropriately.
///
/// \brief Recalculates a calibration constants from sample points.
/// \param calibrator Pointer to the calibrator.
/// \param has_ended Flag indicating that this calculation was initiated by
/// ending the calibration process. Calibrations based on an arbitrary number of
/// points do not get calculated for every point submitted, only on the entire
/// data set at the end of the proceedure because it is computationally expensive.

static ecompass_calibrator_submit_t ecompass_calibrator_calculate(ecompass_calibrator_t *calibrator, int has_ended)
{
    ecompass_calibration_points_t points;

    // Sanity check
    if (!calibrator)
        return ecompassCalibratorSampleFailed;

    calibrator->error = ecompassCalibratorOk;

    // Only recalulate if necessary
    if (!calibrator->needs_recalculation)
        return (calibrator->calibration_complete) ? ecompassCalibratorSampleEnded : ecompassCalibratorSampleAccepted;
    calibrator->needs_recalculation = 0;

    switch (calibrator->config.sensor)
    {
        case ecompassSensorAccelerometer:	points = calibrator->config.accelerometer_sample_points;	break;
        case ecompassSensorMagnetometer:	points = calibrator->config.magnetometer_sample_points;		break;

        case ecompassSensorUnknown:
        case ecompassSensorTemperature:
            calibrator->error = ecompassCalibratorErrBadConfig;
            return ecompassCalibratorSampleFailed;
    }

    switch (points)
    {
        case ecompassPointsTwoOptimal:				return least_squares(calibrator, leastSquaresSimple, LEAST_SQUARES_PRE_SUBTRACTED_OFFSET);
        case ecompassPointsThreeOptimal:			return least_squares(calibrator, leastSquaresSimple, LEAST_SQUARES_PRE_SUBTRACTED_OFFSET);
        case ecompassPointsFourOptimal:				return least_squares(calibrator, leastSquaresSimple, LEAST_SQUARES_PRE_SUBTRACTED_OFFSET);
        case ecompassPointsSixOptimal:				return least_squares(calibrator, leastSquaresSimple, LEAST_SQUARES_PRE_SUBTRACTED_OFFSET);
        case ecompassPointsEightOptimal:			return least_squares(calibrator, leastSquaresSimple, LEAST_SQUARES_PRE_SUBTRACTED_OFFSET);
        case ecompassPointsSixAxisAligned:			return least_squares(calibrator, leastSquaresSimple, LEAST_SQUARES_PRE_SUBTRACTED_OFFSET);
        case ecompassPointsFourteenAxisAligned:		return least_squares(calibrator, leastSquaresSimple, LEAST_SQUARES_PRE_SUBTRACTED_OFFSET);
        case ecompassPointsSixOptimalCubicDiagonal:					return least_squares(calibrator, leastSquaresCubicDiagonal, 0);
        case ecompassPointsEightOptimalCubicDiagonal:				return least_squares(calibrator, leastSquaresCubicDiagonal, 0);
        case ecompassPointsEightAlmostOptimalCubicDiagonal:			return least_squares(calibrator, leastSquaresCubicDiagonal, 0);
        case ecompassPointsSixAxisAlignedCubicDiagonal:				return least_squares(calibrator, leastSquaresCubicDiagonal, 0);
        case ecompassPointsFourteenAxisAlignedCubicDiagonal:		return least_squares(calibrator, leastSquaresCubicDiagonal, 0);
        case ecompassPointsFourteenAxisAlignedQuadCubicDiagonal:	return least_squares(calibrator, leastSquaresQuadCubicDiagonal, 0);
        case ecompassPointsThirtySixOrthodromeCubicDiagonal:		return least_squares(calibrator, leastSquaresCubicDiagonal, 0);
        case ecompassPointsThirtySixOrthodromeCubic:				return least_squares(calibrator, leastSquaresCubic, 0);
        case ecompassPointsThirtySixOrthodromeQuadCubicDiagonal:	return least_squares(calibrator, leastSquaresQuadCubicDiagonal, 0);
        case ecompassPointsThirtySixOrthodromeQuadCubic:			return least_squares(calibrator, leastSquaresQuadCubic, 0);
//		case ecompassPointsTest0D1F2F3F:							return least_squares(calibrator, leastSquaresQuadCubic, 0);
//		case ecompassPointsTest0D1F2F3F4D5D:						return least_squares(calibrator, leastSquaresQuadCubicPlusFourthFifthDiagonal, 0);
//		case ecompassPointsTest0D1F2D3D4D5D:						return least_squares(calibrator, leastSquaresUpToFifthDiagonal, 0);
//		case ecompassPointsTest0D1F2D3D:							return least_squares(calibrator, leastSquaresQuadCubicDiagonal, 0);
//		case ecompassPointsTest0D1F3D:								return least_squares(calibrator, leastSquaresCubicDiagonal, 0);
//		case ecompassPointsTest0D1F:								return least_squares(calibrator, leastSquaresSimple, 0);
        case ecompassPointsGearBox0D1F3D:							return least_squares(calibrator, leastSquaresCubicDiagonal, 0);

        case ecompassPointsArbitrary:
            // Don't recalculate calibration for arbitrary number of points for evey data point submitted
            // Only once the calibration has ended.
            if (has_ended)
                return least_squares_arbitrary(calibrator);
            calibrator->needs_recalculation = 1;
            return ecompassCalibratorSampleAccepted;
    }

    calibrator->error = ecompassCalibratorErrBadConfig;
    return ecompassCalibratorSampleFailed;
}



////////////////////////////////////////////////////////////////////////////////
/// Calculates 12 parameter model (3x3 tranform + 3x1 bias) from samples taken
/// at arbitrary orientations using adaptive least squares optimisation.
///
/// \brief Calculates 12 parameter calibration using adaptive least squares.
/// \param calibrator The target calibrator.
/// \return Calibration result
/// \retval ecompassCalibratorSampleEnded Calculation successful and within
/// specified error limit.
/// \retval ecompassCalibratorSampleAccepted Calculation successful but not yet
/// within specified error limit.
/// \retval ecompassCalibratorSampleFailed Error occured and calibrator->error
/// was set accordingly.

static ecompass_calibrator_submit_t	least_squares_arbitrary(ecompass_calibrator_t *calibrator)
{
    if (!calibrator)
        return ecompassCalibratorSampleFailed;

    // Not implimented. We don't do this kind of calibration on the handset.

    return ecompassCalibratorSampleAccepted;
}



////////////////////////////////////////////////////////////////////////////////
//

static ecompass_calibrator_submit_t least_squares_init(ecompass_calibrator_t *calibrator, int *count_available, int *count_required, ecompass_calibration_points_t *sample_points)
{
    *count_available = calibrator ? calibrator->count : 0;

    if (!calibrator)
    {
        return ecompassCalibratorSampleFailed;
    }

    // Work out which optimal points we are sampling
    switch (calibrator->config.sensor)
    {
        case ecompassSensorAccelerometer:	*sample_points = calibrator->config.accelerometer_sample_points; break;
        case ecompassSensorMagnetometer:	*sample_points = calibrator->config.magnetometer_sample_points; break;
        case ecompassSensorUnknown:
        case ecompassSensorTemperature:
            calibrator->error = ecompassCalibratorErrBadConfig;
            return ecompassCalibratorSampleFailed;
    }

    int num = ecompass_calibrator_get_num_samples_required(*sample_points);
    if (num < 3)
    {
        calibrator->error = ecompassCalibratorErrBadConfig;
        return ecompassCalibratorSampleFailed;
    }

    *count_required = num;

    return ecompassCalibratorSampleAccepted;
}



////////////////////////////////////////////////////////////////////////////////
//

static matrix_t *least_squares_create_measurement_matrix(ecompass_calibrator_t *calibrator, int count, least_squares_type_t type, int axis)
{
    int   i;
    double n;
    matrix_t *mat;
    matrix_t *mat_transposed;

    // Sanity check
    if (!calibrator)
        return NULL;

    // Create working matrices
    switch (type)
    {
        case leastSquaresSimple:			mat = matrix_create(count, 4); break;
        case leastSquaresCubicDiagonal: 	mat = matrix_create(count, 5); break;
        case leastSquaresQuadCubicDiagonal: mat = matrix_create(count, 6); break;
        case leastSquaresCubic:				mat = matrix_create(count, 7); break;
        case leastSquaresQuadCubic:			mat = matrix_create(count, 10); break;
        case leastSquaresUpToFifthDiagonal:	mat = matrix_create(count, 8); break;
        case leastSquaresQuadCubicPlusFourthFifthDiagonal: mat = matrix_create(count, 12); break;
    }

    mat_transposed = matrix_create(0, 0);
    if (!mat || !mat_transposed)
    {
        if (mat)			matrix_destroy(mat);
        if (mat_transposed) matrix_destroy(mat_transposed);
        calibrator->error = ecompassCalibratorErrMemory;
        return NULL;
    }

    // Load the initial measurement matrix
    for (i = 0; i < count; i++)
    {
        matrix_set_element(mat, i, 0, calibrator->data[i].x);
        matrix_set_element(mat, i, 1, calibrator->data[i].y);
        matrix_set_element(mat, i, 2, calibrator->data[i].z);
        matrix_set_element(mat, i, 3, 1.0);

        switch (type)
        {
            case leastSquaresSimple:
                break;

            case leastSquaresCubicDiagonal:
                switch (axis)
                {
                    case 0:  n = calibrator->data[i].x; break;
                    case 1:  n = calibrator->data[i].y; break;
                    case 2:  n = calibrator->data[i].z; break;
                    default: n = 1.0; break;
                }
                matrix_set_element(mat, i, 4, n * n * n);
                break;

            case leastSquaresQuadCubicDiagonal:
                switch (axis)
                {
                    case 0:  n = calibrator->data[i].x; break;
                    case 1:  n = calibrator->data[i].y; break;
                    case 2:  n = calibrator->data[i].z; break;
                    default: n = 1.0; break;
                }
                matrix_set_element(mat, i, 4, n * n * n);
                matrix_set_element(mat, i, 5, n * n);
                break;

            case leastSquaresCubic:
                matrix_set_element(mat, i, 4, calibrator->data[i].x * calibrator->data[i].x * calibrator->data[i].x);
                matrix_set_element(mat, i, 5, calibrator->data[i].y * calibrator->data[i].y * calibrator->data[i].y);
                matrix_set_element(mat, i, 6, calibrator->data[i].z * calibrator->data[i].z * calibrator->data[i].z);
                break;

            case leastSquaresQuadCubic:
                matrix_set_element(mat, i, 4, calibrator->data[i].x * calibrator->data[i].x * calibrator->data[i].x);
                matrix_set_element(mat, i, 5, calibrator->data[i].y * calibrator->data[i].y * calibrator->data[i].y);
                matrix_set_element(mat, i, 6, calibrator->data[i].z * calibrator->data[i].z * calibrator->data[i].z);
                matrix_set_element(mat, i, 7, calibrator->data[i].x * calibrator->data[i].x);
                matrix_set_element(mat, i, 8, calibrator->data[i].y * calibrator->data[i].y);
                matrix_set_element(mat, i, 9, calibrator->data[i].z * calibrator->data[i].z);
                break;

            case leastSquaresUpToFifthDiagonal:
                switch (axis)
                {
                    case 0:  n = calibrator->data[i].x; break;
                    case 1:  n = calibrator->data[i].y; break;
                    case 2:  n = calibrator->data[i].z; break;
                    default: n = 1.0; break;
                }
                matrix_set_element(mat, i, 4, n * n);
                matrix_set_element(mat, i, 5, n * n * n);
                matrix_set_element(mat, i, 6, n * n * n * n);
                matrix_set_element(mat, i, 7, n * n * n * n * n);
                break;

            case leastSquaresQuadCubicPlusFourthFifthDiagonal:
                matrix_set_element(mat, i, 4, calibrator->data[i].x * calibrator->data[i].x * calibrator->data[i].x);
                matrix_set_element(mat, i, 5, calibrator->data[i].y * calibrator->data[i].y * calibrator->data[i].y);
                matrix_set_element(mat, i, 6, calibrator->data[i].z * calibrator->data[i].z * calibrator->data[i].z);
                matrix_set_element(mat, i, 7, calibrator->data[i].x * calibrator->data[i].x);
                matrix_set_element(mat, i, 8, calibrator->data[i].y * calibrator->data[i].y);
                matrix_set_element(mat, i, 9, calibrator->data[i].z * calibrator->data[i].z);
                switch (axis)
                {
                    case 0:  n = calibrator->data[i].x; break;
                    case 1:  n = calibrator->data[i].y; break;
                    case 2:  n = calibrator->data[i].z; break;
                    default: n = 1.0; break;
                }
                matrix_set_element(mat, i, 10, n * n * n * n);
                matrix_set_element(mat, i, 11, n * n * n * n * n);
                break;

        }
    }

    // Calculate X = (X' X)^-1 X'
    calibrator->error = ecompassCalibratorOk;
    if (matrix_transpose(mat_transposed, mat) == matrixOk)
    {
        if (matrix_multiply(mat, mat_transposed, mat) == matrixOk)
        {
            if (matrix_invert(mat, mat) == matrixOk)
            {
                if (matrix_multiply(mat, mat, mat_transposed) != matrixOk)
                    calibrator->error = ecompassCalibratorErrInternal;
            }
            else
                calibrator->error = ecompassCalibratorErrNoSolution;
        }
        else
            calibrator->error = ecompassCalibratorErrInternal;
    }
    else
        calibrator->error = ecompassCalibratorErrInternal;

    matrix_destroy(mat_transposed);
    if (calibrator->error != ecompassCalibratorOk)
    {
        matrix_destroy(mat);
        return NULL;
    }

    return mat;
}



////////////////////////////////////////////////////////////////////////////////
/// Calulates 12 parameter calibration solution using technique described in
/// app note, AN4399. Uses least squares minimisation based on a fixed set of
/// optimally chosen sample points (orientations).
/// \brief 12 parameter calibration solution for measurements at optimal points.
/// \param calibrator The target calibrator.
/// \param type Type of least squared solution.
/// \param pre_subtracted_offset flag selects presubtracted offset. Only
/// applicable when \p type is ecompassCalibratorSampleFailed
/// \return Calibration result
/// \retval ecompassCalibratorSampleEnded Calculation successful and within
/// specified error limit.
/// \retval ecompassCalibratorSampleAccepted Calculation successful but not yet
/// within specified error limit.
/// \retval ecompassCalibratorSampleFailed Error occured and calibrator->error
/// was set accordingly.

static ecompass_calibrator_submit_t least_squares(ecompass_calibrator_t *calibrator, least_squares_type_t type, int pre_subtracted_offset)
{
    int count_available, count_required, i;
    ecompass_calibration_points_t sample_points;
    ecompass_calibration_coeffs_t *coeffs;
    matrix_t	*mat_measurements = NULL;
    matrix_t	*mat_optimal, *mat_solution;
    float		 roll, pitch;
    int			 row;

    // Get pointer to storage for calibration data
    coeffs = &calibrator->calibration.term[0];
    ecompass_calibration_coeffs_init(coeffs);

    // Gather information
    if (least_squares_init(calibrator, &count_available, &count_required, &sample_points) == ecompassCalibratorSampleFailed)
        return ecompassCalibratorSampleFailed;

    // Store orientation for last added point
    if (ecompass_calibrator_get_optimum_orientation(calibrator, count_available - 1, &calibrator->last_roll, &calibrator->last_pitch, 0) == ecompassCalibratorOk)
    {
        calibrator->last_azimuth = 0.0;
        calibrator->last_orientation_is_valid = 1;
    }

    // If we don't have enough samples, accept this one but don't perform calulation
    if (count_available < count_required)
        return ecompassCalibratorSampleAccepted;

    // Create all the matrices we will need
    mat_optimal		 = matrix_create(count_required, 1);
    mat_solution	 = matrix_create(0, 0);
    if (!mat_optimal || !mat_solution /* || !mat_transposed */)
    {
        if (mat_optimal)	  matrix_destroy(mat_optimal);
        if (mat_solution)	  matrix_destroy(mat_solution);
        calibrator->error = ecompassCalibratorErrMemory;
        return ecompassCalibratorSampleFailed;
    }

    for (row = 0; row < 3; row++)
    {
        // Generate the measurements matrix if required. Returns (X' X)^-1 X
        if (mat_measurements == NULL)
        {
            if ((mat_measurements = least_squares_create_measurement_matrix(calibrator, count_required, type, row)) == NULL)
            {
                matrix_destroy(mat_optimal);
                matrix_destroy(mat_solution);
                return ecompassCalibratorSampleFailed;
            }
        }

        // Calculate this row of solution
        switch (row)
        {
            case 0:
                for (i = 0; i < count_required; i++)
                    if (ecompass_calibrator_get_optimum_orientation(calibrator, i, &roll, &pitch, 0) == ecompassCalibratorOk)
                        matrix_set_element(mat_optimal, i, 0, -sin(pitch * M_PI / 180.0));
                break;

            case 1:
                for (i= 0; i < count_required; i++)
                    if (ecompass_calibrator_get_optimum_orientation(calibrator, i, &roll, &pitch, 0) == ecompassCalibratorOk)
                        matrix_set_element(mat_optimal, i, 0, cos(pitch * M_PI / 180.0) * sin(roll * M_PI / 180.0));
                break;

            case 2:
                for (i= 0; i < count_required; i++)
                    if (ecompass_calibrator_get_optimum_orientation(calibrator, i, &roll, &pitch, 0) == ecompassCalibratorOk)
                        matrix_set_element(mat_optimal, i, 0, cos(pitch * M_PI / 180.0) * cos(roll * M_PI / 180.0));
                break;

            default:
                calibrator->error = ecompassCalibratorErrInternal;
                matrix_destroy(mat_optimal);
                matrix_destroy(mat_solution);
                matrix_destroy(mat_measurements);
                return ecompassCalibratorSampleFailed;
                break;
        }

        if (matrix_multiply(mat_solution, mat_measurements, mat_optimal) != matrixOk)
        {
            calibrator->error = ecompassCalibratorErrInternal;
            matrix_destroy(mat_optimal);
            matrix_destroy(mat_solution);
            matrix_destroy(mat_measurements);
            return ecompassCalibratorSampleFailed;
        }

        coeffs->parameter[1][row][0]	= matrix_get_element(mat_solution, 0, 0);
        coeffs->parameter[1][row][1]	= matrix_get_element(mat_solution, 1, 0);
        coeffs->parameter[1][row][2]	= matrix_get_element(mat_solution, 2, 0);
        coeffs->parameter[0][row][row]	= matrix_get_element(mat_solution, 3, 0);

        switch (type)
        {
            case leastSquaresSimple:
                break;

            case leastSquaresCubicDiagonal:
                coeffs->parameter[3][row][row] = matrix_get_element(mat_solution, 4, 0);
                break;

            case leastSquaresQuadCubicDiagonal:
                coeffs->parameter[3][row][row] = matrix_get_element(mat_solution, 4, 0);
                coeffs->parameter[2][row][row] = matrix_get_element(mat_solution, 5, 0);
                break;

            case leastSquaresCubic:
                coeffs->parameter[3][row][0]	= matrix_get_element(mat_solution, 4, 0);
                coeffs->parameter[3][row][1]	= matrix_get_element(mat_solution, 5, 0);
                coeffs->parameter[3][row][2]	= matrix_get_element(mat_solution, 6, 0);
                break;

            case leastSquaresQuadCubic:
                coeffs->parameter[3][row][0]	= matrix_get_element(mat_solution, 4, 0);
                coeffs->parameter[3][row][1]	= matrix_get_element(mat_solution, 5, 0);
                coeffs->parameter[3][row][2]	= matrix_get_element(mat_solution, 6, 0);
                coeffs->parameter[2][row][0]	= matrix_get_element(mat_solution, 7, 0);
                coeffs->parameter[2][row][1]	= matrix_get_element(mat_solution, 8, 0);
                coeffs->parameter[2][row][2]	= matrix_get_element(mat_solution, 9, 0);
                break;

            case leastSquaresUpToFifthDiagonal:
                coeffs->parameter[2][row][row] = matrix_get_element(mat_solution, 4, 0);
                coeffs->parameter[3][row][row] = matrix_get_element(mat_solution, 5, 0);
                coeffs->parameter[4][row][row] = matrix_get_element(mat_solution, 6, 0);
                coeffs->parameter[5][row][row] = matrix_get_element(mat_solution, 7, 0);
                break;

            case leastSquaresQuadCubicPlusFourthFifthDiagonal:
                coeffs->parameter[3][row][0]	= matrix_get_element(mat_solution, 4, 0);
                coeffs->parameter[3][row][1]	= matrix_get_element(mat_solution, 5, 0);
                coeffs->parameter[3][row][2]	= matrix_get_element(mat_solution, 6, 0);
                coeffs->parameter[2][row][0]	= matrix_get_element(mat_solution, 7, 0);
                coeffs->parameter[2][row][1]	= matrix_get_element(mat_solution, 8, 0);
                coeffs->parameter[2][row][2]	= matrix_get_element(mat_solution, 9, 0);
                coeffs->parameter[4][row][row]  = matrix_get_element(mat_solution, 10, 0);
                coeffs->parameter[5][row][row]  = matrix_get_element(mat_solution, 11, 0);
                break;

        }

        // If calibrating for terms higher than first order, we must regenerate
        // the measurements matrix for each row.
        if (type != leastSquaresSimple)
        {
            matrix_destroy(mat_measurements);
            mat_measurements = NULL;
        }
    }

    // De-normalise the offset
    // Can't denormalise offset because of the cubic term. The parameters must remain normalised
    // and we need to push in normalised measurements when we apply calibration and denomalise then.
    //if ((calibrator->config.sensor == ecompassSensorMagnetometer) && (fabs(calibrator->config.magnetic_field.magnitude) > 0.0))
    //{
    //	coeffs->offset[0] *= calibrator->config.magnetic_field.magnitude;
    //	coeffs->offset[1] *= calibrator->config.magnetic_field.magnitude;
    //	coeffs->offset[2] *= calibrator->config.magnetic_field.magnitude;
    //}

    calibrator->calibration.offset_is_pre_subtracted = 0;

    if ((type == leastSquaresSimple) && pre_subtracted_offset)
    {
        int r, c;

        matrix_t *mat, *offset;

        offset = matrix_create(3, 1);
        for (r = 0; r < 3; r++)
            matrix_set_element(offset, r, 0, coeffs->parameter[0][r][r]);

        mat = matrix_create(3, 3);
        for (r = 0; r < 3; r++)
            for (c = 0; c < 3; c++)
                matrix_set_element(mat, r, c, coeffs->parameter[1][r][c]);

        if (matrix_invert(mat, mat) == matrixOk)
        {
            matrix_multiply(offset, mat, offset);
            coeffs->parameter[0][0][0] = -matrix_get_element(offset, 0, 0);
            coeffs->parameter[0][1][1] = -matrix_get_element(offset, 1, 0);
            coeffs->parameter[0][2][1] = -matrix_get_element(offset, 2, 0);
            calibrator->calibration.offset_is_pre_subtracted = 1;
        }
    }

    // Finish up
    for (i = 0; i <= ECOMPASS_MAX_POWER_TERM; i++)
    {
        coeffs->matrix_is_used[i]	  = 0;
        coeffs->matrix_is_diagonal[i] = 0;
    }

    coeffs->is_valid = 1;
    coeffs->matrix_is_used[0]	  = 1;
    coeffs->matrix_is_diagonal[0] = 1;
    coeffs->matrix_is_used[1]	  = 1;
    coeffs->matrix_is_diagonal[1] = 0;
    switch (type)
    {
        case leastSquaresSimple:
            break;

        case leastSquaresCubicDiagonal:
            coeffs->matrix_is_used[3]	  = 1;
            coeffs->matrix_is_diagonal[3] = 1;
            break;

        case leastSquaresQuadCubicDiagonal:
            coeffs->matrix_is_used[2]	  = 1;
            coeffs->matrix_is_diagonal[2] = 1;
            coeffs->matrix_is_used[3]	  = 1;
            coeffs->matrix_is_diagonal[3] = 1;
            break;

        case leastSquaresCubic:
            coeffs->matrix_is_used[3]	  = 1;
            coeffs->matrix_is_diagonal[3] = 0;
            break;

        case leastSquaresQuadCubic:
            coeffs->matrix_is_used[2]	  = 1;
            coeffs->matrix_is_diagonal[2] = 0;
            coeffs->matrix_is_used[3]	  = 1;
            coeffs->matrix_is_diagonal[3] = 0;
            break;

        case leastSquaresUpToFifthDiagonal:
            coeffs->matrix_is_used[2]	  = 1;
            coeffs->matrix_is_diagonal[2] = 1;
            coeffs->matrix_is_used[3]	  = 1;
            coeffs->matrix_is_diagonal[3] = 1;
            coeffs->matrix_is_used[4]	  = 1;
            coeffs->matrix_is_diagonal[4] = 1;
            coeffs->matrix_is_used[5]	  = 1;
            coeffs->matrix_is_diagonal[5] = 1;
            break;

        case leastSquaresQuadCubicPlusFourthFifthDiagonal:
            coeffs->matrix_is_used[2]	  = 1;
            coeffs->matrix_is_diagonal[2] = 0;
            coeffs->matrix_is_used[3]	  = 1;
            coeffs->matrix_is_diagonal[3] = 0;
            coeffs->matrix_is_used[4]	  = 1;
            coeffs->matrix_is_diagonal[4] = 1;
            coeffs->matrix_is_used[5]	  = 1;
            coeffs->matrix_is_diagonal[5] = 1;
            break;
    }

    calibrator->calibration.concrete.is_valid = 0;
    calibrator->submitted_data_changed_parameters = 1;
    matrix_destroy(mat_measurements);
    matrix_destroy(mat_optimal);
    matrix_destroy(mat_solution);
    calibrator->calibration_complete = 1;

    // FIXME: Calculate magnitude error of each of the points
    calculate_magnitude_error(calibrator);
    return ecompassCalibratorSampleEnded;
}



////////////////////////////////////////////////////////////////////////////////
/// Calculates the RMS of the difference of each of the calibrated measurements
/// from that expected. When the measurements used to generate the calibration
/// are themselved calibrated they are expected to lie on a sphere centred at
/// zero with a certain radius (1 for accelerometer, magnetic field strength for
/// magnetometer).
///
/// \brief Calculates magnitude error of clibration.
/// \param calibrator The target calibrator.
/// \return The calculated RMS error.

static void calculate_magnitude_error(ecompass_calibrator_t *calibrator)
{
    ecompass_sensor_reading_t reading;
    double error, error_sqr_sum, expected_magnitude;
    int i, n;

    calibrator->error_magnitude_rms		= ECOMPASS_INVALID_ERROR;
    calibrator->error_magnitude_max		= ECOMPASS_INVALID_ERROR;

    if (!calibrator || !calibrator->calibration_complete)
        return;

    if (calibrator->config.sensor == ecompassSensorMagnetometer)
    {
        if(fabs(calibrator->config.magnetic_field.magnitude) <= 0.0)
            return;
        expected_magnitude = calibrator->config.magnetic_field.magnitude;
    }
    else
        expected_magnitude = 1.0;

    error_sqr_sum = 0.0;
    for (i = n = 0; i < calibrator->count; i++)
    {
        ecompass_sensor_reading_init_with_uncalibrated(&reading, calibrator->data[i].x, calibrator->data[i].y, calibrator->data[i].z);
        ecompass_calibrate(&reading, 0.0, &calibrator->calibration);

        if (reading.is_calibrated)
        {
            error = fabs(sqrt(reading.x * reading.x + reading.y * reading.y + reading.z * reading.z) - expected_magnitude);
            error_sqr_sum += error * error;
            n++;

            if (ECOMPASS_ERROR_IS_INVALID(calibrator->error_magnitude_max) || (fabs(error) > calibrator->error_magnitude_max))
                calibrator->error_magnitude_max = fabs(error);
        }
    }

    if (n > 0)
        calibrator->error_magnitude_rms = sqrt(error_sqr_sum / (double)n);
}



////////////////////////////////////////////////////////////////////////////////
// Returns number of sample points required to calibrate a given sample point
// configuration. Will return -1 when an arbitrary number of samples is required.
//
// \brief Get number of samples required for a particular point configuration.
// \param points The sample point configuration for which the number of samples
// is required.
// \return The number of sample points required to calibrate the specified
// sample point configuration. -1 represents an arbitrary number of samples.

int ecompass_calibrator_get_num_samples_required(ecompass_calibration_points_t points)
{
    switch (points)
    {
        case ecompassPointsThreeOptimal:							return 3;
        case ecompassPointsFourOptimal:								return 4;
        case ecompassPointsSixOptimal:								return 6;
        case ecompassPointsSixAxisAligned:							return 6;
        case ecompassPointsFourteenAxisAligned:						return 14;
        case ecompassPointsEightOptimal:							return 8;
        case ecompassPointsSixOptimalCubicDiagonal:					return 6;
        case ecompassPointsSixAxisAlignedCubicDiagonal:				return 6;
        case ecompassPointsFourteenAxisAlignedCubicDiagonal:		return 14;
        case ecompassPointsFourteenAxisAlignedQuadCubicDiagonal:	return 14;
        case ecompassPointsEightOptimalCubicDiagonal:				return 8;
        case ecompassPointsEightAlmostOptimalCubicDiagonal:			return 8;
        case ecompassPointsThirtySixOrthodromeCubicDiagonal:		return 36;
        case ecompassPointsThirtySixOrthodromeCubic:				return 36;
        case ecompassPointsThirtySixOrthodromeQuadCubicDiagonal:	return 36;
        case ecompassPointsThirtySixOrthodromeQuadCubic:			return 36;
        case ecompassPointsTwoOptimal:								return 2;
        case ecompassPointsArbitrary:								return -1;

        case ecompassPointsGearBox0D1F3D:
//		case ecompassPointsTest0D1F2F3F:
//		case ecompassPointsTest0D1F2F3F4D5D:
//		case ecompassPointsTest0D1F2D3D4D5D:
//		case ecompassPointsTest0D1F2D3D:
//		case ecompassPointsTest0D1F3D:
//		case ecompassPointsTest0D1F:
            return 36;
//			return 82;
    }
}



////////////////////////////////////////////////////////////////////////////////
// For a calibrator type, gives roll and pitch of optimal orientations for
// each sample point based on the calibrator's configuration.
//
// \brief Get optimal roll and pitch of optimal orientations for a specific reading type.
// \param calibrator The calibrator for which the optimal orientation is required.
// \param idx The reading index. This identifies which reading number (starting
// \param roll Pointer to value that takes the roll for the specified orientation.
// May be NULL without causing an error (will not return a roll).
// \param pitch Pointer to value that takes the pitch for the specified orientation.
// May be NULL without causing an error (will not return a pitch).
// from zero) for which the orientation information is required.
// \param for_show Falg that is set that the pitch and roll values are to be used
// for show and not for calibration. The optimal points are calculated for
// accelerometer calibration which assumes the reference vector (gravity) points
// down. The magnetometer is calibrated against the Earth's magnetic field which
// does not point down the pitch and roll values are with respect to that vector
// (not down). However, when these pitch and roll values are shown to the user,
// for orientation of the probe, they should be given with respect to the
// horizontal plane. In summary. When calibration the acceleromter, this \p for_show
// has no effect but when calibration a magnetometer, setting for_show will
// adjust the pitch and roll values so that the user can orientate the probe
// with respect to horizontal plane and not to the earth's magnetic field (to
// make things easier).
// \return Error code.
// \retval
// \retval ecompassCalibratorOk Orientation information has been successfully
// returned.
// \retval ecompassCalibratorErrBadArg \p idx was out of range for specified
// \p reading type. \p pitch and \p roll remain unchanged.

ecompass_calibrator_err_t ecompass_calibrator_get_optimum_orientation(ecompass_calibrator_t *calibrator, int idx, float *roll, float *pitch, int for_show)
{
    int err = 0;
    float p, r;
    ecompass_calibration_points_t sample_points;

//	if (idx < 0)
//		return ecompassCalibratorErrBadArg;

    switch (calibrator->config.sensor)
    {
        case ecompassSensorUnknown:			return ecompassCalibratorErrBadConfig;
        case ecompassSensorTemperature:		return ecompassCalibratorErrBadConfig;
        case ecompassSensorAccelerometer:	sample_points = calibrator->config.accelerometer_sample_points; break;
        case ecompassSensorMagnetometer:	sample_points = calibrator->config.magnetometer_sample_points;  break;
    }

    switch (sample_points)
    {
        case ecompassPointsTwoOptimal:
            switch (idx)
            {
                case 0: p = -35.0; r =   45.0; break;
                case 1: p =  35.0; r = -135.0; break;
                default : err = 1; break;
            }
            break;

        case ecompassPointsThreeOptimal:
            switch (idx)
            {
                case 0: p =   0.0; r =  45.0; break;
                case 1: p = -45.0; r = 180.0; break;
                case 2: p =  45.0; r =  90.0; break;
                default : err = 1; break;
            }
            break;

        case ecompassPointsFourOptimal:
            switch (idx)
            {
                case 0: p =  39.0; r = -158.0; break;
                case 1: p = -66.0; r =  164.0; break;
                case 2: p =  18.0; r =   66.0; break;
                case 3: p =  -1.0; r =  -44.0; break;
                default : err = 1; break;
            }
            break;

        case ecompassPointsSixOptimal:
        case ecompassPointsSixOptimalCubicDiagonal:
            switch (idx)
            {
                case 0: p =   6.0; r =  -55.0; break;
                case 1: p =  -6.0; r =  125.0; break;
                case 2: p =  20.0; r = -147.0; break;
                case 3: p = -20.0; r =   33.0; break;
                case 4: p = -69.0; r = -128.0; break;
                case 5: p =  69.0; r =   52.0; break;
                default : err = 1; break;
            }
            break;

        case ecompassPointsSixAxisAligned:
        case ecompassPointsSixAxisAlignedCubicDiagonal:
            switch (idx)
            {
                case 0: p =   0.0; r =   0.0; break;
                case 1: p =   0.0; r =  90.0; break;
                case 2: p =   0.0; r = 180.0; break;
                case 3: p =   0.0; r = -90.0; break;
                case 4: p =  90.0; r =   0.0; break;
                case 5: p = -90.0; r =   0.0; break;
                default : err = 1; break;
            }
            break;

        case ecompassPointsFourteenAxisAligned:
        case ecompassPointsFourteenAxisAlignedCubicDiagonal:
        case ecompassPointsFourteenAxisAlignedQuadCubicDiagonal:
        {
            static const float pitch14[14] = { 0,  0,  0,  0,   0,   0,   0,    0,    0,   0,   0,   0,  90, -90 };
            static const float roll14[14]  = { 0, 30, 60, 90, 120, 150, 180, -150, -120, -90, -60, -30,   0,   0 };

            if ((idx >= 0) && (idx < 14))
            {
                p = pitch14[idx];
                r = roll14[idx];
            }
            else
                err = 1;

            break;
        }

        case ecompassPointsThirtySixOrthodromeCubicDiagonal:
        case ecompassPointsThirtySixOrthodromeCubic:
        case ecompassPointsThirtySixOrthodromeQuadCubicDiagonal:
        case ecompassPointsThirtySixOrthodromeQuadCubic:
        {
            static const float pitch36[36] = {
                    0, 30, 60,  90,  60,  30,   0,  -30,  -60, -90, -60, -30,
                    0, 30, 60,  90,  60,  30,   0,  -30,  -60, -90, -60, -30,
                    0,  0,  0,   0,   0,   0,   0,    0,    0,   0,   0,   0 };

            static const float roll36[36] = {
                    0,  0,  0, 180, 180, 180, 180,  180,  180,   0,   0,   0,
                    90, 90, 90, -90, -90, -90, -90,  -90,  -90,  90,  90,  90,
                    0, 30, 60,  90, 120, 150, 180, -150, -120, -90, -60, -30 };

            if ((idx >= 0) && (idx < 36))
            {
                p = pitch36[idx];
                r = roll36[idx];
            }
            else
                err = 1;

            break;
        }

//		case ecompassPointsTest0D1F2F3F:
//		case ecompassPointsTest0D1F2F3F4D5D:
//		case ecompassPointsTest0D1F2D3D4D5D:
//		case ecompassPointsTest0D1F2D3D:
//		case ecompassPointsTest0D1F3D:
//		case ecompassPointsTest0D1F:
        case ecompassPointsGearBox0D1F3D:
        {
            static const float roll_array[36]  = { -0, 102, 180, -120, -66, -18, 30, 72, 114, 150, -174, -132, -96, -60, -30, 6, 42, 78, 108, 144, 180, -144, -108, -72, -36, -0, 42, 84, 126, 168, -138, -84, -24, 48, 150, -0 };
            static const float pitch_array[36] = { -90, -72, -60, -54, -48, -48, -42, -36, -30, -30, -24, -24, -18, -12, -12, -6, -6, -0, -0, 6, 6, 12, 12, 18, 24, 24, 30, 30, 36, 42, 48, 48, 54, 60, 72, 90 };
//			static const float roll_array[82]  = { -0, 102, 174, -126, -72, -24, 18, 60, 96, 138, 168, -156, -126, -96, -66, -36, -6, 24, 48, 78, 102, 126, 156, 180, -156, -132, -108, -84, -60, -36, -12, 12, 36, 60, 84, 108, 126, 150, 174, -162, -138, -120, -96, -72, -48, -24, -6, 18, 42, 66, 90, 114, 138, 162, -174, -150, -126, -102, -78, -48, -24, -0, 30, 54, 84, 114, 144, 174, -156, -126, -90, -54, -18, 18, 60, 102, 150, -156, -96, -24, 78, -0 };
//			static const float pitch_array[82] = { -90, -78, -72, -66, -66, -60, -60, -54, -54, -54, -48, -48, -42, -42, -42, -42, -36, -36, -36, -30, -30, -30, -30, -24, -24, -24, -18, -18, -18, -18, -18, -12, -12, -12, -12, -6, -6, -6, -6, -0, -0, -0, -0, 6, 6, 6, 6, 12, 12, 12, 12, 18, 18, 18, 18, 18, 24, 24, 24, 30, 30, 30, 30, 36, 36, 36, 42, 42, 42, 42, 48, 48, 54, 54, 54, 60, 60, 66, 66, 72, 78, 90 };

            if ((idx >= 0) && (idx < (sizeof(roll_array) / sizeof(roll_array[0]))))
            {
                p = pitch_array[idx];
                r = roll_array[idx];
            }
            else
                err = 1;

            break;
        }

        case ecompassPointsEightOptimal:
        case ecompassPointsEightOptimalCubicDiagonal:
        {
            static const float pitch8[8] = { -35.0, -73.0, 5.0, -16.0,  16.0,   -5.0,  73.0,  35.0 };
            static const float roll8[8]  = { -45.0, 161.0, 17.0, 84.0, -96.0, -163.0, -18.0, 135.0 };

            if ((idx >= 0) && (idx < 8))
            {
                p = pitch8[idx];
                r = roll8[idx];
            }
            else
                err = 1;

            break;
        }

        case ecompassPointsEightAlmostOptimalCubicDiagonal:
        {
            static const float pitch8[8] = {  6.0,  18.0,  36.0,  72.0, 288.0, 324.0, 342.0, 354.0 };
            static const float roll8[8]  = { 18.0, 264.0, 138.0, 342.0, 162.0, 318.0,  84.0, 198.0 };

            if ((idx >= 0) && (idx < 14))
            {
                p = pitch8[idx];
                r = roll8[idx];
            }
            else
                err = 1;

            break;
        }

        case ecompassPointsArbitrary:
            p = r = 0.0;
            break;
    }

    if (err)
        return ecompassCalibratorErrBadArg;

    if ((calibrator->config.sensor == ecompassSensorMagnetometer) && for_show)
    {
        p += 90.0 - calibrator->config.magnetic_field.inclination;
        while (p >  180.0) p -= 360.0;
        while (p < -180.0) p += 360.0;
    }

    if ((calibrator->config.sensor == ecompassSensorAccelerometer) && for_show &&
        (sample_points == ecompassPointsThirtySixOrthodromeCubicDiagonal))
    {
        // Adjust pitch and roll so that it looks as if the probe is flipping onto its back
        if ((idx < 24) && (r >= 180))
        {
            r -= 180;
            p = 180 - p;
        }
        if ((idx < 24) && (r <= -90))
        {
            r += 180;
            p = 180 - p;
        }
    }

    if (pitch)
        *pitch = p;

    if (roll)
        *roll = r;

    return ecompassCalibratorOk;
}



////////////////////////////////////////////////////////////////////////////////
// For a calibrator type, gives string description of optimal orientations for
// each sample point based on the clibrator's configuration.
//
// \brief Get string description of optimal orientations for a calibrator.
// \param calibrator The calibrator for which the optimal orientation is required.
// that need to be done for the calibration and their orientations.
// \param idx The reading index. This identifies which reading number (starting
// \param buf Pointer character buffer that will receive a copy of the description
// string.
// \param size Available space pointed to by \p buf. 26 bytes will be enough to
// hold the longest string. If \p size is less than this, the string will be
// truncated to fit.
// \param enable_utf8 flag to enable TUF8 encoding in output string. If non zero
// string will contain unicode degrees symbol encoded in UTF8 if required.
// \param for_show Falg that is set that the pitch and roll values are to be used
// for show and not for calibration. The optimal points are calculated for
// accelerometer calibration which assumes the reference vector (gravity) points
// down. The magnetometer is calibrated against the Earth's magnetic field which
// does not point down the pitch and roll values are with respect to that vector
// (not down). However, when these pitch and roll values are shown to the user,
// for orientation of the probe, they should be given with respect to the
// horizontal plane. In summary. When calibration the acceleromter, this \p for_show
// has no effect but when calibration a magnetometer, setting for_show will
// adjust the pitch and roll values so that the user can orientate the probe
// with respect to horizontal plane and not to the earth's magnetic field (to
// make things easier).
// \return Error code.
// \retval
// \retval ecompassCalibratorOk Orientation information has been successfully
// returned.
// \retval ecompassCalibratorErrBadArg \p idx was out of range for specified
// \p reading type or \b buf was NULL.

ecompass_calibrator_err_t ecompass_calibrator_get_optimum_orientation_string(ecompass_calibrator_t *calibrator, int idx, char *buf, size_t size, int enable_utf8, int for_show, int show_turns)
{
    float p, r;
    float p_turns = 0, r_turns = 0;

    ecompass_calibrator_err_t err;
//	ecompass_calibration_points_t sample_points;

    switch (calibrator->config.sensor)
    {
        case ecompassSensorUnknown:			return ecompassCalibratorErrBadConfig;
        case ecompassSensorTemperature:		return ecompassCalibratorErrBadConfig;
        case ecompassSensorAccelerometer:	/* sample_points = calibrator->config.accelerometer_sample_points; */ break;
        case ecompassSensorMagnetometer:	/* sample_points = calibrator->config.magnetometer_sample_points;  */ break;
    }

    if (!buf)
        return ecompassCalibratorErrBadArg;

//	if (((sample_points == ecompassPointsSixAxisAligned) || (sample_points == ecompassPointsSixAxisAlignedCubic)) && (calibrator->config.sensor == ecompassSensorAccelerometer))
//	{
//		switch (idx)
//		{
//			case 0: snprintf(buf, size, "Flat on back"); break;
//			case 1: snprintf(buf, size, "On right side"); break;
//			case 2: snprintf(buf, size, "Face down"); break;
//			case 3: snprintf(buf, size, "On left side"); break;
//			case 4: snprintf(buf, size, "Pointing up"); break;
//			case 5: snprintf(buf, size, "Pointing down"); break;
//			default : return ecompassCalibratorErrBadArg;
//		}
//		return ecompassCalibratorOk;
//	}

    if ((err = ecompass_calibrator_get_optimum_orientation(calibrator, idx, &r, &p, for_show)) != ecompassCalibratorOk)
        return err;

    if (show_turns)
    {
        float r_prev, p_prev;
        float r_diff, p_diff;

        if ((err = ecompass_calibrator_get_optimum_orientation(calibrator, idx-1, &r_prev, &p_prev, for_show)) == ecompassCalibratorOk)
        {
            r_diff = (r - r_prev);
            p_diff = (p - p_prev);
        }
        else
        {
            r_diff = r;
            p_diff = p;
        }

        if (r_diff >  180) r_diff -= 360.0;
        if (p_diff >  180) p_diff -= 360.0;
        if (r_diff < -180) r_diff += 360.0;
        if (p_diff < -180) p_diff += 360.0;
        r_turns = (r_diff) / 6.0;
        p_turns = (p_diff) / 6.0;
    }

#ifdef ORIENTATION_TURNS_POSITIVE_ONLY
    while (r_turns < 0.0)
        r_turns += 60.0;
    while (p_turns < 0.0)
        p_turns += 60.0;
#endif

    if (show_turns)
    {
        if (enable_utf8)
            snprintf(buf, size, "Roll %.0f\u00B0 (%.0f), Pitch %.0f\u00B0 (%.0f).", r, r_turns, p, p_turns);
        else
            snprintf(buf, size, "Roll %.0f (%.0f), Pitch %.0f (%.0f).", r, r_turns, p, p_turns);
    }
    else
    {
        if (enable_utf8)
            snprintf(buf, size, "Roll %.0f\u00B0, Pitch %.0f\u00B0.", r, p);
        else
            snprintf(buf, size, "Roll %.0f, Pitch %.0f.", r, p);
    }

    return ecompassCalibratorOk;
}



////////////////////////////////////////////////////////////////////////////////
// For a calibrator type, gives expected ideal sensor value of optimal
// orientations for each sample point based on the calibrator's configuration.
// Optionally returns the associated pitch and roll for the measurement.
//
// \brief Get optimal ideal sensor value of optimal orientations for a specific
// reading type.
// \param calibrator The calibrator for which the optimal orientation is required.
// \param idx The reading index. This identifies which reading number (starting
// \param reading Pointer a sensor reading structure that takes the value for
// the specified orientation. May be NULL without causing an error (will not return a value).
// \param for_show Flag that is set that the pitch and roll values are to be used
// for show and not for calibration. The optimal points are calculated for
// accelerometer calibration which assumes the reference vector (gravity) points
// down. The magnetometer is calibrated against the Earth's magnetic field which
// does not point down the pitch and roll values are with respect to that vector
// (not down). However, when these pitch and roll values are shown to the user,
// for orientation of the probe, they should be given with respect to the
// horizontal plane. In summary. When calibration the acceleromter, this \p for_show
// has no effect but when calibration a magnetometer, setting for_show will
// adjust the pitch and roll values so that the user can orientate the probe
// with respect to horizontal plane and not to the earth's magnetic field (to
// make things easier).
// /param roll Optional pointer to aa float that will be set to the roll for
// the associated orientation. May be NULL.
// /param pitch Optional pointer to aa float that will be set to the pitch for
// the associated orientation. May be NULL.
// \return Error code.
// \retval
// \retval ecompassCalibratorOk Orientation information has been successfully
// returned.
// \retval ecompassCalibratorErrBadArg \p idx was out of range for specified
// \p reading type. \p pitch and \p roll remain unchanged.

ecompass_calibrator_err_t ecompass_calibrator_get_optimum_measurement(ecompass_calibrator_t *calibrator, int idx, ecompass_sensor_reading_t *reading, int for_show, float *roll, float *pitch)
{
    ecompass_calibrator_err_t result;
    float r, p;

    matrix_t	*measurement = NULL;

    if ((result = ecompass_calibrator_get_optimum_orientation(calibrator, idx, &r, &p, for_show)) == ecompassCalibratorOk)
    {
        matrix_t	*vec		 = NULL;
        matrix_t	*rotation	 = NULL;
        double sinR, cosR, sinP, cosP;

        if (roll)
            *roll = r;
        if (pitch)
            *pitch = p;

        if (calibrator->config.sensor == ecompassSensorAccelerometer)
        {
            vec	= matrix_create(3, 1);
            matrix_set_element(vec, 0, 0, 0.0);
            matrix_set_element(vec, 1, 0, 0.0);
            matrix_set_element(vec, 2, 0, 1.0);
        }

        sinR = sin(-r * M_PI / 180.0);
        cosR = cos(-r * M_PI / 180.0);
        sinP = sin(-p * M_PI / 180.0);
        cosP = cos(-p * M_PI / 180.0);

        rotation = matrix_create(3, 3);
        matrix_set_element(rotation, 0, 0, cosP);			matrix_set_element(rotation, 0, 1, 0);		matrix_set_element(rotation, 0, 2, sinP);
        matrix_set_element(rotation, 1, 0, sinR * sinP);	matrix_set_element(rotation, 1, 1, cosR);	matrix_set_element(rotation, 1, 2, -sinR * cosP);
        matrix_set_element(rotation, 2, 0, -cosR * sinP);	matrix_set_element(rotation, 2, 1, sinR);	matrix_set_element(rotation, 2, 2, cosR * cosP);

        if (vec && rotation)
            if ((measurement = matrix_create(3, 1)) != NULL)
                matrix_multiply(measurement, rotation, vec);

        matrix_destroy(vec);
        matrix_destroy(rotation);
    }

    if (measurement == NULL)
        result = ecompassCalibratorErrInternal;

    if (reading && measurement)
    {
        reading->uncalibrated_x = matrix_get_element(measurement, 0, 0);
        reading->uncalibrated_y = matrix_get_element(measurement, 1, 0);
        reading->uncalibrated_z = matrix_get_element(measurement, 2, 0);
        reading->x = reading->uncalibrated_x;
        reading->y = reading->uncalibrated_y;
        reading->z = reading->uncalibrated_z;
        reading->is_valid = 1;
        reading->is_calibrated = 0;
        reading->t = 0.0;
        reading->t_is_valid = 0;
    }

    matrix_destroy(measurement);

    return result;
}



////////////////////////////////////////////////////////////////////////////////
/// Returns the seperation distance for optimally placed readings used by
/// calibrations. this is used for redundancy chacking to reject samples if they
/// lie too close to existing ones. If redundancy chacking is not used by the
/// speacified reading type then -1.0 is returned.
///
/// \brief Seperation distance for reading.
/// \param readings The readings type. This determines the number of readings
/// that need to be done for the calibration and their orientations.
/// \return Seperation distance of optimally placed samples for reading type or
/// -1.0 id not applicable.

static double reading_seperation_distance(ecompass_calibration_points_t readings)
{
    switch (readings)
    {
        case ecompassPointsTwoOptimal:								return  2.00000000000;		// 2
        case ecompassPointsThreeOptimal:							return  1.73205080757;		// 3 / sqrt(3)
        case ecompassPointsFourOptimal:								return  1.63299316186;		// 4 / sqrt(6)
        case ecompassPointsSixOptimal:								return  1.41421356237;		// sqrt(2)
        case ecompassPointsSixAxisAligned:							return  1.41421356237;		// sqrt(2)
        case ecompassPointsFourteenAxisAligned:						return  0.517638090205;		// 2 * sin(30/2)
        case ecompassPointsEightOptimal:							return  1.15470053838;		// 2 / sqrt(3)
        case ecompassPointsSixOptimalCubicDiagonal:					return  1.41421356237;		// sqrt(2)
        case ecompassPointsSixAxisAlignedCubicDiagonal:				return  1.41421356237;		// sqrt(2)
        case ecompassPointsFourteenAxisAlignedCubicDiagonal:		return  0.517638090205;		// 2 * sin(30/2)
        case ecompassPointsFourteenAxisAlignedQuadCubicDiagonal:	return  0.517638090205;		// 2 * sin(30/2)
        case ecompassPointsEightOptimalCubicDiagonal:				return  1.15470053838;		// 2 / sqrt(3)
        case ecompassPointsEightAlmostOptimalCubicDiagonal:			return  1.15470053838 - (0.2 * 1.15470053838);		// (2 / sqrt(3)) - 20%

//		case ecompassPointsTest0D1F2F3F:
//		case ecompassPointsTest0D1F2D3D4D5D:
//		case ecompassPointsTest0D1F2F3F4D5D:
//		case ecompassPointsTest0D1F2D3D:
//		case ecompassPointsTest0D1F3D:
//		case ecompassPointsTest0D1F:
        case ecompassPointsGearBox0D1F3D:
            return  0.312;	// For 36 points
//			return  0.20;	// For 82 points

        case ecompassPointsThirtySixOrthodromeCubic:
        case ecompassPointsThirtySixOrthodromeQuadCubicDiagonal:
        case ecompassPointsThirtySixOrthodromeQuadCubic:
        case ecompassPointsThirtySixOrthodromeCubicDiagonal:		return  0.517638090205;		// 2 * sin(30/2)

        case ecompassPointsArbitrary:								return -1.0;
    }

    return -1.0;
}



#if defined(ECOMPASS_UNIT_TEST)

////////////////////////////////////////////////////////////////////////////////
/// Test calibration against single set of data.
///
/// \brief Test calibration.
/// \return Error count. Number of errors detected
/// \retval 0 No error. All tests passed.

static int ecompass_test_calibrator_single(const char *name, ecompass_calibration_points_t sample_points, ecompass_calibration_points_t *parmaeters_expected, const double *data, int size)
{
	ecompass_calibrator_t			  *calibrator;
	ecompass_calibrator_config_t	  config;
	int i, measurement_count, errs = 0;
	const double max_err = 1e-5;

	if ((calibrator = ecompass_calibrator_create()) == NULL)
	{
		printf("ECOMPASS CALIBRATE TEST: Unable to create calibrator for %s test.\n", name);
		return 1;
	}

	switch (sample_points)
	{
		case ecompassPointsTwoOptimal:					measurement_count = 2; break;
		case ecompassPointsThreeOptimal:				measurement_count = 3; break;
		case ecompassPointsFourOptimal:					measurement_count = 4; break;
		case ecompassPointsSixOptimal:					measurement_count = 6; break;
		case ecompassPointsSixAxisAligned:				measurement_count = 6; break;
		case ecompassPointsEightOptimal:				measurement_count = 8; break;
		case ecompassPointsSixOptimalCubicDiagonal:		measurement_count = 6; break;
		case ecompassPointsSixAxisAlignedCubicDiagonal:	measurement_count = 6; break;
		case ecompassPointsEightOptimalCubicDiagonal:	measurement_count = 8; break;
		case ecompassPointsArbitrary:					measurement_count = floor(size / 3.0); break;
	}

	if (size < (measurement_count * 3.0))
	{
		printf("ECOMPASS CALIBRATE TEST: Not enough supplied measurements for %s test.\n", name);
		return 1;
	}

	ecompass_calibrator_configure_defaults(&config);
	config.sensor = ecompassSensorAccelerometer;
	config.accelerometer_sample_points = sample_points;
	config.stabiliser.disable = 1;
	ecompass_calibrator_configure(calibrator, &config);

	ecompass_calibrator_begin(calibrator);

	for (i = 0; i < measurement_count; i++, data += 3)
	{
		switch (ecompass_calibrator_submit_data(calibrator, data[0], data[1], data[2]))
		{
			case ecompassCalibratorSampleAccepted:
				break;

			case ecompassCalibratorSampleUnstable:
			case ecompassCalibratorSampleWaitingOrientation:
				printf("ECOMPASS CALIBRATE TEST: Measurement %d deemed unstable for %s test.\n", i + 1, name);
				errs++;
				break;

			case ecompassCalibratorSampleUnsuitable:
				printf("ECOMPASS CALIBRATE TEST: Measurement %d rejected for %s test.\n", i + 1, name);
				errs++;
				break;

			case ecompassCalibratorSampleEnded:
				if (i != (measurement_count - 1))
				{
					i = measurement_count - 1;
					printf("ECOMPASS CALIBRATE TEST: Measurement %d ended calibration early for %s test.\n", i + 1, name);
					errs++;
				}
				break;

			case ecompassCalibratorSampleFailed:
				switch (calibrator->error)
				{
					case ecompassCalibratorOk:					printf("ECOMPASS CALIBRATE TEST: The calibration failed but no error occured! Please contact vendor support.\n"); break;
					case ecompassCalibratorErrNotCalibrating:	printf("ECOMPASS CALIBRATE TEST: A sample point was taken but there is no calibration in progress.\n"); break;
					case ecompassCalibratorErrNotEnoughData:	printf("ECOMPASS CALIBRATE TEST: Not enough sample points were taken to calculate the calibration parameters.\n"); break;
					case ecompassCalibratorErrBadConfig:		printf("ECOMPASS CALIBRATE TEST: The calibrator was configured with an invalid combination of parameters.\n"); break;
					case ecompassCalibratorErrInProgress:		printf("ECOMPASS CALIBRATE TEST: Calibration already in progress! This should not happen. Please contact vendor support.\n"); break;
					case ecompassCalibratorErrNotInProgress:	printf("ECOMPASS CALIBRATE TEST: There was no calibration in progress.\n"); break;
					case ecompassCalibratorErrMemory:			printf("ECOMPASS CALIBRATE TEST: A memory allocation failure occured during the calibration process.\n"); break;
					case ecompassCalibratorErrBadArg:			printf("ECOMPASS CALIBRATE TEST: Invalid internal argument! This should not happen. Please contact vendor support.\n"); break;
					case ecompassCalibratorErrNoSolution:		printf("ECOMPASS CALIBRATE TEST: There is no calibration solution for the sample points taken either because some points were redundant (too close to one another) or not enough points were supplied (at least 4 samples are required for a least squares fit of a 12 parameter solution).\n"); break;
					case ecompassCalibratorErrInternal:			printf("ECOMPASS CALIBRATE TEST: An unexpected mathematical error occured while calculating the solution. Please contact vendor support.\n"); break;
				}
				i = measurement_count - 1;
				errs++;
				break;

			case ecompassCalibratorSampleLimitExceeded:
				printf("ECOMPASS CALIBRATE TEST: Measurement %d resulted in too many measurements for %s test.\n", i + 1, name);
				errs++;
				break;
		}
	}

	switch (ecompass_calibrator_end(calibrator))
	{
		case ecompassCalibratorOk:
			break;

		case ecompassCalibratorErrNotEnoughData:
			printf("ECOMPASS CALIBRATE TEST: Not enough data supplied for %s test.\n", name);
			errs++;
			break;

		default:
			printf("ECOMPASS CALIBRATE TEST: Error occured during %s test.\n", name);
			errs++;
			break;
	}

	if (calibrator->parameters.transform_is_valid != parmaeters_expected->transform_is_valid)
	{
		errs++;
		printf("ECOMPASS CALIBRATE TEST: Mismatch for %s. transform_is_valid = %d (expected %d)\n", name, calibrator->parameters.transform_is_valid, parmaeters_expected->transform_is_valid);
	}
	if (calibrator->parameters.offset_is_valid != parmaeters_expected->offset_is_valid)
	{
		errs++;
		printf("ECOMPASS CALIBRATE TEST: Mismatch for %s. offset_is_valid = %d (expected %d)\n", name, calibrator->parameters.offset_is_valid, parmaeters_expected->offset_is_valid);
	}
	if (calibrator->parameters.cubic_is_valid != parmaeters_expected->cubic_is_valid)
	{
		errs++;
		printf("ECOMPASS CALIBRATE TEST: Mismatch for %s. cubic_is_valid = %d (expected %d)\n", name, calibrator->parameters.cubic_is_valid, parmaeters_expected->cubic_is_valid);
	}

	for (i = 0; i < 9; i++)
	{
		if (fabs(calibrator->parameters.transform[i] - parmaeters_expected->transform[i]) >= max_err)
		{
			errs++;
			printf("ECOMPASS CALIBRATE TEST: Mismatch for %s. transform[%d] = %g (expected %g)   err = %g\n", name, i, calibrator->parameters.transform[i], parmaeters_expected->transform[i], fabs(calibrator->parameters.transform[i]-parmaeters_expected->transform[i]));
		}
	}

	for (i = 0; i < 3; i++)
	{
		if (fabs(calibrator->parameters.offset[i] - parmaeters_expected->offset[i]) >= max_err)
		{
			errs++;
			printf("ECOMPASS CALIBRATE TEST: Mismatch for %s. offset[%d] = %g (expected %g)   err = %g\n", name, i, calibrator->parameters.offset[i], parmaeters_expected->offset[i], fabs(calibrator->parameters.offset[i]-parmaeters_expected->offset[i]));
		}
	}

	for (i = 0; i < 3; i++)
	{
		if (fabs(calibrator->parameters.cubic[i] - parmaeters_expected->cubic[i]) >= max_err)
		{
			errs++;
			printf("ECOMPASS CALIBRATE TEST: Mismatch for %s. cubic[%d] = %g (expected %g)   err = %g\n", name, i, calibrator->parameters.cubic[i], parmaeters_expected->cubic[i], fabs(calibrator->parameters.cubic[i]-parmaeters_expected->cubic[i]));
		}
	}

	ecompass_calibrator_destroy(calibrator);
	return errs;
}



////////////////////////////////////////////////////////////////////////////////
// Test calibration against worked examples from FreeScale app not AN4399.
//
// \brief Test calibration.
// \return Error count. Number of errors detected
// \retval 0 No error. All tests passed.

int ecompass_test_calibrator(void)
{
	ecompass_calibration_parameters_t	params;
	int errs = 0;

	static const double measurements1[12] =
	{
		-0.5943247, -0.2385511, -0.7171391,
		 0.923716,   0.0971252, -0.362616,
		-0.2464314,  0.8726869,  0.3728409,
		 0.0308571, -0.6703601,  0.6943732
	};

	static const double measurements2[18] =
	{
		-0.092481, -0.782347,  0.545699,
		 0.158219,  0.815862, -0.552812,
		-0.317050, -0.461321, -0.779078,
		 0.371912,  0.491359,  0.764908,
		 0.931625, -0.280464, -0.199491,
		-0.884923,  0.323469,  0.198084
	};

	static const double measurements3[24] =
	{
		 0.570473, -0.567765,  0.559660,
		 0.964165,  0.077288, -0.251511,
		-0.044326,  0.287155,  0.924159,
		 0.321782,  0.938677,  0.095200,
		-0.263805, -0.910211, -0.111366,
		 0.114158, -0.254345, -0.932391,
		-0.920104, -0.037448,  0.260660,
		-0.514526,  0.609291, -0.573760
	};

	ecompass_calibration_parameters_init(&params);
	params.transform[0]   =  1.02354; params.transform[1]   = -0.02844; params.transform[2]   = -0.00383;
	params.transform[3]   =  0.03721; params.transform[4]   =  1.02203; params.transform[5]   =  0.01036;
	params.transform[6]   = -0.02188; params.transform[7]   = -0.00511; params.transform[8]   =  1.02816;
	params.offset[0] = -0.03054; params.offset[1] = -0.01777; params.offset[2] =  0.00255;
	params.cubic[0]  =  0.0;     params.cubic[1]  =  0.0;     params.cubic[2]  =  0.0;
	params.transform_is_valid = 1;
	params.offset_is_valid    = 1;
	params.cubic_is_valid	  = 0;
	errs += ecompass_test_calibrator_single("4 optimal point, 12 parameter", ecompassPointsFourOptimal, &params, measurements1, 12);

	ecompass_calibration_parameters_init(&params);
	params.transform[0]   =  1.01996; params.transform[1]   = -0.02633; params.transform[2]   =  0.00415;
	params.transform[3]   =  0.03330; params.transform[4]   =  1.02498; params.transform[5]   =  0.01562;
	params.transform[6]   = -0.01821; params.transform[7]   = -0.00264; params.transform[8]   =  1.03058;
	params.offset[0] = -0.02796; params.offset[1] = -0.01907; params.offset[2] =  0.00445;
	params.cubic[0]  =  0.0;     params.cubic[1]  =  0.0;     params.cubic[2]  =  0.0;
	params.transform_is_valid = 1;
	params.offset_is_valid    = 1;
	params.cubic_is_valid	  = 0;
	errs += ecompass_test_calibrator_single("6 optimal point, 12 parameter", ecompassPointsSixOptimal, &params, measurements2, 18);

	ecompass_calibration_parameters_init(&params);
	params.transform[0]   =  1.02571; params.transform[1]   = -0.02758; params.transform[2]   =  0.00259;
	params.transform[3]   =  0.03867; params.transform[4]   =  1.04124; params.transform[5]   =  0.01373;
	params.transform[6]   = -0.01294; params.transform[7]   = -0.00286; params.transform[8]   =  1.03461;
	params.offset[0] = -0.02851; params.offset[1] = -0.01864; params.offset[2] =  0.00438;
	params.cubic[0]  = -0.00852; params.cubic[1]  = -0.02428; params.cubic[2]  = -0.01002;
	params.transform_is_valid = 1;
	params.offset_is_valid    = 1;
	params.cubic_is_valid	  = 1;
	errs += ecompass_test_calibrator_single("8 optimal point, 15 parameter", ecompassPointsEightOptimalCubic, &params, measurements3, 24);

	if (errs == 0)
		printf("ECOMPASS CALIBRATE TEST: No errors detected.\n");

	return errs;
}

#endif



