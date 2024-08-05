//
// Created by goann on 13/06/2024.
//

#ifndef LIBTEST_ECOMPASS_H
#define LIBTEST_ECOMPASS_H


////////////////////////////////////////////////////////////////////////////////
///
/// \file   ecompass.h
/// \brief  Electronic compass. Calculates orientation from sensor values.
/// \author Martin Louis
/// \date   Created: 2012-11-8
///
/// Calculates orientation as roll pitch and azimuth from accelerometer and magnetometer
/// sensor readings. Is able to calibrate the sensor values if calibration parameters are
/// supplied. Functions in the ecompass_store module encode and decode these parameters
/// to and from memory images. Can optionally calculate variance of the input sensor
/// readings over several samples and provide a measure of stability of these values.
///
/// \defgroup ecompass_group eCompass.

#include <stdlib.h>
#include <time.h>

/// \ingroup ecompass_group
/// \{



////////////////////////////////////////////////////////////////////////////////
// Compile time option to enable test functions. Force disabled if no debug.

#undef  ECOMPASS_DEBUG					///< Enable output of general debugging
#undef  ECOMPASS_DEBUG_CALCULATION		///< Enable output of pitch & roll calculations
#undef  ECOMPASS_UNIT_TEST				///< Enable testing code.

/// If enabled this will allow the ecompass to measure the stability of the accelerometer
/// and magnetometer values by recording the variance and maximum deviation of the most
/// recent number of samples (configurable)
#define ECOMPASS_SENSOR_VARIANCE

/// Enables variance calculation per axis if ECOMPASS_SENSOR_VARIANCE is defined,
/// otherwise variance is just calculated on the magnitude of the sensor reading.
#define ECOMPASS_SENSOR_VARIANCE_PER_AXIS

// Switch off debug options if not doing debug.
#if !defined(DEBUG)
#undef ECOMPASS_UNIT_TEST
#undef ECOMPASS_DEBUG
#undef ECOMPASS_DEBUG_CALCULATION
#endif

#if defined(ECOMPASS_SENSOR_VARIANCE)
#include <stdint.h>
#include <string>

#define ECOMPASS_STABLE_ACC_DEVIATION		0.0004	///< This defines the stability threshold level for accelerometer readings if we are calculating sensor variance.
#define ECOMPASS_STABLE_MAG_DEVIATION		0.0375	///< This defines the stability threshold level for magnetometer readings if we are calculating sensor variance.
#endif



////////////////////////////////////////////////////////////////////////////////
// Configuration.

// MGL DEBUG
#define ECOMPASS_MAX_POWER_TERM					5			///< Fifth order polynomial (4 coefficients) as function of uncalibrated measurement
//#define ECOMPASS_MAX_POWER_TERM				3			///< Third order polynomial (4 coefficients) as function of uncalibrated measurement
#define ECOMPASS_MAX_TEMPERATURE_POWER_TERM		1			///< First order polynomial (2 coefficients) as function of temperature
#define ECOMPASS_MAX_SCALAR_POWER_TERM			1			///< First order polynomial (2 coefficients) as function of uncalibrated measurement
#define ECOMPASS_SENSOR_VARIANCE_MAX_SAMPLES	50			///< The maximum number of samples supported for calculating sensor variance if ECOMPASS_SENSOR_VARIANCE is defined.

#define ECOMPASS_INVALID_TEMPERATURE			-300.0			///< A value representing an invalid temperature
#define ECOMPASS_TEMPERATURE_IS_VALID(t)		((t) > -273.0)	///< Tests a value to determine if it represents a valid temperature.

/// Number of manual offset slots used. Manual zero offsets allow the operator to "zero"
/// a value for display. Currently only roll can be zeroed. The manual_zero_offset array
/// stores these offsets
#define ECOMPASS_MANUAL_ZERO_OFFSET_COUNT		1
#define ECOMPASS_MANUAL_ZERO_ROLL_OFFSET		0	///< Identifies the manual zero offset slot for roll on the accelerometer sensor.

#define ECOMPASS_MANUAL_ZERO_CLEAR				0	///< Used by ecompass_set_zero_offset() to clear a manual zero offset
#define ECOMPASS_MANUAL_ZERO_SET_AUTO			1	///< Used by ecompass_set_zero_offset() to set a manual zero offset automatically based on the current orientation
#define ECOMPASS_MANUAL_ZERO_SET_VALUE			2	///< Used by ecompass_set_zero_offset() to set a manual zero offset to a specific value using default units
#define ECOMPASS_MANUAL_ZERO_SET_VALUE_DEGREES	3	///< Used by ecompass_set_zero_offset() to set a manual zero offset to a specific value in degrees
#define ECOMPASS_MANUAL_ZERO_SET_VALUE_RADIANS	4	///< Used by ecompass_set_zero_offset() to set a manual zero offset to a specific value in radians

#define ECOMPASS_MAX_NAME_LEN					32	///< Maximum name of an ecompass name.



////////////////////////////////////////////////////////////////////////////////
/// A data type used to represent vector reading from accelerometer and
/// magnetometer sensor with appropriate meta-data.

typedef struct
{
    double			x, y, z;			///< Components of reading (may be calibrated or uncalibrated depending on is_calibrated).
    float           t;					///< Temperature of reading on this sensor.
    double			uncalibrated_x;		///< Uncalibrated X component of reading.
    double			uncalibrated_y;		///< Uncalibrated X component of reading.
    double			uncalibrated_z;		///< Uncalibrated X component of reading.
    unsigned int	is_valid:1;			///< Flag indicating that this vector (x, y, z) represents a valid value.
    unsigned int	is_calibrated:1;	///< Flag indicating that this vector (x, y, z) represents a calibrated value
    unsigned int	t_is_valid:1;		///< Flag indicating that temperature is valid
} ecompass_sensor_reading_t;



