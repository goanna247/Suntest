//
// Created by goann on 13/06/2024.
//

#include "ecompass.h"
#include <math.h>


#ifndef LIBTEST_ECOMPASS_CALIBRATOR_H
#define LIBTEST_ECOMPASS_CALIBRATOR_H


////////////////////////////////////////////////////////////////////////////////
//
//  ecompass_calibrator.h
//
//  Created by Martin Louis on 8/11/12.
//  Copyright (c) 2012 Martin Louis. All rights reserved.
//



/// Macro defining an invalid value for error measurements
#define ECOMPASS_INVALID_ERROR			NAN

/// Macro testing for invalid value of error measurements
#define ECOMPASS_ERROR_IS_INVALID(e)	(isnan(e))



////////////////////////////////////////////////////////////////////////////////
/// Defines the number of sample points used for a calibration and how they are used.

typedef enum
{
    /// Two readings at optimal orientations for accelerometer calibration as
    /// per Freescale app note AN4399
    ecompassPointsTwoOptimal,

    /// Three readings at optimal orientations for accelerometer calibration as
    /// per Freescale app note AN4399
    ecompassPointsThreeOptimal,

    /// Four readings at optimal orientations for accelerometer calibration as
    /// per Freescale app note AN4399
    ecompassPointsFourOptimal,

    /// Six readings at optimal orientations for accelerometer calibration as
    /// per Freescale app note AN4399
    ecompassPointsSixOptimal,

    /// Six readings aligned along the positive and negative direction of each
    /// X, Y and Z axis. Used to calibrate devices that can easily be oriented
    /// on the front, back, top, bottom and sides on a flat surface.
    ecompassPointsSixAxisAligned,

    /// Similar to ecompassPointsSixAxisAligned except with eight additional
    /// orientations added at 30 degree roll intervals to the 0, 90, 180 & 270.
    ecompassPointsFourteenAxisAligned,

    /// Eight readings at optimal orientations for accelerometer calibration as
    /// per Freescale app note AN4399
    ecompassPointsEightOptimal,

    /// Same as ecompassPointsSixOptimal except generates a 15 parameter calibration
    /// including a cubic scaling term for non-linearities.
    ecompassPointsSixOptimalCubicDiagonal,

    /// Same as ecompassPointsSixAxisAligned except generates a 15 parameter calibration
    /// including a cubic scaling term for non-linearities.
    ecompassPointsSixAxisAlignedCubicDiagonal,

    /// Similar to ecompassPointsSixAxisAlignedCubic except with eight additional
    /// orientations added at 30 degree roll intervals to the 0, 90, 180 & 270.
    ecompassPointsFourteenAxisAlignedCubicDiagonal,

    /// Sane as ecompassPointsSixAxisAlignedCubicDiagonal but generates a solution with
    /// second order diagonal term.
    ecompassPointsFourteenAxisAlignedQuadCubicDiagonal,

    /// Same as ecompassPointsEightOptimal except generates a 15 parameter calibration
    /// including a cubic scaling term for non-linearities.
    ecompassPointsEightOptimalCubicDiagonal,

    /// 15 parameter calibration based on 36 measurements as 3 sets of 12 each arranged
    /// in an orthodrome (great cicle) in the axial planes.
    ecompassPointsThirtySixOrthodromeCubicDiagonal,

    /// Calibration based on 36 measurements as 3 sets of 12 each arranged
    /// in an orthodrome (great cicle) in the axial planes with full cubic term.
    ecompassPointsThirtySixOrthodromeCubic,

    /// Calibration based on 36 measurements as 3 sets of 12 each arranged
    /// in an orthodrome (great cicle) in the axial planes with qudratic and cubic diagaonal terms
    ecompassPointsThirtySixOrthodromeQuadCubicDiagonal,

    /// Calibration based on 36 measurements as 3 sets of 12 each arranged
    /// in an orthodrome (great cicle) in the axial planes with full qudratic and cubic terms
    ecompassPointsThirtySixOrthodromeQuadCubic,

    /// Just like ecompassPointsEightOptimalCubicDiagonal (as per Freescale app note AN4399) except orientations
    /// have been adjusted to the nearest attainable positions on our calibration rig.
    ecompassPointsEightAlmostOptimalCubicDiagonal,

    // MGL DEBUG
    /// Test calibration.
//	ecompassPointsTest0D1F2F3F,
//	ecompassPointsTest0D1F2D3D4D5D,
//	ecompassPointsTest0D1F2F3F4D5D,
//	ecompassPointsTest0D1F2D3D,
//	ecompassPointsTest0D1F3D,
//	ecompassPointsTest0D1F,
    ecompassPointsGearBox0D1F3D,

    /// An arbitrary number of readings at arbitrary orientations. The
    /// calibration algorithm will decide when it has enough data for accurate
    /// calibration. Used mainly to calibrate magnetometer.
    ecompassPointsArbitrary

} ecompass_calibration_points_t;



