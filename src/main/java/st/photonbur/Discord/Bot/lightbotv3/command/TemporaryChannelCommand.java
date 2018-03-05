package st.photonbur.Discord.Bot.lightbotv3.command;

import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Category;
import net.dv8tion.jda.core.entities.Guild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import st.photonbur.Discord.Bot.lightbotv3.command.alias.CommandAliasCollectionBuilder;
import st.photonbur.Discord.Bot.lightbotv3.controller.DiscordController;
import st.photonbur.Discord.Bot.lightbotv3.main.LoggerUtils;
import st.photonbur.Discord.Bot.lightbotv3.misc.Utils;

import java.util.*;

public class TemporaryChannelCommand extends Command {
    private static final Logger log = LoggerFactory.getLogger(TemporaryChannelCommand.class);

    public TemporaryChannelCommand() {
        super(new CommandAliasCollectionBuilder()
                .addAliasPart("tempchan", "tc"));
    }

    /**
     * Constructs the temporary channels needed to complete and instantiate a temporary channel set.
     *
     * @param channelName The name to base the new channels off of
     * @param parent      The category to house the new channels under
     */
    private void createTemporaryChannels(String channelName, Category parent) {
        // No parent was found so a new category will have to be created
        if (parent == null) {
            l.getChannelController().createTempCategory(ev.getGuild(), channelName,
                    newCat -> createTemporaryChannels(channelName, newCat));
        } else {
            // Create voice and text channel
            l.getChannelController().createTempVoiceChannel(ev, channelName, parent,
                    vc -> l.getChannelController().createTempTextChannel(ev, channelName, parent,
                            tc -> {
                                // Link the channels together and make sure to delete the voice channel after 10 seconds of inactivity
                                l.getChannelController().getLinkedChannels().putMerging(tc, vc);
                                l.getChannelController().setNewChannelTimeout(vc);
                            }));
        }
    }

    @Override
    protected void execute() {
        // Gather the name of the channel from the remaining input
        String channelName = Utils.drainQueueToString(input);

        // Check if the channel name is available still
        if (ev.getGuild().getVoiceChannelsByName("[T] " + channelName, true).size() != 0 ||
                ev.getGuild().getTextChannelsByName(Utils.ircify("tdc-" + channelName), true).size() != 0) {
            // If no channel is available, send feedback to the user
            if (ev.getGuild().getVoiceChannelsByName("[T] " + channelName, true).size() == 0) {
                handleError("A text channel with the specified name already exists!\n" +
                        "Keep in mind that in order to generate a valid channel name, special characters are deleted.");
            } else {
                handleError("The voice channel name specified was already taken!\n" +
                        "Try again with another name.");
            }
        } else {
            Category parent;
            HashMap<Guild, Category> categories = l.getChannelController().getCategories();

            LoggerUtils.logAndDelete(log, "Creating new temporary channel set\n" +
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

            // Create the temporary channels
            createTemporaryChannels(channelName, parent);

            // Send feedback to the user
            l.getDiscordController().sendMessage(ev,
                    String.format("%s succesfully added temporary channel **%s**.\n" +
                            "This channel will be deleted as soon as every person has left or when nobody has joined within 10 seconds.",
                            ev.getMember().getAsMention(), channelName),
                    DiscordController.AUTOMATIC_REMOVAL_INTERVAL
            );
            log.info("Created group " + channelName + "!");
        }
    }

    @Override
    protected String getDescription() {
        return "Creates a new set of temporary channels which delete upon inactivity.";
    }

    @Override
    protected Permission[] getPermissionsRequired() {
        return new Permission[] {};
    }

    @Override
    protected String getUsage() {
        return "{}tempchan <name>\n" +
                "    <name> specifies the name for the to be generated collection of channels.";
    }
}
