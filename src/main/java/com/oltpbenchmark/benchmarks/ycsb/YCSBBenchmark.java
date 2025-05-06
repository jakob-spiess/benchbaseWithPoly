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

package com.oltpbenchmark.benchmarks.ycsb;

import com.oltpbenchmark.WorkloadConfiguration;
import com.oltpbenchmark.api.BenchmarkModule;
import com.oltpbenchmark.api.Loader;
import com.oltpbenchmark.api.Worker;
import com.oltpbenchmark.benchmarks.ycsb.procedures.InsertRecord;
import com.oltpbenchmark.catalog.Catalog;
import com.oltpbenchmark.catalog.Column;
import com.oltpbenchmark.catalog.Table;
import com.oltpbenchmark.types.DatabaseType;
import com.oltpbenchmark.util.SQLUtil;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
// import java.sql.Statement;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class YCSBBenchmark extends BenchmarkModule {

  private static final Logger LOG = LoggerFactory.getLogger(YCSBBenchmark.class);

  /** The length in characters of each field */
  protected final int fieldSize;

  /** The constant used in the zipfian distribution (to modify the skew) */
  protected final double skewFactor;

  public YCSBBenchmark(WorkloadConfiguration workConf) {
    super(workConf);

    // This (hopefully) tells OLTPBench to parse the <dialect> XML and replace the hardcoded SQL
    // inside procedure classes.
    // I hope this is a good place to use otherwise try putting it in makeWorkersImpl()
    // COMMENTED OUT FOR NOW
    // this.loadProcedures(this.workConf.getDialectPath());

    int fieldSize = YCSBConstants.MAX_FIELD_SIZE;
    if (workConf.getXmlConfig() != null && workConf.getXmlConfig().containsKey("fieldSize")) {
      fieldSize =
          Math.min(workConf.getXmlConfig().getInt("fieldSize"), YCSBConstants.MAX_FIELD_SIZE);
    }
    this.fieldSize = fieldSize;
    if (this.fieldSize <= 0) {
      throw new RuntimeException("Invalid YCSB fieldSize '" + this.fieldSize + "'");
    }

    double skewFactor = 0.99;
    if (workConf.getXmlConfig() != null && workConf.getXmlConfig().containsKey("skewFactor")) {
      skewFactor = workConf.getXmlConfig().getDouble("skewFactor");
      if (skewFactor <= 0 || skewFactor >= 1) {
        throw new RuntimeException("Invalid YCSB skewFactor '" + skewFactor + "'");
      }
    }
    this.skewFactor = skewFactor;

    /*To dynamically change the hardcoded SQL statements in the procedure files,
    like /home/jakob/benchbaseWithPoly/src/main/java/com/oltpbenchmark/benchmarks/ycsb/procedures/UpdateRecord.java
    to MQL statements as specified here: /home/jakob/benchbaseWithPoly/src/main/resources/benchmarks/ycsb/dialect-polypheny_mql.xml
    */
    // this.loadProcedures(this.workConf.getDialectPath());

    // ==> Probably this is done with an argument in the terminal command so find out...
  }

  @Override
  protected List<Worker<? extends BenchmarkModule>> makeWorkersImpl() {
    List<Worker<? extends BenchmarkModule>> workers = new ArrayList<>();
    try {
      // LOADING FROM THE DATABASE IMPORTANT INFORMATION
      // LIST OF USERS
      Table t = this.getCatalog().getTable("USERTABLE");
      String userCount = SQLUtil.getMaxColSQL(this.workConf.getDatabaseType(), t, "ycsb_key");

      try (Connection metaConn = this.makeConnection();
          java.sql.Statement stmt = metaConn.createStatement();
          ResultSet res = stmt.executeQuery(userCount)) {
        int init_record_count = 0;
        while (res.next()) {
          init_record_count = res.getInt(1);
        }

        for (int i = 0; i < workConf.getTerminals(); ++i) {
          workers.add(new YCSBWorker(this, i, init_record_count + 1));
        }
      }
    } catch (SQLException e) {
      LOG.error(e.getMessage(), e);
    }
    return workers;
  }

  @Override
  protected Loader<YCSBBenchmark> makeLoaderImpl() {
    try (Connection conn = makeConnection()) {
      executeDDL(conn);
    } catch (SQLException e) {
      throw new RuntimeException("Error while executing DDL", e);
    }
    return new YCSBLoader(this);
  }

  @Override
  protected Package getProcedurePackageImpl() {
    return InsertRecord.class.getPackage();
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
