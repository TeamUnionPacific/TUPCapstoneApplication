package tk.tup.capstone;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;

import java.sql.*;
//import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.jws.Oneway;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 * @author Daniel Agbay
 */
@WebService(serviceName = "VoiceAssistantOps")
public class VoiceAssistantOps {
    
    private final String db_host = "jdbc:mysql://174.138.38.48/UnionPacific";
    private final String db_username = "unionpacific";
    private final String db_password = "UnionPacificDB";

    /**
     * Web service operation
     * @param trainLineupId
     * @param timeFormat
     * @return String
     */
    @WebMethod(operationName = "getTrainLineup")
    public String getTrainLineup(@WebParam(name = "trainLineupId") int trainLineupId, @WebParam(name = "timeFormat") String timeFormat) {
        Connection conn;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(db_host,db_username,db_password);
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(VoiceAssistantOps.class.getName()).log(Level.SEVERE, null, ex);
            return "Database connection issue";
        }

        String result;
        try {
            // set up the query to get trains based off a lineup id
            String query = "SELECT TrainId, DepartureTime, Name, LineupId FROM TrainSchedule WHERE LineupId = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, trainLineupId); // set the query parameter to trainLineupId

            // execute the query
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            
            /*** convert to JSON ***/
            JSONObject resultJson = new JSONObject();

            // add lineup id
            resultJson.put("lineupId", trainLineupId);

            // add columns
            ArrayList<String> columns = new ArrayList<>();
            int total_cols = rsmd.getColumnCount();
            for(int i=1; i<=total_cols; i++){
              String col = rsmd.getColumnLabel(i);
              columns.add(col);
            }
            resultJson.put("columns", columns);

            // add row data
            ArrayList<ArrayList<String>> rows = new ArrayList<>();
            while(rs.next()){
              ArrayList<String> row = new ArrayList<>();

              row.add(rs.getString("TrainId"));
              
              String depTime = rs.getString("DepartureTime");
              row.add(formatTime(depTime, timeFormat));
              
              row.add(rs.getString("Name"));
              row.add(rs.getString("LineupId"));

              rows.add(row);
            }
            resultJson.put("rows", rows);

            // convert json object to string and close conn
            result = resultJson.toString();
            conn.close();
            
        } catch (SQLException ex){
            result = "SQL query error.";
        }

