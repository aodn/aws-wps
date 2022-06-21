package au.org.emii.aggregationworker.config;

import au.org.aodn.aws.util.S3Storage;
import au.org.aodn.aws.wps.Storage;
import au.org.aodn.aws.wps.status.WpsConfig;
import au.org.emii.aggregationworker.AggregationWorker;
import au.org.emii.download.Downloader;
import au.org.emii.util.IntegerHelper;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import static au.org.aodn.aws.wps.status.WpsConfig.DOWNLOAD_CONNECT_TIMEOUT_CONFIG_KEY;
import static au.org.aodn.aws.wps.status.WpsConfig.DOWNLOAD_READ_TIMEOUT_CONFIG_KEY;

@Configuration
public class AggregationConfig {

    public static final int DEFAULT_CONNECT_TIMEOUT_MS = 60000;
    public static final int DEFAULT_READ_TIMEOUT_MS = 60000;

    @Bean
    public S3Storage createS3Storage() {
        return new S3Storage();
    }

    @Bean
    public AggregationWorker createAggregationRunner(Storage<?> storage, Downloader downloader) {
        return new AggregationWorker(storage, downloader);
    }

    @Bean
    public Downloader createDownloader(RestTemplate restTemplate) {
        return new Downloader(restTemplate);
    }

    @Bean
    protected RestTemplate createRestTemplate(RestTemplateBuilder builder) {
        //  Parse connect timeout
        String downloadConnectTimeoutString = WpsConfig.getProperty(DOWNLOAD_CONNECT_TIMEOUT_CONFIG_KEY);
        int downloadConnectTimeout;
        if (downloadConnectTimeoutString != null && IntegerHelper.isInteger(downloadConnectTimeoutString)) {
            downloadConnectTimeout = Integer.parseInt(downloadConnectTimeoutString);
        } else {
            downloadConnectTimeout = DEFAULT_CONNECT_TIMEOUT_MS;
        }

        // Parse read timeout
        String downloadReadTimeoutString = WpsConfig.getProperty(DOWNLOAD_READ_TIMEOUT_CONFIG_KEY);
        int downloadReadTimeout;
        if (downloadReadTimeoutString != null && IntegerHelper.isInteger(downloadReadTimeoutString)) {
            downloadReadTimeout = Integer.parseInt(downloadConnectTimeoutString);
        } else {
            downloadReadTimeout = DEFAULT_READ_TIMEOUT_MS;
        }

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(20);
        connectionManager.closeExpiredConnections();
        connectionManager.setDefaultMaxPerRoute(20);

        RequestConfig requestConfig = RequestConfig
                .custom()
                .setConnectionRequestTimeout(10000)        // timeout to get connection from pool
                .setSocketTimeout(downloadConnectTimeout)  // standard connection timeout
                .setConnectTimeout(downloadConnectTimeout) // standard connection timeout
                .build();

        HttpClient httpClient = HttpClientBuilder.create()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(downloadReadTimeout);

        return builder.requestFactory(() -> requestFactory).build();
    }

}
