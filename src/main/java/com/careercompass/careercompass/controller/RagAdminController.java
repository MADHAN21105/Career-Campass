package com.careercompass.careercompass.controller;

import com.careercompass.careercompass.config.DataInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
public class RagAdminController {

    @Autowired
    private DataInitializer dataInitializer;

    @PostMapping("/ingest")
    public String triggerIngestion() {
        // Trigger manual ingestion in a separate thread to avoid blocking response too
        // long
        // (For simplicity in this fix, we run it directly, but async is better for
        // large data)
        new Thread(() -> dataInitializer.ingestAllData()).start();

        return "🚀 Ingestion triggered in background! Check server console for progress.";
    }
}
