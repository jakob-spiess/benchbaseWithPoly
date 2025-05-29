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
import org.polypheny.jdbc.*;
import org.polypheny.jdbc.multimodel.*;

public class DeleteRecordMQL extends Procedure {

  // public final SQLStmt deleteStmtMQL = new SQLStmt("DELETE FROM " + TABLE_NAME + " where
  // YCSB_KEY=?");
  public final SQLStmt deleteStmtMQL =
      new SQLStmt("DELETE FROM " + TABLE_NAME + " where YCSB_KEY=?");

  public void run(Connection conn, int keyname) throws SQLException {

    // use correct namespace, hoping/assuming that that namespace is "public"

    // Manually build the MQL query string
    // String query = "db.getCollection(\"usertable\").deleteOne({ YCSB_KEY: " + keyname + " })";
    String query = "db.usertable.deleteOne({ YCSB_KEY: " + keyname + " })";
    System.out.println("MQL delete: " + query);

    try (Connection connection =
        DriverManager.getConnection("jdbc:polypheny://localhost:20590", "pa", ""); ) {
      if (connection.isWrapperFor(PolyConnection.class)) {
        PolyConnection polyConnection = connection.unwrap(PolyConnection.class);
        PolyStatement polyStatement = polyConnection.createPolyStatement();

        // You can now use polyStatement to execute multi-model queries...
        // DocumentResult firstResult =
        //    polyStatement.execute("public", "mongo", "use public").unwrap(DocumentResult.class);
        DocumentResult result =
            polyStatement.execute("public", "mongo", query).unwrap(DocumentResult.class);
      }
    } catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
