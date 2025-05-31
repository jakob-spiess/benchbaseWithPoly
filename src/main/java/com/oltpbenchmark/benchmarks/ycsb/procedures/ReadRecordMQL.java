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
import com.oltpbenchmark.benchmarks.ycsb.YCSBConstants;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.polypheny.jdbc.*;
import org.polypheny.jdbc.multimodel.*;

public class ReadRecordMQL extends Procedure {

  public final SQLStmt readStmtMQL =
      new SQLStmt("SELECT * FROM " + TABLE_NAME + " WHERE YCSB_KEY=?");

  public void run(Connection conn, int keyname, String[] results) throws SQLException {

    String query = "db.getCollection(\"usertable\").findOne({ YCSB_KEY: " + keyname + " })";
    System.out.println("MQL read: " + query);

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
  }

  /**
   * Very basic JSON-like string parser for MQL result (not robust — use real JSON parser in prod)
   */
  private Map<String, String> parseResult(String raw) {
    Map<String, String> map = new LinkedHashMap<>();
    raw = raw.trim();
    if (raw.startsWith("{") && raw.endsWith("}")) {
      raw = raw.substring(1, raw.length() - 1); // remove {}
      String[] entries = raw.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
      for (String entry : entries) {
        String[] kv = entry.split(":", 2);
        if (kv.length == 2) {
          String key = kv[0].trim().replaceAll("^\"|\"$", "");
          String value = kv[1].trim().replaceAll("^\"|\"$", "");
          map.put(key, value);
        }
      }
    }
    return map;
  }
}
