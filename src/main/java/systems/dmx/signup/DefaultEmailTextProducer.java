package systems.dmx.signup;

import java.util.ResourceBundle;

class DefaultEmailTextProducer implements EmailTextProducer {

    private String webAppTitle = "Sign-Up Plugin";

    private ResourceBundle rb;

    DefaultEmailTextProducer() {
    }

    private String getLinkHref(String href) {
        return "<a href=\"" + href + "\">" +
                rb.getString("mail_confirmation_link_label") + "</a>";
    }

    @Override
    public String getConfirmationActiveMailSubject() {
        return rb.getString("mail_confirmation_subject") + " - " + webAppTitle;
    }

    @Override
    public String getConfirmationActiveMailMessage(String username, String href) {
        String linkHref = getLinkHref(href);
        return rb.getString("mail_hello") + " " + username + ",<br><br>" +
                rb.getString("mail_confirmation_active_body") + "<br><br>" + linkHref + "<br><br>" +
                rb.getString("mail_ciao");
    }

    @Override
    public String getConfirmationProceedMailSubject() {
        return rb.getString("mail_confirmation_subject") + " - " + webAppTitle;
    }

    @Override
    public String getUserConfirmationProceedMailMessage(String username, String href) {
        String linkHref = getLinkHref(href);

        return rb.getString("mail_hello") + " " + username + ",<br><br>" +
                rb.getString("mail_confirmation_proceed_1") + "<br>" + linkHref + "<br><br>" +
                rb.getString("mail_confirmation_proceed_2") + "<br><br>" + rb.getString("mail_ciao");
    }

    @Override
    public String getApiUsageRevokedMailSubject() {
        return "API Usage Revoked";
    }

    @Override
    public String getApiUsageRevokedMailText(String username) {
        return "<br>Hi admin,<br><br>" +
                username + " just revoked his/her acceptance to your Terms of " +
                "Service for API-Usage.<br><br>Just wanted to let you know.<br>Cheers!";
    }

    @Override
    public String getAccountActiveEmailSubject() {
        return "Your account on " + webAppTitle + " is now active";
    }

    @Override
    public String getAccountActiveEmailMessage(String username, String hostUrl) {
        return rb.getString("mail_hello") +
                " " + username + ",<br><br>your account on <a href=\"" + hostUrl + "\">" +
                webAppTitle + "</a> is now active.<br><br>" + rb.getString("mail_ciao");
    }

    @Override
    public String getApiUsageRequestedSubject() {
        return "API Usage Requested";
    }

    @Override
    public String getApiUsageRequestedMessage(String username) {
        return "<br>Hi admin,<br><br>" +
                username + " accepted the Terms of Service for API Usage." +
                "<br><br>Just wanted to let you know.<br>Cheers!";
    }

    @Override
    public String getPasswordResetMailSubject() {
        return rb.getString("mail_pw_reset_title") + " " + webAppTitle;
    }

    @Override
    public String getPasswordResetMailMessage(String href, String addressee) {
        return rb.getString("mail_hello") + "!<br><br>" + rb.getString("mail_pw_reset_body") + "<br>" +
                "<a href=\"" + href + "\">" + href + "</a><br><br>" + rb.getString("mail_cheers") + "<br>" +
                rb.getString("mail_signature");
    }

    @Override
    public String getAccountCreationSystemEmailSubject() {
        return "Account registration on " + webAppTitle;
    }

    @Override
    public String getAccountCreationSystemEmailMessage(String username, String mailbox) {
        return "<br>A user has registered.<br><br>Username: " + username + "<br>Email: " + mailbox;
    }
}
