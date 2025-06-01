package com.oltpbenchmark.benchmarks.tpch.procedures;

import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.benchmarks.tpch.TPCHConstants;
import com.oltpbenchmark.benchmarks.tpch.TPCHUtil;
import com.oltpbenchmark.util.RandomGenerator;
import java.sql.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
import org.polypheny.jdbc.*;
import org.polypheny.jdbc.multimodel.*;

public class Q3MQL extends GenericQuery {

  public final SQLStmt query_stmt =
      new SQLStmt(
          """
db.lineitem.aggregate([
{
  $lookup: {
    from: "orders",
    localField: "l_orderkey",
    foreignField: "o_orderkey",
    as: "order"
  }
},
{ $unwind: "$order" },
{
  $lookup: {
    from: "customer",
    localField: "order.o_custkey",
    foreignField: "c_custkey",
    as: "customer"
  }
},
{ $unwind: "$customer" },
{
  $match: {
    "customer.c_mktsegment": "%s",
    "order.o_orderdate": { $lt: "%s" },
    "l_shipdate": { $gt: "%s" }
  }
},
{
  $group: {
    _id: {
      l_orderkey: "$l_orderkey",
      o_orderdate: "$order.o_orderdate",
      o_shippriority: "$order.o_shippriority"
    },
    revenue: {
      $sum: {
        $multiply: [
          "$l_extendedprice",
          { $subtract: [1, "$l_discount"] }
        ]
      }
    }
  }
},
{
  $project: {
    _id: 0,
    l_orderkey: "$_id.l_orderkey",
    o_orderdate: "$_id.o_orderdate",
    o_shippriority: "$_id.o_shippriority",
    revenue: 1
  }
},
{
  $sort: {
    revenue: -1,
    o_orderdate: 1
  }
},
{ $limit: 10 }
])""");

  @Override
  protected PreparedStatement getStatement(
      Connection conn, RandomGenerator rand, double scaleFactor) throws SQLException {
    String segment = TPCHUtil.choice(TPCHConstants.SEGMENTS, rand);

    int day = rand.number(1, 31);
    String date = String.format("1995-03-%02d", day);
    LocalDate baseDate = LocalDate.of(1995, 3, rand.number(1, 31));
    String orderDate = baseDate.toString(); // ISO format: "1995-03-14"
    String shipDate = baseDate.toString(); // e.g., "1995-03-19"

    String query =
        String.format(
            """
db.lineitem.aggregate([
  {
    $lookup: {
      from: "orders",
      localField: "l_orderkey",
      foreignField: "o_orderkey",
      as: "order"
    }
  },
  { $unwind: "$order" },
  {
    $lookup: {
      from: "customer",
      localField: "order.o_custkey",
      foreignField: "c_custkey",
      as: "customer"
    }
  },
  { $unwind: "$customer" },
  {
    $match: {
      "customer.c_mktsegment": "%s",
      "order.o_orderdate": { $lt: "%s" },
      "l_shipdate": { $gt: "%s" }
    }
  },
  {
    $group: {
      _id: {
        l_orderkey: "$l_orderkey",
        o_orderdate: "$order.o_orderdate",
        o_shippriority: "$order.o_shippriority"
      },
      revenue: {
        $sum: {
          $multiply: [
            "$l_extendedprice",
            { $subtract: [1, "$l_discount"] }
          ]
        }
      }
    }
  },
  {
    $project: {
      _id: 0,
      l_orderkey: "$_id.l_orderkey",
      o_orderdate: "$_id.o_orderdate",
      o_shippriority: "$_id.o_shippriority",
      revenue: 1
    }
  },
  {
    $sort: {
      revenue: -1,
      o_orderdate: 1
    }
  },
  { $limit: 10 }
])""",
            segment, orderDate, shipDate);

    try (Connection connection =
        DriverManager.getConnection("jdbc:polypheny://localhost:20590", "pa", ""); ) {
      if (connection.isWrapperFor(PolyConnection.class)) {
        PolyConnection polyConnection = connection.unwrap(PolyConnection.class);
        PolyStatement polyStatement = polyConnection.createPolyStatement();

        // You can now use polyStatement to execute multi-model queries...
        DocumentResult result =
            polyStatement.execute("public", "mongo", query).unwrap(DocumentResult.class);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    return null;
  }
}