////////////////////////////////////////////////////////////////////////////////
/// Configuration options for calibrator. Application can fill in one of the
/// structures, then call ecompass_calibrator_configure() to apply the options
/// (the calibrator will make a copy of the data so the original can be
/// destoryed after the call). This structure can be initialised with defaults
/// by passing it to ecompass_calibrator_configure_defaults().

typedef struct
{
    /// Specifies which sensor type is being calibrated.
    /// Default value is ecompassCalibrateSensorUnknown.
    ecompass_sensor_t	sensor;

    /// The type of reading used to calibrate the accelerometer. This determines
    /// the number of expected readings and how the calibrator is to use them
    /// to calibrate the accelerometer.
    ecompass_calibration_points_t	accelerometer_sample_points;

    /// The type of reading used to calibrate the magnetometer. This determines
    /// the number of expected readings and how the calibrator is to use them
    /// to calibrate the magnetometer.
    ecompass_calibration_points_t	magnetometer_sample_points;

    /// Limits the maximum number of samples that the calibrator can accomodate.
    /// A zero value means no limit. Default value is zero. If a reading is
    /// submitted that would cause this limit to be exceeded,
    size_t	sample_limit;

    /// Collection of stabiliser configuration options. Calibration of
    /// accellerometers required the samples to be free of any induced
    /// acceleration (ie measuring gravitational acceleration only). Normally
    /// the application would simply keep throwing sensor readings from the
    /// acceleromter at the calibrator. The calibrator uses a "stabiliser" to
    /// determine when the data settles down and becomes useable. It will tell
    /// the application when a sample has been accepted, wait for the device
    /// to move to a new orientation stabilise, then stabilise in the new
    /// position before accepting the next sample. The stabiliser can be disabled
    /// in which case the calibrator will assume that each sample submitted
    /// by the application is implicitly stable and will be used for calibration.
    /// The stabiliser is not used in magnetometer calibration.
    struct
    {
        /// Forces the data sample "stabiliser" to be disabled when it would normally
        /// be used. Default value is 0.
        unsigned int disable:1;

        /// The data sample stabiliser (see stabiliser_disable) works by inspecting
        /// the most recent samples and determining their spread from their mean
        /// value in each of the x, y and z components. If all samples are within
        /// a specified limit, the reading is considered stable. This value
        /// determines the number of samples considered.
        /// Default value is 10.
        int sample_count;

        /// The data sample stabiliser (see stabiliser_disable) works by inspecting
        /// the most recent samples and determining their spread from their mean
        /// value in each of the x, y and z components. If all samples are within
        /// a specified limit, the reading is considered stable. This value
        /// determines tthis maximum spread. The value is in the same units as
        /// the submitted data which usually is g's (acceleration due to gravity).
        /// Default value is 0.01.
        double maximum_spread;

        /// Hysteresis for stabilisation. This specifies an additional multiple of
        /// the spread to be considered for determining when the reading become
        /// unstable. The reading is considered stable if the most recent
        /// samples all fall within the designated spread of their mean.
        /// Subsequently the reading is considered unstable when any one of the
        /// points lies more then the spread PLUS the speread multiplied by
        /// the hysteresis. This prevents porderline cases from rapidly toggling
        /// state. Ie:
        /// Becomes unstable if all (point - mean) <= (spread).
        /// Becomes stable if any (point - mean) > (spread * (1 + hysteresis)).
        /// A value of 0.0 effectively disables hysteresis. Default value is 1.0.
        double hysteresis;

    } stabiliser;

    /// Information about the reference magnetic field when calibration magnetometers.
    struct
    {
        /// Magnitude of the reference magnetic field that a magnetometer is
        /// being calibrated to.
        double	magnitude;

        /// Inclination below the horizontal plane in degrees for the reference
        /// magnetic field that a magnetometer is being calibrated to.
        double	inclination;
    } magnetic_field;

    /// Temporary hack to allow temperature compensated calibration of accelerometer
    /// until it can be done properly. Allows a sequence of calibrations to be done
    /// as temperature changes.
//	unsigned int temperature_compensated;

} ecompass_calibrator_config_t;



