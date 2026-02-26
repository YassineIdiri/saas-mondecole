package com.example.mondecole_pocket.controller;

import com.example.mondecole_pocket.dto.UserStatsResponse;
import com.example.mondecole_pocket.entity.enums.UserRole;
import com.example.mondecole_pocket.service.AdminUserService;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/benchmark")
@RequiredArgsConstructor
public class BenchmarkController {

    private final AdminUserService adminUserService;
    private final MeterRegistry meterRegistry;

    @GetMapping("/students/stats")
    @Timed(value = "benchmark.users.slow", description = "Time to fetch user stats (slow version)")
    public ResponseEntity<Map<String, Object>> getStudentStatsSlow(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "1") Long organization
    ) {

        long startTime = System.nanoTime();
        long startMillis = System.currentTimeMillis();

        Pageable pageable = PageRequest.of(page, size);
        Page<UserStatsResponse> users = adminUserService.getUsersWithStatsSlow(organization, UserRole.STUDENT, pageable);
        long endTime = System.nanoTime();
        long endMillis = System.currentTimeMillis();
        long durationMs = endMillis - startMillis;
        long durationNs = endTime - startTime;

        // Enregistrer dans Micrometer
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("benchmark.users.slow.manual")
                .description("Manual timing for slow query")
                .register(meterRegistry));

        Map<String, Object> result = new HashMap<>();
        result.put("version", "SLOW (N+1 Problem)");
        result.put("count", users.getTotalElements());
        result.put("pageSize", users.getNumberOfElements());
        result.put("durationMs", durationMs);
        result.put("durationSeconds", durationMs / 1000.0);
        result.put("data", users.getContent());

        log.warn("⏱️ SLOW query completed in {}ms ({} seconds)",
                durationMs, String.format("%.2f", durationMs / 1000.0));

        return ResponseEntity.ok(result);
    }


    @GetMapping("/teachers/stats")
    @Timed(value = "benchmark.users.slow", description = "Time to fetch user stats (slow version)")
    public ResponseEntity<Map<String, Object>> getTeachersStatsSlow(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "1") Long organization
    ) {
        log.info("🐌 Starting SLOW benchmark - users stats");

        long startTime = System.nanoTime();
        long startMillis = System.currentTimeMillis();

        Pageable pageable = PageRequest.of(page, size);
        Page<UserStatsResponse> users = adminUserService.getUsersWithStatsSlow(organization, UserRole.TEACHER, pageable);
        long endTime = System.nanoTime();
        long endMillis = System.currentTimeMillis();
        long durationMs = endMillis - startMillis;
        long durationNs = endTime - startTime;

        // Enregistrer dans Micrometer
        Timer.Sample sample = Timer.start(meterRegistry);
        sample.stop(Timer.builder("benchmark.users.slow.manual")
                .description("Manual timing for slow query")
                .register(meterRegistry));

        Map<String, Object> result = new HashMap<>();
        result.put("version", "SLOW (N+1 Problem)");
        result.put("count", users.getTotalElements());
        result.put("pageSize", users.getNumberOfElements());
        result.put("durationMs", durationMs);
        result.put("durationSeconds", durationMs / 1000.0);
        result.put("data", users.getContent());

        log.warn("⏱️ SLOW query completed in {}ms ({} seconds)",
                durationMs, String.format("%.2f", durationMs / 1000.0));

        return ResponseEntity.ok(result);
    }



    //Endpoint de benchmark - Version OPTIMISÉE (après)
    @GetMapping("/student/stats/fast")
    @Timed(value = "benchmark.users.fast", description = "Time to fetch user stats (fast version)")
    public ResponseEntity<Map<String, Object>> getStudentsStatsFast(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "1") Long organization
    ) {
        log.info("🚀 Starting FAST benchmark - users stats");
        long startTime = System.nanoTime();
        long startMillis = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(page, size);
        Page<UserStatsResponse> users = adminUserService.getUsersWithStatsFast(organization, UserRole.STUDENT, pageable);
        long endTime = System.nanoTime();
        long endMillis = System.currentTimeMillis();
        long durationMs = endMillis - startMillis;
        Map<String, Object> result = new HashMap<>();
        result.put("version", "FAST (Optimized JOIN)");
        result.put("count", users.getTotalElements());
        result.put("pageSize", users.getNumberOfElements());
        result.put("durationMs", durationMs);
        result.put("durationSeconds", durationMs / 1000.0);
        result.put("data", users.getContent());
        log.info("✅ FAST query completed in {}ms ({} seconds)",
                durationMs, String.format("%.2f", durationMs / 1000.0));
        return ResponseEntity.ok(result);
    }



    //Endpoint de benchmark - Version OPTIMISÉE (après)
    @GetMapping("/teachers/stats/fast")
    @Timed(value = "benchmark.users.fast", description = "Time to fetch user stats (fast version)")
    public ResponseEntity<Map<String, Object>> getTeachersStatsFast(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "1") Long organization
    ) {
        log.info("🚀 Starting FAST benchmark - users stats");

        long startTime = System.nanoTime();
        long startMillis = System.currentTimeMillis();
        Pageable pageable = PageRequest.of(page, size);
        Page<UserStatsResponse> users = adminUserService.getUsersWithStatsFast(organization, UserRole.TEACHER, pageable);
        long endTime = System.nanoTime();
        long endMillis = System.currentTimeMillis();
        long durationMs = endMillis - startMillis;
        Map<String, Object> result = new HashMap<>();
        result.put("version", "FAST (Optimized JOIN)");
        result.put("count", users.getTotalElements());
        result.put("pageSize", users.getNumberOfElements());
        result.put("durationMs", durationMs);
        result.put("durationSeconds", durationMs / 1000.0);
        result.put("data", users.getContent());
        log.info("✅ FAST query completed in {}ms ({} seconds)",
                durationMs, String.format("%.2f", durationMs / 1000.0));
        return ResponseEntity.ok(result);
    }


    @GetMapping("/compare-student")
    public ResponseEntity<Map<String, Object>> compareStudents(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "1") Long organization
    ) {
        log.info("📊 Starting benchmark comparison");

        Pageable pageable = PageRequest.of(0, size);
        // Test 1 : SLOW
        long slowStart = System.currentTimeMillis();
        Page<UserStatsResponse> slowResult = adminUserService.getUsersWithStatsSlow(organization, UserRole.STUDENT, pageable);
        long slowDuration = System.currentTimeMillis() - slowStart;

        // Test 2 : FAST
        long fastStart = System.currentTimeMillis();
        Page<UserStatsResponse> fastResult = adminUserService.getUsersWithStatsFast(organization, UserRole.STUDENT, pageable);
        long fastDuration = System.currentTimeMillis() - fastStart;

        // Calcul du gain
        double improvement = ((double)(slowDuration - fastDuration) / slowDuration) * 100;
        double speedup = (double) slowDuration / fastDuration;

        Map<String, Object> result = new HashMap<>();
        result.put("slowVersionMs", slowDuration);
        result.put("fastVersionMs", fastDuration);
        result.put("improvementPercent", String.format("%.1f%%", improvement));
        result.put("speedupFactor", String.format("%.1fx", speedup));
        result.put("dataSamples", size);

        log.info("📈 Benchmark Results:");
        log.info("   SLOW: {}ms", slowDuration);
        log.info("   FAST: {}ms", fastDuration);
        log.info("   Improvement: {}", String.format("%.1f%%", improvement));

        return ResponseEntity.ok(result);
    }

    @GetMapping("/compare-teacher")
    public ResponseEntity<Map<String, Object>> compareTeachers(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "1") Long organization
    ) {
        log.info("📊 Starting benchmark comparison");

        Pageable pageable = PageRequest.of(0, size);
        // Test 1 : SLOW
        long slowStart = System.currentTimeMillis();
        Page<UserStatsResponse> slowResult = adminUserService.getUsersWithStatsSlow(organization, UserRole.TEACHER, pageable);
        long slowDuration = System.currentTimeMillis() - slowStart;

        // Test 2 : FAST
        long fastStart = System.currentTimeMillis();
        Page<UserStatsResponse> fastResult = adminUserService.getUsersWithStatsFast(organization, UserRole.TEACHER, pageable);
        long fastDuration = System.currentTimeMillis() - fastStart;

        // Calcul du gain
        double improvement = ((double)(slowDuration - fastDuration) / slowDuration) * 100;
        double speedup = (double) slowDuration / fastDuration;

        Map<String, Object> result = new HashMap<>();
        result.put("slowVersionMs", slowDuration);
        result.put("fastVersionMs", fastDuration);
        result.put("improvementPercent", String.format("%.1f%%", improvement));
        result.put("speedupFactor", String.format("%.1fx", speedup));
        result.put("dataSamples", size);

        log.info("📈 Benchmark Results:");
        log.info("   SLOW: {}ms", slowDuration);
        log.info("   FAST: {}ms", fastDuration);
        log.info("   Improvement: {}", String.format("%.1f%%", improvement));

        return ResponseEntity.ok(result);
    }
}