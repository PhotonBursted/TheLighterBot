package st.photonbur.Discord.Bot.lightbotv3.misc.menu;

public enum Controls {
    ONE("\u0031", 1, false),
    TWO("\u0032", 2, false),
    THREE("\u0033", 3, false),
    FOUR("\u0034", 4, false),
    FIVE("\u0035", 5, false),
    SIX("\u0036", 6, false),
    SEVEN("\u0037", 7, false),
    EIGHT("\u0038", 8, false),
    NINE("\u0039", 9, false),
    TEN("\u0040", 10, false),
    
    PREV("\u25C0", -1, true),
    STOP("\u23F9", 0, true),
    ACCEPT("\u2611", 0, true),
    NEXT("\u25B6", 1, true);

    private final String unicode;
    private final int offset;
    private final boolean isRelative;

    Controls(String s, int offset, boolean isRelative) {
        this.unicode = s;
        this.offset = offset;
        this.isRelative = isRelative;
    }

    public int getOffset() {
        return offset;
    }

    public String getUnicode() {
        return unicode;
    }
    
    public boolean isRelative() {
        return isRelative;
    }
}
