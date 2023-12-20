package systems.dmx.signup.usecase;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Wrapper to manage optional services. It deals with the intricacies of the service class not being loadable at
 * runtime as well as OSGi service discovery. The load attempt is never tried again.
 *
 * During instantiation the service class is looked up and if it is not loadable, the service will never be available.
 *
 * When the calling service stops the optional service should be released by calling {@link #release()}.
 *
 * @param <T>
 */
public class OptionalService<T> {

    private ServiceTracker<T, T> tracker = null;

    OptionalService(BundleContext bundleContext, ClassGetter<T> classGetter) {
        Class<T> serviceClass = null;
        try {
            serviceClass = classGetter.get();
        } catch (NoClassDefFoundError e) {
            // Instance will never be available
        }

        if (serviceClass != null) {
            tracker = new ServiceTracker(bundleContext, serviceClass, null);
            tracker.open();
        }
    }

    boolean isPermanentlyAbsent() {
        return tracker == null;
    }

    public void release() {
        if (tracker != null) {
            tracker.close();
        }
    }

    public T get() {
        if (tracker != null) {
            return tracker.getService();
        } else {
            return null;
        }
    }

    interface ClassGetter<T> {
        Class<T> get();
    }

}
