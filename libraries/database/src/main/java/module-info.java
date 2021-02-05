module com.brettonw.bedrock.database {
    requires com.brettonw.bedrock.bag;
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.driver.core;
    requires org.mongodb.bson;
    requires org.apache.logging.log4j;
    exports com.brettonw.bedrock.database;
}
