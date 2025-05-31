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
import com.oltpbenchmark.benchmarks.tpch.TPCHConstants;
import com.oltpbenchmark.benchmarks.tpch.TPCHUtil;
import com.oltpbenchmark.util.RandomGenerator;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Calendar;

public class Q5 extends GenericQuery {

  public final SQLStmt query_stmt =
      new SQLStmt(
          """
            SELECT
               n_name,
               SUM(l_extendedprice * (1 - l_discount)) AS revenue
            FROM
               customer,
               orders,
               lineitem,
               supplier,
               nation,
               region
            WHERE
               c_custkey = o_custkey
               AND l_orderkey = o_orderkey
               AND l_suppkey = s_suppkey
               AND c_nationkey = s_nationkey
               AND s_nationkey = n_nationkey
               AND n_regionkey = r_regionkey
               AND r_name = ?
               AND o_orderdate >= DATE ?
               AND o_orderdate < ?
            GROUP BY
               n_name
            ORDER BY
               revenue DESC
            """);

  @Override
  protected PreparedStatement getStatement(
      Connection conn, RandomGenerator rand, double scaleFactor) throws SQLException {

    String region = TPCHUtil.choice(TPCHConstants.R_NAME, rand);

    int year = rand.number(1993, 1997);
    String dateStr = String.format("%d-01-01", year);

    Date startDate = Date.valueOf(dateStr);

    java.util.Calendar cal = java.util.Calendar.getInstance();
    cal.setTime(startDate);
    cal.add(Calendar.YEAR, 1);
    Date endDate = new Date(cal.getTimeInMillis());

    PreparedStatement stmt = this.getPreparedStatement(conn, query_stmt);
    stmt.setString(1, region);
    stmt.setDate(2, startDate);
    stmt.setDate(3, endDate);
    return stmt;
  }
}
