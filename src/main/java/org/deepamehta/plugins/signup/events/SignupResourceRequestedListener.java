package org.deepamehta.plugins.signup.events;

import de.deepamehta.core.service.EventListener;
import org.thymeleaf.context.AbstractContext;

/**
 *
 * @author malte
 */
public interface SignupResourceRequestedListener extends EventListener {

    void signupResourceRequested(AbstractContext context, String templateName);

}
