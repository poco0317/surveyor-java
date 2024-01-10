package bar.barinade.surveyor.discord.handler;

import java.io.File;
import java.io.FileOutputStream;
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
			
			try {
				File tmpfile = File.createTempFile("emotes", event.getUser().getId()+".zip");
				ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(tmpfile));
				
				StringBuilder emoteList = new StringBuilder();
				emoteList.append("emotename,emoteid,username,userid\n");
				
				for (RichCustomEmoji emote : emotes) {
					emote.getImage().download(512).exceptionally(error -> {
						m_logger.error("Error downloading emote ID "+emote.getId(), error);
						return null;
					}).whenComplete((result, e) -> {
						String emotename = emote.getName();
						String emoteid = emote.getId();
						String userid = emote.getOwner().getId();
						String username = emote.getOwner().getEffectiveName();
						emoteList.append(emotename + "," + emoteid + "," + username + "," + userid + "\n");
						
						m_logger.info("Downloaded emote name {} ({}) by {} ({})",
								emotename, emoteid,
								username, userid);
						ZipEntry entry = new ZipEntry(userid + "/" + emotename + ".png");
						try {
							zip.putNextEntry(entry);
							byte[] imageBytes = result.readAllBytes();
							zip.write(imageBytes, 0, imageBytes.length);
							zip.closeEntry();
						} catch (Throwable exc2) {
							m_logger.error(exc2.getMessage(), exc2);
						}
					}).get();
				}
				
				// print the list of emote info to a file
				ZipEntry entry = new ZipEntry("manifest.csv");
				zip.putNextEntry(entry);
				byte[] strBytes = emoteList.toString().getBytes();
				zip.write(strBytes, 0, strBytes.length);
				zip.closeEntry();
				
				zip.close();
				event.getHook()
					.editOriginalAttachments(AttachedFile.fromData(tmpfile, "emotes.zip"))
						.queue(msg -> msg.editMessage("Here's a zip of "+emotes.size()+" emotes")
							.queue(msg2 -> tmpfile.delete()));
			} catch (Exception fileOpenFailure) {
				m_logger.error(fileOpenFailure.getMessage(), fileOpenFailure);
				event.getHook().editOriginal("There was an error in the zip creation process: "+fileOpenFailure.getClass().getName() + " - " + fileOpenFailure.getMessage()).queue();
			}
			
		}, error -> {
			m_logger.error("Had error while trying to retrieve emotes: "+error.getMessage(), error);
			event.getHook().editOriginal("There was an error while retrieving the emotes: "+error.getClass().getName()+" -- "+error.getMessage()).queue();;
		});
		
	}
	
}
