/*
 * Copyright 2020 by OLTPBenchmark Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.oltpbenchmark.benchmarks.tpch.procedures;

import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.util.RandomGenerator;
import java.sql.*;

public class Q15 extends GenericQuery {

  public final SQLStmt createview_stmt =
      new SQLStmt(
          """
            CREATE view revenue0 (supplier_no, total_revenue) AS
            SELECT
               l_suppkey,
               CAST(SUM(l_extendedprice * (1.00 - l_discount)) AS DOUBLE)

            FROM
               lineitem
            WHERE
               l_shipdate >= DATE '%s'
               AND l_shipdate < DATE '%s'
            GROUP BY
               l_suppkey
            """);

  public final SQLStmt query_stmt =
      new SQLStmt(
          """
            SELECT
               s_suppkey,
               s_name,
               s_address,
               s_phone,
               total_revenue
            FROM
               supplier,
               revenue0
            WHERE
               s_suppkey = supplier_no
               AND total_revenue = (
                  SELECT
                     MAX(total_revenue)
                  FROM
                     revenue0
               )
            ORDER BY
               s_suppkey
            """);

  public final SQLStmt dropview_stmt =
      new SQLStmt(
          """
            DROP VIEW IF EXISTS revenue0
            """);

  @Override
  public void run(Connection conn, RandomGenerator rand, double scaleFactor) throws SQLException {
    // With this query, we have to set up a view before we execute the query, then drop it once
    // we're done.
    try (Statement stmt = conn.createStatement()) {
      try {
        // Drop the view if it exists
        stmt.execute(dropview_stmt.getSQL());

        // Generate random date range (3 months)
        int year = rand.number(1993, 1997);
        int month = rand.number(1, year == 1997 ? 10 : 12);
        String dateStr = String.format("%d-%02d-01", year, month);

        Date startDate = Date.valueOf(dateStr);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTime(startDate);
        cal.add(java.util.Calendar.MONTH, 3);
        Date endDate = new Date(cal.getTimeInMillis());

        // Format the CREATE VIEW SQL with actual dates
        String viewSQL =
            String.format(createview_stmt.getSQL(), startDate.toString(), endDate.toString());
        System.out.println("Executing VIEW SQL:\n" + viewSQL);
        System.out.println("Executing QUERY SQL:\n" + query_stmt.getSQL());

        try {
          // Use regular Statement to execute CREATE VIEW
          stmt.execute(viewSQL);
        } catch (SQLException e) {
          System.err.println("Failed to create view revenue0:");
          e.printStackTrace();
          throw e;
        }

        // Now run the actual query
        super.run(conn, rand, scaleFactor);

      } finally {
        // Clean up view
        stmt.execute(dropview_stmt.getSQL());
      }
    }
  }

  @Override
  protected PreparedStatement getStatement(
      Connection conn, RandomGenerator rand, double scaleFactor) throws SQLException {
    return this.getPreparedStatement(conn, query_stmt);
  }
}
