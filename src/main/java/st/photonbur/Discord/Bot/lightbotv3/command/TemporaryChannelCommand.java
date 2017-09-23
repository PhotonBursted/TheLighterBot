package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.main.Launcher;
import st.photonbur.Discord.Bot.lightbotv3.main.Logger;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;

import java.util.*;

public class TemporaryChannelCommand extends Command {
    @Override
    void execute() throws RateLimitedException {
        // Gather the name of the channel from the remaining input
        String channelName = Utils.drainQueueToString(input);

        // Check if the channel name is available still
        if (ev.getGuild().getVoiceChannelsByName("[T] " + channelName, true).size() == 0 &&
                ev.getGuild().getTextChannelsByName(Utils.ircify("tdc-" + channelName), true).size() == 0) {
            Category parent;
            HashMap<Guild, Category> categories = l.getChannelController().getCategories();

            Logger.logAndDelete("Creating new temporary channel set\n" +
                    " - Name: " + channelName);

            // Determine if a category should be created or not.
            // If a default category has been specified it should point towards that.
            if (categories.containsKey(ev.getGuild()) && categories.get(ev.getGuild()) != null &&
                        ev.getGuild().getCategories().stream().anyMatch(c -> c.getId().equals(categories.get(ev.getGuild()).getId()))) {
                parent = categories.get(ev.getGuild());
            } else {
                // A category was specified but not found.
                // The category is removed from registry and a new parent category will be created.
                categories.remove(ev.getGuild());
                parent = null;
            }

            // No parent was found so a new category will be created
            if (parent == null) {
                parent = l.getChannelController().createTempCategory(ev.getGuild(), channelName);
            }

            // Create the temporary channels
            VoiceChannel vc = l.getChannelController().createTempVoiceChannel(ev, channelName, parent);
            TextChannel tc = l.getChannelController().createTempTextChannel(ev, channelName, parent);

            // Link the channels together and make sure to delete the voice channel after 10 seconds of inactivity
            l.getChannelController().getLinkedChannels().put(vc, tc);
            l.getChannelController().setNewChannelTimeout(vc);

            // Send feedback to the user
            l.getDiscordController().sendMessage(ev,
                    String.format("%s succesfully added temporary channel **%s**.\n" +
                            "This channel will be deleted as soon as every person has left or when nobody has joined within 10 seconds.",
                            ev.getMember().getAsMention(), channelName),
                    DiscordController.AUTOMATIC_REMOVAL_INTERVAL
            );
            Logger.log("Created group " + vc.getName() + "!");
            l.getFileController().saveGuild(ev.getGuild());
        } else {
            // If no channel is available, send feedback to the user
            if (ev.getGuild().getVoiceChannelsByName("[T] " + channelName, true).size() == 0) {
                handleError("A text channel with the specified name already exists!\n" +
                        "Keep in mind that in order to generate a valid channel name, special characters are deleted");
            } else {
                handleError("The voice channel name specified was already taken!\n" +
                        "Try again with another name.");
            }
        }
    }

    @Override
    String[] getAliases() {
        return new String[] {"tempchan", "tc"};
    }

    @Override
    String getDescription() {
        return "Creates a new set of temporary channels which delete upon inactivity.";
    }

    @Override
    Permission[] getPermissionsRequired() {
        return new Permission[] {};
    }

    @Override
    String getUsage() {
        return "{}tempchan <name>\n" +
                "    <name> specifies the name for the to be generated collection of channels.";
    }
}
