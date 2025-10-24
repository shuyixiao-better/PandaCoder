import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Test finding all Parameters lines
 */
public class TestAllParameters {
    
    // Match all Parameters lines
    private static final Pattern PARAMETERS_PATTERN = Pattern.compile(
        "==>\\s+Parameters:\\s*([\\s\\S]*?)(?=\\n\\d{4}-\\d{2}-\\d{2}.*?<==|$)",
        Pattern.CASE_INSENSITIVE
    );
    
    public static void main(String[] args) {
        try {
            System.out.println("Finding all Parameters in log file...\n");
            
            // Read log file
            String logContent = new String(Files.readAllBytes(Paths.get("日志.txt")), "UTF-8");
            
            // Find all Parameters
            Matcher matcher = PARAMETERS_PATTERN.matcher(logContent);
            int count = 0;
            while (matcher.find()) {
                count++;
                String params = matcher.group(1).trim();
                System.out.println("========== Parameters #" + count + " ==========");
                System.out.println("Length: " + params.length() + " chars");
                System.out.println("Start position: " + matcher.start());
                
                // Show preview
                String preview = params.length() > 200 ? 
                    params.substring(0, 200) + "..." : params;
                System.out.println("Preview:");
                System.out.println(preview);
                System.out.println();
            }
            
            System.out.println("Total Parameters found: " + count);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

