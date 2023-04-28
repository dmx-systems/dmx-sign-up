package systems.dmx.signup;

import com.sun.jersey.api.view.Viewable;
import org.osgi.framework.Bundle;
import org.thymeleaf.context.AbstractContext;
import systems.dmx.core.osgi.PluginActivator;

public class FakeThymeleafPlugin extends PluginActivator {

    protected void addTemplateResourceBundle(Bundle templateBundleResource) {
    }

    protected void removeTemplateResourceBundle(Bundle templateBundleResource) {
    }

    protected void initTemplateEngine() {
    }

    protected void viewData(String name, Object value) {

    }

    protected Viewable view(String templateName) {
        return null;
    }

    protected AbstractContext context() {
        return null;
    }


}