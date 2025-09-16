package iuh.fit.cscore_be.controller;

import iuh.fit.cscore_be.enums.ProgrammingLanguage;
import iuh.fit.cscore_be.service.CompilerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Controller for system health checks and compiler information
 * Provides information about supported programming languages and system requirements
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class SystemController {
    
    private final CompilerService compilerService;
    
    /**
     * Get system health status
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());
        health.put("version", "1.0.0");
        
        // Add basic system information
        health.put("javaVersion", System.getProperty("java.version"));
        health.put("operatingSystem", System.getProperty("os.name"));
        health.put("architecture", System.getProperty("os.arch"));
        health.put("availableProcessors", Runtime.getRuntime().availableProcessors());
        health.put("maxMemory", Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB");
        health.put("freeMemory", Runtime.getRuntime().freeMemory() / (1024 * 1024) + " MB");
        
        return ResponseEntity.ok(health);
    }
    
    /**
     * Get compiler status and system requirements
     */
    @GetMapping("/compilers")
    public ResponseEntity<Map<String, Object>> getCompilerStatus() {
        try {
            Map<String, Object> systemRequirements = compilerService.getSystemRequirements();
            return ResponseEntity.ok(systemRequirements);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to get compiler status: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
    
    /**
     * Get supported programming languages
     */
    @GetMapping("/supported-languages")
    public ResponseEntity<Set<ProgrammingLanguage>> getSupportedLanguages() {
        try {
            Set<ProgrammingLanguage> supportedLanguages = compilerService.getSupportedLanguages();
            return ResponseEntity.ok(supportedLanguages);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Check if a specific language is supported
     */
    @GetMapping("/languages/{language}/supported")
    public ResponseEntity<Map<String, Object>> isLanguageSupported(@PathVariable String language) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            ProgrammingLanguage progLang = ProgrammingLanguage.valueOf(language.toUpperCase());
            boolean supported = compilerService.isLanguageSupported(progLang);
            
            response.put("language", language);
            response.put("supported", supported);
            
            if (supported) {
                CompilerService.CompilerInfo info = compilerService.getCompilerInfo(progLang);
                response.put("version", info.getVersion());
                response.put("path", info.getPath());
            }
            
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            response.put("language", language);
            response.put("supported", false);
            response.put("error", "Unknown programming language");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Failed to check language support: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    /**
     * Get installation recommendations for all languages
     */
    @GetMapping("/installation-guide")
    public ResponseEntity<Map<String, Object>> getInstallationGuide() {
        Map<String, Object> guide = new HashMap<>();
        
        guide.put("title", "CSCore Auto-Grading System Setup Guide");
        guide.put("description", "Setup instructions for programming language compilers and interpreters");
        
        Map<String, Object> requirements = new HashMap<>();
        
        // Java setup
        Map<String, Object> java = new HashMap<>();
        java.put("name", "Java Development Kit (JDK)");
        java.put("version", "11 or higher");
        java.put("windows", new String[]{
            "Download Oracle JDK or OpenJDK from official website",
            "Install and add JAVA_HOME to environment variables",
            "Add %JAVA_HOME%\\bin to PATH"
        });
        java.put("linux", new String[]{
            "sudo apt update",
            "sudo apt install default-jdk",
            "export JAVA_HOME=/usr/lib/jvm/default-java"
        });
        java.put("mac", new String[]{
            "brew install openjdk@11",
            "export JAVA_HOME=/usr/local/opt/openjdk@11"
        });
        requirements.put("java", java);
        
        // Python setup
        Map<String, Object> python = new HashMap<>();
        python.put("name", "Python");
        python.put("version", "3.8 or higher");
        python.put("windows", new String[]{
            "Download Python from python.org",
            "Install with 'Add to PATH' option checked",
            "Or install from Microsoft Store"
        });
        python.put("linux", new String[]{
            "sudo apt update",
            "sudo apt install python3 python3-pip"
        });
        python.put("mac", new String[]{
            "brew install python3"
        });
        requirements.put("python", python);
        
        // C/C++ setup
        Map<String, Object> c_cpp = new HashMap<>();
        c_cpp.put("name", "GCC Compiler");
        c_cpp.put("version", "GCC 7.0 or higher");
        c_cpp.put("windows", new String[]{
            "Install MSYS2 from https://www.msys2.org/",
            "Open MSYS2 terminal and run:",
            "pacman -S mingw-w64-ucrt-x86_64-gcc",
            "Add C:\\msys64\\ucrt64\\bin to PATH"
        });
        c_cpp.put("linux", new String[]{
            "sudo apt update",
            "sudo apt install build-essential"
        });
        c_cpp.put("mac", new String[]{
            "xcode-select --install",
            "Or: brew install gcc"
        });
        requirements.put("c_cpp", c_cpp);
        
        guide.put("requirements", requirements);
        guide.put("verification", new String[]{
            "After installation, restart the application server",
            "Check /api/system/compilers endpoint for status",
            "All supported languages should show 'available: true'"
        });
        
        return ResponseEntity.ok(guide);
    }
}
