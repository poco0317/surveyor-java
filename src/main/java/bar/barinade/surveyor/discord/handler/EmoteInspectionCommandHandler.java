package bar.barinade.surveyor.discord.handler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.utils.AttachedFile;

@Component
@Scope("prototype")
@Slf4j
public class EmoteInspectionCommandHandler extends CommandHandlerBase {

	private static final String NAME_CMD_EXPORT_EMOTES = "export_emotes";
	
	@Override
	public CommandData[] getCommandsToUpsert() {
		return new CommandData[] {
				Commands.slash(NAME_CMD_EXPORT_EMOTES, "Provides a zip of all emotes organized by username")
					.setGuildOnly(true)
					.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_GUILD_EXPRESSIONS, Permission.MANAGE_SERVER))
		};
	}

	void cmd_export_emotes(SlashCommandInteractionEvent event) {
		
		if (!event.getGuild().getSelfMember().hasPermission(Permission.MANAGE_GUILD_EXPRESSIONS)) {
			event.getHook().editOriginal("I don't have permission to see the emotes.").queue();
			return;
		}
		
		event.getGuild().retrieveEmojis().queue(emotes -> {
			m_logger.info("Successful retrieval of {} emotes ... packaging", emotes.size());
			event.getHook().editOriginal("Successfully retrieved "+emotes.size()+" emotes from the server. Packaging...").queue();
			
			ByteArrayOutputStream zipBytes = new ByteArrayOutputStream();
			ZipOutputStream zip = new ZipOutputStream(zipBytes);
			
			for (RichCustomEmoji emote : emotes) {
				emote.getImage().download(512).exceptionally(error -> {
					m_logger.error("Error downloading emote ID "+emote.getId(), error);
					return null;
				}).whenComplete((result, e) -> {
					String emotename = emote.getName();
					String foldername = emote.getOwner().getId();
					
					m_logger.info("Downloaded emote name {} ({}) by {} ({})", emote.getName(), emote.getId(), emote.getOwner().getEffectiveName(), emote.getOwner().getId());
					ZipEntry entry = new ZipEntry(foldername + "/" + emotename + ".png");
					try {
						zip.putNextEntry(entry);
						byte[] imageBytes = result.readAllBytes();
						zip.write(imageBytes, 0, imageBytes.length);
						zip.closeEntry();
					} catch (Exception exc2) {
						m_logger.error(e.getMessage(), e);
					}
				});
			}
			try {
				zip.close();
			} catch (IOException e) {
				m_logger.error(e.getMessage(), e);
			}
			
			event.getHook()
				.editOriginalAttachments(AttachedFile.fromData(zipBytes.toByteArray(), "emotes.zip"))
				.complete()
				.editMessage("Here's a zip of "+emotes.size()+" emotes")
				.queue();
		}, error -> {
			m_logger.error("Had error while trying to retrieve emotes: "+error.getMessage(), error);
			event.getHook().editOriginal("There was an error while retrieving the emotes: "+error.getClass().getName()+" -- "+error.getMessage()).queue();;
		});
		
	}
	
}
