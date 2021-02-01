package eu.fasten.crawler;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class PartImplementation implements InvocationHandler {

    public Object invoke(final Object proxy, final Method method, final Object[] args)
            throws Throwable {
        try {
            final Method localMethod = getClass().getMethod(method.getName(), method.getParameterTypes());
            return localMethod.invoke(this, args);
        } catch (NoSuchMethodException e) {
            throw new UnsupportedOperationException("Method " + method.getName() + "() is not supported");
        } catch (IllegalAccessException e) {
            throw
                    new UnsupportedOperationException("Method " + method.getName() + "() is not supported");
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
