package com.gridu.persistence.cassandra;

import com.datastax.driver.core.Session;
import com.datastax.spark.connector.cql.CassandraConnector;
import com.gridu.model.BotRegistry;
import com.gridu.persistence.BaseDao;
import org.apache.spark.SparkContext;
import org.apache.spark.sql.Dataset;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.datastax.spark.connector.japi.CassandraJavaUtil.*;

public class CassandraDao implements BaseDao<BotRegistry> {
    private Logger logger = LoggerFactory.getLogger(getClass());

    public static final String KEY_SPACE = "botsks";
    private SparkContext sc;
    public static final String CASSANDRA_HOST_PROPERTY = "localhost";
    public static final String CASSANDRA_BOT_REGISTRY_TABLE = "botregistry";
    private Session session;


    public CassandraDao(SparkContext sc) {
        this.sc = sc;
        setup();
    }

    @Override
    public void setup() {
        sc.getConf().set("spark.cassandra.connection.host", CASSANDRA_HOST_PROPERTY);
        sc.getConf().set("spark.driver.allowMultipleContexts", "true");
        final CassandraConnector cassandraConnector = CassandraConnector.apply(sc.conf());
        session = cassandraConnector.openSession();
        session.execute("CREATE KEYSPACE IF NOT EXISTS " + KEY_SPACE + " WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1}");
        session.execute("CREATE TABLE IF NOT EXISTS " + KEY_SPACE + "." + CASSANDRA_BOT_REGISTRY_TABLE + " (ip TEXT PRIMARY KEY, url TEXT, count INT)");
    }

    @Override
    public void persist(Dataset<BotRegistry> botRegistryDataset) {
        logger.info(">>> PERSISTING BOTS TO CASSANDRA <<<<<<<");
        botRegistryDataset.show(5,false);
        javaFunctions(botRegistryDataset.toJavaRDD())
                .writerBuilder(KEY_SPACE, CASSANDRA_BOT_REGISTRY_TABLE, mapToRow(BotRegistry.class))
                .withConstantTTL(Duration.standardSeconds(TTL)).saveToCassandra();
    }


    @Override
    public List<BotRegistry> getAllRecords() {
        return javaFunctions(sc)
                .cassandraTable(KEY_SPACE,
                        CASSANDRA_BOT_REGISTRY_TABLE,
                        mapRowTo(BotRegistry.class)).collect();
    }

    @Override
    public void cleanUp() {
        session.close();
        session.execute("DROP KEYSPACE IF EXISTS "+KEY_SPACE);
    }
}
