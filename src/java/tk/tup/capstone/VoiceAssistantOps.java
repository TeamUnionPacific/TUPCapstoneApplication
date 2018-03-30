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
     * @return String
     */
    @WebMethod(operationName = "getTrainLineup")
    public String getTrainLineup(@WebParam(name = "trainLineupId") int trainLineupId) {
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
              row.add(rs.getString("DepartureTime"));
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
     * Web service operation
     * @return integer
     */
    @WebMethod(operationName = "getEmpPosition")
    public String getEmpPosition() {
        Random rand = new Random();
        // 25 is maximum, 1 is minimum
        Integer position = rand.nextInt(25) + 1;
        
        JSONObject resultJson = new JSONObject();
        resultJson.put("position", position);
        
        String result = resultJson.toString();
        return result;
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
    
    
}
