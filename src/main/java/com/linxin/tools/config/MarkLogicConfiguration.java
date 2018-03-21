package com.linxin.tools.config;

import com.marklogic.xcc.ContentSource;
import com.marklogic.xcc.ContentSourceFactory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Service
//@Configuration
@ConfigurationProperties(prefix = "mirdatabase")
public class MarkLogicConfiguration {

    /**
     * MarkLogic user id
     */
    private String user;

    /**
     * MarkLogic user password
     */
    private String password;

    /**
     * MarkLogic server address
     */
    private String host;

    /**
     * MarkLogic server port
     */
    private int port;

    /**
     * Database name
     */
    private String contentName;

    /**
     * Authentication type
     */
    private String authentication;

    /**
     * MarkLogic module path
     */
    private String mlModule;


//    @Bean
//    public ContentSource contentSource() {
//        return ContentSourceFactory.newContentSource(host,
//                port, user,
//                password, contentName);
//    }
//
//    @PostConstruct
//    private void checkConfiguration() {
//        if (StringUtils.isEmpty(host) || StringUtils.isEmpty(password) || StringUtils.isEmpty(user) || StringUtils.isEmpty(port))
//            throw new NullPointerException("Configuration Setting Incomplete, Please provide MarkLogic Configuration Properties");
//    }
}

