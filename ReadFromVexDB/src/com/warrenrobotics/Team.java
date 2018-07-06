package com.warrenrobotics;
	
import org.json.*;
/**
 * This class allows for team statistics to be parsed and stored. 
 * Current statistics:
 * 		Average OPR
 * 		Average DPR
 * 		Average CCRWM
 * 		Average Max Score
 * 		Average Ranking
 * 		Average Autonomous Points
 * 		Average Skills Points
 * 		Average TRSP Points
 * 		Total events in season
 * 		Vrating rank(Custom ranking system developed by Team BNS)
 * 		Vrating(Custom ranking system developed by Team BNS)
 * 		Average Skills Score - Autonomous
 * 		Average Skills Score - Robot
 *		Average Skills Score - Combined
 * 
 * This class is responsible for calculating and storing statistics for a given VEX team.
 * 
 * @author Robert Engle | WHS Robotics | Team 90241B
 * @version 1.0
 * @since 2018-02-21
 *
 */
public class Team {
	/*
	 Fields for team class
	*/
	//Name
	public String name;
	//JSON Array Data
	public JSONArray tData_rankings;
	public JSONObject tData_events; //Is not an array since the only needed piece of data can be acquired from "size"
	public JSONArray tData_season_rankings;
	public JSONArray tData_skills;
	//Data - Rankings
	public double avgOPR;
	public double avgDPR;
	public double avgCCWM;
	public int avgMaxScore;
	public int avgRank;
	public int bestRank;
	public int avgAP;
	public int avgSP;
	public int avgTRSP;
	//Data - Events
	public int numEvents;
	//Data - Season Rankings
	/* Vrating is a custom ranking method developed by Team BNS using a wide variety of different metrics to guage a team
	   A higher vrating represents a better team
	*/
	public int vrating_rank;
	public double vrating;
	//Data - Skills
	public int avgSkillsScore_robot;
	public int avgSkillsScore_auton;
	public int avgSkillsScore_combined;
	
	/**
	 * Constructs a Team object and runs all necessary calculations to compile statistics
	 * 
	 * @param name the name of the team(IE: 90241B)
	 * @param tData_rankings the JSONarray acquired from getting JSON array with key "result"
	 */
	public Team(String name, JSONArray tData_rankings,JSONObject tData_events, JSONArray tData_season_rankings, JSONArray tData_skills){
		//Set data
		this.name = name;
		this.tData_rankings = tData_rankings;
		this.tData_events = tData_events;
		this.tData_season_rankings = tData_season_rankings;
		this.tData_skills = tData_skills;
		//Perform calculations
		performCalculations_rankings();
		performCalculations_events();
		performCalculations_season_rankings();
		performCalculations_skills();
	}
	
	//Instance methods
	
	/**
	 * Performs all calculations under the "rankings" category
	 */
	private void performCalculations_rankings() {
		calculateAvgOPR();
		calculateAvgDPR();
		calculateAvgCCWM();
		calculateAvgMaxScore();
		calculateRanks();
		calculateAvgAP();
		calculateAvgSP();
		calculateAvgTRSP();
		
	}
	
	/**
	 * Performs all calculations under the "events" category
	 */
	private void performCalculations_events() {
		setNumEvents();
	}
	
	/**
	 * Performs all calculations under the "season_rankings" category
	 */
	private void performCalculations_season_rankings() {
		setvrating_rank();
		setvrating();
	}
	
	/**
	 * Performs all calculations under the "skills" category
	 */
	private void performCalculations_skills() {
		calculateAvgSkillsScore_auton();
		calculateAvgSkillsScore_robot();
		calculateAvgSkillsScore_combined();
	}
	/*
	------------------------------------------------------------------------------------------
	//																						//
	//									RANKING CALCULATIONS								//
	//																						//
	------------------------------------------------------------------------------------------
	*/
	//TODO: Add indicators for blank fields
	/**
	 * Calculates the average OPR and sets the classes instance variable to it
	 */
	private void calculateAvgOPR() {
		//Initialize total for average
		double totalOPR = 0.0;
		//Break up array, and search for OPR in each part
		for(int i = 0; i < tData_rankings.length(); i++) {
			//Grab value
			double opr = tData_rankings.getJSONObject(i).getDouble("opr");
			//Add to total
			totalOPR += opr;
		}
		//Compute average
		/*Avoid ArithmeticException by checking if divisor is 0*/
		if(tData_rankings.length() != 0) {
			//If nonzero, set to appropriate value
			this.avgOPR = (totalOPR + 0.0) / tData_rankings.length();
		}else {
			//If zero, set to 0. In the future, add something to indicate a 
			//blank field and report it so the spreadsheet can show that.
			this.avgOPR = 0.0;
		}
	}
	