////////////////////////////////////////////////////////////////////////////////
/// Return values for ecompass_calibrator_submit_data()

typedef enum
{
    /// Sample was accepted. Calibration still ongoing.
    ecompassCalibratorSampleAccepted,

    /// Sample was rejected because values are unstable and hasn't settled yet.
    /// Calibration still ongoing. This value is only returned if the calibrator
    /// is using its stabiliser.
    ecompassCalibratorSampleUnstable,

    /// Sample was is stable but was rejected because a sample has already been
    /// accepted for this particular orientation. The stabiliser is waiting for
    /// the device to be moved to a new orientation before waiting for the
    /// readings to stabilise again. Calibration still ongoing. This value is
    /// only returned if the calibrator is using its stabiliser.
    ecompassCalibratorSampleWaitingOrientation,

    /// Sample was a valid, stable reading but unsuitable for calibration.
    /// because it is either too close to a previous sample (happens when the
    /// operator puts the device into the same orientastion more than once) or
    /// if the reading is too far from its expected value (happens when the
    /// device is grossly misaligned). Calibration still ongoing. This value
    /// can be used to notify the operator of device misalignment.
    ecompassCalibratorSampleUnsuitable,

    /// Calibration has successfully completed. The calibraqtor has enough
    /// samples and has calulated the calibration parameters.
    ecompassCalibratorSampleEnded,

    /// An error has occured that will prevent the calibration from completing
    /// successfully. The error field of the calibrator will provide more
    /// information. This value is most commonly returned due to bad
    /// configuration of the calibrator.
    ecompassCalibratorSampleFailed,

    /// The sample could not be accepted because it would have reculted in the
    /// calibrator exceeding its configured capacity.
    ecompassCalibratorSampleLimitExceeded

} ecompass_calibrator_submit_t;



////////////////////////////////////////////////////////////////////////////////
/// Calibrator errors codes returned by most calibrator functions. All functions
/// operating on a ecompass_calibrator_t will set its error field to one of
/// these values.

typedef enum
{
    ecompassCalibratorOk,					///< No error.
    ecompassCalibratorErrNotCalibrating,	///< Sample data submitted when no calibration was in progress.
    ecompassCalibratorErrNotEnoughData,		///< The calibration has been ended but not enough sensor readings were submitted for an suitable calibration.
    ecompassCalibratorErrBadConfig,			///< Calibrator is not correctly configured (usually sensor type not set).
    ecompassCalibratorErrInProgress,		///< Operation cannot proceed because there is a calibration currently in progress.
    ecompassCalibratorErrNotInProgress,		///< Operation cannot proceed because there is no calibration currently in progress.
    ecompassCalibratorErrMemory,			///< Internal memory allocation failure.
    ecompassCalibratorErrBadArg,			///< Invalid argument passed by application to calibrator function.
    ecompassCalibratorErrNoSolution,		///< No solution to calibration. Usually not enough or degenerate sample points to solve for the required number of unknowns.
    ecompassCalibratorErrInternal			///< Invalid internal state was encountered. Should never happen.
} ecompass_calibrator_err_t;



////////////////////////////////////////////////////////////////////////////////
/// Data point used by calibrator

typedef struct
{
    double x;	///< X axis component of data
    double y;	///< Y axis component of data
    double z;	///< Z axis component of data
} ecompass_calibrator_data_t;



