import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Test large JSON parameter extraction
 */
public class TestLargeJson {
    
    // Updated pattern - match until next timestamp line with <==
    private static final Pattern PARAMETERS_PATTERN = Pattern.compile(
        "==>\\s+Parameters:\\s*([\\s\\S]*?)(?=\\n\\d{4}-\\d{2}-\\d{2}.*?<==|$)",
        Pattern.CASE_INSENSITIVE
    );
    
    public static void main(String[] args) {
        try {
            System.out.println("========================================");
            System.out.println("Testing Large JSON Parameter Extraction");
            System.out.println("========================================\n");
            
            // Read log file
            String logContent = new String(Files.readAllBytes(Paths.get("日志.txt")), "UTF-8");
            System.out.println("Log file size: " + (logContent.length() / 1024) + " KB\n");
            
            // Test parameter extraction
            Matcher matcher = PARAMETERS_PATTERN.matcher(logContent);
            if (matcher.find()) {
                String params = matcher.group(1).trim();
                System.out.println("✅ Parameters extracted successfully!");
                System.out.println("Parameters length: " + params.length() + " chars\n");
                
                // Show first 500 chars
                String preview = params.length() > 500 ? 
                    params.substring(0, 500) + "..." : params;
                System.out.println("Parameters preview (first 500 chars):");
                System.out.println(preview);
                System.out.println();
                
                // Verification
                if (params.length() > 1000) {
                    System.out.println("✅ Parameters contain large JSON (length > 1000)");
                } else {
                    System.out.println("❌ Parameters too short, may not be complete");
                }
                
                // Check expected content
                if (params.contains("根据提供的") || params.contains("prompt")) {
                    System.out.println("✅ Parameters contain expected JSON content");
                } else {
                    System.out.println("⚠️ Parameters may be incomplete");
                }
                
                // Show last 200 chars
                if (params.length() > 200) {
                    String endPreview = params.substring(params.length() - 200);
                    System.out.println("\nParameters end (last 200 chars):");
                    System.out.println(endPreview);
                }
                
            } else {
                System.out.println("❌ Parameters not found");
            }
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
