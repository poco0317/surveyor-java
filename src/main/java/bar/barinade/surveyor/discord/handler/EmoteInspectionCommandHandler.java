package bar.barinade.surveyor.discord.handler;

import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import bar.barinade.surveyor.discord.serverconfig.service.ServerConfigService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.utils.AttachedFile;

@Component
@Scope("prototype")
@Slf4j
public class EmoteInspectionCommandHandler extends CommandHandlerBase {

	private static final String NAME_CMD_EMOTES = "emotes";
	private static final String NAME_CMD_EXPORT = "export";
	private static final String NAME_CMD_AUDITCHANNEL = "auditchannel";
	
	private static final String OPTION_CHANNEL = "channel";
	
	@Autowired
	private ServerConfigService configService;
	
	@Override
	public CommandData[] getCommandsToUpsert() {
		return new CommandData[] {
				Commands.slash(NAME_CMD_EMOTES, "Provides ability to audit or export emotes")
					.setGuildOnly(true)
					.setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MANAGE_GUILD_EXPRESSIONS, Permission.MANAGE_SERVER))
					.addSubcommands(
						new SubcommandData(NAME_CMD_EXPORT, "Export all emotes with info about who added them"),
						new SubcommandData(NAME_CMD_AUDITCHANNEL, "Set the channel to output emote add/update/delete information")
							.addOption(OptionType.CHANNEL, OPTION_CHANNEL, "Text channel to send messages", false)
						)
		};
	}

	void cmd_emotes(SlashCommandInteractionEvent event) {
		
		String subcmd = event.getSubcommandName();
		if (subcmd == null) {
			event.getHook().editOriginal("You must specify a subcommand.").queue();
			return;
		}
		
		switch (subcmd) {
			case NAME_CMD_EXPORT:
				_cmd_export(event);
				break;
			case NAME_CMD_AUDITCHANNEL:
				_cmd_audit(event);
				break;
			default:
				event.getHook().editOriginal("You entered a subcommand that does not exist.").queue();
				break;
		}
		
	}
	
	private void _cmd_audit(SlashCommandInteractionEvent event) {
		Long guildId = event.getGuild().getIdLong();
		if (event.getOption(OPTION_CHANNEL) == null) {
			event.getHook().editOriginal("Disabled emote auditing.").queue();
			configService.setAuditChannel(guildId, null);
			return;
		}
		
		final ChannelType chantype = event.getOption(OPTION_CHANNEL).getChannelType();
		if (!chantype.equals(ChannelType.TEXT)) {
			event.getHook().editOriginal("You must specify a Text Channel. Your channel was of type '"+chantype.toString()+"'").queue();
			return;
		}
		final GuildChannel channel = event.getOption(OPTION_CHANNEL).getAsChannel();
		
		if (!event.getGuild().getSelfMember().hasPermission(channel, Permission.MESSAGE_SEND)) {
			event.getHook().editOriginal("I do not have permission to send messages in '"+channel.getAsMention()+"'").queue();
			return;
		}
		
		configService.setAuditChannel(guildId, channel.getIdLong());
		event.getHook().editOriginal("Emote auditing channel set to '"+channel.getAsMention()+"'").queue();
	}
	
	private void _cmd_export(SlashCommandInteractionEvent event) {
		
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
				emoteList.append("emotename,emoteid,username,userid,created\n");
				
				for (RichCustomEmoji emote : emotes) {
					emote.getImage().download(512).exceptionally(error -> {
						m_logger.error("Error downloading emote ID "+emote.getId(), error);
						return null;
					}).whenComplete((result, e) -> {
						String emotename = emote.getName();
						String emoteid = emote.getId();
						String userid = emote.getOwner().getId();
						String username = emote.getOwner().getEffectiveName();
						String created = emote.getTimeCreated().toString();
						emoteList.append(emotename + "," + emoteid + "," + username + "," + userid + "," + created + "\n");
						
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
