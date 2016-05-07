package gpsLocator;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import org.bson.Document;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.crypto.Cipher;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import static java.lang.System.*;

/**
 * @author Lancelot
 * 
 *         This server can be used to decrypt the byte stream which is encrypted
 *         by client Android Application. with the encryption format is
 *         SHA512RSA. After decrypted, server side will get the location data of
 *         target and keep tracking. Finally, these data will be stored to
 *         mongoDB
 */

public class DecryptionServer extends Thread {

	private boolean OutServer = false;
	private ServerSocket server;
	private String port;
	private String encoding;
	private String transformation;
	private MongoClient mongoClient;
	private MongoDatabase db;
	private MongoCollection<Document> collection;

	public DecryptionServer() {

		try {

			Properties prop = new Properties();
			File configFile = new File("resources/config.properties");
			FileInputStream fis = null;
			fis = new FileInputStream(configFile);
			InputStream input = fis;
			prop.load(input);

			configureFromProperties(prop);
			configureMongoDb();

			server = new ServerSocket(Integer.parseInt(port));

		} catch (Exception e) {
			out.println("Socket Start up Error !");
			out.println("Exception :" + e.toString());
		}
	}

	private final void configureFromProperties(Properties prop) {

		this.transformation = prop.getProperty("transformation");
		this.port = prop.getProperty("port");
		this.encoding = prop.getProperty("encoding");
	}

	private final void configureMongoDb() {

		ServerAddress serverAddress = new ServerAddress("localhost", 27017);
		List<ServerAddress> addrs = new ArrayList<ServerAddress>();
		addrs.add(serverAddress);

		MongoCredential credential = MongoCredential.createScramSha1Credential("", "test",
				"".toCharArray());
		List<MongoCredential> credentials = new ArrayList<MongoCredential>();
		credentials.add(credential);

		this.mongoClient = new MongoClient();
		this.db = mongoClient.getDatabase("test");
		this.collection = db.getCollection("location_log");
		System.out.println("Connect to database successfully");
	}

	private final static byte[] toByteArray(InputStream is) throws IOException {

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int reads = is.read();
		while (reads != -1) {
			baos.write(reads);
			reads = is.read();
		}
		return baos.toByteArray();
	}

	private final static PrivateKey getPrivateKey()
			throws InvalidKeySpecException, IOException, NoSuchAlgorithmException {

		/* Map the file of private key */
		File f = new File("resources/private_key");
		FileInputStream fis = null;
		fis = new FileInputStream(f);
		InputStream input = fis;

		byte[] keyBytes = toByteArray(input);

		/* Define PKCS8E Encoding Standard */
		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
		KeyFactory kf = KeyFactory.getInstance("RSA");

		return kf.generatePrivate(spec);
	}

	private synchronized final byte[] commonDecrypt(byte[] rawBytes) throws IOException, GeneralSecurityException {

		return decrypt(rawBytes, this.transformation, this.encoding);
	}

	private synchronized final byte[] decrypt(byte[] cipherText, String transformation, String encoding)
			throws IOException, GeneralSecurityException {

		Cipher cipher = Cipher.getInstance(transformation);
		cipher.init(Cipher.DECRYPT_MODE, getPrivateKey());

		return cipher.doFinal(cipherText);
	}

	public final void run() {

		Socket socket;
		BufferedInputStream in;

		out.println("Location Log Decryption Server started ");

		while (!OutServer) {

			socket = null;

			try {
				synchronized (server) {
					socket = server.accept();
				}

				in = new BufferedInputStream(socket.getInputStream());
				byte[] data = new byte[128];
				in.read(data);

				String decryptedData = new String(commonDecrypt(data), this.encoding);

				in.close();
				in = null;
				socket.close();

				store2MongoDb(convert2Json(socket, decryptedData));

			} catch (java.io.IOException | GeneralSecurityException | MongoException e) {

				System.err.println(e.toString());
				mongoClient.close();
			}
		}
	}

	private synchronized final String convert2Json(Socket socket, String decryptedData) {

		LocalDateTime timePoint = LocalDateTime.now();
		String now = timePoint.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
		String[] gpsLocation = new String[2];
		String[] networkLocation = new String[2];
		String[] passiveLocation = new String[2];

		for (String extensionRemoved : decryptedData.split("\\+")) {

			String[] locationAry = extensionRemoved.split("-");

			for (int i = 0; i < locationAry.length; i++) {

				if ("GPS".equals(locationAry[0]))
					gpsLocation = locationAry;

				if ("Network".equals(locationAry[0]))
					networkLocation = locationAry;

				if ("Passive".equals(locationAry[0]))
					passiveLocation = locationAry;
			}
		}

		String json = "{'deviceIp': '" + socket.getInetAddress() + "', 'devicePort': '" + socket.getPort()
				+ "', 'lastUpdateDateTime': '" + now + "', 'location': {'gpsLatitude': '" + gpsLocation[1]
				+ "', 'gpsLongitude': '" + gpsLocation[2] + "', 'networkLatitude': '" + networkLocation[1]
				+ "', 'networkLongitude': '" + networkLocation[2] + "', 'passiveLatitude': '" + passiveLocation[1]
				+ "', 'passiveLongitude': '" + passiveLocation[2] + "'} }";

		return json;
	}

	private synchronized final void store2MongoDb(String json) {

		try {

			Document myDoc = Document.parse(json);
			collection.insertOne(myDoc);

		} catch (Exception e) {

			System.err.println(e.getClass().getName() + ": " + e.getMessage());
			mongoClient.close();
		}
	}

	public static void main(String args[]) {
		(new DecryptionServer()).start();
	}
}