        return result;
    }
    
    /**
     * Return time formatted to Military or AMPM time
     * @param depTime
     * @param timeFormat
     * @return String
     */
    public String formatTime(String depTime, String timeFormat){              
        DateTimeFormatter military = DateTimeFormat.forPattern("HH:mm:ss");
        LocalTime time = military.parseLocalTime(depTime);
        String militaryTime = military.print(time);
        if("AMPM".equals(timeFormat)){
            DateTimeFormatter ampm = DateTimeFormat.forPattern("hh:mm:ss a");
            String ampmTime = ampm.print(time);
            return ampmTime;
        }
        else{
            return militaryTime;
        }        
    }

    /**
     * Web service operation
     * @param assistantId
     * @return integer
     */
    @WebMethod(operationName = "getEmpPosition")
    public String getEmpPosition(@WebParam(name = "assistantId") String assistantId) {
        Random rand = new Random();
        // 25 is maximum, 1 is minimum
        Integer position = rand.nextInt(25) + 1;
        
        JSONObject resultJson = new JSONObject();
        resultJson.put("position", position);
        
        return resultJson.toString();
    }

    /**
     * Web service operation
     * @param assistantId
     * @param query
     * @param intent
     * @param assistant
     * @param error
     */
    @WebMethod(operationName = "generateLog")
    @Oneway
    public void generateLog(@WebParam(name = "assistantId") String assistantId, @WebParam(name = "query") String query, @WebParam(name = "intent") String intent, 
            @WebParam(name = "assistant") String assistant, @WebParam(name = "error") String error) {
        
        Connection conn;
        String UnionPacificId = "";
        
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(db_host,db_username,db_password);
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(VoiceAssistantOps.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        try{
            String sqlQuery = "";
            // query for union pacific id based on the assistant
            if("Google".equals(assistant)){
                sqlQuery = "SELECT UnionPacificId FROM users WHERE GoogleAssistantId = ?";
                //select UnionPacificId from users where GoogleAssistantId = assistantId
            }
            else if("Amazon".equals(assistant)){
                sqlQuery = "SELECT UnionPacificId FROM users WHERE AmazonAlexaId = ?";
                //select UnionPacificId from users where AmazonAlexaId = assistantId
            }
            // prepare and execute the query
            PreparedStatement stmt = conn.prepareStatement(sqlQuery);
            stmt.setString(1, assistantId);
            ResultSet rs = stmt.executeQuery();
            
            // retrieve the UnionPacificId
            while(rs.next()){
                UnionPacificId = rs.getString("UnionPacificId");
            }
            
        } catch(SQLException ex){
            
        }
        
        try{
            // log the usage into the database
            String sqlQuery = "INSERT INTO AssistantQueryLogs (UnionPacificId, Query, Intent, Assistant, Error, AddDate) VALUES (?,?,?,?,?,NOW())";
            PreparedStatement stmt = conn.prepareStatement(sqlQuery);
            stmt.setString(1, UnionPacificId);
            stmt.setString(2, query);
            stmt.setString(3, intent);
            stmt.setString(4, assistant);
            stmt.setString(5, error);
            stmt.executeUpdate();
        } catch(SQLException ex){
            System.out.println(ex);
        }
         
    }

    /**
     * Web service operation
     * @param AmazonAlexaId
     * @return PreferredName
     */
    @WebMethod(operationName = "getPreferences")
    public String getPreferences(@WebParam(name = "AmazonAlexaId") String AmazonAlexaId) {
        Connection conn;
        String PreferredName = "";
        String TimeFormat = "";
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(db_host,db_username,db_password);
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(VoiceAssistantOps.class.getName()).log(Level.SEVERE, null, ex);
            return "";
        }
        
        try {
            String query = "SELECT PreferredName, TimeFormat FROM users WHERE AmazonAlexaId = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, AmazonAlexaId);
            ResultSet rs = stmt.executeQuery();
            while(rs.next()){
                PreferredName = rs.getString("PreferredName");
                TimeFormat = rs.getString("TimeFormat");
            }
            
        } catch(SQLException ex) {
            
        }
        
        JSONObject resultJson = new JSONObject();
        resultJson.put("PreferredName", PreferredName);
        resultJson.put("TimeFormat", TimeFormat);
        return resultJson.toString();
    }

    /**
     * Web service operation
     * @param AmazonAlexaId
     * @param PreferredName
     */
    @WebMethod(operationName = "updatePreferredName")
    @Oneway
    public void updatePreferredName(@WebParam(name = "AmazonAlexaId") String AmazonAlexaId, @WebParam(name = "PreferredName") String PreferredName) {
        Connection conn;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(db_host,db_username,db_password);
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(VoiceAssistantOps.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        try {
            String query = "UPDATE users SET PreferredName = ? WHERE AmazonAlexaId = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, PreferredName);
            stmt.setString(2, AmazonAlexaId);
            stmt.executeUpdate();
        } catch(SQLException ex) {}
    }

    /**
     * Web service operation
     * @param AmazonAlexaId
     * @param TimeFormat
     */
    @WebMethod(operationName = "updateTimeFormat")
    @Oneway
    public void updateTimeFormat(@WebParam(name = "AmazonAlexaId") String AmazonAlexaId, @WebParam(name = "TimeFormat") String TimeFormat) {
        Connection conn;
        try {
            Class.forName("com.mysql.jdbc.Driver");
            conn = DriverManager.getConnection(db_host,db_username,db_password);
        } catch (ClassNotFoundException | SQLException ex) {
            Logger.getLogger(VoiceAssistantOps.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        
        try {
            String query = "UPDATE users SET TimeFormat = ? WHERE AmazonAlexaId = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, TimeFormat);
            stmt.setString(2, AmazonAlexaId);
            stmt.executeUpdate();
        } catch(SQLException ex) {}
    }
    
    
}
