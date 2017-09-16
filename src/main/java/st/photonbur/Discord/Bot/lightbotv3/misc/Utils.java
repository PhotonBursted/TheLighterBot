package st.photonbur.Discord.Bot.lightbotv3.misc;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.exceptions.RateLimitedException;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Stream;

public class Utils {
    public static String getFetchedChannelMapAsString(Map<VoiceChannel, TextChannel> map) {
        StringBuilder msg = new StringBuilder();

        map.forEach((vc, tc) -> {
            if (vc != null && tc != null) {
                msg.append(String.format("%s <-> %s (%s)",
                        vc.getName(), tc.getName(), vc.getGuild().getName()));
            }
        });

        return msg.toString();
    }

    public static <T extends ISnowflake> PermissionOverride getPO(Channel c, T t) throws RateLimitedException {
        if (t instanceof Role) {
            return getPO(c, (Role) t);
        } else if (t instanceof User) {
            return getPO(c, c.getGuild().getMember((User) t));
        } else {
            return null;
        }
    }

    public static PermissionOverride getPO(Channel c, Member m) throws RateLimitedException {
        PermissionOverride po;
        if (c.getPermissionOverride(m) != null) {
            po = c.getPermissionOverride(m);
        } else {
            po = c.createPermissionOverride(m).reason("A permission override was required for a bot related action").complete();
        }
        return po;
    }

    public static PermissionOverride getPO(Channel c, Role r) throws RateLimitedException {
        PermissionOverride po;
        if (c.getPermissionOverride(r) != null) {
            po = c.getPermissionOverride(r);
        } else {
            po = c.createPermissionOverride(r).reason("A permission override was required for a bot related action").complete();
        }
        return po;
    }

    public static String ircify(String s) {
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD).replace(" ", "-").replaceAll("[^a-z0-9-]", "").replaceAll("[^\\p{ASCII}]", "");
    }

    public static boolean isInteger(String s) {
        if (s.isEmpty()) {
            return false;
        }
        for (int i = 0; i < s.length(); i++) {
            if (i == 0 && s.charAt(i) == '-') {
                if (s.length() == 1) {
                    return false;
                } else {
                    continue;
                }
            }
            if (Character.digit(s.charAt(i), 10) < 0) {
                return false;
            }
        }
        return true;
    }

    public static void removePermissionsFrom(PermissionOverride po, String reason, Permission... permsToRemove) {
        if (Stream.concat(po.getAllowed().stream(), po.getDenied().stream()).filter(p -> Arrays.stream(permsToRemove).anyMatch(op -> op.equals(p))).count() == permsToRemove.length) {
            po.delete().reason(reason).queue();
        } else {
            po.getManagerUpdatable().clear(permsToRemove).update().reason(reason).queue();
        }
    }

    public static String userAsString(User user) {
        return String.format("%s#%s",
                user.getName(), user.getDiscriminator());
    }

    public static String drainQueueToString(LinkedBlockingQueue<String> input) {
        ArrayList<String> inputParts = new ArrayList<>();
        input.drainTo(inputParts);
        return String.join(" ", inputParts.toArray(new String[inputParts.size()]));
    }
}
