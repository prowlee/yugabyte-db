// Copyright (c) YugaByte, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under the License
// is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
// or implied.  See the License for the specific language governing permissions and limitations
// under the License.
//
package org.yb.cql;

import com.datastax.driver.core.Row;
import com.datastax.driver.core.exceptions.InvalidQueryException;
import com.google.common.net.HostAndPort;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yb.client.YBClient;

import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

public class TestHideRedis extends BaseCQLTest {

  private static final Logger LOG = LoggerFactory.getLogger(TestHideRedis.class);

  private void runInvalidQuery(String query) throws Exception {
    try {
      session.execute(query);
      fail("Did not throw exception");
    } catch (InvalidQueryException e) {
      // Should receive table not found exception.
      LOG.info("Expected exception", e);
    }
  }

  @Test
  public void testHideRedis() throws Exception {
    runInvalidQuery(String.format("CREATE KEYSPACE %s", YBClient.REDIS_KEYSPACE_NAME));
    session.execute("CREATE KEYSPACE test");
    runInvalidQuery(String.format("CREATE TABLE test.\"%s\" (c1 int PRIMARY KEY)",
      YBClient.REDIS_DEFAULT_TABLE_NAME));

    YBClient client = miniCluster.getClient();
    client.createRedisTable(YBClient.REDIS_DEFAULT_TABLE_NAME, 1);
    try {
      // Can't perform SELECT queries on redis table.
      runInvalidQuery(String.format("SELECT * FROM %s.\"%s\"", YBClient.REDIS_KEYSPACE_NAME,
        YBClient.REDIS_DEFAULT_TABLE_NAME));

      // Not listed in cassandra system tables.
      assertEquals(0, session.execute(String.format("SELECT keyspace_name FROM system_schema" +
        ".keyspaces where keyspace_name = '%s'", YBClient.REDIS_KEYSPACE_NAME)).all().size());
      assertEquals(0, session.execute(String.format("SELECT table_name FROM system_schema.tables " +
        "where table_name = '%s'", YBClient.REDIS_DEFAULT_TABLE_NAME)).all().size());

      // Ensure deletes don't work.
      runInvalidQuery(String.format("DROP TABLE %s.\"%s\"", YBClient.REDIS_KEYSPACE_NAME,
        YBClient.REDIS_DEFAULT_TABLE_NAME));

      // Ensure INSERT/UPDATEs don't work.
      runInvalidQuery(String.format("INSERT INTO %s.\"%s\" (%s) values (0)",
        YBClient.REDIS_KEYSPACE_NAME, YBClient.REDIS_DEFAULT_TABLE_NAME,
        YBClient.REDIS_KEY_COLUMN_NAME));

      runInvalidQuery(String.format("UPDATE %s.\"%s\" SET %s = 0",
        YBClient.REDIS_KEYSPACE_NAME, YBClient.REDIS_DEFAULT_TABLE_NAME,
        YBClient.REDIS_KEY_COLUMN_NAME));

    } finally {
      client.deleteTable(YBClient.REDIS_DEFAULT_TABLE_NAME, YBClient.REDIS_KEYSPACE_NAME);
      runInvalidQuery(String.format("DROP KEYSPACE %s", YBClient.REDIS_KEYSPACE_NAME));
      runInvalidQuery(String.format("DROP TABLE test.\"%s\"",
        YBClient.REDIS_DEFAULT_TABLE_NAME));
      runInvalidQuery(String.format("DROP TABLE %s.\"%s\"",
        YBClient.REDIS_KEYSPACE_NAME, YBClient.REDIS_DEFAULT_TABLE_NAME));
    }

    // Verify all servers are alive.
    for (HostAndPort hostAndPort : miniCluster.getTabletServers().keySet()) {
      client.waitForServer(hostAndPort, 10000);
    }
  }
}
