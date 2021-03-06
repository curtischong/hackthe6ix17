package com.example.daniel.museapp;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.choosemuse.libmuse.Accelerometer;
import com.choosemuse.libmuse.AnnotationData;
import com.choosemuse.libmuse.ConnectionState;
import com.choosemuse.libmuse.Eeg;
import com.choosemuse.libmuse.LibmuseVersion;
import com.choosemuse.libmuse.MessageType;
import com.choosemuse.libmuse.Muse;
import com.choosemuse.libmuse.MuseArtifactPacket;
import com.choosemuse.libmuse.MuseConfiguration;
import com.choosemuse.libmuse.MuseConnectionListener;
import com.choosemuse.libmuse.MuseConnectionPacket;
import com.choosemuse.libmuse.MuseDataListener;
import com.choosemuse.libmuse.MuseDataPacket;
import com.choosemuse.libmuse.MuseFileFactory;
import com.choosemuse.libmuse.MuseFileReader;
import com.choosemuse.libmuse.MuseFileWriter;
import com.choosemuse.libmuse.MuseListener;
import com.choosemuse.libmuse.MuseManagerAndroid;
import com.choosemuse.libmuse.MuseVersion;
import com.choosemuse.libmuse.Result;
import com.choosemuse.libmuse.ResultLevel;
import com.djm.tinder.Tinder;
import com.djm.tinder.auth.AuthenticationException;
import com.djm.tinder.like.Like;
import com.djm.tinder.profile.Profile;
import com.djm.tinder.user.Photo;
import com.djm.tinder.user.User;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.bluetooth.BluetoothAdapter;


import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.example.daniel.museapp.classifier.ClassifierModule;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

/**
 * This example will illustrate how to connect to a Muse headband,
 * register for and receive EEG data and disconnect from the headband.
 * Saving EEG data to a .muse file is also covered.
 *
 * For instructions on how to pair your headband with your Android device
 * please see:
 * http://developer.choosemuse.com/hardware-firmware/bluetooth-connectivity/developer-sdk-bluetooth-connectivity-2
 *
 * Usage instructions:
 * 1. Pair your headband if necessary.
 * 2. Run this project.
 * 3. Turn on the Muse headband.
 * 4. Press "Refresh". It should display all paired Muses in the Spinner drop down at the
 *    top of the screen.  It may take a few seconds for the headband to be detected.
 * 5. Select the headband you want to connect to and press "Connect".
 * 6. You should see EEG and accelerometer data as well as connection status,
 *    version information and relative alpha values appear on the screen.
 * 7. You can pause/resume data transmission with the button at the bottom of the screen.
 * 8. To disconnect from the headband, press "Disconnect"
 */
public class MainActivity extends Activity implements OnClickListener{
//EAAGm0PX4ZCpsBAOylQXGYpctyZAPaVeQHq67EuRMpV2JjfhW9mgTLpwcfdZB1XYwWAnJH5Q8OpnZCnJA8qZC89ClJ1UvPAylGoNQyRlCiKw1ZAYvEk6rs7OpoCAvWEfCV51iRmJQwWEEGoXA4NzOAjjyI4iVYn7rSpix6RqjD4MHhMwvec6X42LjXQlSipY80ZAfdkNC8LcYC0yLx7tZCr9RJ3bB03z7titfzDrm9e1VvXTwYRVRFZAbykfHFO0or2AhVgZBGoAqxxHgZDZD
//EAAGm0PX4ZCpsBANBb1ZB49S2kcuKBS15IXTasRy4lNg0ZC2lTASMqliePWxIdDmKSZAH39Xk5rfy5J970LqGcmEGDLt44JVzMvWcCSZCXmlVpKHiL10uiVWdwvuFTmXIgCpr5jOIgWFj3JqAgL9bRr0s5ZCufqlzZARZBunkRe9SaVBADPq7S6lBdCaBKdTK5TfpaGyYrv4NzpEGWzZCJeJkYaOiqBCZCH1GSwEEy4a63wSW739UYHteZCTMdc89Qyp4qqR6Oi8QiG7UgZDZD