////////////////////////////////////////////////////////////////////////////////
/// A data type used to represent scalar reading from TEMPERATURE sensor with
/// appropriate meta-data.

typedef struct
{
    double value;					///< The scalar's value (may be calibrated or uncalibrated depending on is_calibrated).
    double uncalibrated_value;		///< The scalar's uncalibrated value
    unsigned int is_valid:1;		///< Flag indicating that the value is valid.
    unsigned int is_calibrated:1;	///< Flag indicating that the value is calibrated.
} ecompass_scalar_reading_t;



////////////////////////////////////////////////////////////////////////////////
/// A data type representing orientation in the form of roll, pitch and azimuth
/// angles (in radians) applied as axial rotations in that order. Flags indicate
/// the validity of each component (valid roll and pitch can be calculated from
/// accelerometer only but a valid azimuth also requires a valid magnetometer
/// reading).

typedef struct
{
    double			roll;					///< Roll angle in radians [-PI,PI]. 0 = parallel to horizontal plane.
    double			pitch;					///< Pitch angle in radians [-PI/2,PI/2]. 0 = parallel to horizontal plane.
    double			azimuth;				///< Azimuth (aka yaw) in radians [0,2*PI]. 0 = aligned with local geomagnetic field (ie north).
    unsigned int	roll_is_valid:1;		///< Flag indicating that roll is valid.
    unsigned int	pitch_is_valid:1;		///< Flag indicating that pitch is valid.
    unsigned int	azimuth_is_valid:1;		///< Flag indicating that azimuth is valid.
    double			roll_for_display;		///< Roll angle formatted for display with an alternate range and zero point.
} ecompass_orientation_t;



////////////////////////////////////////////////////////////////////////////////
/// Specifies a sensor to be calibrated.

typedef enum
{
    ecompassSensorUnknown = 0,		///< Unknown or undefined sensor.
    ecompassSensorAccelerometer,	///< Accelerometer
    ecompassSensorMagnetometer,		///< Magnetometer
    ecompassSensorTemperature		///< Temperature
} ecompass_sensor_t;



#ifdef ECOMPASS_SENSOR_VARIANCE
////////////////////////////////////////////////////////////////////////////////
/// Specifies which noise metric is returned by ecompass_noise_metric().
typedef enum
{
    ecompassNoiseAccelerometerMean,				///< Represents accelerometer magnitude mean.
    ecompassNoiseAccelerometerVariance,			///< Represents accelerometer magnitude variance.
    ecompassNoiseAccelerometerMaxDeviation,		///< Represents the maximum deviation of the accelerometer magnitude from the mean observed of the most recent samples.
    ecompassNoiseMagnetometerMean,				///< Represents magnetometer magnitude mean.
    ecompassNoiseMagnetometerVariance,			///< Represents magnetometer magnitude variance.
    ecompassNoiseMagnetometerMaxDeviation,		///< Represents the maximum deviation of the magnetometer magnitude from the mean observed of the most recent samples.
#ifdef ECOMPASS_SENSOR_VARIANCE_PER_AXIS
    ecompassNoiseAccelerometerXMean,			///< Represents accelerometer X axis mean.
    ecompassNoiseAccelerometerXVariance,		///< Represents accelerometer X axis variance.
    ecompassNoiseAccelerometerXMaxDeviation,	///< Represents the maximum deviation of the accelerometer X axis from the mean observed of the most recent samples.
    ecompassNoiseMagnetometerXMean,				///< Represents accelerometer Y axis mean.
    ecompassNoiseMagnetometerXVariance,			///< Represents accelerometer Y axis variance.
    ecompassNoiseMagnetometerXMaxDeviation,		///< Represents the maximum deviation of the accelerometer Y axis from the mean observed of the most recent samples.
    ecompassNoiseAccelerometerYMean,			///< Represents accelerometer Z axis mean.
    ecompassNoiseAccelerometerYVariance,		///< Represents accelerometer Z axis variance.
    ecompassNoiseAccelerometerYMaxDeviation,	///< Represents the maximum deviation of the accelerometer Z axis from the mean observed of the most recent samples.
    ecompassNoiseMagnetometerYMean,				///< Represents magnetometer X axis mean.
    ecompassNoiseMagnetometerYVariance,			///< Represents magnetometer X axis variance.
    ecompassNoiseMagnetometerYMaxDeviation,		///< Represents the maximum deviation of the magnetometer X axis from the mean observed of the most recent samples.
    ecompassNoiseAccelerometerZMean,			///< Represents magnetometer Y axis mean.
    ecompassNoiseAccelerometerZVariance,		///< Represents magnetometer Y axis variance.
    ecompassNoiseAccelerometerZMaxDeviation,	///< Represents the maximum deviation of the magnetometer Y axis from the mean observed of the most recent samples.
    ecompassNoiseMagnetometerZMean,				///< Represents magnetometer Z axis mean.
    ecompassNoiseMagnetometerZVariance,			///< Represents magnetometer Z axis variance.
    ecompassNoiseMagnetometerZMaxDeviation,		///< Represents the maximum deviation of the magnetometer Z axis from the mean observed of the most recent samples.
#endif
} ecompass_noise_metric_t;
#endif



////////////////////////////////////////////////////////////////////////////////
/// Structure of calibration coefficients. Represents a concrete set of
/// calibration coefficients calculated for a particular temperature. Is also
/// used to store

