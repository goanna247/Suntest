

package com.work.libtest;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

/**
 * Class to create the various alerts used by the BleMainActivity
 * The strings for these alert dialogs are defined in the dialog_strings.xml file
 * Only a single AlertDialog dialog is reused for each alert.
 *
 */
public class ShowAlertDialogs {

    private AlertDialog dialog;											                            //Object variable for an AlertDialog dialog box
    private AlertDialog.Builder builder; 								                            //Object variable for an AlertDialog.Builder to be able to create dialog boxes

    // ----------------------------------------------------------------------------------------------------------------
    // Constructor creates the AlertDialog.Builder to build each dialog.
    public ShowAlertDialogs(Context context) {
        builder = new AlertDialog.Builder(context); 						                        //Instantiate an AlertDialog.Builder to be able to create dialog boxes
        dialog = builder.create();                                                                  //Instantiate an AlertDialog so that we do not need to check if it is null
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Dismiss any dialog that is showing. Has no effect if the dialog is not showing.
    public void dismiss() {
        dialog.dismiss();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Show the Help dialog when the Help menu is selected. OK button dismisses the dialog.
    public void showHelpMenuDialog(Context context) {
        dialog.dismiss();
        builder.setTitle(R.string.help_title);                                          //Set up the AlertDialog that will contain the Help text
        builder.setMessage(Html.fromHtml(context.getString(R.string.help_contents)));
        builder.setPositiveButton(R.string.help_ok_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                            // User clicked OK button
            }
        });
        builder.setNegativeButton(null, null);
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
        ((TextView)dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Show the About dialog when the About menu is selected. OK button dismisses the dialog.
    public void showAboutMenuDialog(Context context) {
        dialog.dismiss();
        builder.setTitle(R.string.about_title);                                          //Set up the AlertDialog that will contain the About text
        builder.setMessage(context.getString(R.string.about_contents) + BuildConfig.VERSION_NAME);
        builder.setPositiveButton(R.string.about_ok_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                            // User clicked OK button
            }
        });
        builder.setNegativeButton(null, null);
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();                                                                              //Show the AlertDialog that has the About text
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Show the Exit dialog when the Exit menu is selected. Cancel button dismisses the dialog. OK button executes a Runnable.
    public void showExitMenuDialog(final Runnable callback) {
        dialog.dismiss();
        builder.setTitle(R.string.exit_title);                                                       //Set up the AlertDialog that will contain the Exit warning
        builder.setMessage(R.string.exit_contents);
        builder.setPositiveButton(R.string.exit_ok_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                            //User clicked OK button
                callback.run();   // PJH - TODO - how does this exit the app?
            }
        });
        builder.setNegativeButton(R.string.exit_cancel_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                        //User clicked Cancel button
            }                                                                                       //Don't do anything, continue as before
        });
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Show a dialog that the connection attempt failed.
    public void showFailedToConnectDialog(final Runnable callback) {
        dialog.dismiss();
        builder.setTitle(R.string.noconnect_title);                                                      //Set up the AlertDialog that will let the user know that the connection failed
        builder.setMessage(R.string.noconnect_contents);
        builder.setPositiveButton(R.string.noconnect_ok_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                            //User clicked OK button
                callback.run();
            }
        });
        builder.setNegativeButton(R.string.noconnect_cancel_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                        //User clicked Cancel button
            }                                                                                       //Don't do anything, continue as before
        });
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Show a dialog that shows that the probe has disconnected and needs to be reconnected to access data
    public void showFailToAccessProbe(final Runnable callback) {
        dialog.dismiss();
        builder.setTitle(R.string.probedisconnect_title);                                                      //Set up the AlertDialog that will let the user know that the connection failed
        builder.setMessage(R.string.probedisconnect_contents);
        builder.setPositiveButton(R.string.probedisconnect_reconnect, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                            //User clicked OK button
                callback.run();
            }
        });
        builder.setNegativeButton(R.string.probedisconnect_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                        //User clicked Cancel button

            }                                                                                       //Don't do anything, continue as before
        });
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Show a dialog that shows that asks to repeat measurement at depth or next depth
    public void measurementProgression(final Runnable callback) {
        dialog.dismiss();
        builder.setTitle(R.string.measurementProgression_title);                                                      //Set up the AlertDialog that will let the user know that the connection failed
        builder.setMessage(R.string.measurementProgression_contents);
        builder.setPositiveButton(R.string.measurementProgression_next, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                            //User clicked OK button
                callback.run();
            }
        });
        builder.setNegativeButton(R.string.measurementProgression_repeat, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                        //User clicked Cancel button
//                callback.run();
            }                                                                                       //Don't do anything, continue as before
        });
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Show a dialog that the existing connection was lost. Probably caused by the remote Bluetooth device disconnecting, powering off, or going out of range.
    public void showLostConnectionDialog(final Runnable callback) {
        dialog.dismiss();
        builder.setTitle(R.string.lost_title);                                                     //Set up the AlertDialog that will let the user know that the connection failed
        builder.setMessage(R.string.lost_contents);
        builder.setPositiveButton(R.string.lost_ok_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                            //User clicked OK button
                callback.run();
            }
        });
        builder.setNegativeButton(R.string.lost_cancel_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                        //User clicked Cancel button
            }                                                                                       //Don't do anything, continue as before
        });
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Show a dialog that the existing connection was lost. Probably caused by the remote Bluetooth device disconnecting, powering off, or going out of range.
//    public void showLostConnectionDialog(final Runnable callback) {
//        dialog.dismiss();
//        builder.setTitle(R.string.lost_title);                                                     //Set up the AlertDialog that will let the user know that the connection failed
//        builder.setMessage(R.string.lost_contents);
//        builder.setPositiveButton(R.string.lost_ok_button, new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {                                            //User clicked OK button
//                callback.run();
//            }
//        });
//        builder.setNegativeButton(R.string.lost_cancel_button, new DialogInterface.OnClickListener() {
//            public void onClick(DialogInterface dialog, int id) {                                        //User clicked Cancel button
//            }                                                                                       //Don't do anything, continue as before
//        });
//        dialog = builder.create();
//        dialog.setCanceledOnTouchOutside(false);
//        dialog.show();
//    }
//

    // ----------------------------------------------------------------------------------------------------------------
    // Show a dialog that the device is faulty. Service discovery failed to find the right service and characteristics.
    public void removeAllSurveys(final Runnable callback) {
        dialog.dismiss();
        builder.setTitle(R.string.remove_title);                                                       //Set up the AlertDialog that will let the user know that the device is faulty (failed to discover services)
        builder.setMessage(R.string.remove_content);
        builder.setPositiveButton(R.string.remove_ok_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                            //User clicked OK button
                callback.run();
            }
        });
        builder.setNegativeButton(R.string.remove_cancel_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                        //User clicked Cancel button
            }                                                                                       //Don't do anything, continue as before
        });
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Show a dialog that the device is faulty. Service discovery failed to find the right service and characteristics.
    public void showFaultyDeviceDialog(final Runnable callback) {
        dialog.dismiss();
        builder.setTitle(R.string.faulty_title);                                                       //Set up the AlertDialog that will let the user know that the device is faulty (failed to discover services)
        builder.setMessage(R.string.faulty_contents);
        builder.setPositiveButton(R.string.faulty_ok_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                            //User clicked OK button
                callback.run();
            }
        });
        builder.setNegativeButton(R.string.faulty_cancel_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                        //User clicked Cancel button
            }                                                                                       //Don't do anything, continue as before
        });
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Show a dialog that location access is now required to do a Bluetooth LE scan.
    public void showLocationPermissionDialog(final Runnable callback) {
        dialog.dismiss();
        builder.setTitle(R.string.location_title);                                           //Set up the AlertDialog that will let the user know that they must grant location permission
        builder.setMessage(R.string.location_contents);
        builder.setPositiveButton(R.string.location_continue_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                      //User clicked Continue button
                callback.run();                                                                     //Run the method to request permission again
            }
        });
        builder.setNegativeButton(null, null);
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }






    // ----------------------------------------------------------------------------------------------------------------
    // Show a dialog that warns that probe is not in the correct location for the current reading
    public void showAcceptLocationErrorDialog(Context context) {
        dialog.dismiss();
        builder.setTitle("Acceptance Test - Location Error");                                           //Set up the AlertDialog that will let the user know that they must grant location permission
        builder.setMessage("A Location has not yet been selected.\n\nSelect the Location and try again.");
        builder.setPositiveButton(R.string.about_ok_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                            // User clicked OK button
            }
        });
        builder.setNegativeButton(null, null);
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();                                                                              //Show the AlertDialog that has the About text
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Show a dialog that warns that probe is not in the correct location for the current reading
    public void showAcceptPositionErrorDialog(Context context) {
        dialog.dismiss();
        builder.setTitle("Acceptance Test - Position Error");                                           //Set up the AlertDialog that will let the user know that they must grant location permission
        builder.setMessage("The probe has not been positioned accurately enough, to be able to take this new reading.\n\nReposition the probe and try again.");
        builder.setPositiveButton(R.string.about_ok_button, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {                                            // User clicked OK button
            }
        });
        builder.setNegativeButton(null, null);
        dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();                                                                              //Show the AlertDialog that has the About text
    }


}
