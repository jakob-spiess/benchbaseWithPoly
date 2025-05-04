package com.oltpbenchmark.benchmarks.ycsb.procedures;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.benchmarks.ycsb.YCSBConstants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReadRecord extends Procedure {
  public final SQLStmt readStmt = new SQLStmt("db.usertable.find({ YCSB_KEY: ? }).limit(1)");

  public void run(Connection conn, int keyname, String[] results) throws SQLException {
    try (PreparedStatement stmt = this.getPreparedStatement(conn, readStmt)) {
      stmt.setInt(1, keyname);
      try (ResultSet r = stmt.executeQuery()) {
        while (r.next()) {
          for (int i = 0; i < YCSBConstants.NUM_FIELDS; i++) {
            results[i] = r.getString(i + 1);
          }
        }
      }
    }
  }
}