typedef struct
{
    /// The temperature for which the coefficients are valid. This is only
    /// meaningful when the coeffs represent a concrete set. When the coeffs
    /// represent temperature compensated polynomial terms, they apply as a
    /// group over a range of temperatures. If the reading temperature differs
    /// from this, the coeffs are invalidated, causing them to be recalculated
    /// for the appropriate temperature.
    float temperature;

    /// Calibration parameters organised as an array of 3x3 martrices. Each of these
    /// matrix is the coefficient of a power of unclibrated measurement (∈ ℝ3).
    double parameter[ECOMPASS_MAX_POWER_TERM + 1][3][3];

    /// Flag indicating whether each power term matrix is used in the final calculation.
    uint8_t matrix_is_used[ECOMPASS_MAX_POWER_TERM + 1];

    /// Flag indicating whether each power term matrix is diagonal. This is mainly to
    /// reduce storeage requirements when parameters are packed into memory but also
    /// marginally speeds up calculation
    uint8_t matrix_is_diagonal[ECOMPASS_MAX_POWER_TERM + 1];

    /// Flag indicating that the coefficients as a whole are valid. When
    /// representing concrete coefficients, clearing this will result in a
    /// recalculation.
    unsigned int is_valid:1;

} ecompass_calibration_coeffs_t;



////////////////////////////////////////////////////////////////////////////////
/// Calibration parameters. Stores the calibration model and associated parameter
/// values. An ecompass_calibration_parameters_t structure can be initialised
/// using the function compass_calbration_parameters_init() that sets the model
/// to none, offset vector to zero, and gain to the identity matrix.
///
/// \see ecompass_calibration_model_t compass_calbration_init()

typedef struct
{
    /// Identifies the sensor that this calibration applies to. This alue is not
    /// used for much other tahn diagnostics.
    ecompass_sensor_t sensor;

    /// A concrete set of calibration coefficients calculate for a particular
    /// temperature.
    ecompass_calibration_coeffs_t concrete;

    /// An array of calibration coefficients. Each element represent the
    /// coefficients for the associated power of temperature in the temperature
    /// compensation polynomial. These values are used to calulate a concrete
    /// set of calibration parameters for a reading once the temperature is
    /// known.
    ecompass_calibration_coeffs_t term[ECOMPASS_MAX_TEMPERATURE_POWER_TERM + 1];

    /// The the maximum temperature difference allowed between a reading and the
    /// temperature of the concrete coefficients before the coefficients are
    /// recalulated for the new temperature.
    float temperature_threshold;

    /// Flag indicating that offset temperature polynomial co-efficients are known and valid
    unsigned int offset_is_pre_subtracted:1;

} ecompass_calibration_t;




////////////////////////////////////////////////////////////////////////////////
/// Calibration parameters for scalar values (eg temperature). Calibration
/// consistes of a polynomial function: calibrated = poly(uncalibrated).
///
/// \see ecompass_calibration_model_t compass_calibration_scalar_init()

typedef struct
{
    /// Identifies the sensor that this calibration applies to. This alue is not
    /// used for much other tahn diagnostics.
    ecompass_sensor_t sensor;

    /// Terms for the polynomial.
    double term[ECOMPASS_MAX_SCALAR_POWER_TERM + 1];

    /// Validity flag
    unsigned int is_valid:1;
} ecompass_scalar_calibration_t;



////////////////////////////////////////////////////////////////////////////////
/// Structure representing the context or state of a single ecompass. Most
/// functions require a pointer to such a structure but if a NULL context is
/// specified, the functions will fall back to a shared global context.
/// Ie. If you are managing a single ecompass, you can just pass NULL as the
/// ecompass_t parameter. If you are managing multiple ecompasses,  you will need
/// to declare a ecompass_t for each one and pass it to the functions as
/// appropriate

