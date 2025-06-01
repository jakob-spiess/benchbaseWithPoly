package com.oltpbenchmark.benchmarks.tpch.procedures;

import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.util.RandomGenerator;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Q1MQL extends GenericQuery {

  public final SQLStmt query_stmt =
      new SQLStmt(
          """
      SELECT
         l_returnflag,
         l_linestatus,
         SUM(l_quantity) AS sum_qty,
         SUM(l_extendedprice) AS sum_base_price,
         SUM(l_extendedprice * (1 - l_discount)) AS sum_disc_price,
         SUM(l_extendedprice * (1 - l_discount) * (1 + l_tax)) AS sum_charge,
         AVG(l_quantity) AS avg_qty,
         AVG(l_extendedprice) AS avg_price,
         AVG(l_discount) AS avg_disc,
         COUNT(*) AS count_order
      FROM
         lineitem
      WHERE
         l_shipdate <= DATE '1998-12-01' - INTERVAL '90' DAY
      GROUP BY
         l_returnflag,
         l_linestatus
      ORDER BY
         l_returnflag,
         l_linestatus
    """);

  @Override
  protected PreparedStatement getStatement(
      Connection conn, RandomGenerator rand, double scaleFactor) throws SQLException {
    return this.getPreparedStatement(conn, query_stmt);
  }
}
