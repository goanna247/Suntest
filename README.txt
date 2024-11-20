# Boreline probe app

## App overview:
There are 2 different types of readings, core and bore shots that different probes return. Currently all probes used only return 1 of these types however there is discussion that probe that are being developed will return both in order to fit more information while still working in the "existing" structure. IMO this isnt must easier than adding a type of reading, however it would have to be at the end of the information returned so as to not mess with the current order.


## Bluetooth
The probes use Bluetooth Low Energy

Each page keeps an existing bluetooth connection with the probe, hence almost all pages have bluetooth capabilities
which link back to the BluetoothLE

The bluetooth structure used relies on callbacks and an (inefficient) queueing system

### Queueing system:

### Making a new activity which uses the current bluetooth system
A new activity that fits into the existing bluetooth infastructure can be easily done, any bluetooth capabilities will not be useable until after the app is connected to a probe, hence has gone to the device scan page and the user has chosen a probe to connect with.

The app passes information about the probe it is currently "connected" to between pages. (The reason i use the quotations is because even if the probe is not currently connected this is the probe that the app will try to reconnect to even if the connection fails). This information is stored as Strings (easy to pass between pages, and different languages as i was trying out cpp for some stuff a time ago), the 3 key things passed are the probe name (EXTRA_DEVICE_NAME), the probe address (EXTRA_DEVICE_ADDRESS) and the connection status (EXTRA_CONNECTION_STATUS). These are passed between activities using intent.



There are 3 main operations in BLE:
- Writing (1) - used to write data to a characterisitic
- Reading (2) - read from a characteristic
- Notifying (3) - used to tell a characterisitic to regularly send data, used mostly for getting bore and core shots.

need to create a BluetoothLeService, a BluetoothGatt,

## Calibration

### Variables:
NUM_CAL_PARAMS_EXPECTED_DURING_PARSING = 38
arrays for acc_a - c as well as mag_a - c and temp_params

## Structure
The app starts the the Main activity which is used to launch all other pages

             Main Activity
                  |
                  Device Scan Activity (Probe is selected
                  |
                  CalibrationActivity (Probes calibration data is selected and loaded into the Calibration
                                       helper class to be used later)
                  |
                  Main Activity (Now displays the connected probe number)
            |                            |
      Take Measurements              Probe Data
            |                            |
      View Measurement              Sensor Data

## Activities
MainActivity
Orientation Activity

## Classes
BluetoothLEService
CalibrationHelper
Globals
Measurement
Operation
Probe Data

## Installing on a device
after launching the probe on a device you will have to go into settings and give the app a load of permissions,
most can be set in the default permissions screen, however you will have to go into location
settings and turn on always precise location. This permission is required for the bluetooth to work,
who knows why bluetooth requires location settings but for some reason it does.

## What Next?
There are hundreds of to-dos on this project the most important next steps are
- fixing the connection issues (if disconnected sometimes doesnt reconnect, this is really important
  for when probe comes out of the hole)
- Calibration takes a long time
- Setting the probe into getting a measurement mode takes a long time
- There are alot of depricated stuff within the project that should be removed for ease of use