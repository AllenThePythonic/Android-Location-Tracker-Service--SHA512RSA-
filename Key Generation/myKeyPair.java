/*
 * This class is for generating the pair key - public key & private key
 * and save to each file - public_key & private_key
 */
package KeyPairGeneration;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;

public class myKeyPair {

	/* Initialize the Key Pair Generator */
	public static KeyPair generateKeyPair(String algorithm, int keysize)
			throws NoSuchAlgorithmException {

		KeyPairGenerator keyGen = KeyPairGenerator.getInstance(algorithm);
		keyGen.initialize(keysize);
		return keyGen.genKeyPair();
	}

	public static void main(String args[]) {

		DataOutputStream out = null;
		KeyPair generateRSAKey = null;

		try {
			generateRSAKey = generateKeyPair("RSA", 1024);

			/* Create the file for public key */
			out = new DataOutputStream(new FileOutputStream("public_key"));

			System.out.println("public key : " + generateRSAKey.getPublic());
			System.out.println("private key : " + generateRSAKey.getPrivate());

			/* Write the public key to file */
			out.write(generateRSAKey.getPublic().getEncoded());
			out.flush();
			out.close();

			/* Create the file for private key */
			out = new DataOutputStream(new FileOutputStream("private_key"));

			/* Write the private key to file */
			out.write(generateRSAKey.getPrivate().getEncoded());
			out.flush();
			out.close();

		} catch (NoSuchAlgorithmException | IOException e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}
}