////////////////////////////////////////////////////////////////////////////////
/// Stabiliser structure is used to monitor submitted data values and determine
/// if they have become stable (settled down to a near constant value). Used
/// to reject accelerometer data taken when the device is in motion.

typedef struct
{
    ecompass_calibrator_data_t *data;			///< Rolling circular buffer of samples used to detect stability
    unsigned int				capacity;		///< Number of elements that the data array can store.
    unsigned int				count;			///< Number of elements currently stored in the data array.
    unsigned int				put;			///< Put index in circular data buffer.
    unsigned int				get;			///< Get index in circular data buffer.
    double						maximum_spread;	///< The maximum spread allowed for the sample in the data buffer to be consifdered stable (see ecompass_calibrator_config_t).
    double						hysteresis;		///< Hysteresis factor (see ecompass_calibrator_config_t).
    unsigned int				is_stable:1;	///< Flag indicating that data is currently considered stable.
} ecompass_calibrator_stabiliser_t;



////////////////////////////////////////////////////////////////////////////////
/// Context structure for an ecompass calibrator

typedef struct
{
    ecompass_calibrator_data_t			*data;			///< Rolling circular buffer of samples used to detect stability
    int									capacity;		///< Number of elements that the data array can store.
    int									count;			///< Number of elements currently stored in the data array.
    ecompass_calibrator_config_t		config;			///< Configuration options for calibrator.
    ecompass_calibrator_err_t			error;			///< Error codes set by calls to ecompass_calibrator_submit_data() or ecompass_calibrator_end().
    ecompass_calibration_t				calibration;	///< Calibration parameters generated by this calibrator

    /// Stabiliser used to monitor submitted samples for automatic accelerometer
    /// calibration modes and determine wether data has settled and therfore
    /// useable.
    ecompass_calibrator_stabiliser_t stabiliser;

    /// Flag indicating that a calibration is in progress. This should not be
    /// modified by the application.
    unsigned int calibration_in_progress:1;

    /// Flag indicating that current calibration is complete.
    unsigned int calibration_complete:1;

    /// Flag indicating that a the calibration constants need to be recalulated
    /// because more data has become available or configuration has changed.
    unsigned int needs_recalculation:1;

    /// If a new reading is too close to an existing reading it can be rejected
    /// as redundant (usually due to the operator mistakenly putting the
    /// device into the same orientation twice). This value determines how
    /// close samples may be. The application should not modify this. The
    /// calibrator will set the value appropriately when begining a calibratio
    /// based on the reading type. A negative value indicates that redundancy
    /// checking is disabled.
    double redundancy_limit_squared;

    /// Flag indicating that the most recently submitted data sample resulted in
    /// an update of the calibration parameters.
    unsigned int submitted_data_changed_parameters:1;

    /// Flag indicating that last submitted data had an assigned orientation
    unsigned int last_orientation_is_valid:1;

    /// Assigned roll of last submitted data
    float last_roll;

    /// Assigned pitch of last submitted data
    float last_pitch;

    /// Assigned azimuth of last submitted data
    float last_azimuth;

    /// RMS error for magnitude of calibrated points.
    double error_magnitude_rms;

    /// Maximum error for magnitude of calibrated points.
    double error_magnitude_max;

} ecompass_calibrator_t;



////////////////////////////////////////////////////////////////////////////////
/// Creates an ecompass calibrator and initialises it.
///
/// \brief Create an ecompass calibrator.
/// \return Pointer to new ecompas calibrator.

ecompass_calibrator_t *ecompass_calibrator_create(void);



////////////////////////////////////////////////////////////////////////////////
/// Destroys an ecompass calibrator and releases all related resources. On
/// return the tructure pointed to by \p calibrator will no longer be valid.
///
/// \brief Destroy an ecompass calibrator.
/// \param calibrator Pointer to the calibrator to destroy.

void ecompass_calibrator_destroy(ecompass_calibrator_t *calibrator);



