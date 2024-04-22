package systems.dmx.signup.usecase;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import systems.dmx.sendmail.SendmailService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SendMailUseCaseTest {

    private SendmailService sendmailService = mock();

    private SendMailUseCase subject = new SendMailUseCase(sendmailService);

    @Test
    @DisplayName("invoke() should send text email using Sendmail service when is isHtml is cleared")
    void invoke_should_send_text_email() {
        // given:
        doNothing().when(sendmailService).doEmailRecipientAs(any(), any(), any(), any(), any(), any());

        String givenSender = "sender";
        String givenSenderName = "sender name";
        String givenSubject = "subject";
        String givenMessage = "message";
        String givenRecipients = "foo;baz;bar";
        boolean givenIsHtml = false;

        // when:
        subject.invoke(givenSender, givenSenderName, givenSubject, givenMessage, givenRecipients, givenIsHtml);

        // then:
        verify(sendmailService).doEmailRecipientAs(givenSender, givenSenderName, givenSubject, givenMessage, null, givenRecipients);
    }

    @Test
    @DisplayName("invoke() should send HTML email using Sendmail service when is isHtml is set")
    void invoke_should_send_html_email() {
        // given:
        doNothing().when(sendmailService).doEmailRecipientAs(any(), any(), any(), any(), any(), any());

        String givenSender = "sender";
        String givenSenderName = "sender name";
        String givenSubject = "subject";
        String givenMessage = "message";
        String givenRecipients = "foo;baz;bar";
        boolean givenIsHtml = true;

        // when:
        subject.invoke(givenSender, givenSenderName, givenSubject, givenMessage, givenRecipients, givenIsHtml);

        // then:
        verify(sendmailService).doEmailRecipientAs(givenSender, givenSenderName, givenSubject, null, givenMessage, givenRecipients);
    }

}