package tk.tup.capstone;

import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;

import java.sql.*;
import org.json.JSONObject;
import java.util.ArrayList;

/**
 * Class containing all operations used by our reporting web application
 * @author Daniel Agbay
 */
@WebService(serviceName = "WebAppOps")
public class WebAppOps extends UnionPacificDB{
    
    /**
     * Web service operation
     * @param date_start
     * @param date_end
     * @param assistants
     * @param intents
     * @param users
     * @return 
     */
    @WebMethod(operationName = "getAssistantQueryLogs")
    public String getAssistantQueryLogs(@WebParam(name = "date_start") String date_start, 
            @WebParam(name = "date_end") String date_end, 
            @WebParam(name = "assistants") String[] assistants, 
            @WebParam(name = "intents") String[] intents, 
            @WebParam(name = "users") String[] users) 
    {
        String result = "";
        String error = this.openConn();
        if(!error.isEmpty()){ return error; }
        
        date_start += " 00:00:00";
        date_end += " 23:59:59";
        try {
            // dynamically build the query based on the parameters that were passed 
            String query = "SELECT UnionPacificId, Query, Intent, Assistant, Error, AddDate FROM AssistantQueryLogs ";
            PreparedStatement stmt;
            // if no parameters passed: query for date range only
            if(assistants.length == 0 && intents.length == 0 && users.length == 0){
                query += "WHERE AddDate BETWEEN ? AND ?";
                stmt = this.conn.prepareStatement(query);
                stmt.setString(1, date_start);
                stmt.setString(2, date_end);
            }
            // else: build query and set parameters
            else{
                query += "WHERE ";
                boolean and = false; // value to determine if "AND" clause is needed
                //create list of parameters to add to prepared statement later
                ArrayList<String> params = new ArrayList<>();
                // add each parameter of each array
                if(assistants.length > 0){
                    query += "Assistant IN (";
                    for(int i=0; i<assistants.length; i++){
                        query += "?";
                        if(i != assistants.length-1){ query+=","; }
                        params.add(assistants[i]);
                    }
                    query += ") ";
                    and = true;
                }
                if(intents.length > 0){
                    if(and){ query += "AND "; }
                    query += "Intent IN (";
                    for(int i=0; i<intents.length; i++){
                        query += "?";
                        if(i != intents.length-1){ query+=","; }
                        params.add(intents[i]);
                    }
                    query += ") ";
                    and = true;
                }
                if(users.length > 0){
                    if(and){ query += "AND "; }
                    query += "UnionPacificId IN (";
                    for(int i=0; i<users.length; i++){
                        query += "?";
                        if(i != users.length-1){ query += ","; }
                        params.add(users[i]);
                    }
                    query += ") ";
                }
                
                query += "AND AddDate BETWEEN ? AND ?";
                params.add(date_start);
                params.add(date_end);
                
                // prepare the statement and set the parameters
                stmt = this.conn.prepareStatement(query);
                for(int i=1; i<=params.size(); i++){
                    stmt.setString(i, params.get(i-1));
                }
            }
            
//            // execute the query
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData rsmd = rs.getMetaData();
            
            JSONObject resultJson = new JSONObject();
            
            // add columns
            ArrayList<String> columns = new ArrayList<>();
            int total_cols = rsmd.getColumnCount();
            for(int i=1; i<=total_cols; i++){
              String col = rsmd.getColumnLabel(i);
              columns.add(col);
            }
            resultJson.put("columns", columns);

            // add rows
            ArrayList<ArrayList<String>> rows = new ArrayList<>();
            while(rs.next()){
              ArrayList<String> row = new ArrayList<>();

              row.add(rs.getString("UnionPacificId"));
              row.add(rs.getString("Query"));
              row.add(rs.getString("Intent"));
              row.add(rs.getString("Assistant"));
              row.add(rs.getString("Error"));
              String addDate = rs.getString("AddDate").substring(0,10);
              row.add(addDate);
//              row.add(rs.getString("AddDate"));

              rows.add(row);
            }
            resultJson.put("rows", rows);
            
            result = resultJson.toString();

        } catch(SQLException e){
            result = this.handleException(e);
        } finally{
            this.closeConn();
        }
        
        return result;
    }
    
    
}
