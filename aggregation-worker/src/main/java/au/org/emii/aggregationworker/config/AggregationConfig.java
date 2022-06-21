package au.org.emii.aggregationworker.config;

import au.org.aodn.aws.util.S3Storage;
import au.org.aodn.aws.wps.Storage;
import au.org.emii.aggregationworker.AggregationWorker;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AggregationConfig {

    @Bean
    public S3Storage createS3Storage() {
        return new S3Storage();
    }

    @Bean
    public AggregationWorker createAggregationRunner(Storage<?> storage) {
        return new AggregationWorker(storage);
    }
}