typedef struct
{
    // Name of ecompass
    char name[ECOMPASS_MAX_NAME_LEN + 1];

    /// Current accelerometer reading. Components are normally in g's but since
    /// they are normalised by the calculations it donsn't really matter.
    /// This value is set by the application using ecompass_accelerometer_reading()
    /// and should not be set directly.
    /// The default state has its is_valid member set to 0 and the x,y,z
    /// components undefined.
    ecompass_sensor_reading_t		accelerometer;

    /// Current magnetometer reading. Components are normally in micro teslas
    /// but since the calculations don't rely on the magnitude it doesn't really
    /// matter AS LONG AS the units are consistent with those used in the
    /// calibration.
    /// This value is set by the application using ecompass_magnetometer_reading()
    /// and should not be set directly.
    /// The default state has its is_valid member set to 0 and the x,y,z
    /// components undefined.
    ecompass_sensor_reading_t		magnetometer;

    /// Calculated orientation of the ecompass. This is automatically updated
    /// whenever the ecompas state changes (ie. accelerometer and/or magnetometer
    /// readings updated, ecompass calibration or options changed).
    /// Also recalculated whenerver calibration parameters change.
    /// The default state has its roll_is_valid, pitch_is_valid and
    /// azimuth_is_valid members set to 0 and roll, pitch, azimuth undefined.
    ecompass_orientation_t	orientation;

    /// Temperature of ecompass as a whole
    ecompass_scalar_reading_t temperature;

    /// Calibration parameters used to generate calibrate accelerometer value
    /// from raw input.
    ecompass_calibration_t accelerometer_calibration;

    /// Calibration parameters used to generate calibrate magnetometer value
    /// from raw input.
    ecompass_calibration_t magnetometer_calibration;

    /// Calibration parameters for temperature.
    ecompass_scalar_calibration_t temperature_calibration;

    /// Timestamp for most recent update to accelerometer or magnetometer calibration.
    time_t calibration_timestamp;

    /// Flag indicating that calibration_timestamp is valid.
    unsigned int calibration_timestamp_is_valid:1;

    /// Flag indicating that roll stablisation should be disabled active. This
    /// application using ecompass_roll_stabilisation_enable() and should not
    /// is set by the be set directly. Default state is 0.
    /// See roll_stabilisation_enable() function for discussion.
    unsigned int			roll_stabilisation_disable:1;

    /// Factor used by rool stablisation when active. This is set by the
    /// application using ecompass_roll_stabilisation_factor() and should not
    /// be set directly. Default value is 0.01
    /// See roll_stabilisation_enable() function for discussion.
    double					roll_stabilisation_factor;

    /// Flag indicating that uncompensated (for device tilt) azimuth are to be
    /// considered valid. See allow_uncompensated_azimuth() function for
    /// discussion.
    unsigned int			allow_uncompensated_azimuth:1;

#if defined(ECOMPASS_MANUAL_ZERO_OFFSET_COUNT) && (ECOMPASS_MANUAL_ZERO_OFFSET_COUNT > 0)
    // Zero point for roll_for_display.
    double manual_zero_offset[ECOMPASS_MANUAL_ZERO_OFFSET_COUNT];
#endif


#ifdef ECOMPASS_SENSOR_VARIANCE
    double	mean_accelerometer;				///< Calculated accelerometer mean
    double	mean_magnetometer;				///< Calculated magnetometer mean
    double	variance_accelerometer;			///< Calculated accelerometer variance
    double	variance_magnetometer;			///< Calculated magnetometer variance
    double	max_deviation_accelerometer;	///< Maximum accelerometer deviation from mean
    double	max_deviation_magnetometer;		///< Maximum magnetometer deviation from mean
    uint8_t	magnetometer_is_stable;			///< Flag indicating that magnetometer is stable
    uint8_t	accelerometer_is_stable;		///< Flag indicating that accelerometer is stable
    int		 variance_use_number;			///< Flag used to select calculation by number of measurements or duration
    uint64_t variance_duration_usec;		///< Duration over which calculations are considered
    int      variance_number;				///< Number of measurements over which calculations are considered
    unsigned int variance_needs_update:1;	///< Variances and deviations need recalculating
    int variance_put;						///< Variance buffer put index
    int variance_get;						///< Variance buffer get index
    int variance_count;						///< Number of elements in variance buffer

#ifdef ECOMPASS_SENSOR_VARIANCE_PER_AXIS
    double	mean_accelerometer_axis[3];				///< Calculated accelerometer mean per axis
    double	mean_magnetometer_axis[3];				///< Calculated magnetometer mean per axis
    double	variance_accelerometer_axis[3];			///< Calculated accelerometer variance per axis
    double	variance_magnetometer_axis[3];			///< Calculated magnetometer variance per axis
    double	max_deviation_accelerometer_axis[3];	///< Maximum accelerometer deviation from mean per axis
    double	max_deviation_magnetometer_axis[3];		///< Maximum magnetometer deviation from mean per axis
    uint8_t	magnetometer_is_stable_axis[3];			///< Flag indicating that magnetometer is stable per axis
    uint8_t	accelerometer_is_stable_axis[3];		///< Flag indicating that accelerometer is stable per axis
#endif

    /// Buffer holds recent sensor values for variance and deviation calculation
    struct
    {
        double		 acc_x, acc_y, acc_z;	///< Accelerometer sensor value
        double		 mag_x, mag_y, mag_z;	///< Magnetometer sensor value
        uint64_t	 timestamp;				///< Timestamp for sensor values (usec)
        unsigned int acc_is_valid:1;		///< Flag indicating that accelerometer sensor values are valid
        unsigned int mag_is_valid:1;		///< Flag indicating that magnetometer sensor values are valid

    } variance_buffer[ECOMPASS_SENSOR_VARIANCE_MAX_SAMPLES];
#endif
} ecompass_t;



/// Flag indicating that roll_for_display ranges from [-180:180] instead of [0:360]
/// Global value that applies to all ecompasses.
extern unsigned int ecompass_roll_for_display_range_is_symmetric;



////////////////////////////////////////////////////////////////////////////////
/// A "default" ecompass context used when functions that require a context are
/// given NULL as a parameter.

extern ecompass_t ecompass_shared_instance;



////////////////////////////////////////////////////////////////////////////////
/// Initialises sensor reading to default values.
///
/// \brief Initialises sensor reading.
/// \param reading Pointer to the reading to initialise.

void ecompass_sensor_reading_init(ecompass_sensor_reading_t *reading);

static std::string calibrateAllData(std::string data);

////////////////////////////////////////////////////////////////////////////////
/// Initialises scalar reading to default values.
///
/// \brief Initialises sensor reading.
/// \param reading Pointer to the reading to initialise.

void ecompass_scalar_reading_init(ecompass_scalar_reading_t *reading);



////////////////////////////////////////////////////////////////////////////////
/// Initialises orientation to default values.
///
/// \brief Initialises sensor reading.
/// \param orientation Pointer to the orientation to initialise.

void ecompass_orientation_init(ecompass_orientation_t *orientation);



////////////////////////////////////////////////////////////////////////////////
/// Initialises sensor reading to default values and populates it with an
/// initial uncalibrated value.
///
/// \brief Initialises sensor reading with uncalibrated reading.
/// \param reading Pointer to the reading to initialise.
/// \param x X component of uncalibrated reading.
/// \param y Y component of uncalibrated reading.
/// \param z Z component of uncalibrated reading.

void ecompass_sensor_reading_init_with_uncalibrated(ecompass_sensor_reading_t *reading, double x, double y, double z);



////////////////////////////////////////////////////////////////////////////////
/// Initialises calibration coeffs by setting them all to sero and invalidating
/// them.
///
/// \brief Initialise calibration coefficients.
/// \param coeffs Pointer to the coefficients to initialise.

void ecompass_calibration_coeffs_init(ecompass_calibration_coeffs_t *coeffs);



////////////////////////////////////////////////////////////////////////////////
/// Initialises calibration parameters by setting the model to
/// ecompassCalibrationModelNone, the offset vector to zero, and the gain to the
/// identity matrix. This results in parameters that have no effect in the input.
///
/// \brief Initialise calibration parameters.
/// \param cal Pointer to the parameters to initialise.
/// \param sensor The sensor that the calibration belongs to.

