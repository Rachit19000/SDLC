package com.sdlc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SdlcBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SdlcBackendApplication.class, args);
        System.out.println("\n🚀 SDLC Backend running on http://localhost:3001/api/v1");
        System.out.println("📝 Test login with: rachitjainemail@gmail.com / password123\n");
    }
}
