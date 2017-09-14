package st.photonbur.Discord.Bot.lightbotv3.entity;

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
