package info.safetynetaccess.blsscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BlsschedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BlsschedulerApplication.class, args);
	}

}
