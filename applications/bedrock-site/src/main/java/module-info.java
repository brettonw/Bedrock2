module com.brettonw.bedrock.site {
    requires com.brettonw.bedrock.bag;
    requires com.brettonw.bedrock.service;
    requires com.brettonw.bedrock.secret;
    requires tomcat.servlet.api;
    requires org.apache.logging.log4j;
    opens com.brettonw.bedrock.site to com.brettonw.bedrock.service;
}
