package systems.dmx.signup;

public interface EmailTextProducer {

    String getConfirmationActiveMailSubject();

    String getConfirmationActiveMailMessage(String username, String key);

    String getConfirmationProceedMailSubject();

    String getUserConfirmationProceedMailMessage(String username, String key);

    String getApiUsageRevokedMailSubject();

    String getApiUsageRevokedMailText(String username);

    String getAccountActiveEmailSubject();

    String getAccountActiveEmailMessage(String username);

    String getApiUsageRequestedSubject();

    String getApiUsageRequestedMessage(String username);

    String getPasswordResetMailSubject();

    String getPasswordResetMailMessage(String addressee, String key);

    String getAccountCreationSystemEmailSubject();

    String getAccountCreationSystemEmailMessage(String username, String mailbox);
}
