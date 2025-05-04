package com.oltpbenchmark.benchmarks.ycsb.procedures;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.benchmarks.ycsb.YCSBConstants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class ScanRecord extends Procedure {
  public final SQLStmt scanStmt =
      new SQLStmt("db.usertable.find({ YCSB_KEY: { $gt: ?, $lt: ? } }).sort({ YCSB_KEY: 1 })");

  public void run(Connection conn, int start, int count, List<String[]> results)
      throws SQLException {
    try (PreparedStatement stmt = this.getPreparedStatement(conn, scanStmt)) {
      stmt.setInt(1, start);
      stmt.setInt(2, start + count);
      try (ResultSet r = stmt.executeQuery()) {
        while (r.next()) {
          String[] data = new String[YCSBConstants.NUM_FIELDS];
          for (int i = 0; i < data.length; i++) {
            data[i] = r.getString(i + 1);
          }
          results.add(data);
        }
      }
    }
  }
}
