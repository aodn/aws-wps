package au.org.emii.aggregationworker.config;

import au.org.aodn.aws.wps.LocalStorage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class AggregationConfigTest {

    @Bean
    @Primary
    public LocalStorage createLocalStorage() {
        return new LocalStorage();
    }
}
