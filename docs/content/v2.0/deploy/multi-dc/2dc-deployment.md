---
title: Two data center (2DC)
linkTitle: Two data center (2DC)
description: Two data center (2DC) deployments
beta: /latest/faq/general/#what-is-the-definition-of-the-beta-feature-tag
block_indexing: true
menu:
  v2.0:
    parent: multi-dc
    identifier: 2dc-deployment
    weight: 633
type: page
isTocNested: true
showAsideToc: true
---


{{< tip title="Recommended Reading" >}}

[9 Techniques to Build Cloud-Native, Geo-Distributed SQL Apps with Low Latency](https://blog.yugabyte.com/9-techniques-to-build-cloud-native-geo-distributed-sql-apps-with-low-latency/) highlights the various multi-DC deployment strategies (including 2DC deployments) for a distributed SQL database like YugabyteDB.

{{< /tip >}}

For details on the two data center (2DC) deployment architecture and supported replication scenarios, see [Two data center (2DC) deployments](../../../architecture/2dc-deployments).

Follow the steps below to set up a 2DC deployment using either unidirectional (aka master-follower) or bidirectional (aka multi-master) replication between the data centers.

## 1. Set up

### Producer universe

To create the producer universe, follow these steps.

1. Create the “yugabyte-producer” universe.

2. Create the the tables for the APIs being used.

### Consumer universe

To create the consumer universe, follow these steps.

1. Create the “yugabyte-consumer” universe.

2. Create the tables for the APIs being used.

Make sure to create the same tables as you did for the producer universe.

After creating the required tables, you can now set up asysnchronous replication using the steps below.

## 2. Unidirectional (aka master-follower) replication

1. Look up the producer universe UUID and the table IDs for the two tables and the index table on master UI.

2. Run the following `yb-admin` [`setup_universe_replication`](../../admin/yb-admin/#setup-universe-replication) command from the YugabyteDB home directory in the producer universe.

```sh
./bin/yb-admin -master_addresses <consumer_universe_master_addresses>
setup_universe_replication <producer_universe_uuid>
  <producer_universe_master_addresses>
  <table_id>,[<table_id>..]
```

**Example**

```sh
./bin/yb-admin -master_addresses 127.0.0.11:7100,127.0.0.12:7100,127.0.0.13:7100 \
setup_universe_replication e260b8b6-e89f-4505-bb8e-b31f74aa29f3 \
	127.0.0.1:7100,127.0.0.2:7100,127.0.0.3:7100 \
	000030a5000030008000000000004000,000030a5000030008000000000004005,dfef757c415c4b2cacc9315b8acb539a
```

{{< note title="Note" >}}

There should be three table IDs in the command above — two of those are YSQL for base table and index, and one for CQL table. Also, make sure to specify all master addresses for both producer and consumer universes in the command.

{{< /note >}}

## 3. Bidirectional (aka multi-master) replication

To set up bidirectional replication, follow the steps above in the Unidirectional replication section above and then do the same steps for the the “yugabyte-consumer” universe.

Note that this time, “yugabyte-producer” will be set up to consume data from “yugabyte-consumer”.

## 4. Load data into producer universe

1. Download the YugabyteDB workload generator JAR file (`yb-sample-apps.jar`) from [GitHub](https://github.com/yugabyte/yb-sample-apps).

2. Start loading data into “yugabyte-producer” using the YugabyteDB workload generator JAR.

**YSQL example**

```sh
java -jar yb-sample-apps.jar --workload SqlSecondaryIndex  --nodes 127.0.0.1:5433
```

**YCQL example**

```sh
java -jar yb-sample-apps.jar --workload CassandraBatchKeyValue --nodes 127.0.0.1:9042
```

For bidirectional replication, repeat this step in the "yugabyte-consumer" universe.

## 5. Verify replication

**For unidirectional replication**

Connect to “yugabyte-consumer” universe using the YSQL shell (`ysqlsh`) or the YCQL shell (`cqlsh`), and then confirm that you can see expected records.

**For bidirectional replication**

Repeat the steps above, but pump data into “yugabyte-consumer”. To avoid primary key conflict errors, keep the key space for the two universes separate.
