package top.itning.smp.smpgateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;

/**
 * @author itning
 */
@SpringBootApplication
@EnableEurekaClient
@EnableZuulProxy
public class SmpGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmpGatewayApplication.class, args);
    }

}
