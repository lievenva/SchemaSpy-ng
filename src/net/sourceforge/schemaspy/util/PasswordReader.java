package net.sourceforge.schemaspy.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Arrays;

/**
 * This class prompts the user for a password and attempts to mask input with
 * "*"
 */
public class PasswordReader {
    /**
     * Matches the contract of Java 1.6+'s {@link java.io.Console#readPassword}
     * except that our own IOError is thrown in place of the 1.6-specific IOError.
     * By matching the contract we can use this implementation when
     * running in a 1.5 JVM or the much better implementation that
     * was introduced in 1.6 when running in a JVM that supplies it.
     *
     * @param fmt
     * @param args
     * @return
     */
    public char[] readPassword(String fmt, Object ... args) {
        InputStream in = System.in;
        char[] lineBuffer;
        char[] buf = lineBuffer = new char[128];
        int room = buf.length;
        int offset = 0;
        int c;
        boolean reading = true;

        Masker masker = new Masker(String.format(fmt, args));
        masker.start();

        try {
            while (reading) {
                switch (c = in.read()) {
                    case -1:
                    case '\n':
                        reading = false;
                        break;

                    case '\r':
                        int c2 = in.read();
                        if ((c2 != '\n') && (c2 != -1)) {
                            if (!(in instanceof PushbackInputStream)) {
                                in = new PushbackInputStream(in);
                            }
                            ((PushbackInputStream)in).unread(c2);
                        } else {
                            reading = false;
                        }
                        break;

                    default:
                        if (--room < 0) {
                            buf = new char[offset + 128];
                            room = buf.length - offset - 1;
                            System.arraycopy(lineBuffer, 0, buf, 0, offset);
                            Arrays.fill(lineBuffer, ' ');
                            lineBuffer = buf;
                        }
                        buf[offset++] = (char)c;
                        break;
                }
            }
        } catch (IOException exc) {
            throw new IOError(exc);
        } finally {
            masker.stopMasking();
        }

        if (offset == 0) {
            return null;
        }
        char[] ret = new char[offset];
        System.arraycopy(buf, 0, ret, 0, offset);
        Arrays.fill(buf, ' ');
        return ret;
    }

    private static class Masker extends Thread {
        private volatile boolean mask;
        private final char echochar = '*';

        /**
         *@param prompt The prompt displayed to the user
         */
        public Masker(String prompt) {
            System.out.print(prompt);

            // set our priority to one above the caller's priority
            setPriority(Thread.currentThread().getPriority() + 1);
        }

        /**
         * Mask until asked to stop
         */
        @Override
        public void run() {
            mask = true;
            while (mask) {
                // backspace over what was typed then splat it
                System.out.print("\010" + echochar);
                try {
                    sleep(100);
                } catch (InterruptedException iex) {
                    interrupt();
                    mask = false;
                }
            }
        }

        /**
         * Stop masking the password
         */
        public void stopMasking() {
            mask = false;
        }
    }

    /**
     * Our own implementation of the Java 1.6 IOError class.
     */
    public class IOError extends Error {
        private static final long serialVersionUID = 20100629L;

        public IOError(Throwable cause) {
            super(cause);
        }
    }
}
