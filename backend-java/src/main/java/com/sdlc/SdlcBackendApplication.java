package com.sdlc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SdlcBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SdlcBackendApplication.class, args);
        System.out.println("\n🚀 SDLC Backend running on http://localhost:3001/api/v1");
        System.out.println("🔐 GitHub OAuth login at: http://localhost:3001/api/v1/auth/github");
        System.out.println("📂 Per-user GitHub repos at: /api/v1/github/repos");
        System.out.println("📄 Async document ingestion pipeline active");
        System.out.println("🔒 Multi-user isolation: each user sees only their own repos\n");
    }
}
