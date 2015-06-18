/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.yarn.server.timelineservice.storage.common;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.BufferedMutator;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.client.Table;

/**
 * Implements behavior common to tables used in the timeline service storage.
 *
 * @param <T> reference to the table instance class itself for type safety.
 */
public abstract class BaseTable<T> {

  /**
   * Name of config variable that is used to point to this table
   */
  private final String tableNameConfName;

  /**
   * Unless the configuration overrides, this will be the default name for the
   * table when it is created.
   */
  private final String defaultTableName;

  /**
   * @param tableNameConfName name of config variable that is used to point to
   *          this table.
   */
  protected BaseTable(String tableNameConfName, String defaultTableName) {
    this.tableNameConfName = tableNameConfName;
    this.defaultTableName = defaultTableName;
  }

  /**
   * Used to create a type-safe mutator for this table.
   *
   * @param hbaseConf used to read table name
   * @param conn used to create a table from.
   * @return a type safe {@link BufferedMutator} for the entity table.
   * @throws IOException
   */
  public TypedBufferedMutator<T> getTableMutator(Configuration hbaseConf,
      Connection conn) throws IOException {

    TableName tableName = this.getTableName(hbaseConf);

    // Plain buffered mutator
    BufferedMutator bufferedMutator = conn.getBufferedMutator(tableName);

    // Now make this thing type safe.
    // This is how service initialization should hang on to this variable, with
    // the proper type
    TypedBufferedMutator<T> table =
        new BufferedMutatorDelegator<T>(bufferedMutator);

    return table;
  }

  /**
   * @param hbaseConf used to read settings that override defaults
   * @param conn used to create table from
   * @param scan that specifies what you want to read from this table.
   * @return scanner for the table.
   * @throws IOException
   */
  public ResultScanner getResultScanner(Configuration hbaseConf,
      Connection conn, Scan scan) throws IOException {
    Table table = conn.getTable(getTableName(hbaseConf));
    return table.getScanner(scan);
  }

  /**
   * Get the table name for this table.
   *
   * @param hbaseConf
   */
  public TableName getTableName(Configuration hbaseConf) {
    TableName table =
        TableName.valueOf(hbaseConf.get(tableNameConfName, defaultTableName));
    return table;

  }

  /**
   * Used to create the table in HBase. Should be called only once (per HBase
   * instance).
   *
   * @param admin
   * @param hbaseConf
   */
  public abstract void createTable(Admin admin, Configuration hbaseConf)
      throws IOException;

}