	/**
	 * Calculates the average DPR and sets the classes instance variable to it
	 */
	private void calculateAvgDPR() {
		//Initialize total for average
		double totalDPR = 0.0;
		//Break up array, and search for DPR in each part
		for(int i = 0; i < tData_rankings.length(); i++) {
			//Grab value
			double dpr = tData_rankings.getJSONObject(i).getDouble("dpr");
			//Add to total
			totalDPR += dpr;
		}
		//Compute average
		/*Avoid ArithmeticException by checking if divisor is 0*/
		if(tData_rankings.length() != 0) {
			//If nonzero, set to appropriate value
			this.avgDPR = (totalDPR + 0.0) / tData_rankings.length();
		}else {
			//If zero, set to 0
			this.avgDPR = 0.0;
		}
	}
	
	/**
	 * Calculates the average CCWM and sets the classes instance variable to it
	 */
	private void calculateAvgCCWM() {
		//Initialize total for average
		double totalCCWM = 0.0;
		//Break up array, and search for DPR in each part
		for(int i = 0; i < tData_rankings.length(); i++) {
			//Grab value
			double ccwm = tData_rankings.getJSONObject(i).getDouble("ccwm");
			//Add to total
			totalCCWM += ccwm;
		}
		//Avoid ArithmeticException by checking if divisor is 0
		if(tData_rankings.length() != 0) {
			//If nonzero, set to appropriate value
			this.avgCCWM = (totalCCWM + 0.0) / tData_rankings.length();
		}else {
			//If zero, set to 0
			this.avgCCWM = 0.0;
		}
	}
	
	/**
	 * Calculates the average max score and sets the classes instance variable to it
	 */
	private void calculateAvgMaxScore(){
		//Initialize total for average
		int totalScore = 0;
		//Break up array, and search for max score in each part
		for(int i = 0; i < tData_rankings.length(); i++){
			//Grab value
			int maxScore = tData_rankings.getJSONObject(i).getInt("max_score");
			//Add to total
			totalScore += maxScore;
		}
		//Compute average
		/*Avoid ArithmeticException by checking if divisor is 0*/
		if(tData_rankings.length() != 0) {
			//If nonzero, set to appropriate value
			this.avgMaxScore = (int)totalScore / tData_rankings.length();
		}else {
			//If zero, set to 0
			this.avgMaxScore = 0;
		}
	}
	
	/**
	 * Parses through the teams ranks, and sets both the highest ranking and the 
	 * average ranking.
	 */
	private void calculateRanks(){
		//Initialize total for average
		int totalRank = 0;
		/* Break up array, and search though each ranking. For the average, follow
		 * the same procedure above. 
		 */
		for(int i = 0; i < tData_rankings.length(); i++) {
			//Grab value
			int rank = tData_rankings.getJSONObject(i).getInt("rank");
			//Add to total
			totalRank += rank;
		}
		//Compute average
		/*Avoid ArithmeticException by checking if divisor is 0*/
		if(tData_rankings.length() != 0) {
			//If nonzero, set to appropriate value
			this.avgRank = (int)totalRank / tData_rankings.length();
		}else {
			//If zero, set to 0
			this.avgRank = 0;
		}
	}
	
	/**
	 * Calculates the average autonomous points for a team
	 */
	private void calculateAvgAP() {
		//Initialize total for average
		int totalAP = 0;
		//Break up array, and search for APs in each part
		for(int i = 0; i < tData_rankings.length(); i++) {
			//Grab value
			int AP = tData_rankings.getJSONObject(i).getInt("ap");
			//Add to total
			totalAP += AP;
		}
		//Compute average
		/*Avoid ArithmeticException by checking if divisor is 0*/
		if(tData_rankings.length() != 0) {
			//If nonzero, set to appropriate value
			this.avgAP = (int)totalAP / tData_rankings.length();
		}else {
			//If zero, set to 0
			this.avgAP = 0;
		}

	}
	
