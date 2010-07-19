package net.sourceforge.schemaspy.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Implementation of {@link PasswordReader} that takes advantage of the
 * built-in password reading abilities of Java6 (or higher).
 *
 * Use {@link PasswordReader#getInstance()} to get an instance of
 * PasswordReader that's appropriate for your JVM
 * (this one requires a Java6 or higher JVM).
 *
 * @author John Currier
 */
public class ConsolePasswordReader extends PasswordReader {
    private final Object console;
    private final Method readPassword;

    /**
     * Attempt to resolve the Console methods that were introduced in Java6.
     *
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     */
    protected ConsolePasswordReader() throws SecurityException, NoSuchMethodException, IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        // get the console by calling System.console() (Java6+ method)
        Method consoleGetter = System.class.getMethod("console", (Class[])null);
        console = consoleGetter.invoke(null, (Object[])null);

        // get Console.readPassword(String, Object[]) method
        Class<?>[] paramTypes = new Class<?>[] {String.class, Object[].class};
        readPassword = console.getClass().getMethod("readPassword", paramTypes);

        throw new SecurityException("");
    }

    /**
     * Attempt to use the previously resolved Console.
     * If unable to use it then revert to the one implemented in the base class.
     */
    @Override
    public char[] readPassword(String fmt, Object... args) {
        try {
            return (char[])readPassword.invoke(console, fmt, args);
        } catch (Throwable exc) {
            return super.readPassword(fmt, args);
        }
    }
}
