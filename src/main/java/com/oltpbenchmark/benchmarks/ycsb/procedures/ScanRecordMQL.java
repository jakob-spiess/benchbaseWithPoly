package com.oltpbenchmark.benchmarks.ycsb.procedures;

import static com.oltpbenchmark.benchmarks.ycsb.YCSBConstants.TABLE_NAME;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.SQLStmt;
import com.oltpbenchmark.benchmarks.ycsb.YCSBConstants;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.Map;
import org.polypheny.jdbc.*;
import org.polypheny.jdbc.multimodel.*;

public class ScanRecordMQL extends Procedure {

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public final SQLStmt scanStmtMQL =
      new SQLStmt("SELECT * FROM " + TABLE_NAME + " WHERE YCSB_KEY>? AND YCSB_KEY<?");

  // FIXME: The value in ysqb is a byteiterator
  public void run(Connection conn, int start, int count, List<String[]> results)
      throws SQLException {

    try (Connection connection =
        DriverManager.getConnection("jdbc:polypheny://localhost:20590", "pa", "")) {

      if (connection.isWrapperFor(PolyConnection.class)) {
        PolyConnection polyConnection = connection.unwrap(PolyConnection.class);
        PolyStatement polyStatement = polyConnection.createPolyStatement();

        for (int key = start; key < start + count; key++) {
          String query = "db.usertable.findOne({ YCSB_KEY: " + key + " })";
          System.out.println("MQL scan: " + query);

          try {
            DocumentResult result =
                polyStatement.execute("public", "mongo", query).unwrap(DocumentResult.class);

            String raw = result.toString(); // Ex: {YCSB_KEY=..., field0=..., ...}

            // I mean the stuff executes and there are no Stack Traces on the Polypheny side of
            // things, so I think better not worry that this is printed...
            if (!raw.contains("{")) {
              // System.err.println("Unexpected result format (no '{' found): " + raw);
              continue;
            }

            raw = raw.substring(raw.indexOf('{'), raw.lastIndexOf('}') + 1);
            raw =
                raw.replaceAll("=", "\":\"")
                    .replaceAll(", ", "\", \"")
                    .replaceAll("\\{", "{\"")
                    .replaceAll("}", "\"}");

            Map<String, String> map =
                objectMapper.readValue(raw, new TypeReference<Map<String, String>>() {});

            String[] row = new String[YCSBConstants.NUM_FIELDS];
            for (int i = 0; i < YCSBConstants.NUM_FIELDS; i++) {
              row[i] = map.getOrDefault("field" + i, null);
            }

            results.add(row);

          } catch (Exception e) {
            System.err.println("Failed to scan key " + key);
            e.printStackTrace();
          }
        }
      }
    } catch (Exception e) {
      System.err.println("Connection error:");
      e.printStackTrace();
    }
  }
}

/*
public class ScanRecordMQL extends Procedure {
  public final SQLStmt scanStmtMQL =
      new SQLStmt("SELECT * FROM " + TABLE_NAME + " WHERE YCSB_KEY>? AND YCSB_KEY<?");

  private static final ObjectMapper objectMapper = new ObjectMapper();

  public void run(Connection conn, int keyname, String[] results) throws SQLException {

    String query = "db.usertable.findOne({ YCSB_KEY: " + keyname + " })";
    System.out.println("MQL scan: " + query);

    try (Connection connection =
        DriverManager.getConnection("jdbc:polypheny://localhost:20590", "pa", "")) {

      if (connection.isWrapperFor(PolyConnection.class)) {
        PolyConnection polyConnection = connection.unwrap(PolyConnection.class);
        PolyStatement polyStatement = polyConnection.createPolyStatement();

        DocumentResult result =
            polyStatement.execute("public", "mongo", query).unwrap(DocumentResult.class);

        String raw = result.toString(); // Returns something like: {YCSB_KEY=..., field0=..., ...}

        if (!raw.contains("{")) {
          System.err.println("Unexpected result format (no '{' found): " + raw);
          return;
        }

        raw =
            raw.substring(
                raw.indexOf('{'), raw.lastIndexOf('}') + 1); // strip surrounding metadata if needed
        raw =
            raw.replaceAll("=", "\":\"")
                .replaceAll(", ", "\", \"")
                .replaceAll("\\{", "{\"")
                .replaceAll("}", "\"}");

        // Now 'raw' is like: {"YCSB_KEY":"123", "field0":"foo", "field1":"bar", ...}

        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> map =
            mapper.readValue(raw, new TypeReference<Map<String, String>>() {});

        for (int i = 0; i < YCSBConstants.NUM_FIELDS; i++) {
          results[i] = map.getOrDefault("field" + i, null);
        }
      }
    } catch (Exception e) {
      System.err.println("Failed to scan key " + keyname);
      e.printStackTrace();
    }
  }
}
*/
