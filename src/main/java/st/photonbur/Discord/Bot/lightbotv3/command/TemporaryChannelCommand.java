package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import st.photonbur.Discord.Bot.lightbotv3.controller.ChannelController;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
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
            HashMap<Guild, Category> categories = ChannelController.getCategories();

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
                parent = ChannelController.createTempCategory(ev.getGuild(), channelName);
            }

            // Create the temporary channels
            VoiceChannel vc = ChannelController.createTempVoiceChannel(ev, channelName, parent);
            TextChannel tc = ChannelController.createTempTextChannel(ev, channelName, parent);

            // Link the channels together and make sure to delete the voice channel after 10 seconds of inactivity
            ChannelController.getLinkedChannels().put(vc, tc);
            ChannelController.setNewChannelTimeout(vc);

            // Send feedback to the user
            DiscordController.sendMessage(ev, ev.getMember().getAsMention() + " succesfully added temporary channel **" + channelName + "**.\n" +
                    "This channel will be deleted as soon as every person has left or when nobody has joined within 10 seconds.", 120
            );
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
    Set<String> getAliases() {
        return new HashSet<>(Collections.singletonList("tempchan"));
    }

    @Override
    String getDescription() {
        return null;
    }

    @Override
    Set<Permission> getPermissionsRequired() {
        return new HashSet<>();
    }

    @Override
    String getUsage() {
        return null;
    }
}
