import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.codehaus.jackson.map.ObjectMapper;

public class MeetMeHalfway {

	private static final String URL = "http://maps.googleapis.com/maps/api/geocode/json";
	private static GoogleResponse location1 = null;
	private static GoogleResponse location2 = null;
	private static boolean fullPrint = false;
	private static String term;
	private static int limit;

	private GoogleResponse convertToLatLong(String fullAddress) throws IOException {
		URL url = new URL(URL + "?address=" + URLEncoder.encode(fullAddress, "UTF-8") + "&sensor=false");
		URLConnection conn = url.openConnection();

		InputStream in = conn.getInputStream();
		ObjectMapper mapper = new ObjectMapper();
		GoogleResponse response = (GoogleResponse) mapper.readValue(in, GoogleResponse.class);
		in.close();
		return response;
	}

	private GoogleResponse convertFromLatLong(String latlongString) throws IOException {
		URL url = new URL(URL + "?latlng=" + URLEncoder.encode(latlongString, "UTF-8") + "&sensor=false");
		URLConnection conn = url.openConnection();

		InputStream in = conn.getInputStream();
		ObjectMapper mapper = new ObjectMapper();
		GoogleResponse response = (GoogleResponse) mapper.readValue(in, GoogleResponse.class);
		in.close();
		return response;
	}
	
	private static String cliSetup(String[] args) throws IOException {
		Options options = new Options();
		
		options.addOption("k", false, "display cool message");
		options.addOption("arg1", true, "location of first place");
		options.addOption("arg2", true, "location of second place");
		options.addOption("fp", false, "full print of all debug sections");
		options.addOption("term", true, "type of place we are looking for");
		options.addOption("lim", true, "number of items you want to return from search");
		
		CommandLineParser parser = new DefaultParser();
		
		try {
			CommandLine cmd = parser.parse(options, args);
			if (cmd.hasOption("k")) {
				System.out.println("Kendall Rules");
			}
			if (cmd.hasOption("arg1")) {
				location1 = new MeetMeHalfway().convertToLatLong(cmd.getOptionValue("arg1"));
			}
			if (cmd.hasOption("arg2")) {
				location2 = new MeetMeHalfway().convertToLatLong(cmd.getOptionValue("arg2"));
			}
			if (cmd.hasOption("term")) {
				term = cmd.getOptionValue("term");
			}
			if (location1 == null || location2 == null) {
				System.out.println("One of the locations was not set prorperly");
				return "failure";
			}
			if (cmd.hasOption("fp")) {
				fullPrint = true;
			}
			if (cmd.hasOption("lim")) {
				limit = Integer.parseInt(cmd.getOptionValue("lim"));
			}
			
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return "success";
	}
	
	public static void main(String[] args) throws IOException {
		float midpointLat = 0;
		float midpointLng = 0;
		String strMidpointLat;
		String strMidpointLng;
		GoogleResponse midPointResp = null;
		
		if(cliSetup(args).equals("failure")) {
			return;
		}
		
		if (location1.getStatus().equals("OK") && location2.getStatus().equals("OK")) {
			midpointLat = Float.valueOf(location1.getResults()[0].getGeometry().getLocation().getLat())
					+ Float.valueOf(location2.getResults()[0].getGeometry().getLocation().getLat());
			midpointLat /= 2;

			midpointLng = Float.valueOf(location1.getResults()[0].getGeometry().getLocation().getLng())
					+ Float.valueOf(location2.getResults()[0].getGeometry().getLocation().getLng());
			midpointLng /= 2;
		} else {
			System.out.println(location1.getStatus() + " && " + location2.getStatus());
		}

		if (midpointLat != 0 && midpointLng != 0) {
			strMidpointLat = Float.toString(midpointLat);
			strMidpointLng = Float.toString(midpointLng);

			midPointResp = new MeetMeHalfway().convertFromLatLong(strMidpointLat + "," + strMidpointLng);
			if (midPointResp.getStatus().equals("OK") && fullPrint) {
				for (Result result : midPointResp.getResults()) {
					System.out.println("address is :" + result.getFormatted_address());
				}
			} else {
				//System.out.println(midPointResp.getStatus());
			}
		}

		YelpAPI yelpAPI = new YelpAPI();
		yelpAPI.run(term, midPointResp.getResults()[1].getFormatted_address(), limit, fullPrint);
	}
}