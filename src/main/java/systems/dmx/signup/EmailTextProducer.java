package systems.dmx.signup;

import java.util.ResourceBundle;

public interface EmailTextProducer {

    String getConfirmationActiveMailSubject();

    String getConfirmationActiveMailMessage(String username, String href);

    String getConfirmationProceedMailSubject();

    String getUserConfirmationProceedMailMessage(String username, String href);

    String getApiUsageRevokedMailSubject();

    String getApiUsageRevokedMailText(String username);

    String getAccountActiveEmailSubject();

    String getAccountActiveEmailMessage(String username, String hostUrl);

    String getApiUsageRequestedSubject();

    String getApiUsageRequestedMessage(String username);

    String getPasswordResetMailSubject();

    String getPasswordResetMailMessage(String href, String addressee);

    String getAccountCreationSystemEmailSubject();

    String getAccountCreationSystemEmailMessage(String username, String mailbox);
}
