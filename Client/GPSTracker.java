package com.example.lancelot.gps_auto_sender;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Properties;

import javax.crypto.Cipher;

public class GPSTracker extends Thread implements LocationListener {

    private String transformation;

    private String address;

    private String port;

    private String encoding;

    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

    private final String threadName = "tracker";

    protected LocationManager locationManager;

    // Flag for GPS status
    boolean isGPSEnabled = false;

    // Flag for network status
    boolean isNetworkEnabled = false;

    // Flag for Passive status
    boolean isPassiveEnabled = false;

    // Flag for GPS status
    boolean canGetLocation = false;

    private Context mContext;

    private Thread t;

    private Location networkLocation;

    private Location gpsLocation;

    private Location passiveLocation;

    private double gpsLatitude = 0;

    private double gpsLongitude = 0;

    private double networkLatitude = 0;

    private double networkLongitude = 0;

    private double passiveLatitude = 0;

    private double passiveLongitude = 0;

    private PublicKey publicKey;

    public GPSTracker(Context context, PublicKey publicKey) throws NoSuchAlgorithmException, IOException {

        this.mContext = context;
        this.publicKey = publicKey;

        Properties prop = new Properties();
        AssetManager assetManager = context.getAssets();
        InputStream input = assetManager.open("config.properties");

        if (input != null) {
            prop.load(input);
        } else {
            throw new FileNotFoundException("property file not found in the classpath");
        }

        this.transformation = prop.getProperty("transformation");
        this.address = prop.getProperty("server"); // Server IP
        this.port = prop.getProperty("port"); // Server Port
        this.encoding = prop.getProperty("encoding");
    }

    private synchronized void getLocation() {

        try {

            locationManager = (LocationManager) mContext
                    .getSystemService(mContext.LOCATION_SERVICE);

            // Getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);

            // Getting Passive status
            isPassiveEnabled = locationManager
                    .isProviderEnabled(LocationManager.PASSIVE_PROVIDER);

            // Getting network status
            isNetworkEnabled = locationManager
                    .isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            this.canGetLocation = true;

            networkLocation = locationManager
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

            gpsLocation = locationManager
                    .getLastKnownLocation(LocationManager.GPS_PROVIDER);

            passiveLocation = locationManager
                    .getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);

            // First get location from Network Provider
            if (isNetworkEnabled) {
                if (locationManager != null) {
                    if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }

                    if (networkLocation != null) {
                        this.networkLatitude = networkLocation.getLatitude();
                        this.networkLongitude = networkLocation.getLongitude();
                    }
                }
            }

            // if GPS Enabled get lat/long using GPS Services
            if (isGPSEnabled) {

                if (locationManager != null) {

                    if (gpsLocation != null) {
                        this.gpsLatitude = gpsLocation.getLatitude();
                        this.gpsLongitude = gpsLocation.getLongitude();
                    }
                }
            }

            // if Passive Enabled get lat/long using GPS Services
            if (isPassiveEnabled) {

                if (locationManager != null) {

                    if (passiveLocation != null) {
                        this.passiveLatitude = passiveLocation.getLatitude();
                        this.passiveLongitude = passiveLocation.getLongitude();
                    }
                }
            }

        } catch (Exception e) {
            Log.e("GPSTracker()", e.toString());
        }
    }

    /*private String getLocalIpAddress() {

        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException e) {
            Log.e("GPSTracker()", e.getMessage(), e);
        }
        return null;
    }*/

    public String getGPSLocation() {

        if (this.gpsLatitude != 0 && this.gpsLongitude != 0)
            return "GPS-" + this.gpsLatitude + "-" + this.gpsLongitude;
        else
            return "GPS-N/A-N/A";
    }

    public String getPassiveLocation() {

        if (this.passiveLatitude != 0 && this.passiveLongitude != 0)
            return "Passive-" + this.passiveLatitude + "-" + this.passiveLongitude;
        else
            return "Passive-N/A-N/A";
    }

    public String getNetworkLocation() {

        if (this.networkLatitude != 0 && this.networkLongitude != 0)
            return "Network-" + this.networkLatitude + "-" + this.networkLongitude;
        else
            return "Network-N/A-N/A";
    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    private byte[] commonEncrypt(String rawText) throws IOException, GeneralSecurityException {
        return encrypt(rawText, this.transformation, this.encoding);
    }

    private byte[] encrypt(String rawText, String transformation, String encoding)
            throws IOException, GeneralSecurityException {

        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(rawText.getBytes(encoding));
    }

    @Override
    public void run() {

        System.out.println("Running " + threadName);

        while (true) {
            try {

                this.getLocation();
                send2Server(commonEncrypt(getGPSLocation() + "+" + getNetworkLocation() + "+" + getPassiveLocation()));
                Thread.sleep(50000);
            } catch (Exception e) {

                Log.e("GPSTracker", "Thread " + threadName + " interrupted. Reason" + e);
                System.out.println("Thread " + threadName + " exiting.");
            }
        }
    }

    private void send2Server(byte[] dataStream) throws IOException {

        Socket client = new Socket(this.address, Integer.parseInt(this.port));

        try {

            BufferedOutputStream out = new BufferedOutputStream(client
                    .getOutputStream());

            out.write(dataStream);
            out.flush();
            out.close();
            client.close();

        } catch (java.io.IOException e) {

            System.out.println("Socket Connection Error on Client !");
            System.out.println("IOException :" + e.toString());
        }
    }

    @Override
    public void start() {

        System.out.println("Starting " + threadName);

        if (t == null) {
            t = new Thread(this, threadName);
            t.start();
        }
    }
}
