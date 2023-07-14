package systems.dmx.signup.mapper;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IsValidEmailAdressMapper {
    private String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    private Pattern pattern = Pattern.compile(emailPattern);

    public boolean map(String maybeEmail) {
        return pattern.matcher(maybeEmail).matches();
        /*
        try {
            InternetAddress emailAddress = new InternetAddress(maybeEmail);
            emailAddress.validate();
            return true;
        } catch (AddressException ex) {
            return false;
        }*/
    }

}
