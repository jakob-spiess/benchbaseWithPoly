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
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
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
    throw new UnsupportedOperationException("TPCC workers not yet implemented");
  }

  @Override
  protected Loader<TPCCBenchmark> makeLoaderImpl() {
    try (Connection conn = makeConnection()) {
      this.executeDDL(conn);
    } catch (SQLException e) {
      throw new RuntimeException("Error executing TPCC DDL", e);
    }
    return new TPCCLoader(this);
  }

  private void executeDDL(Connection conn) {
    String ddlPath = workConf.getDDLPath();

    if (ddlPath == null) {
      LOG.info(workConf.toString());
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