void ecompass_calibration_init(ecompass_calibration_t *cal, ecompass_sensor_t sensor);



////////////////////////////////////////////////////////////////////////////////
/// Scalar calibration parameters are empty if they have not been assigned any parameters.
///
/// \brief Inicates if scalar calibration parameters are empty.
/// \param param Pointer to the parameters to test.
/// \return Bollean indicating if \p param is uninitialised.
/// \retval 1 \p is empty (uninitialised)
/// \retval 0 \p has been assigned parameters.

int ecompass_scalar_calibration_is_empty(ecompass_scalar_calibration_t *cal);



////////////////////////////////////////////////////////////////////////////////
/// Calibration parameters are empty if they have not been assigned any parameters.
///
/// \brief Idicated if cxalibration parameters are empty.
/// \param param Pointer to the parameters to test.
/// \return Bollean indicating if \p param is uninitialised.
/// \retval 1 \p is empty (uninitialised)
/// \retval 0 \p has been assigned parameters.

int ecompass_calibration_is_empty(ecompass_calibration_t *cal);



////////////////////////////////////////////////////////////////////////////////
/// Initialises scalar calibration parameters. This results in parameters that
/// have no effect in the input.
///
/// \brief Initialise calibration parameters.
/// \param param Pointer to the parameters to initialise.

void ecompass_scalar_calibration_init(ecompass_scalar_calibration_t *cal, ecompass_sensor_t sensor);

//std::vector<uint8_t> buildCalibrationData(ecompass_t *ecompass, uint8_t* inputBuffer, size_t inputBufferlen);
std::string buildCalibrationData(ecompass_t *ecompass, uint8_t* inputBuffer, size_t inputBufferlen);
////////////////////////////////////////////////////////////////////////////////
/// Applies calibration parameters a ecompass_sensor_reading_t structure.
/// Assumes that the uncalibrated x, y, and z components have been populated and
/// the is_valid flag MUST be set. Will populate the calibrated x, y, z components
/// and sets the is_calibrated flag if appropriate. Will also set the temperature
/// fields if \p termperature is valid.
///
/// \brief Calculate a calibrated value.
/// \param reading Pointer to ecompass_sensor_reading_t structure with uncalibrated
/// x, y, z fields poulated. The is_valid MUST be set.
/// \param temperature The temperature that the unclibrated point was taken at
/// If ECOMPASS_TEMPERATURE_IS_VALID(\p temperature) then tempeature compensation
/// will be attempted.
/// \param param Calibration parameters to use.

void ecompass_calibrate(ecompass_sensor_reading_t *reading, float temperature, ecompass_calibration_t *cal);

static std::string ecompass_show(const ecompass_t *ecompass);
static std::string ecompass_show_calibration(const ecompass_calibration_t *cal, const char *title, int concrete);
static std::string ecompass_show_calibration_coeffs(const ecompass_calibration_t *cal, const char *title, int concrete);
static std::string ecompass_show_calibration_coeff(const ecompass_calibration_t *cal, int concrete, int power, int row, int col);

////////////////////////////////////////////////////////////////////////////////
/// Initialises the specified ecompass context. If NULL is passes, the globally
/// shared context is initialised. Not that the globally shared context is
/// automatially initialised so this only needs to be done if you want to
/// reset it to defaults.
///
/// \brief Initialises an ecompass context to default state.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context is initialised.

void ecompass_init(ecompass_t *ecompass);



////////////////////////////////////////////////////////////////////////////////
/// Sets the nanme for an ecomapss. The name is not really significant. It is
/// only used when printing diagnostics.
///
/// \brief Sets the name for an ecompass.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context is initialised.
/// \param name Name for eCompass.

void ecompass_set_name(ecompass_t *ecompass, const char *name);



////////////////////////////////////////////////////////////////////////////////
/// Invalidates the reading values of an ecompass. This includes the accelerometer
/// magnetometer, temperature and orientation values.
///
/// \brief Invalidate reading.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context is invalidated.

void ecompass_invalidate_reading(ecompass_t *ecompass);



////////////////////////////////////////////////////////////////////////////////
/// Sets all of the manual zero offsets to a value of zero.
///
/// \brief Clear manual zero offsets.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context is set to zero.

void ecompass_clear_manual_zero_offsets(ecompass_t *ecompass);



////////////////////////////////////////////////////////////////////////////////
/// Roll stabilisation is required in cases where the pitch angle becomes
/// vertical (ie. +/- PI) and the roll axis coincides with the gravitation
/// vector (down) and so can't be calculated. The first problem this causes is
/// that roll is undefined at vertical pitch. The second problem is that NEAR
/// vertical pitch the accelerometer components used to calulate roll become
/// which causes them to be swamped by sensor noise. A common solution in
/// aerospace application is to use a modified the accelelrometer z component
/// for roll calculations which adds a small multiple of the x component.
/// This ensures that compensation is only seen near vertial pitch where x >> z.
/// The effect of this compensation is to drive roll smoothly to zero as pitch
/// becomes vertial. Smaller values of the stabilisation factor (the mulitple of
/// x added to z) result in smaller errors in roll near vertical (due to less
/// compensation) but greater susceptibilty to sensor noise in the accelerometer
/// reading.
///
/// Azimuth calculations have a similar problem at vertical pitch. The azimuth
/// is essentially the horizontal angle from north to the plane in which the
/// device is pitching. At vertical pitch there is no such unique plane and
/// azimuth can't be calculated (ie pitching down by PI at any azimuth will
/// result in the same vertical orientation). A tilt compensated azimuth
/// using a stablised roll value results in a well-behaved orientation since
/// as roll gets driven to zero, azimuth will be driven to the local negative z
/// direction of the device (ie up in the device's frame of reference - where
/// roll is zero).
///
/// To picture the effect of roll stabilisation and tilt compensated azimuth,
/// imagine the device pitching down past vertical. On the other side of
/// vertical the azimuth jumps around by PI (180 degrees) ince it is pointing
/// in the opposite direction and the roll also changes to reflect the fact that
/// the device has now flipped onto its back. Rather than a mathematical
/// singularity at vertical the roll smoothly swings through zero and the
/// azimuth swings though the device's local up vector and the resulting angles
/// will orient the device correctly.
///
/// \brief Disables roll stabilisation for orientation calculations.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context will be used.
/// \param disable Desired disabled state for roll stabilisation. 1 = disabled,
/// 0 = enabled. Default state is enabled.

