package systems.dmx.signup.usecase;

import systems.dmx.sendmail.SendmailService;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class SendMailUseCase {

    private final SendmailService sendmailService;

    @Inject
    public SendMailUseCase(SendmailService sendmailService) {
        this.sendmailService = sendmailService;
    }

    public void invoke(String sender, String senderName, String subject, String message, String recipientValues, boolean isHtml) {
        String textMessage = isHtml ? null : message;
        String htmlMessage = isHtml ? message : null;
        sendmailService.doEmailRecipientAs(sender, senderName, subject, textMessage, htmlMessage, recipientValues);
    }
}