////////////////////////////////////////////////////////////////////////////////
/// Sets a structure of calibrator configuration options to their default values.
/// Such a structure still neeeds to be applied to calibrator using
/// ecompass_calibrator_configure() to take effect.
///
/// \brief Set calibrator configuration options to default values.
/// \param config A structure to be initialised with default values.
/// \return Error code.
/// \retval ecompassCalibratorOk Default configuration parameters were successfully applied.
/// \retval ecompassCalibratorErrBadArg \p config was NULL.

ecompass_calibrator_err_t ecompass_calibrator_configure_defaults(ecompass_calibrator_config_t *config);



////////////////////////////////////////////////////////////////////////////////
/// Sets configuration options for a calibrator from a structure passed by the.
/// application. The calibrator copies this data into itself so the application
/// may destory or reuse the structure after this function returns. If NULL
/// is specified for \p config, the calibrator's options will be set to default.
/// Note that the default sensor config is unknown so this still needs to be
/// explicitly set for the calibrator to be functional. Configuring a calibrator
/// will fail if there is any calibration in progress at the time. It must be
/// aborted (or ended) first.
///
/// The application may set a blank ecompass_calibrator_config_t structure to
/// defaults using ecompass_calibrator_configure_defaults().
///
/// \brief Configure a calibrator.
/// \param calibrator Calibrator that will get its mode changed.
/// \param config A structure containing the configuration options to be applied
/// A value of NULL will set the target \p calibrator optione do default.
/// \return Error code.
/// \retval ecompassCalibratorOk Configuration parameters were successfully set.
/// \retval ecompassCalibratorErrBadArg \p calibrator was NULL.
/// \retval ecompassCalibratorErrInProgress Configuration cannot be applied
/// because a calibration is currently in progress.

ecompass_calibrator_err_t ecompass_calibrator_configure(ecompass_calibrator_t *calibrator, ecompass_calibrator_config_t *config);



////////////////////////////////////////////////////////////////////////////////
/// Returns the reading type that the calibrator is configured for. This
/// determines the calibration procedure and depends in the sensor currently
/// being calibrated.
///
/// \brief Readings type (ecompass_calibration_readings_t) that the calibrator
/// is configured to use.
/// \param calibrator The calibrator.
/// \return The reading type that the calibrator is configured to use.

ecompass_calibration_points_t ecompass_calibrator_reading_type(ecompass_calibrator_t *calibrator);


////////////////////////////////////////////////////////////////////////////////
/// Starts a new calibration operation. Will abort any calibration currently in
/// progress and clear its buffered samples.
///
/// \brief Begins new calibration.
/// \param calibrator Pointer to the calibrator.
/// \return Error code.
/// \retval ecompassCalibratorOk Calibration was successfully started.
/// \retval ecompassCalibratorErrBadArg \p calibrator was NULL.
/// \retval ecompassCalibratorErrMemory Internal memory allocation failure.

ecompass_calibrator_err_t ecompass_calibrator_begin(ecompass_calibrator_t *calibrator);



////////////////////////////////////////////////////////////////////////////////
/// Finalises a calibration that is currently in progres. Samples gathered so
/// far are retained as a courtesy to the application in case it needs them
/// (eg to plot samples). They will be cleared when the next calibration begins
/// or when ecompass_calibrator_clear() is called.
///
/// \brief Ends a calibration.
/// \param calibrator Pointer to the calibrator.
/// \return Error code.
/// \retval ecompassCalibratorOk Calibration was successfully ended and
/// calibration results are valid.
/// \retval ecompassCalibratorErrBadArg \p calibrator was NULL.
/// \retval ecompassCalibratorErrNotEnoughData Calibration has ended but there
/// were not enough valid sensor readings to calculate any valid calibration
/// parameters.
/// \retval ecompassCalibratorErrBadConfig Calibration has ended but no valid
/// calibration parameters could be calculated because the calibrator is
/// incorrectly configured.

ecompass_calibrator_err_t ecompass_calibrator_end(ecompass_calibrator_t *calibrator);



////////////////////////////////////////////////////////////////////////////////
/// Aborts a calibration currently in progress. Samples gathered so far are
/// retained as a courtesy to the application in case it needs them (eg to plot
/// samples) but the calibrator will not finalise the current calibration.
/// They will be cleared when the next calibration begins or when
/// ecompass_calibrator_clear() is called.
///
/// \brief Aborts a calibration.
/// \param calibrator Pointer to the calibrator.
/// \return Error code.
/// \retval ecompassCalibratorOk Calibration was successfully aborted.
/// \retval ecompassCalibratorErrBadArg \p calibrator was NULL.

