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
import com.oltpbenchmark.util.SQLUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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

  // TPCC parameters (configurable via XML: warehouses, districtsPerWarehouse, items)
  protected final int numWarehouses;
  protected final int numDistrictsPerWarehouse;
  protected final int numItems;

  public TPCCBenchmark(WorkloadConfiguration workConf) {
    super(workConf);

    if (workConf.getXmlConfig() != null) {
      this.numWarehouses =
          workConf.getXmlConfig().containsKey("warehouses")
              ? workConf.getXmlConfig().getInt("warehouses")
              : 1;
      this.numDistrictsPerWarehouse =
          workConf.getXmlConfig().containsKey("districtsPerWarehouse")
              ? workConf.getXmlConfig().getInt("districtsPerWarehouse")
              : 10;
      this.numItems =
          workConf.getXmlConfig().containsKey("items")
              ? workConf.getXmlConfig().getInt("items")
              : 100000;
    } else {
      this.numWarehouses = 1;
      this.numDistrictsPerWarehouse = 10;
      this.numItems = 100000;
    }

    // No need to set dialect path manually — it's picked up from the config XML
  }

  @Override
  protected Package getProcedurePackageImpl() {
    return NewOrder.class.getPackage();
  }

  @Override
  protected List<Worker<? extends BenchmarkModule>> makeWorkersImpl() {
    List<Worker<? extends BenchmarkModule>> workers = new ArrayList<>();
    try {
      // Count existing records in USERTABLE
      Table t = this.getCatalog().getTable("USERTABLE");
      String userCountSql = SQLUtil.getMaxColSQL(this.workConf.getDatabaseType(), t, "tcpp_key");
      try (Connection metaConn = this.makeConnection();
          java.sql.Statement stmt = metaConn.createStatement();
          ResultSet res = stmt.executeQuery(userCountSql)) {
        int initRecordCount = 0;
        while (res.next()) {
          initRecordCount = res.getInt(1);
        }
        int startKey = initRecordCount + 1;
        for (int i = 0; i < workConf.getTerminals(); i++) {
          workers.add(
              new TPCCWorker(
                  this,
                  i,
                  startKey,
                  this.numWarehouses,
                  this.numDistrictsPerWarehouse,
                  this.numItems));
        }
      }
    } catch (SQLException e) {
      LOG.error(e.getMessage(), e);
    }
    return workers;
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

    // Fallback if not defined
    if (ddlPath == null) {
      ddlPath =
          "/home/jakob/benchbaseWithPoly/src/main/resources/benchmarks/tpcc/ddl-polypheny.sql";
      LOG.info("No DDL path provided in config. Using fallback path: {}", ddlPath);
    }

    File ddlFile = new File(ddlPath);
    if (!ddlFile.exists()) {
      throw new RuntimeException("DDL file not found at path: " + ddlPath);
    }

    try {
      String ddl = Files.readString(ddlFile.toPath(), StandardCharsets.UTF_8);
      try (java.sql.Statement sqlStmt = conn.createStatement()) {
        for (String stmt : ddl.split(";")) {
          stmt = stmt.trim();
          if (stmt.isEmpty()) continue;

          LOG.info("Executing DDL: {}", stmt);
          sqlStmt.execute(stmt);

          try {
            net.sf.jsqlparser.statement.Statement parsed = CCJSqlParserUtil.parse(stmt);
            if (parsed instanceof CreateTable
                && workConf.getDatabaseType() == DatabaseType.POLYPHENY
                && getCatalog() instanceof Catalog) {

              Catalog catalog = (Catalog) getCatalog();
              CreateTable ct = (CreateTable) parsed;
              String tableName = ct.getTable().getName();
              List<ColumnDefinition> colDefs = ct.getColumnDefinitions();
              Table table = new Table(tableName, "\"");

              Set<String> pkCols = new HashSet<>();
              if (ct.getIndexes() != null) {
                for (Index idx : ct.getIndexes()) {
                  if ("PRIMARY KEY".equalsIgnoreCase(idx.getType())) {
                    pkCols.addAll(idx.getColumnsNames());
                  }
                }
              }

              for (ColumnDefinition cd : colDefs) {
                String colName = cd.getColumnName();
                String dt = cd.getColDataType().getDataType().toUpperCase();
                int sqlType;
                Integer size = null;
                switch (dt) {
                  case "INT":
                  case "INTEGER":
                    sqlType = java.sql.Types.INTEGER;
                    break;
                  default:
                    sqlType = java.sql.Types.VARCHAR;
                    break;
                }
                boolean isPK = pkCols.contains(colName);
                Column column = new Column(colName, "\"", table, sqlType, size, !isPK);
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
    } catch (IOException | SQLException e) {
      throw new RuntimeException("Failed to execute DDL file: " + ddlPath, e);
    }
  }
}
