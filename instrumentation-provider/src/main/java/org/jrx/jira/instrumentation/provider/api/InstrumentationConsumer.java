package org.jrx.jira.instrumentation.provider.api;

import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.annotation.AnnotationList;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.felix.framework.BundleWiringImpl;
import org.apache.felix.framework.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * Created by dzharikhin (https://github.com/dzharikhin) on 27.10.2016.
 * * Published via OSGi - implement this interface and publish as a service to apply class transformations
 */
public interface InstrumentationConsumer {

    void applyInstrumentation(Instrumentation instrumentation);

    /**
     * Matcher against annotation value
     * @param <T> type of value to match
     * @param <V> type of annotation value
     * //TODO not tested!!!
     */
    class VersionElementMatcher<T extends TypeDescription, V> implements ElementMatcher<T> {

        private final AnnotationDescription annotation;
        private final String propertyName;
        private final Class<V> aClass;
        private final V targetValue;

        public VersionElementMatcher(
            AnnotationDescription annotation,
            String propertyName,
            Class<V> aClass,
            V targetValue
        ) {
            this.annotation = annotation;
            this.propertyName = propertyName;
            this.aClass = aClass;
            this.targetValue = targetValue;
        }

        @Override
        public boolean matches(T target) {
            final AnnotationList declaredAnnotations = target.getDeclaredAnnotations();
            if (declaredAnnotations != null) {
                final AnnotationDescription annotationDescription = declaredAnnotations.ofType(annotation.getAnnotationType());
                if (annotationDescription != null) {
                    return annotationDescription.getValue(
                        annotation
                            .getAnnotationType()
                            .getDeclaredMethods()
                            .filter(named(propertyName)).getOnly()
                    ).resolve(aClass).equals(targetValue);
                }
            }
            return false;
        }
    }

    /**
     * Class loader to proxy classloading parent chain to bundleClass loader
     * May be useful to make visible classes from bundle to webappCL
     * @param <T> BundleClassLoaderType
     * //TODO NOT TESTED properly!!!
     */
    class BundleProxyClassLoader<T extends BundleWiringImpl.BundleClassLoader> extends ClassLoader {

        private static final Logger log = LoggerFactory.getLogger(BundleProxyClassLoader.class);

        private final Set<T> proxies;
        private final Method loadClass;
        private final Method shouldDelegate;

        public BundleProxyClassLoader(ClassLoader parent, T proxy) {
            super(parent);
            this.loadClass = getLoadClassMethod();
            this.shouldDelegate = getShouldDelegateMethod();
            this.proxies = new HashSet<>();
            proxies.add(proxy);
        }

        private Method getLoadClassMethod() throws IllegalStateException {
            try {
                Method loadClass = ClassLoader.class.getDeclaredMethod("loadClass", String.class, boolean.class);
                loadClass.setAccessible(true);
                return loadClass;
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Failed to get loadClass method", e);
            }
        }

        private Method getShouldDelegateMethod() throws IllegalStateException {
            try {
                Method shouldDelegate = BundleWiringImpl.class.getDeclaredMethod("shouldBootDelegate", String.class);
                shouldDelegate.setAccessible(true);
                return shouldDelegate;
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Failed to get shouldDelegate method", e);
            }
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            synchronized (getClassLoadingLock(name)) {
                log.trace("Trying to find already loaded class {}", name);
                Class<?> c = findLoadedClass(name);
                if (c == null) {
                    log.trace("This is new class. Trying to load {} with OSGi", name);
                    c = tryToLoadWithProxies(name, resolve);
                    if (c == null) {
                        log.trace("Failed to load with OSGi. Trying to load {} with parent CL", name);
                        c = super.loadClass(name, resolve);
                    }
                }
                if (c == null) {
                    throw new ClassNotFoundException(name);
                }
                return c;
            }
        }

        private Class<?> tryToLoadWithProxies(String name, boolean resolve) {
            for (T proxy : proxies) {
                try {
                    final String pkgName = Util.getClassPackage(name);
                    //avoid cycle
                    if(!isShouldDelegatePackageLoad(proxy, pkgName)) {
                        log.trace("The load of class {} should not be delegated to OSGI parent, so let's try to load with bundles", name);
                        return (Class<?>) this.loadClass.invoke(proxy, name, resolve);
                    }
                } catch (ReflectiveOperationException e) {
                    log.trace("Class {} is not found with {}", name, proxy);
                }
            }
            return null;
        }

        private boolean isShouldDelegatePackageLoad(T proxy, String pkgName) throws IllegalAccessException, InvocationTargetException {
            return (boolean)this.shouldDelegate.invoke(
                    FieldUtils.readDeclaredField(proxy, "m_wiring", true),
                    pkgName
            );
        }
    }

}