	/**
	 * Calculates the average skills points for a team
	 */
	private void calculateAvgSP() {
		//Initialize total for average
		int totalSP = 0;
		//Break up array, and search for SPs in each part
		for(int i = 0; i < tData_rankings.length(); i++) {
			//Grab value
			int SP = tData_rankings.getJSONObject(i).getInt("sp");
			//Add to total
			totalSP += SP;
		}
		//Compute average
		/*Avoid ArithmeticException by checking if divisor is 0*/
		if(tData_rankings.length() != 0) {
			//If nonzero, set to appropriate value
			this.avgSP = (int)totalSP / tData_rankings.length();
		}else {
			//If zero, set to 0
			this.avgSP = 0;
		}

	}
	
	/**
	 * Calculates the average TRSPs (custom ranking method for SPs) for a team
	 */
	private void calculateAvgTRSP() {
		//Initialize total for average
		int totalTRSP = 0;
		//Break up array, and search for SPs in each part
		for(int i = 0; i < tData_rankings.length(); i++) {
			//Grab value
			int TRSP = tData_rankings.getJSONObject(i).getInt("trsp");
			//Add to total
			totalTRSP += TRSP;
		}
		//Compute average
		/*Avoid ArithmeticException by checking if divisor is 0*/
		if(tData_rankings.length() != 0) {
			//If nonzero, set to appropriate value
			this.avgTRSP = (int)totalTRSP / tData_rankings.length();
		}else {
			//If zero, set to 0
			this.avgTRSP = 0;
		}

	}
	
	/*
	------------------------------------------------------------------------------------------
	//																						//
	//								   EVENTS CALCULATIONS								    //
	//																						//
	------------------------------------------------------------------------------------------
	*/
	
	/**
	 * Sets the total number of events a team has competed in within the season
	 */
	private void setNumEvents() { this.numEvents = tData_events.getInt("size"); }
	
	/*
	------------------------------------------------------------------------------------------
	//																						//
	//								SEASON RANKING CALCULATIONS								//
	//																						//
	------------------------------------------------------------------------------------------
	*/
	
	/**
	 * Sets the team's vrating rank
	 */
	private void setvrating_rank() { 
		//Check if length of array is nonzero(if zero, trying to grab "vrating_rank" will throw JSONException)
		if(tData_season_rankings.length() != 0) {
			//If nonzero, set properly
			this.vrating_rank = tData_season_rankings.getJSONObject(0).getInt("vrating_rank"); 
		}else { //Is zero
			//If zero, set to zero
			this.vrating_rank = 0;
		}
		
	}
	
	/**
	 * Set's the team's vrating
	 */
	private void setvrating() { 
		//Check if length of array is nonzero(if zero, trying to grab "vrating_rank" will throw JSONException)
		if(tData_season_rankings.length() != 0) {
			//If nonzero, set properly
			this.vrating = tData_season_rankings.getJSONObject(0).getDouble("vrating");
		}else { //Is zero
			//If zero, set to zero
			this.vrating = 0.0;
		}
		 
	}
	/*
	------------------------------------------------------------------------------------------
	//																						//
	//									  SKILLS CALCULATIONS								//
	//																						//
	------------------------------------------------------------------------------------------
	*/
	/*
	 * "type":0 - Autonomous
	 * "type":1 - Robot
	 * "type":2 - Combined
	 */
	
	/**
	 * Calculates the average skills score for autonomous mode
	 */
	private void calculateAvgSkillsScore_auton() {
		//Initialize total for average
		int totalScore = 0;
		//Break up array, search for skills score in each part
		for(int i = 0; i < tData_skills.length(); i++) {
			//Only check autonomous results
			if(tData_skills.getJSONObject(i).getInt("type") == 0){
				//Grab value
				int score = tData_skills.getJSONObject(i).getInt("score");
				//Add to total
				totalScore += score;
			}
		}
		//Compute average
		/*Avoid ArithmeticException by checking if divisor is 0*/
		if(tData_skills.length() != 0) {
			//If nonzero, set to appropriate value
			this.avgSkillsScore_auton = (int)totalScore/tData_skills.length();
		}else {
			//If zero, set to zero
			this.avgSkillsScore_auton = 0;
		}
	}
	
	/**
	 * Calculates the average skills score for driver control mode
	 */
	private void calculateAvgSkillsScore_robot() {
		//Initialize total for average
		int totalScore = 0;
		//Break up array, search for skills score in each part
		for(int i = 0; i < tData_skills.length(); i++) {
			//Only check robot results
			if(tData_skills.getJSONObject(i).getInt("type") == 1){
				//Grab value
				int score = tData_skills.getJSONObject(i).getInt("score");
				//Add to total
				totalScore += score;
			}
		}
		//Compute average
		/*Avoid ArithmeticException by checking if divisor is 0*/
		if(tData_skills.length() != 0) {
			//If nonzero, set to appropriate value
			this.avgSkillsScore_robot = (int)totalScore/tData_skills.length();
		}else {
			//If zero, set to zero
			this.avgSkillsScore_robot = 0;
		}
	}
	
