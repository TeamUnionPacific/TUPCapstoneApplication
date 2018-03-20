/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tk.tup.trainlineup2;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;

import java.sql.*;
//import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Random;
import org.json.JSONException;

/**
 *
 * @author agbaydan
 */
@WebService(serviceName = "TrainLineup")
public class TrainLineup {

    /**
     * Web service operation
     */
    @WebMethod(operationName = "getTrainLineup")
    public String getTrainLineup(@WebParam(name = "trainLineupId") int trainLineupId) {
        String result = "";

        try{
          // connect to database
          Class.forName("com.mysql.jdbc.Driver");
          Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/UnionPacific","unionpacific","UnionPacificDB");
          System.out.println("Connection Succeeded!");

          // set up the query to get trains based off a lineup id
          String query = "SELECT TrainId, DepartureTime, Name, LineupId FROM TrainSchedule WHERE LineupId = ?";
          PreparedStatement stmt = conn.prepareStatement(query);
          stmt.setInt(1, trainLineupId); // set the query parameter to trainLineupId

          // execute the query
          ResultSet rs = stmt.executeQuery();

          /*** convert to JSON ***/
          JSONObject resultJson = new JSONObject();

          // add lineup id
          resultJson.put("lineupId", trainLineupId);

          // add columns
          ArrayList<String> columns = new ArrayList<>();
          ResultSetMetaData rsmd = rs.getMetaData();
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

          // return the json object as string
          result = resultJson.toString();

          // close the connection
          conn.close();
        }
        catch(ClassNotFoundException | SQLException | JSONException e){
          System.out.println("Connection failed");
          System.out.println(e);

          result = "failure";
        }

        return result;
    }

    /**
     * Web service operation
     */
    @WebMethod(operationName = "getEmpPosition")
    public int getEmpPosition() {
        Random rand = new Random();
        // 50 is maximum, 1 is minimum
        int  n = rand.nextInt(50) + 1;
        return n;
    }
}
