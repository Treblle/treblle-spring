package com.treblle.springboot.filter;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Minimal Spring Boot app used by integration tests. */
@SpringBootApplication
public class TestApplication {

    @RestController
    @RequestMapping("/api")
    static class TestController {

        @GetMapping("/users")
        public Map<String, Object> users() {
            return Map.of("users", java.util.List.of("alice", "bob"));
        }

        @PostMapping("/echo")
        public Map<String, Object> echo(@RequestBody Map<String, Object> body) {
            return body;
        }

        @GetMapping("/boom")
        public Map<String, Object> boom() {
            throw new IllegalStateException("kaboom");
        }

        @GetMapping("/health")
        public Map<String, Object> health() {
            return Map.of("status", "up");
        }
    }
}
