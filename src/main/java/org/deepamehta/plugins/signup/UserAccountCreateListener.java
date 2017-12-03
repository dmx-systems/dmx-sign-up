package org.deepamehta.plugins.signup;

import de.deepamehta.core.Topic;
import de.deepamehta.core.service.EventListener;

/**
 *
 * @author malte
 */
public interface UserAccountCreateListener extends EventListener {
    
    void userAccountCreated(Topic usernameTopic);

}
