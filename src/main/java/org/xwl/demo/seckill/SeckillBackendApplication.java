package org.xwl.demo.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//@MapperScan("org.xwl.demo.seckill.dao")
public class SeckillBackendApplication {
	
    public static void main(String[] args) {
    	
        SpringApplication.run(SeckillBackendApplication.class, args);
    }

}

