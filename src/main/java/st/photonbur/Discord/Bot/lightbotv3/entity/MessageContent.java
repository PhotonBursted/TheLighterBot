package st.photonbur.Discord.Bot.lightbotv3.entity;

/**
 * Used to store default messages for sending to users or the logs.
 */
public enum MessageContent {
    NOT_IN_VOICE_CHANNEL("It is required to be in a voice channel in order to be able to change user limits!"),
    AFK_CHANNEL_ACTION_NOT_PERMITTED("The channel you are in is not allowed to be changed by this command!"),
    PERMISSIONS_REQUIRED_PERMANENT_CHANNEL_SIZE_CHANGE("MANAGE_CHANNEL permissions are required to change the size of permanent voice channels!"),
    INVALID_INPUT("The input was invalid: %s");

    MessageContent(String message) {
        this.message = message;
    }

    private final String message;

    public static String format(MessageContent mc, String... s) {
        return String.format(mc.getMessage(), (Object[]) s);
    }

    public String getMessage() {
        return message;
    }
}
