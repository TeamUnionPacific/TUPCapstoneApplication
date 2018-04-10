
package tk.tup.capstone;

import java.sql.*;
import org.json.JSONObject;

/**
 *
 * @author Daniel Agbay
 */
public class UnionPacificDB {
    protected final String db_host = "jdbc:mysql://174.138.38.48/UnionPacific";
    protected final String db_username = "unionpacific";
    protected final String db_password = "UnionPacificDB";
    
    protected Connection conn;
    
    /**
     * 
     * open and assign conn:
     * return JSON string with error message from exception if thrown, else return empty string
     * @return String
     */
    protected String openConn(){
        try{
            Class.forName("com.mysql.jdbc.Driver");
            this.conn = DriverManager.getConnection(db_host,db_username,db_password);
            return "";
        } catch(ClassNotFoundException | SQLException e) {
            JSONObject resultJson = new JSONObject();
            resultJson.put("error",e.getMessage());
            return resultJson.toString();
        }
    }
    
    protected void closeConn(){
        try{
            this.conn.close();
        } catch(SQLException e){
            
        }
    }
    
}