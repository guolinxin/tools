package com.linxin.tools.config;

import com.linxin.tools.data.provisioning.PPAPIClient;
import com.linxin.tools.data.provisioning.PPAPIService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.InterceptingClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "ppapi")
public class PPAPIRestConfig {

    private String username;

    private String password;

    private String url;

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @Bean
    public PPAPIClient ppapiClient() throws SQLException {
        return new PPAPIClient(url, restTemplate());
    }

    @Bean
    public PPAPIService ppapiService(PPAPIClient ppapiClient) {
        return new PPAPIService(ppapiClient);
    }

    private RestTemplate restTemplate() throws SQLException {
        List<ClientHttpRequestInterceptor> interceptors = Collections
                .<ClientHttpRequestInterceptor>singletonList(new BasicAuthorizationInterceptor(
                        username, password));
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(new InterceptingClientHttpRequestFactory(restTemplate.getRequestFactory(),
                interceptors));

        restTemplate.setErrorHandler(new LoggingRestTemplateErrorHandler());

        return restTemplate;
    }


    class LoggingRestTemplateErrorHandler extends DefaultResponseErrorHandler {
        Logger logger = LoggerFactory.getLogger(this.getClass());

        @Override
        public void handleError(final ClientHttpResponse clientHttpResponse) throws IOException {
            try {
                super.handleError(clientHttpResponse);
            } catch (HttpStatusCodeException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    logger.info(e.getMessage());
                    logger.info(e.getResponseBodyAsString());
                } else {
                    logger.error(e.getMessage());
                    logger.error(e.getResponseBodyAsString());
                }
                throw e;
            }
        }
    }
}
