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
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class Q17 extends GenericQuery {

  public final SQLStmt query_stmt =
      new SQLStmt(
          """
      WITH avg_quantity AS (
        SELECT
          l_partkey,
          0.2 * AVG(l_quantity) AS threshold
        FROM
          lineitem
        GROUP BY
          l_partkey
      )
      SELECT
        SUM(l_extendedprice) / 7.0 AS avg_yearly
      FROM
        lineitem
        JOIN part ON p_partkey = l_partkey
        JOIN avg_quantity ON avg_quantity.l_partkey = lineitem.l_partkey
      WHERE
        p_brand = ?
        AND p_container = ?
        AND l_quantity < avg_quantity.threshold
      """);

  @Override
  protected PreparedStatement getStatement(
      Connection conn, RandomGenerator rand, double scaleFactor) throws SQLException {
    String brand = TPCHUtil.randomBrand(rand);
    String containerS1 = TPCHUtil.choice(TPCHConstants.CONTAINERS_S1, rand);
    String containerS2 = TPCHUtil.choice(TPCHConstants.CONTAINERS_S2, rand);
    String container = String.format("%s %s", containerS1, containerS2);

    PreparedStatement stmt = this.getPreparedStatement(conn, query_stmt);
    stmt.setString(1, brand);
    stmt.setString(2, container);
    return stmt;
  }
}
