package bar.barinade.surveyor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SurveyorApplication {

	public static void main(String[] args) {
		SpringApplication.run(SurveyorApplication.class, args);
	}

}
