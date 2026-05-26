package com.resumescreener.controller;

import com.resumescreener.dto.ErrorResponse;
import com.resumescreener.model.Session;
import com.resumescreener.service.SessionManager;
import com.resumescreener.util.SensitiveDataMasker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/resume")
@CrossOrigin(origins = "${cors.allowed.origins:http://localhost:4200}")
@Slf4j
public class ResumeController {

    @Autowired
    private SessionManager sessionManager;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadResume(
            @RequestParam("file") MultipartFile file,
            @RequestParam("jobDescription") String jobDescription) {
        try {
            log.info("Resume upload request: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("File is empty", 400));
            }

            if (file.getSize() > 10 * 1024 * 1024) { // 10MB limit
                return ResponseEntity.badRequest().body(new ErrorResponse("File too large (max 10MB)", 400));
            }

            String resumeText = new String(file.getBytes());

            Session session = sessionManager.createSession(
                file.getOriginalFilename(),
                resumeText,
                jobDescription
            );

            Map<String, String> response = new HashMap<>();
            response.put("sessionId", session.getId());
            response.put("fileName", session.getResumeFileName());
            response.put("message", "Resume uploaded successfully");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("File upload failed", e);
            return ResponseEntity.status(500).body(new ErrorResponse("File upload failed", 500));
        }
    }

    @GetMapping("/{sessionId}/preview")
    public ResponseEntity<?> getPreview(@PathVariable String sessionId) {
        try {
            Session session = sessionManager.getSession(sessionId);

            String maskedResumeText = SensitiveDataMasker.maskSensitiveData(session.getResumeText());
            String preview = maskedResumeText.substring(0, Math.min(500, maskedResumeText.length())) + "...";

            Map<String, String> response = new HashMap<>();
            response.put("sessionId", session.getId());
            response.put("fileName", SensitiveDataMasker.maskResumeName(session.getResumeFileName()));
            response.put("resumeTextPreview", preview);
            response.put("jobDescription", session.getJobDescription());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(new ErrorResponse(e.getMessage(), 404));
        }
    }
}
