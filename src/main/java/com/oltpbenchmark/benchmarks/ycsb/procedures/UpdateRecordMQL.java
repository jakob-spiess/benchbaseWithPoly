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

package com.oltpbenchmark.benchmarks.ycsb.procedures;

import static com.oltpbenchmark.benchmarks.ycsb.YCSBConstants.TABLE_NAME;

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import org.polypheny.jdbc.*;
import org.polypheny.jdbc.multimodel.*;

public class UpdateRecordMQL extends Procedure {

  public final SQLStmt updateAllStmtMQL =
      new SQLStmt(
          "UPDATE "
              + TABLE_NAME
              + " SET FIELD1=?,FIELD2=?,FIELD3=?,FIELD4=?,FIELD5=?,"
              + "FIELD6=?,FIELD7=?,FIELD8=?,FIELD9=?,FIELD10=? WHERE YCSB_KEY=?");

  public void run(Connection conn, int keyname, String[] vals) throws SQLException {

    // int keyname = 123; // example key
    // String[] vals = {"val1", "val2", "val3", "val4", "val5", "val6", "val7", "val8", "val9",
    // "val10"};

    for (int i = 0; i < 10; i++) {
      String fieldName = "FIELD" + (i + 1);
      String fieldValue = escapeForJson(vals[i]);

      // MQL query for Polypheny
      String query =
          String.format(
              "db.collection.updateOne({ \"YCSB_KEY\": %d }, { \"$set\": { \"%s\": \"%s\" } })",
              keyname, fieldName, fieldValue);

      System.out.println("MQL read: " + query);

      try (Connection connection =
          DriverManager.getConnection("jdbc:polypheny://localhost:20590", "pa", "")) {
        if (connection.isWrapperFor(PolyConnection.class)) {
          PolyConnection polyConnection = connection.unwrap(PolyConnection.class);
          PolyStatement polyStatement = polyConnection.createPolyStatement();

          polyStatement.execute("public", "mongo", query);
        }
      } catch (SQLException e) {
        e.printStackTrace();
      }
    }
    /*
    try (Connection connection =
        DriverManager.getConnection("jdbc:polypheny://localhost:20590", "pa", "")) {
      if (connection.isWrapperFor(PolyConnection.class)) {
        PolyConnection polyConnection = connection.unwrap(PolyConnection.class);
        PolyStatement polyStatement = polyConnection.createPolyStatement();

        DocumentResult result =
            polyStatement.execute("public", "mongo", query).unwrap(DocumentResult.class);

        // Fallback: parse result string into key-value pairs manually
        String raw = result.toString(); // returns document as string
        // System.out.println("Raw result: " + raw);

        // Try to extract values from raw JSON-like string
        // Assumes format: { "YCSB_KEY": 123, "field0": "value0", ..., "field9": "value9" }

        Map<String, String> map = parseResult(raw);
        for (int i = 0; i < YCSBConstants.NUM_FIELDS; i++) {
          results[i] = map.getOrDefault("field" + i, null);
        }

      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
    */
  }

  String escapeForJson(String input) {
    if (input == null) return "";
    return input
        .replace("\\", "\\\\") // escape backslashes
        .replace("\"", "\\\"") // escape double quotes
        .replace("\b", "\\b")
        .replace("\f", "\\f")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
