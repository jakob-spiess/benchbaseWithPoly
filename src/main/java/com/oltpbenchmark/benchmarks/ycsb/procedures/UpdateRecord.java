package com.oltpbenchmark.benchmarks.ycsb.procedures;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class UpdateRecord extends Procedure {

  public final SQLStmt updateAllStmt =
      new SQLStmt(
          "db.usertable.updateOne("
              + "{ YCSB_KEY: ? }, "
              + "{ $set: { FIELD1: ?, FIELD2: ?, FIELD3: ?, FIELD4: ?, FIELD5: ?, "
              + "FIELD6: ?, FIELD7: ?, FIELD8: ?, FIELD9: ?, FIELD10: ? } })");

  public void run(Connection conn, int keyname, String[] vals) throws SQLException {
    // Get a prepared statement from the MQL string we wrote above
    try (PreparedStatement stmt = this.getPreparedStatement(conn, updateAllStmt)) {

      // First parameter is the key — goes into: { YCSB_KEY: ? }
      stmt.setInt(1, keyname);

      // Next 10 parameters are the values to update — go into the $set part
      for (int i = 0; i < vals.length; i++) {
        stmt.setString(i + 2, vals[i]); // +2 because keyname was param 1
      }

      // Actually run the query
      stmt.executeUpdate();
    }
  }
}
