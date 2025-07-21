import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DateTest {
    public static void main(String[] args) {
        // Test with your exact format
        String emailContent = "From: test@example.com\nTo: support@example.com\nSent: Friday, July 11, 2025 1:14 PM\nSubject: Test Issue\n\nThis is a test email.";
        
        System.out.println("Email content:");
        System.out.println(emailContent);
        System.out.println("\n" + "=".repeat(50));
        
        // Try to extract the sent date
        Pattern sentPattern = Pattern.compile("(?i)sent:\\s*(.+?)(?=\\n|$)", Pattern.MULTILINE);
        Matcher sentMatcher = sentPattern.matcher(emailContent);
        
        if (sentMatcher.find()) {
            String dateString = sentMatcher.group(1).trim();
            System.out.println("Found Sent date: '" + dateString + "'");
            
            // Try to parse it
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy h:mm a", Locale.ENGLISH);
            try {
                LocalDate parsedDate = LocalDate.parse(dateString, formatter);
                System.out.println("Successfully parsed date: " + parsedDate);
            } catch (Exception e) {
                System.out.println("Failed to parse date: " + e.getMessage());
            }
        } else {
            System.out.println("No Sent date found!");
        }
    }
}