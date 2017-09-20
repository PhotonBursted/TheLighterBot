package st.photonbur.Discord.Bot.lightbotv3.misc.menu;

public enum Controls {
    ONE("\u0031", 1),
    TWO("\u0032", 2),
    THREE("\u0033", 3),
    FOUR("\u0034", 4),
    FIVE("\u0035", 5),
    SIX("\u0036", 6),
    SEVEN("\u0037", 7),
    EIGHT("\u0038", 8),
    NINE("\u0039", 9),
    TEN("\u0040", 10),
    PREV("\u25C0", -1),
    STOP("\u23F9", 0),
    ACCEPT("\u2611", 0),
    NEXT("\u25B6", 1);

    private final String unicode;
    private final int offset;

    Controls(String s, int offset) {
        this.unicode = s;
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }

    public String getUnicode() {
        return unicode;
    }
}
