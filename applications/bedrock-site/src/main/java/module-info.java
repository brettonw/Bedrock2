module com.brettonw.bedrock.site {
    requires com.brettonw.bedrock.bag;
    requires com.brettonw.bedrock.service;
    requires com.brettonw.bedrock.secret;
    requires java.servlet;
    requires com.brettonw.bedrock.logger;
    opens com.brettonw.bedrock.site to com.brettonw.bedrock.service;
}
