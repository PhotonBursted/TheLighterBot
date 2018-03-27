package st.photonbur.Discord.Bot.lightbotv3.misc;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import st.photonbur.Discord.Bot.lightbotv3.entity.permissible.PermissibleEntity;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils {
    /**
     * Drains all string inputs of a queue and puts it into a single space-separated string.
     * After this, the input queue will be completely empty.
     *
     * @param input The {@link LinkedBlockingQueue} to drain
     * @return The string consisting of all queue elements concatenated
     */
    public static String drainQueueToString(Queue<String> input) {
        // Return the elements concatenated by spaces
        return input.stream().collect(Collectors.joining(" "));
    }

    /**
     * Gets the permission override for the channel, whether it exists or not.
     *
     * @param c        The channel to look in
     * @param entity   The entity to find the override for
     * @param callback The action to carry out once the override has been found
     */
    public static void getPO(Channel c, PermissibleEntity entity, Consumer<PermissionOverride> callback) {
        if (entity.isOfClass(Member.class)) {
            getPO(c, ((Member) entity.get()), callback);
        }
        if (entity.isOfClass(Role.class)) {
            getPO(c, ((Role) entity.get()), callback);
        }
    }

    /**
     * Gets the permission override for the channel, whether it exists or not, and acts upon it.
     *
     * @param c        The channel to look in
     * @param m        The member to look for
     * @param callback The action to carry out once the override has been found
     */
    @SuppressWarnings("Duplicates")
    public static void getPO(Channel c, Member m, Consumer<PermissionOverride> callback) {
        PermissionOverride po = c.getPermissionOverride(m);

        if (po != null) {
            if (callback != null) callback.accept(po);
        } else {
            c.createPermissionOverride(m)
                    .reason("A permission override was required for a bot related action")
                    .queue((cpo) -> {
                        if (callback != null) callback.accept(cpo);
                    });
        }
    }

    /**
     * Gets the permission override for the channel, whether it exists or not.
     *
     * @param c        The channel to look in
     * @param r        The role to look for
     * @param callback The action to carry out once the override has been found
     */
    @SuppressWarnings("Duplicates")
    public static void getPO(Channel c, Role r, Consumer<PermissionOverride> callback) {
        PermissionOverride po = c.getPermissionOverride(r);

        if (po != null) {
            if (callback != null) callback.accept(po);
        } else {
            c.createPermissionOverride(r)
                    .reason("A permission override was required for a bot related action")
                    .queue((cpo) -> {
                        if (callback != null) callback.accept(cpo);
                    });
        }
    }

    /**
     * Checks if the supplied voice channel has a user limit or not
     *
     * @param vc The voice channel to check for their user limit
     * @return True if the voice channel has a user limit applied to it as of now
     */
    public static boolean hasLimit(VoiceChannel vc) {
        return vc.getUserLimit() >= 1 && vc.getUserLimit() <= 99;
    }

    /**
     * Turns a string into one that is IRC safe.<br/>
     * This means all characters should be lower case ASCII characters with spaces replaced by dashes
     *
     * @param s The string to turn into IRC safe
     * @return The IRC safe variant of the input string
     */
    public static String ircify(String s) {
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD).replace(" ", "-").replaceAll("[^a-z0-9-]", "").replaceAll("[^\\p{ASCII}]", "");
    }

    /**
     * Tests if the input string is actually parsable into an integer.
     *
     * @param s The string to test for integeryness
     * @return True if the string is safely interpretable as integer
     */
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

    /**
     * Removes permissions from an override in a safe yet clean way.<br/>
     * This means it won't destroy the entire override if it doesn't need to.
     *
     * @param po            The permission override to affect
     * @param reason        The reason for removing the permission
     * @param permsToRemove The permissions to try and clear
     */
    public static void removePermissionsFrom(PermissionOverride po, String reason, Permission... permsToRemove) {
        // If the total amount of allowed and denied permissions adds up to the permissions to be cleared, destroy the override.
        // Clearing these permissions would just leave an empty override without any use anyway.
        // Otherwise, try to clear all permissions which aren't needed anymore.
        if (Stream.concat(po.getAllowed().stream(), po.getDenied().stream())
                .filter(p -> Arrays.stream(permsToRemove).anyMatch(op -> op.equals(p)))
                .count() == permsToRemove.length &&
                !(po.isRoleOverride() && po.getRole().getGuild().getPublicRole().equals(po.getRole()))) {
            po.delete().reason(reason).queue();
        } else {
            po.getManager()
                    .clear(permsToRemove)
                    .reason(reason)
                    .queue();
        }
    }

    public static void shortenQueueBy(Queue<String> input, int aliasLength) {
        for (int i = 0; i < aliasLength; i++) {
            input.poll();
        }
    }

    /**
     * @param user The user to turn into a string representation
     * @return The string with form {name}#{discriminator} linked to the user
     */
    public static String userAsString(User user) {
        return String.format("%s#%s",
                user.getName(), user.getDiscriminator());
    }
}
