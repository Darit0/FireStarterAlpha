package interntask.fileuploader;

import org.springframework.boot.SpringApplication;

public class TestFileUploaderApplication {

	public static void main(String[] args) {
		SpringApplication.from(FileUploaderApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
