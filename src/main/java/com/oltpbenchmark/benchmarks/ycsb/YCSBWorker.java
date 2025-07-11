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

import com.oltpbenchmark.api.Procedure;
import com.oltpbenchmark.api.Procedure.UserAbortException;
import com.oltpbenchmark.api.TransactionType;
import com.oltpbenchmark.api.Worker;
import com.oltpbenchmark.benchmarks.ycsb.procedures.*;
import com.oltpbenchmark.distributions.CounterGenerator;
import com.oltpbenchmark.distributions.UniformGenerator;
import com.oltpbenchmark.distributions.ZipfianGenerator;
import com.oltpbenchmark.types.TransactionStatus;
import com.oltpbenchmark.util.TextGenerator;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * YCSBWorker Implementation I forget who really wrote this but I fixed it up in 2016...
 *
 * @author pavlo
 */
class YCSBWorker extends Worker<YCSBBenchmark> {

  private final ZipfianGenerator readRecord;
  private static CounterGenerator insertRecord;
  private final UniformGenerator randScan;

  private final char[] data;
  private final String[] params = new String[YCSBConstants.NUM_FIELDS];
  private final String[] results = new String[YCSBConstants.NUM_FIELDS];

  private final UpdateRecord procUpdateRecord;
  private final UpdateRecordMQL procUpdateRecordMQL;
  private final ScanRecord procScanRecord;
  private final ScanRecordMQL procScanRecordMQL;
  private final ReadRecord procReadRecord;
  private final ReadModifyWriteRecord procReadModifyWriteRecord;
  private final InsertRecord procInsertRecord;
  private final InsertRecordMQL procInsertRecordMQL;
  private final DeleteRecord procDeleteRecord;
  private final DeleteRecordMQL procDeleteRecordMQL;
  private final ReadRecordMQL procReadRecordMQL;
  private final ReadRecordCypher procReadRecordCypher;

  public YCSBWorker(YCSBBenchmark benchmarkModule, int id, int init_record_count) {
    super(benchmarkModule, id);
    this.data = new char[benchmarkModule.fieldSize];
    this.readRecord =
        new ZipfianGenerator(
            rng(), init_record_count, benchmarkModule.skewFactor); // pool for read keys
    this.randScan = new UniformGenerator(1, YCSBConstants.MAX_SCAN);

    synchronized (YCSBWorker.class) {
      // We must know where to start inserting
      if (insertRecord == null) {
        insertRecord = new CounterGenerator(init_record_count);
      }
    }

    // This is a minor speed-up to avoid having to invoke the hashmap look-up
    // everytime we want to execute a txn. This is important to do on
    // a client machine with not a lot of cores
    this.procUpdateRecord = this.getProcedure(UpdateRecord.class);
    this.procUpdateRecordMQL = this.getProcedure(UpdateRecordMQL.class);
    this.procScanRecord = this.getProcedure(ScanRecord.class);
    this.procScanRecordMQL = this.getProcedure(ScanRecordMQL.class);
    this.procReadRecord = this.getProcedure(ReadRecord.class);
    this.procReadModifyWriteRecord = this.getProcedure(ReadModifyWriteRecord.class);
    this.procInsertRecord = this.getProcedure(InsertRecord.class);
    this.procInsertRecordMQL = this.getProcedure(InsertRecordMQL.class);
    this.procDeleteRecord = this.getProcedure(DeleteRecord.class);
    this.procDeleteRecordMQL = this.getProcedure(DeleteRecordMQL.class);
    this.procReadRecordMQL = this.getProcedure(ReadRecordMQL.class);
    this.procReadRecordCypher = this.getProcedure(ReadRecordCypher.class);
  }

  @Override
  protected TransactionStatus executeWork(Connection conn, TransactionType nextTrans)
      throws UserAbortException, SQLException {
    Class<? extends Procedure> procClass = nextTrans.getProcedureClass();

    if (procClass.equals(DeleteRecord.class)) {
      deleteRecord(conn);
    } else if (procClass.equals(DeleteRecordMQL.class)) {
      deleteRecordMQL(conn);
    } else if (procClass.equals(ReadRecordMQL.class)) {
      readRecordMQL(conn);
    } else if (procClass.equals(ReadRecordCypher.class)) {
      readRecordCypher(conn);
    } else if (procClass.equals(InsertRecord.class)) {
      insertRecord(conn);
    } else if (procClass.equals(InsertRecordMQL.class)) {
      insertRecordMQL(conn);
    } else if (procClass.equals(ReadModifyWriteRecord.class)) {
      readModifyWriteRecord(conn);
    } else if (procClass.equals(ReadRecord.class)) {
      readRecord(conn);
    } else if (procClass.equals(ScanRecord.class)) {
      scanRecord(conn);
    } else if (procClass.equals(ScanRecordMQL.class)) {
      scanRecordMQL(conn);
    } else if (procClass.equals(UpdateRecordMQL.class)) {
      updateRecordMQL(conn);
    }
    return (TransactionStatus.SUCCESS);
  }

  private void updateRecordMQL(Connection conn) throws SQLException {
    int keyname = readRecord.nextInt();
    this.buildParameters();
    this.procUpdateRecordMQL.run(conn, keyname, this.params);
  }

  private void scanRecord(Connection conn) throws SQLException {
    int keyname = readRecord.nextInt();
    int count = randScan.nextInt();
    this.procScanRecord.run(conn, keyname, count, new ArrayList<>());
  }

  private void scanRecordMQL(Connection conn) throws SQLException {
    int keyname = readRecord.nextInt();
    int count = randScan.nextInt();
    this.procScanRecordMQL.run(conn, keyname, count, new ArrayList<>());
  }

  private void readRecord(Connection conn) throws SQLException {
    int keyname = readRecord.nextInt();
    this.procReadRecord.run(conn, keyname, this.results);
  }

  private void readRecordMQL(Connection conn) throws SQLException {
    int keyname = readRecord.nextInt();
    this.procReadRecordMQL.run(conn, keyname, this.results);
  }

  private void readRecordCypher(Connection conn) throws SQLException {
    int keyname = readRecord.nextInt();
    this.procReadRecordCypher.run(conn, keyname, this.results);
  }

  private void readModifyWriteRecord(Connection conn) throws SQLException {
    int keyname = readRecord.nextInt();
    this.buildParameters();
    this.procReadModifyWriteRecord.run(conn, keyname, this.params, this.results);
  }

  private void insertRecord(Connection conn) throws SQLException {
    int keyname = insertRecord.nextInt();
    this.buildParameters();
    this.procInsertRecord.run(conn, keyname, this.params);
  }

  private void insertRecordMQL(Connection conn) throws SQLException {
    int keyname = insertRecord.nextInt();
    this.buildParameters();
    this.procInsertRecordMQL.run(conn, keyname, this.params);
  }

  private void deleteRecord(Connection conn) throws SQLException {
    int keyname = readRecord.nextInt();
    this.procDeleteRecord.run(conn, keyname);
  }

  private void deleteRecordMQL(Connection conn) throws SQLException {
    int keyname = readRecord.nextInt();
    this.procDeleteRecordMQL.run(conn, keyname);
  }

  private void buildParameters() {
    for (int i = 0; i < this.params.length; i++) {
      this.params[i] = new String(TextGenerator.randomFastChars(rng(), this.data));
    }
  }
}
