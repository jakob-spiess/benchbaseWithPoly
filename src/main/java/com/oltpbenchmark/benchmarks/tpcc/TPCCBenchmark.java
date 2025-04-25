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

package com.oltpbenchmark.benchmarks.tpcc;

import com.oltpbenchmark.WorkloadConfiguration;
import com.oltpbenchmark.api.BenchmarkModule;
import com.oltpbenchmark.api.Loader;
import com.oltpbenchmark.api.Worker;
import com.oltpbenchmark.benchmarks.tpcc.procedures.NewOrder;
import com.oltpbenchmark.catalog.Catalog;
import com.oltpbenchmark.catalog.Column;
import com.oltpbenchmark.catalog.Table;
import com.oltpbenchmark.types.DatabaseType;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TPCCBenchmark extends BenchmarkModule {
  private static final Logger LOG = LoggerFactory.getLogger(TPCCBenchmark.class);

  public TPCCBenchmark(WorkloadConfiguration workConf) {
    super(workConf);
  }

  @Override
  protected Package getProcedurePackageImpl() {
    return (NewOrder.class.getPackage());
  }

  @Override
  protected List<Worker<? extends BenchmarkModule>> makeWorkersImpl() {
    List<Worker<? extends BenchmarkModule>> workers = new ArrayList<>();

    try {
      List<TPCCWorker> terminals = createTerminals();
      workers.addAll(terminals);
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
    }

    return workers;
  }

  @Override
  protected Loader<TPCCBenchmark> makeLoaderImpl() {
    try (Connection conn = makeConnection()) {
      executeDDL(conn);
    } catch (SQLException e) {
      throw new RuntimeException("Error while executing DDL", e);
    }
    return new TPCCLoader(this);
  }

  protected List<TPCCWorker> createTerminals() throws SQLException {

    TPCCWorker[] terminals = new TPCCWorker[workConf.getTerminals()];

    int numWarehouses = (int) workConf.getScaleFactor();
    if (numWarehouses <= 0) {
      numWarehouses = 1;
    }

    int numTerminals = workConf.getTerminals();

    // We distribute terminals evenly across the warehouses
    // Eg. if there are 10 terminals across 7 warehouses, they
    // are distributed as
    // 1, 1, 2, 1, 2, 1, 2
    final double terminalsPerWarehouse = (double) numTerminals / numWarehouses;
    int workerId = 0;

    for (int w = 0; w < numWarehouses; w++) {
      // Compute the number of terminals in *this* warehouse
      int lowerTerminalId = (int) (w * terminalsPerWarehouse);
      int upperTerminalId = (int) ((w + 1) * terminalsPerWarehouse);
      // protect against double rounding errors
      int w_id = w + 1;
      if (w_id == numWarehouses) {
        upperTerminalId = numTerminals;
      }
      int numWarehouseTerminals = upperTerminalId - lowerTerminalId;

      if (LOG.isDebugEnabled()) {
        LOG.debug(
            String.format(
                "w_id %d = %d terminals [lower=%d / upper%d]",
                w_id, numWarehouseTerminals, lowerTerminalId, upperTerminalId));
      }

      final double districtsPerTerminal =
          TPCCConfig.configDistPerWhse / (double) numWarehouseTerminals;
      for (int terminalId = 0; terminalId < numWarehouseTerminals; terminalId++) {
        int lowerDistrictId = (int) (terminalId * districtsPerTerminal);
        int upperDistrictId = (int) ((terminalId + 1) * districtsPerTerminal);
        if (terminalId + 1 == numWarehouseTerminals) {
          upperDistrictId = TPCCConfig.configDistPerWhse;
        }
        lowerDistrictId += 1;

        TPCCWorker terminal =
            new TPCCWorker(this, workerId++, w_id, lowerDistrictId, upperDistrictId, numWarehouses);
        terminals[lowerTerminalId + terminalId] = terminal;
      }
    }

    return Arrays.asList(terminals);
  }

  private void executeDDL(Connection conn) {
    String ddlPath = workConf.getDDLPath();

    if (ddlPath == null) {
      LOG.info("No DDL path provided, skipping DDL execution.");
      return;
    }

    File ddlFile = new File(ddlPath);
    if (!ddlFile.exists()) {
      throw new RuntimeException("DDL file not found at path: " + ddlPath);
    }

    try {
      String ddl = Files.readString(ddlFile.toPath(), StandardCharsets.UTF_8);
      try (java.sql.Statement sqlStmt = conn.createStatement()) {
        // Execute each SQL statement via JDBC
        for (String statement : ddl.split(";")) {
          statement = statement.trim();
          if (!statement.isEmpty()) {
            LOG.info("Executing DDL: {}", statement);
            sqlStmt.execute(statement);

            // Try parsing the statement with JSQLParser
            try {
              net.sf.jsqlparser.statement.Statement parsedStmt = CCJSqlParserUtil.parse(statement);
              if (parsedStmt instanceof CreateTable
                  && workConf.getDatabaseType() == DatabaseType.POLYPHENY
                  && getCatalog() instanceof Catalog) {

                Catalog catalog = (Catalog) getCatalog();
                CreateTable createTable = (CreateTable) parsedStmt;
                String tableName = createTable.getTable().getName();
                List<ColumnDefinition> columnDefs = createTable.getColumnDefinitions();

                Table table = new Table(tableName, "\"");

                List<Index> indexes = createTable.getIndexes();
                Set<String> pkColumns = new HashSet<>();
                if (indexes != null) {
                  for (Index indexObj : indexes) {
                    if ("PRIMARY KEY".equalsIgnoreCase(indexObj.getType())) {
                      pkColumns.addAll(indexObj.getColumnsNames());
                    }
                  }
                }

                for (int i = 0; i < columnDefs.size(); i++) {
                  ColumnDefinition colDef = columnDefs.get(i);
                  String colName = colDef.getColumnName();
                  String dataType = colDef.getColDataType().getDataType().toUpperCase();

                  int sqlType;
                  Integer size = null;

                  switch (dataType) {
                    case "INT":
                    case "INTEGER":
                      sqlType = java.sql.Types.INTEGER;
                      break;
                    case "VARCHAR":
                    case "TEXT":
                    case "STRING":
                    default:
                      sqlType = java.sql.Types.VARCHAR;
                      break;
                  }

                  boolean isPrimaryKey = pkColumns.contains(colName);
                  boolean nullable = !isPrimaryKey;

                  Column column = new Column(colName, "\"", table, sqlType, size, nullable);
                  table.addColumn(column);
                }

                catalog.addTable(table.getName(), table.getColumns());
              }
            } catch (JSQLParserException e) {
              LOG.warn(
                  "Skipping catalog update for statement due to parse error: {}", e.getMessage());
            }
          }
        }
      }

    } catch (IOException | SQLException e) {
      throw new RuntimeException("Failed to execute DDL file: " + ddlFile.getAbsolutePath(), e);
    }
  }
}
