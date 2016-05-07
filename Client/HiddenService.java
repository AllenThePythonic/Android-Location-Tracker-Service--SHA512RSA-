package com.example.lancelot.gps_auto_sender;

import android.app.IntentService;
import android.content.Intent;
import android.content.res.AssetManager;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class HiddenService extends IntentService implements Serializable {

    public HiddenService() {
        super("My Hidden Service");
    }

    /* For extracting DataInputStream */
    private DataInputStream extracted(FileInputStream in) {
        return new DataInputStream(in);
    }

    public static byte[] toByteArrayUsingJava(InputStream is) throws IOException{

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int reads = is.read();
        while(reads != -1) {
            baos.write(reads); reads = is.read();
        }
        return baos.toByteArray();
    }

    /* Get private key from file */
    private PublicKey getPublicKey()
            throws InvalidKeySpecException, IOException,
            NoSuchAlgorithmException {

		/* Map the file of private key */
        AssetManager assetManager = getAssets();
        InputStream input = assetManager.open("public_key");

        byte[] keyBytes = toByteArrayUsingJava(input);

		/* Define X509 Encoding Standard */
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");

        return kf.generatePublic(spec);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        try {

            GPSTracker tracker = new GPSTracker(this.getApplicationContext(), getPublicKey());
            tracker.start();

        } catch (InvalidKeySpecException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}