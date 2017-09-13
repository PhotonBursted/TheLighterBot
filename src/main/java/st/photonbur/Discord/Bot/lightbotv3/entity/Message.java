package st.photonbur.Discord.Bot.lightbotv3.entity;

public enum Message {
    ;

    Message(String message) {
        this.message = message;
    }

    private final String message;

    public String getMessage() {
        return message;
    }
}
