package com.warrenrobotics;

import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.SpreadsheetProperties;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;

/**
 * Interacts with Google Sheets using the Google Sheets API v4 to automatically 
 * assign certain statistics to them.<br><br>
 * 
 * 
 * This class is responsible for all interactions with Google Sheets.
 * 
 * @author Robert Engle | WHS Robotics | Team 90241B
 * @version 1.2
 * @since 2018-02-21
 *
 */
public class TeamAPI {
	//Spreadsheet/user information
	private String spreadsheetId; 
	private String spreadsheetURL;
	private String usrEmail;
	//Event information
	private String season;
	private String eventName;
	private String[] teamList;
	private String sku;
	//Authentication information
	private String accessToken_sheets;
	private String accessToken_drive;
	private ValueRange response; //Currently depreciated
	private GoogleCredential credential_sheets;
	private GoogleCredential credential_drive;
	//Constants
	public final JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
	
	/**
	 * Constructs a TeamAPI object to interpret data from a Google Sheets using the Google Sheets API v4
	 * 
	 * @param spreadsheetId the id of the spreadsheet(commonly found in the link of the spreadsheet)
	 * @param link the URL of the RobotEvents page
	 */
	public TeamAPI(String link, String usrEmail) throws IOException, GeneralSecurityException, InterruptedException{
		//Print date of start time
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.printf("%s - Running Program%n", dateFormat.format(date));
		//Set user email
		this.usrEmail = usrEmail;
		//Process link into SKU, grab season, set event name, and set team list
		processLink(link);
		//Assign access tokenS
		setAccessToken_sheets();
		setAccessToken_drive();
		//Assign credentials
		setCredential_sheets();
		setCredential_drive();
		//Create sheet service with authenticated credential
		Sheets sheetsService = createSheetsService();
		//Create drive service with authenticated credential
		Drive driveService = createDriveService();
		//Create spreadsheet
		executeCreateRequest(sheetsService);
		//Execute a write request
		executeWriteRequest(sheetsService);
		//Transfer ownership
		transferOwnership(driveService);
	}
	
	/*
	------------------------------------------------------------------------------------------
	//																						//
	//								  AUTHENTICATION METHODS								//
	//																						//
	------------------------------------------------------------------------------------------
	*/
	
	/**
	 * Builds and sets an authenticated credential for a Sheets objects
	 * 
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	private void setCredential_sheets() throws GeneralSecurityException, IOException {
		//Create new transport
		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
	    //Print message
		System.out.println("Building authenticated credential(Sheets API)...");
		//Build authenticated credential
		GoogleCredential newCredential = new GoogleCredential.Builder()
				.setTransport(httpTransport)
				.setClientSecrets(Constants.GOOGLE_CLIENT_ID_SHEETS, Constants.GOOGLE_CLIENT_SECRET_SHEETS)
				.build()
				.setAccessToken(this.accessToken_sheets);
		this.credential_sheets = newCredential;
	}
	
	/**
	 * Builds and sets an authenticated credential for a Drive object
	 * 
	 * @throws GeneralSecurityException
	 * @throws IOException for when an I/O error occurs
	 */
	private void setCredential_drive() throws GeneralSecurityException, IOException {
		//Create new transport
		HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
	    //Print message
		System.out.println("Building authenticated credential(Drive API)...");
		//Build authenticated credential
		GoogleCredential newCredential = new GoogleCredential.Builder()
				.setTransport(httpTransport)
				.setClientSecrets(Constants.GOOGLE_CLIENT_ID_DRIVE, Constants.GOOGLE_CLIENT_SECRET_DRIVE)
				.build()
				.setAccessToken(this.accessToken_drive);
		this.credential_drive = newCredential;
	}
	
	/**
	 * Creates a Sheets object that can be used to make a request for data
	 * 
	 * @return a Sheets object that can be used to grab and write data
	 * @throws IOException for when an I/O error occurs
	 * @throws GeneralSecurityException
	 */
	private Sheets createSheetsService() throws IOException, GeneralSecurityException {
		//Create new transport
		HttpTransport httpTransportSheets = GoogleNetHttpTransport.newTrustedTransport();
	    //Build a Sheets object and return it
	    return new Sheets.Builder(httpTransportSheets, jsonFactory, this.credential_sheets)
	        .setApplicationName("VexInfo.io - Sheets Usage")
	        .build();
	}
	
