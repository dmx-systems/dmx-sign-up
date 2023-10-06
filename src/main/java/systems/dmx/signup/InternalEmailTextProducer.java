package systems.dmx.signup;

public class InternalEmailTextProducer implements EmailTextProducer {


    private String fail() throws UnsupportedOperationException {
        throw new UnsupportedOperationException("Implement EmailTextProducer and set via SignupService::setEmailTextProducer");
    }

    @Override
    public boolean isHtml() {
        fail();
        return false;
    }

    @Override
    public String getConfirmationActiveMailSubject() {
        return fail();
    }

    @Override
    public String getConfirmationActiveMailMessage(String username, String key) {
        return fail();
    }

    @Override
    public String getConfirmationProceedMailSubject() {
        return fail();
    }

    @Override
    public String getUserConfirmationProceedMailMessage(String username, String key) {
        return fail();
    }

    @Override
    public String getApiUsageRevokedMailSubject() {
        return fail();
    }

    @Override
    public String getApiUsageRevokedMailText(String username) {
        return fail();
    }

    @Override
    public String getAccountActiveEmailSubject() {
        return fail();
    }

    @Override
    public String getAccountActiveEmailMessage(String username) {
        return fail();
    }

    @Override
    public String getApiUsageRequestedSubject() {
        return fail();
    }

    @Override
    public String getApiUsageRequestedMessage(String username) {
        return fail();
    }

    @Override
    public String getPasswordResetMailSubject() {
        return fail();
    }

    @Override
    public String getPasswordResetMailMessage(String addressee, String key) {
        return fail();
    }

    @Override
    public String getAccountCreationSystemEmailSubject() {
        return fail();
    }

    @Override
    public String getAccountCreationSystemEmailMessage(String username, String mailbox) {
        return fail();
    }
}
