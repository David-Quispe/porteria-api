package pe.tecsup.porteria;

import org.springframework.boot.SpringApplication;

public class TestPorteriaApplication {

    public static void main(String[] args) {
        SpringApplication.from(PorteriaApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