ecompass_calibrator_err_t ecompass_calibrator_abort(ecompass_calibrator_t *calibrator);



////////////////////////////////////////////////////////////////////////////////
/// Clears any currently held data samples used by the calibrator. The sampled
/// data are held in order to calculate the calibration constants. Even though
/// they not needed once the calibration is complete, they are retained as a
/// courtesy to the application in case it needs them (eg to plot samples).
/// Calling this function will release all of these points. Clearing data
/// will fail if a calibration is currently in progress. It must be ended or
/// aborted first.
///
/// \brief Release buffered calibration samples.
/// \param calibrator Pointer to the calibrator.
/// \return Error code.
/// \retval ecompassCalibratorOk Calibration data cleared.
/// \retval ecompassCalibratorErrBadArg \p calibrator was NULL.
/// \retval ecompassCalibratorErrInProgress Could not clear calibration data
/// because a calibration is currently in progress.

ecompass_calibrator_err_t ecompass_calibrator_clear(ecompass_calibrator_t *calibrator);



////////////////////////////////////////////////////////////////////////////////
/// Submits a data sample to calibrator. The calibrator will pre-process the
/// sample (filtering and testing for stability if required) and adds the sample
/// to the sample set and recalulating calibration parameters if appropriate.
/// The result member of \p calibrator to indicate how it went.
///
/// \brief Submits data sample to calibrator.
/// \param calibrator Pointer to the calibrator.
/// \param x The x axis component of data sample.
/// \param y The y axis component of data sample.
/// \param z The z axis component of data sample.
/// \return Indication of whether the data was accepted and if not, why not.
/// See ecompass_calibrator_submit_t for details.

ecompass_calibrator_submit_t ecompass_calibrator_submit_data(ecompass_calibrator_t *calibrator, double x, double y, double z);



////////////////////////////////////////////////////////////////////////////////
/// Undoes last submitted measurement by removing it.
///
/// \brief Removes last submitted sample point.
/// \param calibrator Pointer to the calibrator.
/// \return Submission status. See ecompass_calibrator_submit_t for details.
/// Returns ecompassCalibratorSampleFailed if \p calbrator was NULL or it had
/// no measurements to undo otherwise it returns value is the same as
/// reasons as ecompass_calibrator_calculate().

ecompass_calibrator_submit_t ecompass_calibrator_undo_last(ecompass_calibrator_t *calibrator);



////////////////////////////////////////////////////////////////////////////////
/// Debugging functio that forces calibrator to accept the next measurement and
/// advance. Only has an effect when there is a calibration in progress.
///
/// \brief Forces calibrator to accept next measurement.

//void ecompass_calibrator_debug_force_accept_next_measurement(ecompass_calibrator_t *calibrator);



////////////////////////////////////////////////////////////////////////////////
/// Returns the number of data points the calibrator is curretly holding.
///
/// \brief Data count for calibrator.
/// \param calibrator Pointer to the calibrator.
/// \return Number of data points currently held by \p calibrator.

int ecompass_calibrator_data_count(ecompass_calibrator_t *calibrator);



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

int ecompass_calibrator_will_exceed_data_limit(ecompass_calibrator_t *calibrator);



////////////////////////////////////////////////////////////////////////////////
/// The calibrator may use a stabiliser for certain modes (specificaly when auto
/// calibrating the acceleromter) in order to determine if the data is usable.
/// This function may be helpful to determine if the stabiliser needs configuring
/// for a calibration or to provide operator feedback that the data has become
/// stable and accepted so they can move the device to a new orientation.
///
/// \brief Indicates if a calibrator uses stabiliser in current mode.
/// \param calibrator Pointer to calibrator.
/// \return Boolean indication that \p calibrator uses a stabiliser.
/// \retval 0 The calibrator does NOT use a stabiliser.
/// \retval 1 The calibrator does use a stabiliser.

