package systems.dmx.signup;

import java.util.ResourceBundle;

public interface EmailTextProducer {

    String getSubject();
}

class EmailTextProducerImpl implements EmailTextProducer {

    private ResourceBundle rb;

    // TODO
    private String webAppTitle = null;

    EmailTextProducerImpl(ResourceBundle rb) {
        this.rb = rb;
    }

    @Override
    public String getSubject() {
        return rb.getString("mail_confirmation_subject") + " - " + webAppTitle;
    }
}
