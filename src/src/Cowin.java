package src;
import java.awt.AWTException;
import java.awt.Image;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.TrayIcon.MessageType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Cowin {

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	public static void main(String[] args)  {
		try {
			
			String date = String.valueOf(args[0]);
			int age = Integer.parseInt(args[1]);
			int doseNumber = Integer.parseInt(args[2]);
			int pinCodePrefix = Integer.parseInt(args[3]);

			System.out.println("Checking vaccines for age " + age + ", dose number " + doseNumber + ", pincode starting with " + pinCodePrefix);

			String cowin = "https://cdn-api.co-vin.in/api/v2/appointment/sessions/public/calendarByDistrict?district_id=363&date="+date;
			System.out.println(cowin);


			Runnable poll = new Runnable() {
				public void run() {
					System.out.println("Refreshing...");
					JSONObject json=null;
					try {
						json = readJsonFromUrl(cowin);
					} catch (JSONException | IOException e) {
						e.printStackTrace();
					}
					JSONArray js = (JSONArray) json.get("centers");

					for(Object o : js) {
						JSONObject centre = (JSONObject) o;
						String hospName = (String) centre.get("name");
						String address = (String) centre.get("address");
						String pincode = String.valueOf(centre.get("pincode"));
						String feeType = (String) centre.get("fee_type");

						if(pincode.startsWith(String.valueOf(pinCodePrefix))) {
							JSONArray vaccineFees = new JSONArray();

							if(!feeType.equalsIgnoreCase("free"))
								vaccineFees = (JSONArray) centre.get("vaccine_fees");

							JSONArray sessions = (JSONArray) centre.get("sessions");

							for(Object sesh : sessions) {
								JSONObject sesh1 = (JSONObject) sesh;
								int availableCapDose1 = (int) sesh1.get("available_capacity_dose1");
								int availableCapDose2 = (int) sesh1.get("available_capacity_dose2");
								int minAge = (int) sesh1.get("min_age_limit");
								String date1 = (String) sesh1.get("date");
								try {
								if(minAge>=45 && age>=45) {
									printDoseInfo(hospName, doseNumber, pincode, feeType, availableCapDose1, availableCapDose2, minAge, date1);
								} else if((minAge>=18 && minAge < 45) && (age>=18 && age < 45)) {
									printDoseInfo(hospName, doseNumber, pincode, feeType, availableCapDose1, availableCapDose2, minAge, date1);
								}
								} catch (AWTException e ) {
									System.out.println(e.getMessage());
								}
							}

						}
					}
				}
			};

			ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
			executor.scheduleAtFixedRate(poll, 0, 5, TimeUnit.SECONDS);

		} catch (Exception e) {
			System.out.println(e + e.getMessage());
		}
	}

	private static void printDoseInfo(String hospName, int doseNumber, String pincode, String feeType, int availableCapDose1,
			int availableCapDose2, int minAge, String date) throws AWTException {
		if(doseNumber==1 && availableCapDose1>0) {
			displayTray("Dose " + doseNumber +  " for age " + minAge + "+ at pincode " + pincode  + " Number of doses remaining: " + availableCapDose1 + "  " + hospName + " Date :  " + date + " Feetype: " + feeType );
			System.out.println("Dose " + doseNumber +  " for age " + minAge + "+ at pincode " + pincode  + " Number of doses remaining: " + availableCapDose1 + "  " + hospName + " Date :  " + date + " Feetype: " + feeType );

		} else if (doseNumber == 2 && availableCapDose2>0) {
			displayTray("Dose " + doseNumber +  " for age " + minAge + "+ at pincode " + pincode  + " Number of doses remaining: " + availableCapDose2 + "  " + hospName + " Date :  " + date + " Feetype: " + feeType );
			System.out.println("Dose " + doseNumber +  " for age " + minAge + "+ at pincode " + pincode  + " Number of doses remaining: " + availableCapDose2 + "  " + hospName + " Date :  " + date + " Feetype: " + feeType );
		}
	}

	public static void displayTray(String message) throws AWTException {
		if (SystemTray.isSupported()) {
			//Obtain only one instance of the SystemTray object
			SystemTray tray = SystemTray.getSystemTray();

			//If the icon is a file
			Image image = Toolkit.getDefaultToolkit().createImage("icon.png");
			//Alternative (if the icon is on the classpath):
			//Image image = Toolkit.getDefaultToolkit().createImage(getClass().getResource("icon.png"));

			TrayIcon trayIcon = new TrayIcon(image, "Tray Demo");
			//Let the system resize the image if needed
			trayIcon.setImageAutoSize(true);
			//Set tooltip text for the tray icon
			trayIcon.setToolTip("System tray icon demo");
			tray.add(trayIcon);

			trayIcon.displayMessage("Dose Available!!!!!!!", message, MessageType.INFO);
		}
		else {
			System.err.println("System tray not supported!");
		}
	}

}