int ecompass_calibrator_uses_stabiliser(ecompass_calibrator_t *calibrator);



////////////////////////////////////////////////////////////////////////////////
/// Returns a boolean indicating wither a particular configuration would use
/// a stabiliser if it were enabled.
///
/// \brief Indicates if a calibrator config would stabiliser if allowed to.
/// \param config Pointer to calibrator configuration to test.
/// \return Boolean indication that \p calibrator uses a stabiliser.
/// \retval 0 The calibrator config would NOT use a stabiliser.
/// \retval 1 The calibrator config would use a stabiliser.

int ecompass_calibrator_would_use_stabiliser(ecompass_calibrator_config_t *config);



////////////////////////////////////////////////////////////////////////////////
/// Resets the stabiliser by clearing buffered data and setting into a specified
/// state. Can be used to "un-pause" a calibration and forget about
///
/// \brief Reset stabiliser.
/// \param calibrator The calibrator whose stabiliser is to be reset.
/// \param is_stable The state that the calibrator should be set to. If set to
/// zero, the stabiliser will be unstable and will wait for the measurements to
/// stablise before accepting a value. If set to non-zero, the stabiliser will be
/// stable and wait for the measurements to move FIRST and then re-stabilise
/// before accepting a value.
/// \return Error code.
/// \retval ecompassCalibratorOk The calibrator's stabiliser has been reset/
/// \retval ecompassCalibratorErrBadArg \p calibrator was NULL.

int ecompass_calibrator_reset_stabiliser(ecompass_calibrator_t *calibrator, int is_stable);



////////////////////////////////////////////////////////////////////////////////
/// Returns number of sample points required to calibrate a given sample point
/// configuration. Will return -1 when an arbitrary number of samples is required.
///
/// \brief Get number of samples required for a particular point configuration.
/// \param points The sample point configuration for which the number of samples
/// is required.
/// \return The number of sample points required to calibrate the specified
/// sample point configuration. -1 represents an arbitrary number of samples.

int ecompass_calibrator_get_num_samples_required(ecompass_calibration_points_t points);



////////////////////////////////////////////////////////////////////////////////
/// For a calibrator type, gives roll and pitch of optimal orientations for
/// each sample point based on the calibrator's configuration.
///
/// \brief Get optimal roll and pitch of optimal orientations for a specific reading type.
/// \param calibrator The calibrator for which the optimal orientation is required.
/// \param idx The reading index. This identifies which reading number (starting
/// \param roll Pointer to value that takes the roll for the specified orientation.
/// May be NULL without causing an error (will not return a roll).
/// \param pitch Pointer to value that takes the pitch for the specified orientation.
/// May be NULL without causing an error (will not return a pitch).
/// from zero) for which the orientation information is required.
/// \param for_show Falg that is set that the pitch and roll values are to be used
/// for show and not for calibration. The optimal points are calculated for
/// accelerometer calibration which assumes the reference vector (gravity) points
/// down. The magnetometer is calibrated against the Earth's magnetic field which
/// does not point down the pitch and roll values are with respect to that vector
/// (not down). However, when these pitch and roll values are shown to the user,
/// for orientation of the probe, they should be given with respect to the
/// horizontal plane. In summary. When calibration the acceleromter, this \p for_show
/// has no effect but when calibration a magnetometer, setting for_show will
/// adjust the pitch and roll values so that the user can orientate the probe
/// with respect to horizontal plane and not to the earth's magnetic field (to
/// make things easier).
/// \return Error code.
/// \retval ecompassCalibratorOk Orientation information has been successfully
/// returned.
/// \retval ecompassCalibratorErrBadArg \p idx was out of range for specified
/// \p reading type. \p pitch and \p roll remain unchanged.

ecompass_calibrator_err_t ecompass_calibrator_get_optimum_orientation(ecompass_calibrator_t *calibrator, int idx, float *roll, float *pitch, int for_show);