	/**
	 * Calculates the average skills score for both autonomous and driver control modes
	 */
	private void calculateAvgSkillsScore_combined() {
		//Initialize total for average
		int totalScore = 0;
		//Break up array, search for skills score in each part
		for(int i = 0; i < tData_skills.length(); i++) {
			//Only check combined results
			if(tData_skills.getJSONObject(i).getInt("type") == 2){
				//Grab value
				int score = tData_skills.getJSONObject(i).getInt("score");
				//Add to total
				totalScore += score;
			}
		}
		//Compute average
		/*Avoid ArithmeticException by checking if divisor is 0*/
		if(tData_skills.length() != 0) {
			//If nonzero, set to appropriate value
			this.avgSkillsScore_combined = (int)totalScore/tData_skills.length();
		}else {
			//If zero, set to zero
			this.avgSkillsScore_combined = 0;
		}
	}
	/*
	------------------------------------------------------------------------------------------
	//																						//
	//									   GETTER METHODS								    //
	//																						//
	------------------------------------------------------------------------------------------
	*/
	
	/**
	 * Retrieves the current team name(IE: "90241B")
	 * 
	 * @return the team name
	 */
	public String getName() { return this.name; }
	/**
	 * Retrieves the average OPR for select team(average of all matches in season)
	 * 
	 * @return the average OPR of the team
	 */
	public double getAvgOPR() { return avgOPR; }
	
	/**
	 * Retrieves the average DPR for select team(average of all matches in season)
	 * 
	 * @return the average DPR of the team
	 */
	public double getAvgDPR() { return avgDPR; }
	
	/**
	 * Retrieves the average DPR for select team(average of all matches in season)
	 * 
	 * @return the average CCWM of the team
	 */
	public double getAvgCCWM() { return avgCCWM; }
	
	/**
	 * Retrieves the average max score for select team(of all matches in season)
	 * 
	 * @return the average max score of the team
	 */
	public int getAvgMaxScore() { return avgMaxScore; }
	
	/**
	 * Retrieves the best rank that a team has achieved throughout the season
	 * 
	 * @return the best rank of the team
	 */
	public int getBestRank() { return bestRank; }
	
	/**
	 * Retrieves the average rank that a team has achieved throughout the season
	 * NOTE:Best ranking WON'T appear in spreadsheet
	 * 
	 * @return the average rank of the team
	 */
	public int getAvgRank() { return avgRank; }
	
	/**
	 * Retrieves average autonomous points for a team
	 * 
	 * @return a rounded-down integer of the average autonomous points
	 */
	public int getAvgAP() { return this.avgAP; }
	
	/**
	 * Retrieves average skills points for a team
	 * 
	 * @return a rounded-down integer of the average skills points
	 */
	public int getAvgSP() { return this.avgSP; }
	
	/**
	 * Retrieves average TRSP points for a team
	 * 
	 * @return a rounded-down integer of the average TRSP points
	 */
	public int getAvgTRSP() { return this.avgTRSP; }
	
	/**
	 * Retrieves the number of events a team has competed in during the season
	 * 
	 * @return the number of events
	 */
	public int getNumEvents() { return this.numEvents; }
	
	/**
	 * Retrieves the vrating rankings of a team in the season
	 * 
	 * @return the vrating ranking
	 */
	public int getvrating_rank() { return this.vrating_rank; }
	
	/**
	 * Retrieves the vrating of a team in the season
	 * 
	 * @return the vrating
	 */
	public double getvrating() { return this.vrating; }
	
	/**
	 * Retrieves the average skills score for autonomous mode
	 * 
	 * @return the average skills score for autonomous
	 */
	public int getAvgSkillsScore_auton() { return this.avgSkillsScore_auton; }
	
	/**
	 * Retrieves the average skills score for driver control mode
	 * 
	 * @return the average skills score for driver control mode
	 */
	public int getAvgSkillsScore_robot() { return this.avgSkillsScore_robot; }
	
	/**
	 * Retrieves the average skills score for both autonomous and driver control mode
	 * 
	 * @return the average skills score for both autonomous and driver control mode
	 */
	public int getAvgSkillsScore_combined() { return this.avgSkillsScore_combined; }
	
	/**
	 * A toString method that simply returns the team name
	 */
	public String toString() { return "Team " + this.name; }
}