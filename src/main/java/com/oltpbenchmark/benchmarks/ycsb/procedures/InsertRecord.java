package com.oltpbenchmark.benchmarks.ycsb.procedures;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class InsertRecord extends Procedure {
  public final SQLStmt insertStmt =
      new SQLStmt(
          "db.usertable.insertOne({ YCSB_KEY: ?, FIELD0: ?, FIELD1: ?, FIELD2: ?, FIELD3: ?, FIELD4: ?, FIELD5: ?, FIELD6: ?, FIELD7: ?, FIELD8: ?, FIELD9: ? })");

  public void run(Connection conn, int keyname, String[] vals) throws SQLException {
    try (PreparedStatement stmt = this.getPreparedStatement(conn, insertStmt)) {
      stmt.setInt(1, keyname);
      for (int i = 0; i < vals.length; i++) {
        stmt.setString(i + 2, vals[i]);
      }
      stmt.executeUpdate();
    }
  }
}
