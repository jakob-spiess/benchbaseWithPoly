package com.oltpbenchmark.benchmarks.tpch.procedures;

import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.util.RandomGenerator;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Q4MQL extends GenericQuery {

  public final SQLStmt query_stmt =
      new SQLStmt(
          """
    SELECT
       o_orderpriority,
       COUNT(*) AS order_count
    FROM
       orders
    WHERE
       o_orderdate >= DATE ?
       AND o_orderdate < ?
       AND EXISTS
       (
          SELECT
             *
          FROM
             lineitem
          WHERE
             l_orderkey = o_orderkey
             AND l_commitdate < l_receiptdate
       )
    GROUP BY
       o_orderpriority
    ORDER BY
       o_orderpriority
    """);

  @Override
  protected PreparedStatement getStatement(
      Connection conn, RandomGenerator rand, double scaleFactor) throws SQLException {

    int year = rand.number(1993, 1997);
    int month = rand.number(1, 10);
    String dateStr = String.format("%d-%02d-01", year, month);

    // Parse startDate
    Date startDate = Date.valueOf(dateStr);

    // Calculate endDate by adding 3 months
    // Use java.util.Calendar or java.time API for safer date arithmetic:
    java.util.Calendar cal = java.util.Calendar.getInstance();
    cal.setTime(startDate);
    cal.add(java.util.Calendar.MONTH, 3);
    Date endDate = new Date(cal.getTimeInMillis());

    PreparedStatement stmt = this.getPreparedStatement(conn, query_stmt);
    stmt.setDate(1, startDate);
    stmt.setDate(2, endDate);

    return stmt;
  }
}
