package st.photonbur.Discord.Bot.lightbotv3.entity;

/**
 * Used to store default messages for sending to users or the logs.
 */
public enum MessageContent {
    ;

    MessageContent(String message) {
        this.message = message;
    }

    private final String message;

    public String getMessage() {
        return message;
    }
}