    /**
     * Tag used for logging purposes.
     */
    private final String TAG = "TestLibMuseAndroid";
    private long oldTime = 0;

    /**
     * The MuseManager is how you detect Muse headbands and receive notifications
     * when the list of available headbands changes.
     */
    private MuseManagerAndroid manager;

    /**
     * A Muse refers to a Muse headband.  Use this to connect/disconnect from the
     * headband, register listeners to receive EEG data and get headband
     * configuration and version information.
     */
    public Muse muse;

    /**
     * The ConnectionListener will be notified whenever there is a change in
     * the connection state of a headband, for example when the headband connects
     * or disconnects.
     *
     * Note that ConnectionListener is an inner class at the bottom of this file
     * that extends MuseConnectionListener.
     */
    private ConnectionListener connectionListener;

    /**
     * The DataListener is how you will receive EEG (and other) data from the
     * headband.
     *
     * Note that DataListener is an inner class at the bottom of this file
     * that extends MuseDataListener.
     */
    private DataListener dataListener;

    /**
     * Data comes in from the headband at a very fast rate; 220Hz, 256Hz or 500Hz,
     * depending on the type of headband and the preset configuration.  We buffer the
     * data that is read until we can update the UI.
     *
     * The stale flags indicate whether or not new data has been received and the buffers
     * hold the values of the last data packet received.  We are displaying the EEG, ALPHA_RELATIVE
     * and ACCELEROMETER values in this example.
     *
     * Note: the array lengths of the buffers are taken from the comments in
     * MuseDataPacketType, which specify 3 values for accelerometer and 6
     * values for EEG and EEG-derived packets.
     */
    private final double[] eegBuffer = new double[6];
    private boolean eegStale;
    private final double[] alphaBuffer = new double[6];
    private boolean alphaStale;
    private final double[] accelBuffer = new double[3];
    private boolean accelStale;

    /**
     * We will be updating the UI using a handler instead of in packet handlers because
     * packets come in at a very high frequency and it only makes sense to update the UI
     * at about 60fps. The update functions do some string allocation, so this reduces our memory
     * footprint and makes GC pauses less frequent/noticeable.
     */
    private final Handler handler = new Handler();

    /**
     * In the UI, the list of Muses you can connect to is displayed in a Spinner object for this example.
     * This spinner adapter contains the MAC addresses of all of the headbands we have discovered.
     */
    private ArrayAdapter<String> spinnerAdapter;

    /**
     * It is possible to pause the data transmission from the headband.  This boolean tracks whether
     * or not the data transmission is enabled as we allow the user to pause transmission in the UI.
     */
    private boolean dataTransmission = true;

    /**
     * To save data to a file, you should use a MuseFileWriter.  The MuseFileWriter knows how to
     * serialize the data packets received from the headband into a compact binary format.
     * To read the file back, you would use a MuseFileReader.
     */
    private final AtomicReference<MuseFileWriter> fileWriter = new AtomicReference<>();

    /**
     * We don't want file operations to slow down the UI, so we will defer those file operations
     * to a handler on a separate thread.
     */
    private final AtomicReference<Handler> fileHandler = new AtomicReference<>();

    //--------------------------------------
    // Lifecycle / Connection code

    private ClassifierModule classifier;

    private ArrayList<User> gatheredUsers = new ArrayList<User>();
    private Profile yourProfile;
    private int currentUser = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // We need to set the context on MuseManagerAndroid before we can do anything.
        // This must come before other LibMuse API calls as it also loads the library.
        manager = MuseManagerAndroid.getInstance();
        manager.setContext(this);

        Log.i(TAG, "LibMuse version=" + LibmuseVersion.instance().getString());

        WeakReference<MainActivity> weakActivity =
                new WeakReference<MainActivity>(this);
        // Register a listener to receive connection state changes.
        connectionListener = new ConnectionListener(weakActivity);
        // Register a listener to receive data from a Muse.
//        dataListener = new DataListener(weakActivity);
        // Register a listener to receive notifications of what Muse headbands
        // we can connect to.
        manager.setMuseListener(new MuseL(weakActivity));

