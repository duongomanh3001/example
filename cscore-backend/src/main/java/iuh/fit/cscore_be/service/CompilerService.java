package iuh.fit.cscore_be.service;

import iuh.fit.cscore_be.enums.ProgrammingLanguage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for managing and detecting available compilers and interpreters
 * Ensures system compatibility across different operating systems
 */
@Service
@Slf4j
public class CompilerService {
    
    private final Map<ProgrammingLanguage, CompilerInfo> availableCompilers = new HashMap<>();
    private final Map<String, List<String>> compilerPaths = new HashMap<>();
    
    @PostConstruct
    public void initializeCompilers() {
        log.info("Initializing compiler detection...");
        
        detectJavaCompiler();
        detectPythonInterpreter();
        detectCCompiler();
        detectCppCompiler();
        
        logAvailableCompilers();
    }
    
    /**
     * Check if a programming language is supported on this system
     */
    public boolean isLanguageSupported(ProgrammingLanguage language) {
        return availableCompilers.containsKey(language) && availableCompilers.get(language).isAvailable();
    }
    
    /**
     * Get compiler information for a language
     */
    public CompilerInfo getCompilerInfo(ProgrammingLanguage language) {
        return availableCompilers.get(language);
    }
    
    /**
     * Get all supported languages
     */
    public Set<ProgrammingLanguage> getSupportedLanguages() {
        return availableCompilers.entrySet().stream()
                .filter(entry -> entry.getValue().isAvailable())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
    
    /**
     * Get system requirements for setup
     */
    public Map<String, Object> getSystemRequirements() {
        Map<String, Object> requirements = new HashMap<>();
        
        String os = System.getProperty("os.name");
        requirements.put("operatingSystem", os);
        requirements.put("javaVersion", System.getProperty("java.version"));
        requirements.put("architecture", System.getProperty("os.arch"));
        
        Map<String, Object> compilers = new HashMap<>();
        for (ProgrammingLanguage lang : ProgrammingLanguage.values()) {
            CompilerInfo info = availableCompilers.get(lang);
            if (info != null) {
                Map<String, Object> compilerData = new HashMap<>();
                compilerData.put("available", info.isAvailable());
                compilerData.put("version", info.getVersion());
                compilerData.put("path", info.getPath());
                compilerData.put("recommendations", getInstallationRecommendations(lang));
                compilers.put(lang.getName(), compilerData);
            }
        }
        requirements.put("compilers", compilers);
        
        return requirements;
    }
    
    /**
     * Detect Java compiler
     */
    private void detectJavaCompiler() {
        List<String> candidates = Arrays.asList("javac", "java");
        
        for (String cmd : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd, "-version");
                Process process = pb.start();
                
                boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    continue;
                }
                
                if (process.exitValue() == 0 || process.exitValue() == 1) { // javac returns 1 for version
                    String version = readVersionOutput(process);
                    String path = findExecutablePath(cmd);
                    
                    availableCompilers.put(ProgrammingLanguage.JAVA, 
                        new CompilerInfo(true, version, path, cmd));
                    
                    log.info("Java compiler detected: {} at {}", version, path);
                    break;
                }
            } catch (Exception e) {
                log.debug("Failed to detect Java with command: {}", cmd, e);
            }
        }
        
        if (!availableCompilers.containsKey(ProgrammingLanguage.JAVA)) {
            availableCompilers.put(ProgrammingLanguage.JAVA, 
                new CompilerInfo(false, null, null, null));
            log.warn("Java compiler not found. Please install JDK.");
        }
    }
    
    /**
     * Detect Python interpreter
     */
    private void detectPythonInterpreter() {
        List<String> candidates = Arrays.asList("python3", "python", "py");
        
        for (String cmd : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
                Process process = pb.start();
                
                boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    continue;
                }
                
                if (process.exitValue() == 0) {
                    String version = readVersionOutput(process);
                    String path = findExecutablePath(cmd);
                    
                    availableCompilers.put(ProgrammingLanguage.PYTHON, 
                        new CompilerInfo(true, version, path, cmd));
                    
                    log.info("Python interpreter detected: {} at {}", version, path);
                    break;
                }
            } catch (Exception e) {
                log.debug("Failed to detect Python with command: {}", cmd, e);
            }
        }
        
        if (!availableCompilers.containsKey(ProgrammingLanguage.PYTHON)) {
            availableCompilers.put(ProgrammingLanguage.PYTHON, 
                new CompilerInfo(false, null, null, null));
            log.warn("Python interpreter not found. Please install Python 3.x");
        }
    }
    
    /**
     * Detect C compiler
     */
    private void detectCCompiler() {
        String os = System.getProperty("os.name").toLowerCase();
        List<String> candidates = new ArrayList<>();
        
        if (os.contains("win")) {
            candidates.addAll(Arrays.asList(
                "C:\\msys64\\ucrt64\\bin\\gcc.exe",
                "C:\\msys64\\mingw64\\bin\\gcc.exe",
                "gcc"
            ));
        } else {
            candidates.addAll(Arrays.asList("gcc", "clang"));
        }
        
        for (String cmd : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
                Process process = pb.start();
                
                boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    continue;
                }
                
                if (process.exitValue() == 0) {
                    String version = readVersionOutput(process);
                    String path = Files.exists(Paths.get(cmd)) ? cmd : findExecutablePath(cmd);
                    
                    availableCompilers.put(ProgrammingLanguage.C, 
                        new CompilerInfo(true, version, path, cmd));
                    
                    log.info("C compiler detected: {} at {}", version, path);
                    break;
                }
            } catch (Exception e) {
                log.debug("Failed to detect C compiler with command: {}", cmd, e);
            }
        }
        
        if (!availableCompilers.containsKey(ProgrammingLanguage.C)) {
            availableCompilers.put(ProgrammingLanguage.C, 
                new CompilerInfo(false, null, null, null));
            log.warn("C compiler not found. Please install GCC or Clang.");
        }
    }
    
    /**
     * Detect C++ compiler
     */
    private void detectCppCompiler() {
        String os = System.getProperty("os.name").toLowerCase();
        List<String> candidates = new ArrayList<>();
        
        if (os.contains("win")) {
            candidates.addAll(Arrays.asList(
                "C:\\msys64\\ucrt64\\bin\\g++.exe",
                "C:\\msys64\\mingw64\\bin\\g++.exe",
                "g++"
            ));
        } else {
            candidates.addAll(Arrays.asList("g++", "clang++"));
        }
        
        for (String cmd : candidates) {
            try {
                ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
                Process process = pb.start();
                
                boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    continue;
                }
                
                if (process.exitValue() == 0) {
                    String version = readVersionOutput(process);
                    String path = Files.exists(Paths.get(cmd)) ? cmd : findExecutablePath(cmd);
                    
                    availableCompilers.put(ProgrammingLanguage.CPP, 
                        new CompilerInfo(true, version, path, cmd));
                    
                    log.info("C++ compiler detected: {} at {}", version, path);
                    break;
                }
            } catch (Exception e) {
                log.debug("Failed to detect C++ compiler with command: {}", cmd, e);
            }
        }
        
        if (!availableCompilers.containsKey(ProgrammingLanguage.CPP)) {
            availableCompilers.put(ProgrammingLanguage.CPP, 
                new CompilerInfo(false, null, null, null));
            log.warn("C++ compiler not found. Please install G++ or Clang++.");
        }
    }
    
    /**
     * Read version output from process
     */
    private String readVersionOutput(Process process) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                return line.trim();
            }
        } catch (Exception e) {
            log.debug("Error reading stdout for version", e);
        }
        
        // Try stderr for some compilers
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line = reader.readLine();
            if (line != null && !line.trim().isEmpty()) {
                return line.trim();
            }
        } catch (Exception e) {
            log.debug("Error reading stderr for version", e);
        }
        
        return "Version unknown";
    }
    
    /**
     * Find executable path using 'which' or 'where' command
     */
    private String findExecutablePath(String executable) {
        String os = System.getProperty("os.name").toLowerCase();
        String command = os.contains("win") ? "where" : "which";
        
        try {
            ProcessBuilder pb = new ProcessBuilder(command, executable);
            Process process = pb.start();
            
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return executable;
            }
            
            if (process.exitValue() == 0) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line = reader.readLine();
                    if (line != null && !line.trim().isEmpty()) {
                        return line.trim();
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Failed to find path for executable: {}", executable, e);
        }
        
        return executable;
    }
    
    /**
     * Get installation recommendations for a language
     */
    private List<String> getInstallationRecommendations(ProgrammingLanguage language) {
        String os = System.getProperty("os.name").toLowerCase();
        
        return switch (language) {
            case JAVA -> os.contains("win") ? 
                Arrays.asList("Install Oracle JDK or OpenJDK", "Add JAVA_HOME to PATH") :
                Arrays.asList("sudo apt install default-jdk", "brew install openjdk");
                
            case PYTHON -> os.contains("win") ? 
                Arrays.asList("Download from python.org", "Install from Microsoft Store") :
                Arrays.asList("sudo apt install python3", "brew install python3");
                
            case C, CPP -> os.contains("win") ? 
                Arrays.asList("Install MSYS2", "pacman -S mingw-w64-ucrt-x86_64-gcc") :
                Arrays.asList("sudo apt install build-essential", "brew install gcc");
                
            case JAVASCRIPT -> 
                Arrays.asList("Install Node.js from nodejs.org");
                
            default -> Arrays.asList("Check language documentation");
        };
    }
    
    /**
     * Log available compilers
     */
    private void logAvailableCompilers() {
        log.info("=== COMPILER DETECTION RESULTS ===");
        for (ProgrammingLanguage lang : ProgrammingLanguage.values()) {
            CompilerInfo info = availableCompilers.get(lang);
            if (info != null && info.isAvailable()) {
                log.info("{}: ✓ Available - {} ({})", lang.getName(), info.getVersion(), info.getPath());
            } else {
                log.warn("{}: ✗ Not available", lang.getName());
            }
        }
        log.info("================================");
    }
    
    /**
     * Compiler information holder
     */
    public static class CompilerInfo {
        private final boolean available;
        private final String version;
        private final String path;
        private final String command;
        
        public CompilerInfo(boolean available, String version, String path, String command) {
            this.available = available;
            this.version = version;
            this.path = path;
            this.command = command;
        }
        
        public boolean isAvailable() { return available; }
        public String getVersion() { return version; }
        public String getPath() { return path; }
        public String getCommand() { return command; }
    }
}
