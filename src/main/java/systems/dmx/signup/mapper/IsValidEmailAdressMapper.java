package systems.dmx.signup.mapper;

import javax.inject.Inject;
import java.util.regex.Pattern;

public class IsValidEmailAdressMapper {

    @Inject
    public IsValidEmailAdressMapper() {}

    private String emailPattern = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    private Pattern pattern = Pattern.compile(emailPattern);

    public boolean map(String maybeEmail) {
        return pattern.matcher(maybeEmail).matches();
        /* needs some OSGi magic to work
        try {
            InternetAddress emailAddress = new InternetAddress(maybeEmail);
            emailAddress.validate();
            return true;
        } catch (AddressException ex) {
            return false;
        }*/
    }

}
