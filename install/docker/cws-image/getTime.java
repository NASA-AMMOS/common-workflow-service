import java.util.Calendar;
import org.joda.time.DateTime;
import java.util.TimeZone;
import java.sql.Timestamp;

public class getTime {

    public static void main(String[] args) {

        Timestamp thisMachineTime = new Timestamp(DateTime.now().getMillis());
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        TimeZone tz = Calendar.getInstance().getTimeZone();
        System.out.println("Current Time: " + sdf.format(thisMachineTime)); // Format the date using the specified pattern.
        System.out.println("Time Zone: " + tz.getDisplayName());
    }
}