	/**
	 * Creates a Drive object that can be used to make a request for data
	 * 
	 * @return a Drive object that can be used to edit permissions
	 * @throws IOException for when an I/O error occurs
	 * @throws GeneralSecurityException
	 */
	private Drive createDriveService() throws IOException, GeneralSecurityException{
		//Create new transport
		HttpTransport httpTransportDrive = GoogleNetHttpTransport.newTrustedTransport();
		//Build drive object and return it
		return new Drive.Builder(httpTransportDrive, jsonFactory, this.credential_drive)
				.setApplicationName("VexInfo.io - Drive Usage")
				.build();
	}

	/**
	 * Retrieves an access token using the refresh token for the Sheets API
	 * 
	 * @throws IOException for when an I/O error occurs
	 * @GeneralSecurityException 
	 */
	public void setAccessToken_sheets() throws IOException, GeneralSecurityException{
		/*
		 * Note to users who plan to use this:
		 * 
		 * On Github, the Constants.java file will not show since I put it in 
		 * git ignore, due to it having sensitive data. In order to use this on
		 * your own, make a new file Constants.java as an interface, and simply input
		 * the values "GOOGLE_CLIENT_ID_SHEETS" and "GOOGLE_CLIENT_SECRET_SHEETS", as well as 
		 * "GOOGLE_REFRESH_TOKEN_SHEETS".
		 */
		//Create a token response using refresh token and oauth credentials
		TokenResponse token_response = new GoogleRefreshTokenRequest(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), 
				Constants.GOOGLE_REFRESH_TOKEN_SHEETS, Constants.GOOGLE_CLIENT_ID_SHEETS, Constants.GOOGLE_CLIENT_SECRET_SHEETS)
				.execute();
		//Set the access token
		this.accessToken_sheets = token_response.getAccessToken();   
	}
	
	/**
	 * Retrieves an access token using the refresh token for the Drive API
	 * @throws IOException
	 * @throws GeneralSecurityException
	 */
	public void setAccessToken_drive() throws IOException, GeneralSecurityException {
		/*
		 * Note to users who plan to use this:
		 * 
		 * On Github, the Constants.java file will not show since I put it in 
		 * git ignore, due to it having sensitive data. In order to use this on
		 * your own, make a new file Constants.java as an interface, and simply input
		 * the values "GOOGLE_CLIENT_ID_DRIVE" and "GOOGLE_CLIENT_SECRET_DRIVE", as well as 
		 * "GOOGLE_REFRESH_TOKEN_DRIVE".
		 */
		//Create a token response using refresh token and oauth credentials
		TokenResponse token_response = new GoogleRefreshTokenRequest(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), 
				Constants.GOOGLE_REFRESH_TOKEN_DRIVE, Constants.GOOGLE_CLIENT_ID_DRIVE, Constants.GOOGLE_CLIENT_SECRET_DRIVE)
				.execute();
		//Set the access token
		this.accessToken_drive = token_response.getAccessToken();
	}
	
	//IMPLEMENT LATER - GETTING TOKENS FROM AUTHORIZATION CODE USING POST
	/*
	public void setTokens() throws ClientProtocolException, IOException {
		HttpClient client = new DefaultHttpClient();
		HttpPost post = new HttpPost("https://accounts.google.com/o/oauth2/token");
		List<NameValuePair> pairs = new ArrayList<>();
		pairs.add(new BasicNameValuePair("code", Constants.GOOGLE_OAUTH2_AUTHCODE));
	    pairs.add(new BasicNameValuePair("client_id", Constants.GOOGLE_CLIENT_ID));
	    pairs.add(new BasicNameValuePair("client_secret", Constants.GOOGLE_CLIENT_SECRET));
	    pairs.add(new BasicNameValuePair("redirect_uri", "https://developers.google.com/oauthplayground"));
	    pairs.add(new BasicNameValuePair("grant_type", "authorization_code"));
	    post.setEntity(new UrlEncodedFormEntity(pairs));
	    org.apache.http.HttpResponse response = client.execute(post);
	    String responseBody = EntityUtils.toString(response.getEntity());
	    System.out.println(responseBody);
	}
	*/
	
	/*
	------------------------------------------------------------------------------------------
	//																						//
	//									 SHEETS METHODS										//
	//																						//
	------------------------------------------------------------------------------------------
	*/
	
	/**
	 * Executes a create request to make a new spreadsheet
	 * 
	 * @param sheetsService the Sheets object with an authenticated credential
	 * @throws IOException for when an I/O error occurs
	 */
	public void executeCreateRequest(Sheets sheetsService) throws IOException {
		System.out.printf("Creating Spreadsheet for Event...%nEvent Name: %s%n", this.eventName);
		//Time how long algorithmn takes
		long curTime = System.currentTimeMillis();
		//Create a request body and set appropriate title
		Spreadsheet requestBody = new Spreadsheet()
				.setProperties(new SpreadsheetProperties().set("title", "VexInfo.io - " + this.eventName));
		//Create a request to create a spreadsheet
		Sheets.Spreadsheets.Create request = sheetsService.spreadsheets().create(requestBody);
		//Execute the request and grab response
		Spreadsheet response = request.execute();
		//Set the proper spreadsheetId for the rest of the program
		this.spreadsheetId = response.getSpreadsheetId();
		//Set the URL of spreadsheet
		this.spreadsheetURL = response.getSpreadsheetUrl();
		//Get how long algorithmn has taken
		long timeTaken = System.currentTimeMillis() - curTime;
		//Print success message(Format below)
		System.out.printf("Sheet Created In %d ms%n%s%n", timeTaken, this.spreadsheetURL);
	}
	
	/**
	 * Executes a get request for all data in the spreadsheet<br><br>
	 * 
	 * CURRENTLY DEPRECIATED UNTIL FURTHER NOTICE. 
	 * MAY BE USED IN THE EVENT THAT A USER WOULD WANT TO MAKE A SPREADSHEET WITH ONLY SPECIFIC TEAMS
	 * 
	 * @param sheetsService the Sheets object with an authenticated credential
	 * @throws IOException for when an I/O error occurs
	 */
	public void executeGetRequest(Sheets sheetsService) throws IOException{
		//Setup a request for getting spreadsheet data
		Sheets.Spreadsheets.Values.Get request =
		    sheetsService.spreadsheets().values().get(this.spreadsheetId, "Sheet1");
		    request.setValueRenderOption("FORMATTED_VALUE");
		    request.setDateTimeRenderOption("SERIAL_NUMBER");
		//Get a response as a ValueRange(which can converted to JSON Objects)
		this.response = request.execute();
	}
	
	/**
	 * Executes a write request to write data to a spreadsheet.
	 * 
	 * @param sheetsService the Sheets object with an authenticated credential
	 * @throws IOException for when an I/O error occurs
	 * @throws InterruptedException for when a thread is being occupied and interrupted
	 */
	public void executeWriteRequest(Sheets sheetsService) throws IOException, InterruptedException{
		//Debugging for how long algorithm takes to run with certain data sets
		long startTime = System.currentTimeMillis();
		//Initialize ApilRateLimiter object
		//ApilRateLimiter apiRateLimiter = new ApilRateLimiter(Constants.SHEETS_QUOTA_PER_SECOND); // Currently not using
		//Build column #1 of the spreadsheet
		String[] names = new String[19];
		//Assign proper values
		putNames(names);
		//Build list
		List<List<Object>> topValues = Arrays.asList(Arrays.asList(names));
		//Configure body for request as ValueRange
		ValueRange topBody = new ValueRange().setValues(topValues);
		//Build request and execute
		@SuppressWarnings("unused")
		UpdateValuesResponse topResult = 
				sheetsService.spreadsheets().values().update(this.spreadsheetId, "Sheet1!A1:S1", topBody)
				.setValueInputOption("USER_ENTERED")
				.setIncludeValuesInResponse(false)
				.execute();
		//Print initialize message
		System.out.printf("Initialize - %d Teams%n-----------------------------------------------------%n", teamList.length);
		//Loop through team list
		for(int i = 0; i < teamList.length; i++) {
			//Time how long each loop takes
			long sTime = System.currentTimeMillis();
			//Start values as null
			List<List<Object>> values = null;
			String range = null;
			String printMsg = null;
			//ONE-TEAM V. TWO-TEAM SETTINGS
			if((i + 1) == teamList.length) {//At end, grabbing second team will throw out of bounds exception
				//ONE-TEAM SETTING
				//Grab team name
				String n = teamList[i];
				//Parse team and calculate data
				Team t = new Team.TeamBuilder(n, this.season)
						.setTeamData()
						.setEventData()
						.setRankingData()
						.setSeasonData()
						.setSkillsData()
						.build();
				//Initialize array for inputting data
				String[] valuesArr = new String[19];
				//Build array with proper data
				buildValues(valuesArr, t);
				//Configure body for input
				values = Arrays.asList(Arrays.asList(valuesArr));
				//Configure range as Sheet1!F#:S# where # is a number based on the current team(i+2)
				range = "Sheet1!A" + (i + 2) + ":S" + (i + 2);
				//Setup print message
				printMsg = "COLUMN#" + (i + 2) + " STATS UPDATED: " + t.number + " (";
			}else {//Can still grab two teams without exception
				//TWO-TEAM SETTING
				//Grab first team name
				String n1 = teamList[i];
				//Parse team 1 and calculate data
				Team t1 = new Team.TeamBuilder(n1, this.season)
						.setTeamData()
						.setEventData()
						.setRankingData()
						.setSeasonData()
						.setSkillsData()
						.build();
				//Initialize array for inputting data
				String[] valuesArr1 = new String[19];
				//Build array with proper data
				buildValues(valuesArr1, t1);
				//Grab second team name
				String n2 = teamList[i + 1];
				//Parse team 2 and calculate data
				Team t2 = new Team.TeamBuilder(n2, this.season)
						.setTeamData()
						.setEventData()
						.setRankingData()
						.setSeasonData()
						.setSkillsData()
						.build();
				//Initialize array for inputting data
				String[] valuesArr2 = new String[19];
				//Build array with proper data
				buildValues(valuesArr2, t2);
				//Configure body for input
				values = Arrays.asList(Arrays.asList(valuesArr1), Arrays.asList(valuesArr2));
				//Configure range as Sheet1!F#:S# where # is a number based on the current team and next team(i+3)
				range = "Sheet1!A" + (i + 2) + ":S" + (i + 3);
				//Setup print message
				printMsg = String.format("COLUMN# %d,%d UPDATED: %s, %s(", (i+2), (i+3), t1.number, t2.number);
				//Increment counter since we go by two's in this mode
				i++;
			}
			//Configure body as a ValueRange object
			ValueRange body = new ValueRange().setValues(values);
			//Reserve quota(currently disabled, quota was updated by google)
			//apiRateLimiter.reserve(Constants.SHEETS_QUOTA_PER_SECOND);
			//Time how long write request takes
			long tTime = System.currentTimeMillis();
			//Send write request and receive response
			@SuppressWarnings("unused")
			UpdateValuesResponse result = 
					sheetsService.spreadsheets().values()
					.update(this.spreadsheetId, range, body)
					.setValueInputOption("USER_ENTERED")
					.setIncludeValuesInResponse(false)
					.execute();
			//Grab current time
			long nextTime = System.currentTimeMillis();
			//Grab how long it took in total
			long timeTakenTotal = nextTime - sTime;
			//Grab how long it took for just the write request
			long timeTakenWrite = nextTime - tTime;
			//Print out success message
			System.out.printf("%s%dms total, %dms write)%n", printMsg, timeTakenTotal, timeTakenWrite);
		}
		//Establish how long algorithm took to run(milliseconds)
		long runtime = System.currentTimeMillis() - startTime;
		//Convert to seconds
		double runtimeInSeconds = (double)runtime/1000;
		//Print success message
		System.out.printf("Success - %d TEAMS UPDATED IN %f SECONDS%n", teamList.length, runtimeInSeconds);
		//Print break
		System.out.println("-----------------------------------------------------");
	}
	/*
	------------------------------------------------------------------------------------------
	//																						//
	//									  DRIVE METHODS										//
	//																						//
	------------------------------------------------------------------------------------------
	*/
	
	/**
	 * Transfers the ownership of the Google Sheet to usrEmail, which is specified
	 * in the constructor of {@link TeamAPI}
	 * 
	 * @param driveService an authenticated Drive object
	 * @throws IOException for when an I/O error occurs
	 */
	private void transferOwnership(Drive driveService) throws IOException {
		//Print message
		System.out.printf("Transferring ownership to %s%n", this.usrEmail);
		//Time how long it takes
		long curTime = System.currentTimeMillis();
		//Build request body
		Permission body = new Permission()
				.setRole("owner")
				.setType("user")
				.setEmailAddress(this.usrEmail);
		//Execute Drive request
		@SuppressWarnings("unused")
		Permission permission = driveService.permissions().create(this.spreadsheetId, body)
				.setFileId(this.spreadsheetId)
				.setEmailMessage(String.format("VexInfo.io - %s%n%n%s", this.eventName, this.spreadsheetURL))
				.setSendNotificationEmail(true)
				.setSupportsTeamDrives(true)
				.setTransferOwnership(true)
				.setUseDomainAdminAccess(false)
				.setFields("emailAddress")
				.execute();
		//Time taken
		double timeTaken = ((double)System.currentTimeMillis() - curTime)/1000;
		//Print message
		System.out.printf("Ownership transferred to %s(%f ms)%n", this.usrEmail, timeTaken);
	}
	
	/*
	------------------------------------------------------------------------------------------
	//																						//
	//									PROCESSING METHODS									//
	//																						//
	------------------------------------------------------------------------------------------
	*/
	
	/**
	 * Processes the response into an array of strings containing team names(IE: ["90241A", "90241B"])<br><br>
	 * 
	 * CURRENTLY DEPRECIATED UNTIL FURTHER NOTICE. 
	 * MAY BE USED IN THE EVENT THAT A USER WOULD WANT TO MAKE A SPREADSHEET WITH ONLY SPECIFIC TEAMS
	 * 
	 * @return the team names as an array of strings
	 */
	@SuppressWarnings("unused")
	private void processResponseIntoTeamList() {
		//Build string from ValueRange
		String responseStr = this.response.toString();
		//Build a json object from the string
		JSONObject json = new JSONObject(responseStr);
		//Build a json array from the "values" section
		JSONArray values = json.getJSONArray("values");
		//Make an array of strings for team names. Is length - 1 since the first iteration of values doesn't contain a team
		String[] teams = new String[values.length() - 1];
		//Start at index 1 to ignore top of row(which shows "Team")
		for(int i = 1; i < values.length(); i++) {
			//Must input at i - 1 since array starts at 0 still
			teams[i - 1] = values.getJSONArray(i).getString(0);
		}
		//Set the class team list
		this.teamList = teams;
	}
	
	/**
	 * Processes a RobotEvents.com link to be able to get an events SKU, 
	 * the season for that event, the name of the event, and a team list
	 * for the event.<br><br>
	 * 
	 * <b>Note:</b> Team lists can only be generated <u>4 weeks</u> before the start date
	 * of the tournament
	 * 
	 * @param s the URL of the robot events link
	 * @throws JSONException for when JSON API encounters error
	 * @throws IOException for when an I/O error occurs
	 */
	private void processLink(String s) throws JSONException, IOException {
		//Create URL from link
		URL link = new URL(s);
		//Get file path of url
		String[] filePath = link.getPath().split("/");
		//Get and set event code
		this.sku = filePath[filePath.length - 1].replaceAll(".html", "");
		//Get and set season from API
		JSONObject eventJson = Team.TeamBuilder.readJsonFromUrl("https://api.vexdb.io/v1/get_events?sku=" + this.sku)
				.getJSONArray("result")
				.getJSONObject(0);
		this.season = eventJson.getString("season");
		//Set event name
		this.eventName = eventJson.getString("name");
		//Build JSON array from SKU
		JSONArray result = Team.TeamBuilder
				.readJsonFromUrl("https://api.vexdb.io/v1/get_teams?sku=" + this.sku)
				.getJSONArray("result");
		//Build team list
		String[] teams = new String[result.length()];
		for(int i = 0; i < result.length(); i++) {
			teams[i] = result.getJSONObject(i).getString("number");
		}
		this.teamList = teams;
	}
	
	/**
	 * Builds values in an array representing certain statistics of the team.
	 * <ul>
	 * 		<b>Writes in this specific order:</b>(19 fields total)
	 * 		<ol start=0>
	 * 			<li>number</li>
	 * 			<li>teamName</li>
	 * 			<li>teamOrg</li>
	 * 			<li>teamLocation</li>
	 * 			<li>teamLink</li>
	 * 			<li>avgOPR</li>
	 * 			<li>avgDPR</li>
	 * 			<li>avgCCWM</li>
	 * 			<li>avgAP</li>
	 * 			<li>avgSP</li>
	 * 			<li>avgTSRP</li>
	 * 			<li>vratingRank</li>
	 * 			<li>vrating</li>
	 * 			<li>avgRank</li>
	 * 			<li>avgSkills_auton</li>
	 * 			<li>avgSkills_robot</li>
	 * 			<li>avgSkills_combined</li>
	 * 			<li>avgMaxScore</li>
	 * 			<li>totalEvents</li>
	 * 		</ol>
	 * </ul>
	 * @param arr the array to write the statistics to
	 * @param t the team who the statistics are for
	 */
	
	@SuppressWarnings("static-access")
	private void buildValues(String[] arr, Team t) {
		//TODO: Iteravely build a list instead of setting individual values
		//0.
		arr[0] = t.getNumber();
		//1.
		arr[1] = t.getTeamName();
		//2.
		arr[2] = t.getTeamOrg();
		//3.
		arr[3] = t.getTeamLocation();
		//4.
		arr[4] = t.getTeamLink();
		//5.
		arr[5] = t.fieldIndicators.get("opr") ? Double.toString(t.getAvgOPR()) : "NOT_FOUND";
		//6.
		arr[6] = t.fieldIndicators.get("dpr") ? Double.toString(t.getAvgDPR()) : "NOT_FOUND";
		//7.
		arr[7] = t.fieldIndicators.get("ccwm") ? Double.toString(t.getAvgCCWM()) : "NOT_FOUND";
		//8.
		arr[8] = t.fieldIndicators.get("ap") ? Double.toString(t.getAvgAP()) : "NOT_FOUND";
		//9.
		arr[9] = t.fieldIndicators.get("sp") ? Integer.toString(t.getAvgSP()) : "NOT_FOUND";
		//10.
		arr[10] = t.fieldIndicators.get("trsp") ? Integer.toString(t.getAvgTRSP()) : "NOT_FOUND";
		//11.
		arr[11] = t.fieldIndicators.get("vrating_rank") ? Integer.toString(t.getvrating_rank()) : "NOT_FOUND";
		//12.
		arr[12] = t.fieldIndicators.get("vrating") ? Double.toString(t.getvrating()) : "NOT_FOUND";
		//13.
		arr[13] = t.fieldIndicators.get("rank") ? Integer.toString(t.getAvgRank()) : "NOT_FOUND";
		//14.
		arr[14] = t.fieldIndicators.get("skills_auton") ? Integer.toString(t.getAvgSkillsScore_auton()) : "NOT_FOUND";
		//15.
		arr[15] = t.fieldIndicators.get("skills_robot") ? Integer.toString(t.getAvgSkillsScore_robot()) : "NOT_FOUND";
		//16.
		arr[16] = t.fieldIndicators.get("skills_combined") ? Integer.toString(t.getAvgSkillsScore_combined()) : "NOT_FOUND";
		//17.
		arr[17] = t.fieldIndicators.get("max_score") ? Integer.toString(t.getAvgMaxScore()) : "NOT_FOUND";
		//18.
		arr[18] = Integer.toString(t.getNumEvents());
	}
	
	public void putNames(String[] a) {
		//Represents column #1 of the spreadsheet
		a[0] = "Team";
		a[1] = "Team Name";
		a[2] = "Organization";
		a[3] = "Location";
		a[4] = "VexDB Link";
		a[5] = "Average OPR";
		a[6] = "Average DPR";
		a[7] = "Average CCWM";
		a[8] = "Average AP's";
		a[9] = "Average SP's";
		a[10] = "Average TSRP's";
		a[11] = "Vrating Rank";
		a[12] = "Vrating";
		a[13] = "Average Rank";
		a[14] = "Average Skills Score(Auton)";
		a[15] = "Average Skills Score(Robot)";
		a[16] = "Average Skills Score(Combined)";
		a[17] = "Average Max Score";
		a[18] = "Total Events This Season";
	}
	/*
	------------------------------------------------------------------------------------------
	//																						//
	//									   GETTER METHODS								    //
	//																						//
	------------------------------------------------------------------------------------------
	*/
	
	/**
	 * Retrieves the current team list provided by proccessResponseIntoTeamList()
	 * 
	 * @return the team list as an array of Strings
	 */
	public List<String> getTeamList() { return Arrays.asList(this.teamList); }
	
	public String toString() {
		return String.format("VexInfo.io - %s (%s)", this.eventName, this.spreadsheetId);
	}
}