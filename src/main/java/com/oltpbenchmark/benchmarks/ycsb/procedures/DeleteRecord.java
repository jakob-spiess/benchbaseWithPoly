package com.oltpbenchmark.benchmarks.ycsb.procedures;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DeleteRecord extends Procedure {

  // Placeholder statement name to be overridden by the dialect XML
  public final SQLStmt deleteStmt = new SQLStmt("DIALECT_PLACEHOLDER");

  public void run(Connection conn, int keyname) throws SQLException {
    // Get the statement from the dialect file
    final String stmtStr = getPreparedStatement("deleteStmt");

    // Replace '?' with the actual key value
    final String resolvedStmt = stmtStr.replace("?", String.valueOf(keyname));

    try (Statement stmt = conn.createStatement()) {
      stmt.executeUpdate(resolvedStmt);
    }
  }
}
