module com.brettonw.bedrock.service {
    requires javax.servlet.api;
    requires org.apache.commons.io;
    requires com.brettonw.bedrock.bag;
    requires com.brettonw.bedrock.secret;
    requires org.apache.logging.log4j;
    exports com.brettonw.bedrock.service;
}
