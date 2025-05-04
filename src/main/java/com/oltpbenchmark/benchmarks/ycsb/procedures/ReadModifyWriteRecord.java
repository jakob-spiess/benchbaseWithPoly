package com.oltpbenchmark.benchmarks.ycsb.procedures;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.benchmarks.ycsb.YCSBConstants;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ReadModifyWriteRecord extends Procedure {
  public final SQLStmt selectStmt =
      new SQLStmt(
          "db.usertable.findOneAndUpdate("
              + "{ YCSB_KEY: ? }, "
              + "{ $set: { FIELD0: ?, FIELD1: ?, FIELD2: ?, FIELD3: ?, FIELD4: ?, FIELD5: ?, FIELD6: ?, FIELD7: ?, FIELD8: ?, FIELD9: ? } }, "
              + "{ returnDocument: 'after' })");

  public void run(Connection conn, int keyname, String[] fields, String[] results)
      throws SQLException {
    try (PreparedStatement stmt = this.getPreparedStatement(conn, selectStmt)) {
      stmt.setInt(1, keyname);
      for (int i = 0; i < fields.length; i++) {
        stmt.setString(i + 2, fields[i]);
      }

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
