package com.oltpbenchmark.benchmarks.ycsb.procedures;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeleteRecord extends Procedure {

  public final SQLStmt deleteStmt = new SQLStmt("db.usertable.deleteOne({ YCSB_KEY: ? })");

  public void run(Connection conn, int keyname) throws SQLException {
    try (PreparedStatement stmt = this.getPreparedStatement(conn, deleteStmt)) {
      stmt.setInt(1, keyname);
      stmt.executeUpdate();
    }
  }
}