        // Muse 2016 (MU-02) headbands use Bluetooth Low Energy technology to
        // simplify the connection process.  This requires access to the COARSE_LOCATION
        // or FINE_LOCATION permissions.  Make sure we have these permissions before
        // proceeding.
        ensurePermissions();

        // Load and initialize our UI.
        initUI();

        // Start up a thread for asynchronous file operations.
        // This is only needed if you want to do File I/O.
        fileThread.start();



        /*StartUpTinder sut = new StartUpTinder();
        sut.execute();
        TinderRecommend tr = new TinderRecommend();
        tr.execute();*/
        statusText = (TextView) findViewById(R.id.con_status) ;
        statusText2 = (TextView) findViewById(R.id.status_text) ;
        new StartUpTinder().execute();
        new TinderRecommend().execute();

    }

    public void loadNextImage(){
        Log.i("test", Integer.toString(currentUser));
        Log.i("test", Integer.toString(gatheredUsers.size()));
        if (currentUser < gatheredUsers.size()) {
            ImageView imageView = (ImageView) findViewById(R.id.imageView);
            Picasso.with(MainActivity.this)
                    .load(gatheredUsers.get(currentUser).getPhotos().get(0).getUrl())
                    .resize(800, 800)
                    //.fit()
                    .centerInside()
                    .into(imageView, new Callback() {
                        @Override
                        public void onSuccess() {
                            ImageView imageViewCheck = (ImageView) findViewById(R.id.imageViewCheck);
                            ImageView imageViewCancel = (ImageView) findViewById(R.id.imageViewCancel);
                            imageViewCheck.setVisibility(View.GONE);
                            imageViewCancel.setVisibility(View.GONE);
                            try{
                                classifier.collectData();
                            }catch (NullPointerException e){
                                Log.i("WARNING", "CONNECT YOUR MUSE!");
                            }
                        }

                        @Override
                        public void onError() {
                            Log.e("Picasso", "Error loading image");
                        }
                    });
            // new TinderResult().execute();
            currentUser++;
            if(currentUser == gatheredUsers.size()){
                new StartUpTinder().execute();
                new TinderRecommend().execute();
                currentUser = 0;
            }
        }
    }

    public void likeTinder() {
        ImageView imageViewCheck = (ImageView) findViewById(R.id.imageViewCheck);
        imageViewCheck.setVisibility(View.VISIBLE);
        new TinderResult().execute();
        this.loadNextImage();
    }

    public void dislikeTinder() {
        ImageView imageViewCancel = (ImageView) findViewById(R.id.imageViewCancel);
        imageViewCancel.setVisibility(View.VISIBLE);
        this.loadNextImage();
    }

    protected void onPause() {

        super.onPause();
        // It is important to call stopListening when the Activity is paused
        // to avoid a resource leak from the LibMuse library.
        manager.stopListening();
    }

    public boolean isBluetoothEnabled() {
        return BluetoothAdapter.getDefaultAdapter().isEnabled();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.refresh) {
            // The user has pressed the "Refresh" button.
            // Start listening for nearby or paired Muse headbands. We call stopListening
            // first to make sure startListening will clear the list of headbands and start fresh.
            manager.stopListening();
            manager.startListening();

        } else if (v.getId() == R.id.connect) {

            // The user has pressed the "Connect" button to connect to
            // the headband in the spinner.

            // Listening is an expensive operation, so now that we know
            // which headband the user wants to connect to we can stop
            // listening for other headbands.
            manager.stopListening();

            List<Muse> availableMuses = manager.getMuses();
            Spinner musesSpinner = (Spinner) findViewById(R.id.muses_spinner);

            // Check that we actually have something to connect to.
            if (availableMuses.size() < 1 || musesSpinner.getAdapter().getCount() < 1) {
                Log.w(TAG, "There is nothing to connect to");
            } else {

                // Cache the Muse that the user has selected.
                muse = availableMuses.get(musesSpinner.getSelectedItemPosition());
                // Unregister all prior listeners and register our data listener to
                // receive the MuseDataPacketTypes we are interested in.  If you do
                // not register a listener for a particular data type, you will not
                // receive data packets of that type.

                muse.unregisterAllListeners();
                muse.registerConnectionListener(connectionListener);
//                muse.registerDataListener(dataListener, MuseDataPacketType.EEG);
//                muse.registerDataListener(dataListener, MuseDataPacketType.ALPHA_RELATIVE);
//                muse.registerDataListener(dataListener, MuseDataPacketType.ACCELEROMETER);
//                muse.registerDataListener(dataListener, MuseDataPacketType.BATTERY);
//                muse.registerDataListener(dataListener, MuseDataPacketType.DRL_REF);
//                muse.registerDataListener(dataListener, MuseDataPacketType.QUANTIZATION);

                // Initiate a connection to the headband and stream the data asynchronously.
                muse.runAsynchronously();

                classifier = new ClassifierModule(this);
                classifier.init();
                classifier.listenData();
            }

        } else if (v.getId() == R.id.disconnect) {

            // The user has pressed the "Disconnect" button.
            // Disconnect from the selected Muse.
            if (muse != null) {
                muse.disconnect();
            }

        } else if (v.getId() == R.id.pause) {

            // The user has pressed the "Pause/Resume" button to either pause or
            // resume data transmission.  Toggle the state and pause or resume the
            // transmission on the headband.
            if (muse != null) {
                dataTransmission = !dataTransmission;
                muse.enableDataTransmission(dataTransmission);
            }
        } else if(v.getId() == R.id.next_button){
            Log.i("test", "click");
            loadNextImage();
        } else if(v.getId() == R.id.debug){

            Log.i("hello", "statechange");
            if(debugState){
                refreshButton.setVisibility(View.GONE);
                nextImageBtn.setVisibility(View.GONE);
                connectButton.setVisibility(View.GONE);
                disconnectButton.setVisibility(View.GONE);
                pauseButton.setVisibility(View.GONE);
                statusText.setVisibility(View.GONE);
                statusText2.setVisibility(View.GONE);
                musesSpinner.setVisibility(View.GONE);
                debugState =false;
            }else{
                refreshButton.setVisibility(View.VISIBLE);
                nextImageBtn.setVisibility(View.VISIBLE);
                connectButton.setVisibility(View.VISIBLE);
                disconnectButton.setVisibility(View.VISIBLE);
                pauseButton.setVisibility(View.VISIBLE);
                statusText.setVisibility(View.VISIBLE);
                statusText2.setVisibility(View.VISIBLE);
                musesSpinner.setVisibility(View.VISIBLE);
                debugState = true;
            }
        }
    }
    Boolean debugState = true;
    //--------------------------------------
    // Permissions

    /**
     * The ACCESS_COARSE_LOCATION permission is required to use the
     * Bluetooth Low Energy library and must be requested at runtime for Android 6.0+
     * On an Android 6.0 device, the following code will display 2 dialogs,
     * one to provide context and the second to request the permission.
     * On an Android device running an earlier version, nothing is displayed
     * as the permission is granted from the manifest.
     *
     * If the permission is not granted, then Muse 2016 (MU-02) headbands will
     * not be discovered and a SecurityException will be thrown.
     */
    private void ensurePermissions() {

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            // We don't have the ACCESS_COARSE_LOCATION permission so create the dialogs asking
            // the user to grant us the permission.

            DialogInterface.OnClickListener buttonListener =
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which){
                            dialog.dismiss();
                            ActivityCompat.requestPermissions(MainActivity.this,
                                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                    0);
                        }
                    };

            // This is the context dialog which explains to the user the reason we are requesting
            // this permission.  When the user presses the positive (I Understand) button, the
            // standard Android permission dialog will be displayed (as defined in the button
            // listener above).
            AlertDialog introDialog = new AlertDialog.Builder(this)
                    .setTitle(R.string.permission_dialog_title)
                    .setMessage(R.string.permission_dialog_description)
                    .setPositiveButton(R.string.permission_dialog_understand, buttonListener)
                    .create();
            introDialog.show();
        }
    }


    //--------------------------------------
    // Listeners

    /**
     * You will receive a callback to this method each time a headband is discovered.
     * In this example, we update the spinner with the MAC address of the headband.
     */
    public void museListChanged() {
        final List<Muse> list = manager.getMuses();
        spinnerAdapter.clear();
        for (Muse m : list) {
            spinnerAdapter.add(m.getName() + " - " + m.getMacAddress());
        }
    }

    /**
     * You will receive a callback to this method each time there is a change to the
     * connection state of one of the headbands.
     * @param p     A packet containing the current and prior connection states
     * @param muse  The headband whose state changed.
     */
    TextView statusText;
    TextView statusText2;
    public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {

        final ConnectionState current = p.getCurrentConnectionState();

        // Format a message to show the change of connection state in the UI.
        final String status = p.getPreviousConnectionState() + " -> " + current;
        Log.i(TAG, status);

        // Update the UI with the change in connection state.
        handler.post(new Runnable() {
            @Override
            public void run() {

                statusText = (TextView) findViewById(R.id.con_status);
                statusText.setText(status);

            }
        });

        if (current == ConnectionState.DISCONNECTED) {
            Log.i(TAG, "Muse disconnected:" + muse.getName());
            // Save the data file once streaming has stopped.
            saveFile();
            // We have disconnected from the headband, so set our cached copy to null.
            this.muse = null;
            connectButton.setTextColor(Color.BLACK);
        }else{
            connectButton.setTextColor(Color.GREEN);
        }
    }

    /**
     * You will receive a callback to this method each time the headband sends a MuseDataPacket
     * that you have registered.  You can use different listeners for different packet types or
     * a single listener for all packet types as we have done here.
     * @param p     The data packet containing the data from the headband (eg. EEG data)
     * @param muse  The headband that sent the information.
     */
    public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) throws Exception {
        writeDataPacketToFile(p);

        // valuesSize returns the number of data values contained in the packet.
        final long n = p.valuesSize();
        switch (p.packetType()) {
            case EEG:
                assert(eegBuffer.length >= n);
                getEegChannelValues(eegBuffer,p);
                eegStale = true;
                break;
            case ACCELEROMETER:
                assert(accelBuffer.length >= n);
                getAccelValues(p);
                accelStale = true;
                break;
            case ALPHA_RELATIVE:
                assert(alphaBuffer.length >= n);
                getEegChannelValues(alphaBuffer,p);
                alphaStale = true;
                break;
            case BATTERY:
            case DRL_REF:
            case QUANTIZATION:
            default:
                break;
        }
    }

    /**
     * You will receive a callback to this method each time an artifact packet is generated if you
     * have registered for the ARTIFACTS data type.  MuseArtifactPackets are generated when
     * eye blinks are detected, the jaw is clenched and when the headband is put on or removed.
     * @param p     The artifact packet with the data from the headband.
     * @param muse  The headband that sent the information.
     */
    public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
    }

    /**
     * Helper methods to get different packet values.  These methods simply store the
     * data in the buffers for later display in the UI.
     *
     * getEegChannelValue can be used for any EEG or EEG derived data packet type
     * such as EEG, ALPHA_ABSOLUTE, ALPHA_RELATIVE or HSI_PRECISION.  See the documentation
     * of MuseDataPacketType for all of the available values.
     * Specific packet types like ACCELEROMETER, GYRO, BATTERY and DRL_REF have their own
     * getValue methods.
     */

    String bowlingJson(double[] buffer) {
        return "{ \"EEG1\":" + buffer[0] + ", \"EEG2\":" + buffer[1] + ", \"EEG3\":" + buffer[2] + ", \"EEG4\":" + buffer[3] + ", \"AUX_LEFT\":" + buffer[4] + ", \"AUX_RIGHT\":" + buffer[5] + "}";

    }


    private void getEegChannelValues(double[] buffer, MuseDataPacket p) throws Exception {
        buffer[0] = p.getEegChannelValue(Eeg.EEG1);
        buffer[1] = p.getEegChannelValue(Eeg.EEG2);
        buffer[2] = p.getEegChannelValue(Eeg.EEG3);
        buffer[3] = p.getEegChannelValue(Eeg.EEG4);

        buffer[4] = p.getEegChannelValue(Eeg.AUX_LEFT);
        buffer[5] = p.getEegChannelValue(Eeg.AUX_RIGHT);
    }

    private void getAccelValues(MuseDataPacket p) {
        accelBuffer[0] = p.getAccelerometerValue(Accelerometer.X);
        accelBuffer[1] = p.getAccelerometerValue(Accelerometer.Y);
        accelBuffer[2] = p.getAccelerometerValue(Accelerometer.Z);
    }


    //--------------------------------------
    // UI Specific methods

    /**
     * Initializes the UI of the example application.
     */
    Button connectButton;
    Button disconnectButton;
    Button pauseButton;
    Button nextImageBtn;
    Button refreshButton;
    Button debug;
    Spinner musesSpinner;
    private void initUI() {
        setContentView(R.layout.activity_main);
        refreshButton = (Button) findViewById(R.id.refresh);
        refreshButton.setOnClickListener(this);
        connectButton = (Button) findViewById(R.id.connect);
        connectButton.setOnClickListener(this);
        disconnectButton = (Button) findViewById(R.id.disconnect);
        disconnectButton.setOnClickListener(this);
        pauseButton = (Button) findViewById(R.id.pause);
        pauseButton.setOnClickListener(this);
        nextImageBtn = (Button) findViewById(R.id.next_button);
        nextImageBtn.setOnClickListener(this);
        debug = (Button) findViewById(R.id.debug);
        debug.setOnClickListener(this);

        spinnerAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item);
        musesSpinner = (Spinner) findViewById(R.id.muses_spinner);
        musesSpinner.setAdapter(spinnerAdapter);
    }


    //--------------------------------------
    // File I/O

    /**
     * We don't want to block the UI thread while we write to a file, so the file
     * writing is moved to a separate thread.
     */
    private final Thread fileThread = new Thread() {
        @Override
        public void run() {
            Looper.prepare();
            fileHandler.set(new Handler());
            final File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            final File file = new File(dir, "new_muse_file.muse" );
            // MuseFileWriter will append to an existing file.
            // In this case, we want to start fresh so the file
            // if it exists.
            if (file.exists()) {
                file.delete();
            }
            Log.i(TAG, "Writing data to: " + file.getAbsolutePath());
            fileWriter.set(MuseFileFactory.getMuseFileWriter(file));
            Looper.loop();
        }
    };

    /**
     * Writes the provided MuseDataPacket to the file.  MuseFileWriter knows
     * how to write all packet types generated from LibMuse.
     * @param p     The data packet to write.
     */
    private void writeDataPacketToFile(final MuseDataPacket p) {
        Handler h = fileHandler.get();
        if (h != null) {
            h.post(new Runnable() {
                @Override
                public void run() {
                    fileWriter.get().addDataPacket(0, p);
                }
            });
        }
    }

    /**
     * Flushes all the data to the file and closes the file writer.
     */
    private void saveFile() {
        Handler h = fileHandler.get();
        if (h != null) {
            h.post(new Runnable() {
                @Override public void run() {
                    MuseFileWriter w = fileWriter.get();
                    // Annotation strings can be added to the file to
                    // give context as to what is happening at that point in
                    // time.  An annotation can be an arbitrary string or
                    // may include additional AnnotationData.
                    w.addAnnotationString(0, "Disconnected");
                    w.flush();
                    w.close();
                }
            });
        }
    }

    /**
     * Reads the provided .muse file and prints the data to the logcat.
     * @param name  The name of the file to read.  The file in this example
     *              is assumed to be in the Environment.DIRECTORY_DOWNLOADS
     *              directory.
     */
    private void playMuseFile(String name) {

        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        File file = new File(dir, name);

        final String tag = "Muse File Reader";

        if (!file.exists()) {
            Log.w(tag, "file doesn't exist");
            return;
        }

        MuseFileReader fileReader = MuseFileFactory.getMuseFileReader(file);

        // Loop through each message in the file.  gotoNextMessage will read the next message
        // and return the result of the read operation as a Result.
        Result res = fileReader.gotoNextMessage();
        while (res.getLevel() == ResultLevel.R_INFO && !res.getInfo().contains("EOF")) {

            MessageType type = fileReader.getMessageType();
            int id = fileReader.getMessageId();
            long timestamp = fileReader.getMessageTimestamp();

            Log.i(tag, "type: " + type.toString() +
                    " id: " + Integer.toString(id) +
                    " timestamp: " + String.valueOf(timestamp));

            switch(type) {
                // EEG messages contain raw EEG data or DRL/REF data.
                // EEG derived packets like ALPHA_RELATIVE and artifact packets
                // are stored as MUSE_ELEMENTS messages.
                case EEG:
                case BATTERY:
                case ACCELEROMETER:
                case QUANTIZATION:
                case GYRO:
                case MUSE_ELEMENTS:
                    MuseDataPacket packet = fileReader.getDataPacket();
                    Log.i(tag, "data packet: " + packet.packetType().toString());
                    break;
                case VERSION:
                    MuseVersion version = fileReader.getVersion();
                    Log.i(tag, "version" + version.getFirmwareType());
                    break;
                case CONFIGURATION:
                    MuseConfiguration config = fileReader.getConfiguration();
                    Log.i(tag, "config" + config.getBluetoothMac());
                    break;
                case ANNOTATION:
                    AnnotationData annotation = fileReader.getAnnotation();
                    Log.i(tag, "annotation" + annotation.getData());
                    break;
                default:
                    break;
            }

            // Read the next message.
            res = fileReader.gotoNextMessage();
        }
    }

    //--------------------------------------
    // Listener translators
    //
    // Each of these classes extend from the appropriate listener and contain a weak reference
    // to the activity.  Each class simply forwards the messages it receives back to the Activity.
    class MuseL extends MuseListener {
        final WeakReference<MainActivity> activityRef;

        MuseL(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void museListChanged() {
            activityRef.get().museListChanged();
        }
    }

    class ConnectionListener extends MuseConnectionListener {
        final WeakReference<MainActivity> activityRef;

        ConnectionListener(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseConnectionPacket(final MuseConnectionPacket p, final Muse muse) {
            activityRef.get().receiveMuseConnectionPacket(p, muse);
        }
    }

    class DataListener extends MuseDataListener {
        final WeakReference<MainActivity> activityRef;

        DataListener(final WeakReference<MainActivity> activityRef) {
            this.activityRef = activityRef;
        }

        @Override
        public void receiveMuseDataPacket(final MuseDataPacket p, final Muse muse) {
            try {
                activityRef.get().receiveMuseDataPacket(p, muse);
            } catch (Exception e) {
                System.out.println(e);
            }
        }

        @Override
        public void receiveMuseArtifactPacket(final MuseArtifactPacket p, final Muse muse) {
            activityRef.get().receiveMuseArtifactPacket(p, muse);
        }
    }

    /*public class PostExample {
        public static final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");

        OkHttpClient client = new OkHttpClient();

        String post(String url, String json) throws IOException {
            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            try (Response response = client.newCall(request).execute()) {
                return response.body().string();
            }
        }

        String bowlingJson(double[] buffer) {
            return "{ 'EEG1':" + buffer[0] + ", 'EEG2':" + buffer[1] + ", 'EEG3':" + buffer[2] + ", 'EEG4':" + buffer[3] + ", 'AUX_LEFT':" + buffer[4] + ", 'AUX_RIGHT':" + buffer[5] + "}";
        }

        public static void main(String[] args) throws IOException {
            PostExample example = new PostExample();
            String json = example.bowlingJson("Jesse", "Jake");
            String response = example.post("https://muse.1lab.me/post", json);
            System.out.println(response);
        }
    }*/

    class StartUpTinder extends AsyncTask<Void, Void, Profile> {

        private Exception exception;

        protected Profile doInBackground(Void... voids) {
            try {
                final Tinder tinder = Tinder.fromAccessToken(ACCESS_TOKEN);
                yourProfile = tinder.getProfile();
                Log.i("tinder", String.format("About me: %s", yourProfile.getName()));
                return yourProfile;
            } catch (AuthenticationException e) {
                Log.e("tinder",  "Whoops, unable to authenticate to the tinder API. Check your Facebook access token / app's permissions.");
            } catch (Exception e) {
                Log.e("tinder", e.toString());
                Log.e("tinder", "there was an error");
            }
            return null;
        }

        @Override
        protected void onPostExecute(Profile tinder) {
            super.onPostExecute(tinder);

            //Log.i("test", tinder.toString());
            // TODO: check this.exception
            // TODO: do something with the feed
        }
    }

    final String ACCESS_TOKEN = "EAAGm0PX4ZCpsBAPGg1bCVnZByFwq1JKKeh8Li2B8Q4F7YNm3TyAZBeptg6XWRIpdPHeNc5eH8DTh3ZBPywZCvSOTukyaLfrUb2ZAmMo1Xlhn4gkINC21B37qfcZAKeGUzZCqQnteVmktDqVqJFfTkpaI6KaZCBplMgikFOitKzGa9kYmj9VtL7jIwZAEAnDD53Uk2SCFIvyhu5ohlwayQjIqFlU1ZAzd5g9kZBFrJRBzifZC2aOBk0ZCI9JGDKKhE2EeffZA0ZAK67Q2ypgQcQZDZD";

    class TinderRecommend extends AsyncTask<Void, Void, ArrayList<User>> {

        private Exception exception;

        protected ArrayList<User> doInBackground(Void... voids) {
            try {
                final Tinder tinder = Tinder.fromAccessToken(ACCESS_TOKEN);
                final ArrayList<User> users = tinder.getRecommendations();
                for (User user : users) {
                    Log.i("User",(String.format("See %s", user.getName())));
                    ArrayList<Photo> photos = user.getPhotos();
                    for(Photo photo : photos){
                        Log.i("User",(String.format("See %s", photo.getUrl())));
                    }
                }
                gatheredUsers = users;
            } catch (AuthenticationException e) {
                Log.i("ERROR",  "Whoops, unable to authenticate to the tinder API. Check your Facebook access token / app's permissions.");
            } catch (Exception e) {
                Log.i("ERROR", e.toString());
                Log.i("ERROR", "there was an error");
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList tinder) {
            super.onPostExecute(tinder);

            //Log.i("test", tinder.toString());
            // TODO: check this.exception
            // TODO: do something with the feed
        }
    }

    class TinderResult extends AsyncTask<Void, Void, Like> {

        private Exception exception;

        protected Like doInBackground(Void... voids) {
            try {
                final Tinder tinder = Tinder.fromAccessToken(ACCESS_TOKEN);
                Like like = tinder.like(gatheredUsers.get(currentUser));
                if (like.isMatch() == true) {
                    System.out.println(String.format("Matched with %s!", gatheredUsers.get(currentUser).getName()));
                }

            } catch (AuthenticationException e) {
                Log.i("ERROR",  "Whoops, unable to authenticate to the tinder API. Check your Facebook access token / app's permissions.");
            } catch (Exception e) {
                Log.i("ERROR", e.toString());
                Log.i("ERROR", "there was an error");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Like tinder) {
            super.onPostExecute(tinder);

            //Log.i("test", tinder.toString());
            // TODO: check this.exception
            // TODO: do something with the feed
        }
    }


}