void ecompass_roll_stabilisation_disable(ecompass_t *ecompass, int disable);



////////////////////////////////////////////////////////////////////////////////
/// Sets the factor used by roll stabilisation if enabled. This factor is the
/// multiplier for the x accelerometer component that is added to the z component
/// for stabilisation. A value of zero effectively disables any compensation
/// (but use ecompass_roll_stabilisation_enable() for this as it is more
/// efficient). The value should be non-negative and as small as possible for
/// increased accuracy far from vertical but big enough to suppress sensor noise
/// (and therefore increase accuracy near vertical). Default value is 0.01 which
/// should be suitable for most situations.
/// See ecompass_roll_stabilisation_enable() for detailed discussion.
///
/// \brief Specified roll stabilisation factor.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context will be used.
/// \param factor Roll stabilisation factor. default value is 0.01.

void ecompass_roll_stabilisation_factor(ecompass_t *ecompass, double factor);



////////////////////////////////////////////////////////////////////////////////
/// Normally azimuth is calculated from tilt compensated magnetometer
/// readings whereby the magnetometer vertor is rotated to compensate for
/// device tilt using acceleromter pitch and roll measurements rather than
/// using the x, y components and ignoring z (accurate only near horizontal)
/// The compensated calculation is more accurate but requires a valid
/// accelerometer reading. Normally if no such reading is availble the
/// aziumuth is calulated using the uncompensated approach but is marked as
/// invalid. When allow_uncompensated_azimuth enabled, the uncompensated
/// result is marked as valid. This should always be disabled unless
/// there is no accelerometer or for diagnostic purposes. Default is 0.
///
/// \brief Allow or disallow uncompensated (for device tilt) azimuth calculation
/// to be considered valid.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context will be used.
/// \param allow Desired allowability of uncompensated azimuth values.
/// 0 = disallow, 1 = allow.

void allow_uncompensated_azimuth(ecompass_t *ecompass, int allow);



////////////////////////////////////////////////////////////////////////////////
/// \brief Invalidate the current accelerometer reading.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context will be used.

void ecompass_accelerometer_invalidate(ecompass_t *ecompass);



////////////////////////////////////////////////////////////////////////////////
/// Sets the current accelerometer reading and recalculates orientation. The
/// current (most recently set) magnetometer reading will be used for the
/// calculation. If the \p accelerometer_is_pre_calibrated parameter is set,
/// the \p x, \p y, \p z components are assumed to be in g's (gravitaional
/// acceleration units). If not, calibration is attempted and it is assumed that
/// the components will be transformed to g's by the calibration parameters.
///
/// \brief Calculate orientation using new accelerometer reading.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context will be used.
/// \param x Accelerometer x component reading in g's.
/// \param y Accelerometer y component reading in g's.
/// \param z Accelerometer z component reading in g's.
/// \param t Temperature at which the reading were taken.
/// \param accelerometer_is_pre_calibrated Flag indicating that the \p x, \p y
/// and \p z components are pre calibrated. If set, calibration will not be
/// applied but the accelerometer vector will still be marked as calibrated.

void ecompass_accelerometer_reading(ecompass_t *ecompass, double x, double y, double z, float t, int is_pre_calibrated);



////////////////////////////////////////////////////////////////////////////////
/// \brief Invalidate the current magnetometer reading.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context will be used.

void ecompass_magnetometer_invalidate(ecompass_t *ecompass);



////////////////////////////////////////////////////////////////////////////////
/// Sets the current magnetometer reading and recalculates orientation. The
/// current (most recently set) accelerometer reading will be used for the
/// calculation. If the \p is_pre_calibrated parameter is set, the \p x, \p y,
/// \p z components are assumed to be in micro Teslas. If not, calibration is
/// attempted and it is assumed that the components will be transformed to micro
/// Teslas by the calibration parameters.
///
/// \brief Calculate orientation using new magnetometer reading.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context will be used.
/// \param x Magnetometer x component reading in micro Teslas.
/// \param y Magnetometer y component reading in micro Teslas.
/// \param z Magnetometer z component reading in micro Teslas.
/// \param t Temperature at which the reading were taken.
/// \param is_pre_calibrated Flag indicating that the \p x, \p y
/// and \p z components are pre calibrated. If set, calibration will not be
/// applied but the magnetometer vector will still be marked as calibrated.

void ecompass_magnetometer_reading(ecompass_t *ecompass, double x, double y, double z, float t, int is_pre_calibrated);



////////////////////////////////////////////////////////////////////////////////
/// \brief Invalidate both the current accelerometer and magnetometer readings.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context will be used.

void ecompass_accelerometer_and_magnetometer_invalidate(ecompass_t *ecompass);



