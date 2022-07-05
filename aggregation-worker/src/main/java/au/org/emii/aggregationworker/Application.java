package au.org.emii.aggregationworker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * Created by craigj on 6/04/17.
 */
@SpringBootApplication
public class Application {

    protected static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {

        SpringApplicationBuilder sb = new SpringApplicationBuilder();
        SpringApplication application = sb.
                sources(Application.class)
                .main(Application.class)
                .web(WebApplicationType.NONE)
                .build();

        application.run(Application.class, args);
        logger.info("Batch job execute completed");
    }
}
