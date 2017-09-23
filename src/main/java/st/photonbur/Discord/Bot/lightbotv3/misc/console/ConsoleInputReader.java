package st.photonbur.Discord.Bot.lightbotv3.misc.console;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.stream.Collectors;

public class ConsoleInputReader extends InputStream {
    /**
     * The listeners responsible for carrying out user inputs when they occur
     */
    private final HashSet<ConsoleInputListener> listeners;
    /**
     * Stores the received characters stdIn a list of integers
     */
    private final ArrayList<Integer> buffer = new ArrayList<>();
    /**
     * Stores the OS-specific line separator.
     */
    private static final String lineSep = System.getProperty("line.separator");

    /**
     * The stream to read from
     */
    private InputStream stdIn;
    /**
     * The stream to output the console input to
     */
    private PrintStream stdOut;

    public ConsoleInputReader(InputStream in, PrintStream out) {
        stdIn = in;
        stdOut = out;

        listeners = new HashSet<>();
    }

    /**
     * Adds a byte into buffer and checks if the end of the line has occured.
     *
     * @param b The byte to put into buffer
     */
    private void addByte(int b) {
        // Add the byte into buffer
        buffer.add(b);

        // Check if the end of the buffer is a new line
        if (bufferEndsWith(lineSep)) {
            // Remove the newline characters
            for (int i = 0; i < lineSep.length(); i++) {
                buffer.remove(buffer.size() - 1);
            }

            // Let the user handle the input
            listeners.forEach(listener -> listener.onConsoleInput(new ConsoleInputEvent(toString())));

            // Clear the buffer to accept new input
            buffer.clear();
        }
    }

    /**
     * Adds a listener to this instance of the reader.
     * Every listener will get called when a new input is detected.
     * @param listener The instance to add to be informed about new inputs
     */
    public void addListener(ConsoleInputListener listener) {
        listeners.add(listener);
    }

    /**
     * Checks if the buffer ends with a certain set of characters.
     * @param mold The string to check against
     */
    private boolean bufferEndsWith(String mold) {
        boolean matches = buffer.size() >= mold.length();

        if (matches) {
            for (int i = 0; i < mold.length(); i++) {
                matches = matches && (int) mold.charAt(i) == buffer.get(buffer.size() - mold.length() + i);
            }
        }

        return matches;
    }

    @Override
    public int read() throws IOException {
        int b = stdIn.read();

        // Write the read byte to an external stream and add it into the buffer
        stdOut.write(b);
        addByte(b);

        return b;
    }

    /**
     * @return The buffer in string form
     */
    @Override
    public String toString() {
        return String.join("", buffer.stream()
                .map(code -> Character.toString((char) ((int) code)))
                .collect(Collectors.toList()).toArray(new String[buffer.size()]));
    }
}