////////////////////////////////////////////////////////////////////////////////
/// Sets the current accelerometer and magnetometer reading and recalculates
/// orientation. If the \p accelerometer_is_pre_calibrated parameter is set,
/// the \p ax, \p ay, \p az components are assumed to be in g's (gravitaional
/// acceleration units). If not, calibration is attempted and it is assumed that
/// the components will be transformed to g's by the calibration parameters.
/// If the \p is_pre_calibrated parameter is set, the \p mx, \p my, \p mz
/// components are assumed to be in micro Teslas. If not, calibration is
/// attempted and it is assumed that the components will be transformed to micro
/// Teslas by the calibration parameters.
///
/// \brief Calculate orientation using new accelerometer and magnetometer readings.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context will be used.
/// \param ax Accelerometer x component reading in g's.
/// \param ay Accelerometer y component reading in g's.
/// \param az Accelerometer z component reading in g's.
/// \param at Accelerometer temperature.
/// \param mx Magnetometer x component reading in micro Teslas.
/// \param my Magnetometer y component reading in micro Teslas.
/// \param mz Magnetometer z component reading in micro Teslas.
/// \param at Magnetometer temperature.
/// \param accelerometer_is_pre_calibrated Flag indicating that the \p ax, \p ay
/// and \p az components are pre calibrated. If set, calibration will not be
/// applied but the accelerometer vector will still be marked as calibrated.
/// \param magnetometer_is_pre_calibrated Flag indicating that the \p mx, \p my
/// and \p mz components are pre calibrated. If set, calibration will not be
/// applied but the magnetometer vector will still be marked as calibrated.

void ecompass_accelerometer_and_magnetometer_reading(ecompass_t *ecompass,
                                                     double ax, double ay, double az, float at,
                                                     double mx, double my, double mz, float mt,
                                                     int accelerometer_is_pre_calibrated, int magnetometer_is_pre_calibrated);



////////////////////////////////////////////////////////////////////////////////
/// \brief Invalidate the current temperature reading.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context will be used.

void ecompass_temperature_invalidate(ecompass_t *ecompass);



////////////////////////////////////////////////////////////////////////////////
/// Sets the current overall temperature of the ecompass reading.
///
/// \brief Set ecompass temperature.
/// \param ecompass Pointer to the ecompass context. Can be NULL in which case
/// the global shared context will be used.
/// \param t Temperature in degrees celcius. Use constant ECOMPASS_INVALID_TEMPERATURE
/// to represent unknown temperature.

void ecompass_temperature_reading(ecompass_t *ecompass, float t);



////////////////////////////////////////////////////////////////////////////////
/// Copies the accelerometer calibration parameters of an ecompass_t to a
/// parameters structure. If the ecompass pointer is NULL, the parameters are
/// set to defaults. If the parameters are NULL, no action is taken.
///
/// \brief Get accelerometer calibration parameters for an ecompass
/// \param ecompass Pointer to ecompass. If NULL, \p param is set to defaults.
/// \param cal pointer to structure to which the parameters are copied.

void ecompass_calibration_get_accelerometer(const ecompass_t *ecompass, ecompass_calibration_t *cal);



////////////////////////////////////////////////////////////////////////////////
/// Copies the accelerometer calibration parameters to an ecompass_t from a
/// parameters structure. If the parameters pointer is NULL, the ecompass
/// parameters are set to defaults. If the ecompass is NULL, no action is taken.
///
/// \brief Set accelerometer calibration parameters for an ecompass
/// \param ecompass Pointer to ecompass.
/// \param cal pointer to structure from which the parameters are copied.
/// If NULL, calibration parameters for \p ecompass are set to defaults.

void ecompass_calibration_set_accelerometer(ecompass_t *ecompass, const ecompass_calibration_t *cal);




////////////////////////////////////////////////////////////////////////////////
/// Copies the magnetometer calibration parameters of an ecompass_t to a
/// parameters structure. If the ecompass pointer is NULL, the parameters are
/// set to defaults. If the parameters are NULL, no action is taken.
///
/// \brief Get magnetometer calibration parameters for an ecompass
/// \param ecompass Pointer to ecompass. If NULL, \p param is set to defaults.
/// \param cal pointer to structure to which the parameters are copied.

void ecompass_calibration_get_magnetometer(const ecompass_t *ecompass, ecompass_calibration_t *cal);



////////////////////////////////////////////////////////////////////////////////
/// Copies the magnetometer calibration parameters to an ecompass_t from a
/// parameters structure. If the parameters pointer is NULL, the ecompass
/// parameters are set to defaults. If the ecompass is NULL, no action is taken.
///
/// \brief Set magnetometer calibration parameters for an ecompass
/// \param ecompass Pointer to ecompass.
/// \param cal pointer to structure from which the parameters are copied.
/// If NULL, calibration parameters for \p ecompass are set to defaults.

void ecompass_calibration_set_magnetometer(ecompass_t *ecompass, const ecompass_calibration_t *cal);



////////////////////////////////////////////////////////////////////////////////
/// Copies the temperature calibration parameters of an ecompass_t to a
/// parameters structure. If the ecompass pointer is NULL, the parameters are
/// set to defaults. If the parameters are NULL, no action is taken.
///
/// \brief Get temperature calibration parameters for an ecompass
/// \param ecompass Pointer to ecompass. If NULL, \p param is set to defaults.
/// \param cal pointer to structure to which the parameters are copied.

void ecompass_calibration_get_temperature(const ecompass_t *ecompass, ecompass_scalar_calibration_t *cal);



////////////////////////////////////////////////////////////////////////////////
/// Copies the temperature calibration parameters to an ecompass_t from a
/// parameters structure. If the parameters pointer is NULL, the ecompass
/// parameters are set to defaults. If the ecompass is NULL, no action is taken.
///
/// \brief Set temperature calibration parameters for an ecompass
/// \param ecompass Pointer to ecompass.
/// \param cal pointer to structure from which the parameters are copied.
/// If NULL, calibration parameters for \p ecompass are set to defaults.

void ecompass_calibration_set_temperature(ecompass_t *ecompass, const ecompass_scalar_calibration_t *cal);



////////////////////////////////////////////////////////////////////////////////
/// Sets the calibration timestamp of an ecompass to the specified value.
///
/// \brief Set calibration timestamp.
/// \param ecompass Pointer to ecompass.
/// \param timestamp Time to which the timestamp should be set.

