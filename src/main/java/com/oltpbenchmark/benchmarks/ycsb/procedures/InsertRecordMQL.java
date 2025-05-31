package com.oltpbenchmark.benchmarks.ycsb.procedures;

import static com.oltpbenchmark.benchmarks.ycsb.YCSBConstants.TABLE_NAME;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.polypheny.jdbc.*;
import org.polypheny.jdbc.multimodel.*;

public class InsertRecordMQL extends Procedure {
  public final SQLStmt insertStmtMQL =
      new SQLStmt("INSERT INTO " + TABLE_NAME + " VALUES (?,?,?,?,?,?,?,?,?,?,?)");

  public void run(Connection conn, int keyname, String[] vals) throws SQLException {

    String query = "db.getCollection(\"usertable\").insertOne({ YCSB_KEY: " + keyname + " })";

    /*
    StringBuilder query =
     new StringBuilder();
    query.append("db.").append(TABLE_NAME).append(".insertOne({ ");
    query.append("YCSB_KEY: ").append(keyname);
    for (int i = 0; i < YCSBConstants.NUM_FIELDS; i++) {
      query.append(", field").append(i).append(": \"").append(escape(vals[i])).append("\"");
    }
    query.append(" })");
    */

    System.out.println("Generated MQL Insert Query:\n" + query.toString());

    try (Connection connection =
        DriverManager.getConnection("jdbc:polypheny://localhost:20590", "pa", "")) {
      if (connection.isWrapperFor(PolyConnection.class)) {
        PolyConnection polyConnection = connection.unwrap(PolyConnection.class);
        PolyStatement polyStatement = polyConnection.createPolyStatement();

        polyStatement.execute("public", "mongo", query);
      }
    } catch (SQLException e) {
      e.printStackTrace();
      throw e;
    }
  }

  // might not be needed anymore
  private static String escape(String s) {
    if (s == null) return "";
    return s.replace("\\", "\\\\") // Escape backslash first
        .replace("\"", "\\\"") // Then escape quotes
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
