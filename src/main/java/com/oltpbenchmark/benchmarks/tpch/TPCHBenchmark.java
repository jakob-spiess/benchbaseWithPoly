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

/*
 *   TPC-H implementation
 *
 *   Ben Reilly (bd.reilly@gmail.com)
 *   Ippokratis Pandis (ipandis@us.ibm.com)
 *
 */

package com.oltpbenchmark.benchmarks.tpch;

import com.oltpbenchmark.WorkloadConfiguration;
import com.oltpbenchmark.api.BenchmarkModule;
import com.oltpbenchmark.api.Loader;
import com.oltpbenchmark.api.Worker;
import com.oltpbenchmark.benchmarks.tpch.procedures.Q1;
import com.oltpbenchmark.catalog.Catalog;
import com.oltpbenchmark.catalog.Column;
import com.oltpbenchmark.catalog.Table;
import com.oltpbenchmark.types.DatabaseType;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.alter.AlterOperation;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TPCHBenchmark extends BenchmarkModule {
  private static final Logger LOG = LoggerFactory.getLogger(TPCHBenchmark.class);

  public TPCHBenchmark(WorkloadConfiguration workConf) {
    super(workConf);
  }

  @Override
  protected Package getProcedurePackageImpl() {
    return (Q1.class.getPackage());
  }

  @Override
  protected List<Worker<? extends BenchmarkModule>> makeWorkersImpl() {
    List<Worker<? extends BenchmarkModule>> workers = new ArrayList<>();

    int numTerminals = workConf.getTerminals();
    LOG.info(String.format("Creating %d workers for TPC-H", numTerminals));
    for (int i = 0; i < numTerminals; i++) {
      workers.add(new TPCHWorker(this, i));
    }
    return workers;
  }

  @Override
  protected Loader<TPCHBenchmark> makeLoaderImpl() {
    try (Connection conn = makeConnection()) {
      executeDDL(conn);
    } catch (SQLException e) {
      throw new RuntimeException("Error while executing DDL", e);
    }

    return new TPCHLoader(this);
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
                    String indexType = indexObj.getType();
                    if ("PRIMARY KEY".equalsIgnoreCase(indexType)) {
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
              } else if (parsedStmt instanceof Alter) {
                Alter alterStmt = (Alter) parsedStmt;
                String tableName = alterStmt.getTable().getName();

                if (workConf.getDatabaseType() == DatabaseType.POLYPHENY
                    && getCatalog() instanceof Catalog) {
                  Catalog catalog = (Catalog) getCatalog();
                  Table table = catalog.getTable(tableName);
                  if (table == null) {
                    LOG.warn(
                        "Table {} not found in catalog during ALTER TABLE parsing.", tableName);
                    continue;
                  }

                  for (AlterExpression expr : alterStmt.getAlterExpressions()) {
                    if (expr.getOperation() == AlterOperation.ADD && expr.getIndex() != null) {
                      Index index = expr.getIndex();
                      List<String> columnNames = index.getColumnsNames();

                      for (String columnName : columnNames) {
                        Column col = table.getColumn(columnName);
                        if (col != null) {
                          col.markAsIndexed(); // Now a valid method!
                        } else {
                          LOG.warn(
                              "Column {} not found in table {} for index.", columnName, tableName);
                        }
                      }
                    }
                  }
                }
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