void ecompass_calibration_timestamp_set(ecompass_t *ecompass, time_t timestamp);



////////////////////////////////////////////////////////////////////////////////
/// Sets the calibration timestamp of an ecompass to the current time.
///
/// \brief Set calibration timestamp to current time.
/// \param ecompass Pointer to ecompass.

void ecompass_calibration_timestamp_now(ecompass_t *ecompass);



////////////////////////////////////////////////////////////////////////////////
/// Invalidates the calibration timestamp of an ecompass. The ecompass
/// essentially has no timestamp of last calibration.
///
/// \brief Invalidate calibration timestamp.
/// \param ecompass Pointer to ecompass.

void ecompass_calibration_timestamp_invalidate(ecompass_t *ecompass);



////////////////////////////////////////////////////////////////////////////////
/// Sets a manual zero offset value.
///
/// \brief Set manual zero offset.
/// \param ecompass Pointer to ecompass.
/// \param which The offset is to be set. Values may be ECOMPASS_MANUAL_ZERO_ACC_ROLL
/// \param how How to set the value. May be ECOMPASS_MANUAL_ZERO_CLEAR,
/// ECOMPASS_MANUAL_SET_AUTO, ECOMPASS_MANUAL_ZERO_SET_VALUE, ECOMPASS_MANUAL_ZERO_SET_VALUE_DEGREES
/// or ECOMPASS_MANUAL_ZERO_SET_VALUE_RADIANS.
/// \param value The value to be set when \p how is ECOMPASS_MANUAL_ZERO_SET_VALUE,
/// ECOMPASS_MANUAL_ZERO_SET_VALUE_DEGREES or ECOMPASS_MANUAL_ZERO_SET_VALUE_RADIANS.
/// Ignored otherwise.
/// \return Error code
/// \retval 0 No error.
/// \retval -1 Value was unchanged.
/// \retval -2 Invalid parameters

int ecompass_set_zero_offset(ecompass_t *ecompass, int which, int how, double value);



////////////////////////////////////////////////////////////////////////////////
/// Gets a manual zero offset value.
///
/// \brief Set manual zero offset.
/// \param ecompass Pointer to ecompass.
/// \param which The offset is to be gotten. Values may be ECOMPASS_MANUAL_ZERO_ACC_ROLL

double ecompass_get_zero_offset(ecompass_t *ecompass, int which);



#ifdef ECOMPASS_SENSOR_VARIANCE

////////////////////////////////////////////////////////////////////////////////
/// Returns an noise metric (variance or maximum deviation from mean) of recent
/// accelerometer or magnetometer sensor measurements.
///
/// \brief Noise metric for recent measurements.
/// \param ecompass Pointer to ecompass.
/// \param which Selects the noise metric to be returned.
/// \param variance_is_stable If not null, this pointer receives the flag
/// indicating stablity of the selected varience metric.
/// \return The selected noise metric
/// \retval <0 On error

double ecompass_noise_metric(ecompass_t *ecompass, ecompass_noise_metric_t which, int *is_stable);



////////////////////////////////////////////////////////////////////////////////
/// Controls wether noinse metrics are calculated of a most recent number of
/// samples or the the samples within a specified duration.
///
/// \brief Forces errors to be calculated of specified number of samples rather
/// than specified time period.
/// \param ecompass Pointer to ecompass.
/// \param enable Boolean value that enables calcualtion over a specified number
/// if set or, a specified duration if not.

void ecompass_noise_metric_enable_number(ecompass_t *ecompass, int enable);



////////////////////////////////////////////////////////////////////////////////
/// Noise metrics are calculated over the most recent sensor values defined by
/// time period \p duration_sec (i.e. those no older than \p duration_sec).
/// This is only used if ecompass_noise_metric_enable_number() is not set.
///
/// \brief Set time duration over which noise metrics are calculated.
/// \param ecompass Pointer to ecompass.
/// \param duration_sec Duration in seconds for noise metric calulation.

void ecompass_noise_metric_duration(ecompass_t *ecompass, double duration_sec);




////////////////////////////////////////////////////////////////////////////////
/// Noise metrics are calculated over the most recent number of sensor values
/// defined by \p number. This is only used if
/// ecompass_noise_metric_enable_number() is set.
///
/// \brief Set number of measurements over which noise metrics are calculated.
/// \param ecompass Pointer to ecompass.
/// \param duration_sec Number of measurements.

void ecompass_noise_metric_number(ecompass_t *ecompass, int number);

#endif







#if defined (ECOMPASS_DEBUG)

////////////////////////////////////////////////////////////////////////////////
/// Prints out calibration parameters for all teh sensors of an ecompass.
///
/// \brief Prints calibration parameters for an ecompass.
/// \param ecompass The ecompass whose parameters are to be printed.

void ecompass_print(const ecompass_t *ecompass);



////////////////////////////////////////////////////////////////////////////////
/// Prints out calibration coeffs. Normally prints out each coefficient as a
/// termperature compensated polynomial (in T) but can also print the concrete
/// calibration currently in use.
///
/// \brief Prints calibration parameters.
/// \param cal The parameters that are to be printed.
/// \param title Title used for the print.
/// \param concrete Flag that selects the current concrete calibration coeffs
/// when set rather than the termperature polynomial.

void ecompass_print_calibration(const ecompass_calibration_t *cal, const char *title, int concrete);

#endif



#if defined(ECOMPASS_UNIT_TEST)

////////////////////////////////////////////////////////////////////////////////
/// Test packing and unpacking of calibration parameters to buffer. Dumps test
/// results to console.
///
/// \brief Test claibration parameter packing,
/// \return Error count. Number of errors detected
/// \retval 0 No error. All tests passed.

int ecompass_test_packing(void);

#endif



#endif //LIBTEST_ECOMPASS_H
