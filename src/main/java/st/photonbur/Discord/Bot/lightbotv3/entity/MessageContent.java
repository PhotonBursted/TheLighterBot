package st.photonbur.Discord.Bot.lightbotv3.entity;

/**
 * Used to store default messages for sending to users or the logs.
 */
public enum MessageContent {
    AFK_CHANNEL_ACTION_NOT_PERMITTED("The channel you are in is not allowed to be changed by this command!"),
    BLACKLISTED("You are blacklisted! No interaction with the bot is allowed."),
    INVALID_INPUT("The input was invalid: %s"),
    NOT_IN_VOICE_CHANNEL("It is required to be in a voice channel in order to be able to change user limits!"),
    PERMISSIONS_REQUIRED_GENERAL("You lack %s permissions to do this!"),
    PERMISSIONS_REQUIRED_PERMANENT_CHANNEL_SIZE_CHANGE("MANAGE_CHANNEL permissions are required to change the size of permanent voice channels!");

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