////////////////////////////////////////////////////////////////////////////////
/// For a calibrator type, gives string description of optimal orientations for
/// each sample point based on the calibrator's configuration.
///
/// \brief Get string description of optimal orientations for a calibrator.
/// \param calibrator The calibrator for which the optimal orientation is required.
/// \param idx The reading index. This identifies which reading number (starting
/// \param buf Pointer character buffer that will receive a copy of the description
/// string.
/// \param size Available space pointed to by \p buf. 26 bytes will be enough to
/// hold the longest string. If \p size is less than this, the string will be
/// truncated to fit.
/// \param enable_utf8 flag to enable TUF8 encoding in output string. If non zero
/// string will contain unicode degrees symbol encoded in UTF8 if required.
/// \param for_show Flag that is set that the pitch and roll values are to be used
/// for show and not for calibration. The optimal points are calculated for
/// accelerometer calibration which assumes the reference vector (gravity) points
/// down. The magnetometer is calibrated against the Earth's magnetic field which
/// does not point down the pitch and roll values are with respect to that vector
/// (not down). However, when these pitch and roll values are shown to the user,
/// for orientation of the probe, they should be given with respect to the
/// horizontal plane. In summary. When calibration the acceleromter, this \p for_show
/// has no effect but when calibration a magnetometer, setting for_show will
/// adjust the pitch and roll values so that the user can orientate the probe
/// with respect to horizontal plane and not to the earth's magnetic field (to
/// make things easier).
/// \return Error code.
/// \retval
/// \retval ecompassCalibratorOk Orientation information has been successfully
/// returned.
/// \retval ecompassCalibratorErrBadArg \p idx was out of range for specified
/// \p reading type or \b buf was NULL.

ecompass_calibrator_err_t ecompass_calibrator_get_optimum_orientation_string(ecompass_calibrator_t *calibrator, int idx, char *buf, size_t size, int enable_utf8, int for_show, int show_turns);



////////////////////////////////////////////////////////////////////////////////
/// For a calibrator type, gives expected ideal sensor value of optimal
/// orientations for each sample point based on the calibrator's configuration.
/// Optionally returns the associated pitch and roll for the measurement.
///
/// \brief Get optimal ideal sensor value of optimal orientations for a specific
/// reading type.
/// \param calibrator The calibrator for which the optimal orientation is required.
/// \param idx The reading index. This identifies which reading number (starting
/// \param reading Pointer a sensor reading structure that takes the value for
/// the specified orientation. May be NULL without causing an error (will not return a value).
/// \param for_show Flag that is set that the pitch and roll values are to be used
/// for show and not for calibration. The optimal points are calculated for
/// accelerometer calibration which assumes the reference vector (gravity) points
/// down. The magnetometer is calibrated against the Earth's magnetic field which
/// does not point down the pitch and roll values are with respect to that vector
/// (not down). However, when these pitch and roll values are shown to the user,
/// for orientation of the probe, they should be given with respect to the
/// horizontal plane. In summary. When calibration the acceleromter, this \p for_show
/// has no effect but when calibration a magnetometer, setting for_show will
/// adjust the pitch and roll values so that the user can orientate the probe
/// with respect to horizontal plane and not to the earth's magnetic field (to
/// make things easier).
/// /param roll Optional pointer to aa float that will be set to the roll for
/// the associated orientation. May be NULL.
/// /param pitch Optional pointer to aa float that will be set to the pitch for
/// the associated orientation. May be NULL.
/// \return Error code.
/// \retval
/// \retval ecompassCalibratorOk Orientation information has been successfully
/// returned.
/// \retval ecompassCalibratorErrBadArg \p idx was out of range for specified
/// \p reading type. \p pitch and \p roll remain unchanged.

ecompass_calibrator_err_t ecompass_calibrator_get_optimum_measurement(ecompass_calibrator_t *calibrator, int idx, ecompass_sensor_reading_t *reading, int for_show, float *roll, float *pitch);



#if defined(ECOMPASS_UNIT_TEST)

////////////////////////////////////////////////////////////////////////////////
/// Test calibration against worked examples from FreeScale app not AN4399.
///
/// \brief Test calibration.
/// \return Error count. Number of errors detected
/// \retval 0 No error. All tests passed.

int ecompass_test_calibrator(void);

#endif





#endif //LIBTEST_ECOMPASS_CALIBRATOR_H
