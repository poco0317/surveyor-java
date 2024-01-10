package bar.barinade.surveyor.discord.handler;

import java.awt.Color;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import bar.barinade.surveyor.discord.serverconfig.service.ServerConfigService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.audit.ActionType;
import net.dv8tion.jda.api.audit.AuditLogEntry;
import net.dv8tion.jda.api.audit.AuditLogKey;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.emoji.EmojiAddedEvent;
import net.dv8tion.jda.api.events.emoji.EmojiRemovedEvent;
import net.dv8tion.jda.api.events.emoji.GenericEmojiEvent;
import net.dv8tion.jda.api.events.emoji.update.EmojiUpdateNameEvent;
import net.dv8tion.jda.api.events.emoji.update.EmojiUpdateRolesEvent;
import net.dv8tion.jda.api.events.sticker.GuildStickerAddedEvent;
import net.dv8tion.jda.api.events.sticker.GuildStickerRemovedEvent;
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateAvailableEvent;
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateDescriptionEvent;
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateNameEvent;
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateTagsEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.restaction.pagination.AuditLogPaginationAction;
import net.dv8tion.jda.api.utils.FileUpload;

@Component
@Scope("prototype")
@Slf4j
public class EmoteAuditEventHandler extends ListenerAdapter {
	
	@Autowired
	private ServerConfigService configService;

	private TextChannel getAuditChannel(GenericEmojiEvent event) {
		Guild guild = event.getGuild();
		if (guild == null) return null;
		Long guildId = guild.getIdLong();
		Long channelId = configService.getAuditChannel(guildId);
		if (channelId == null) return null;
		TextChannel textChannel = guild.getTextChannelById(channelId);
		return textChannel;
	}
	
	private EmbedBuilder startEmbed(GenericEmojiEvent event, String header, String message, int color) {
		
		return new EmbedBuilder()
    			// .setTitle("Title")
    			.setDescription(message)
    			.setColor(color)
    			.setTimestamp(Instant.now())
    			.setAuthor(header)
    			// .setFooter("")
    			.setThumbnail("attachment://" + event.getEmoji().getName() + ".png");
	}
	
	private AuditLogEntry getAuditLog(ActionType action, Guild guild) {
		AuditLogPaginationAction auditLogs = guild.retrieveAuditLogs();
		auditLogs.type(action);
		auditLogs.limit(5);
		List<AuditLogEntry> logs = auditLogs.complete();
		if (logs.isEmpty()) return null;
		return logs.get(0);
	}
	
    @Override
    public void onEmojiAdded(EmojiAddedEvent event) {
    	TextChannel auditChannel = getAuditChannel(event);
    	if (auditChannel == null) return;
    	
    	User adder = getAuditLog(ActionType.EMOJI_CREATE, event.getGuild()).getUser();
    	RichCustomEmoji emote = event.getEmoji();
    	MessageEmbed embed = startEmbed(
    			event,
    			"Custom Emote Added",
    			"Added by "+adder.getAsMention(),
    			Color.green.getRGB())
    			.build();
    	InputStream emoteStream;
		try {
			emoteStream = emote.getImage().download(512).get();
		} catch (InterruptedException | ExecutionException e) {
			m_logger.error(e.getMessage(), e);
			auditChannel.sendMessage("Error in onEmojiAdded: Downloading emote '"+emote.getName()+"' failed. '"+adder.getAsMention()+"' added this emote. - "+e.getMessage()).queue();
			return;
		}
    	
    	auditChannel.sendFiles(FileUpload.fromData(emoteStream, event.getEmoji().getName() + ".png")).setEmbeds(embed).queue();
    }
    
    @Override
    public void onEmojiRemoved(EmojiRemovedEvent event) {
    	TextChannel auditChannel = getAuditChannel(event);
    	if (auditChannel == null) return;
    	
    	
    	User remover = getAuditLog(ActionType.EMOJI_DELETE, event.getGuild()).getUser();
    	RichCustomEmoji emote = event.getEmoji();
    	MessageEmbed embed = startEmbed(
    			event,
    			"Custom Emote Removed",
    			"Removed by "+remover.getAsMention(),
    			Color.red.getRGB())
    			.build();
    	InputStream emoteStream;
		try {
			emoteStream = emote.getImage().download(512).get();
		} catch (InterruptedException | ExecutionException e) {
			m_logger.error(e.getMessage(), e);
			auditChannel.sendMessage("Error in onEmojiRemoved: Downloading emote '"+emote.getName()+"' failed. '"+remover.getAsMention()+"' deleted this emote. - "+e.getMessage()).queue();
			return;
		}
    	
    	auditChannel.sendFiles(FileUpload.fromData(emoteStream, event.getEmoji().getName() + ".png")).setEmbeds(embed).queue();
    }

    @Override
    public void onEmojiUpdateName(EmojiUpdateNameEvent event) {
    	TextChannel auditChannel = getAuditChannel(event);
    	if (auditChannel == null) return;
    	
    	AuditLogEntry auditLog = getAuditLog(ActionType.EMOJI_UPDATE, event.getGuild());
    	User editor = auditLog.getUser();
    	RichCustomEmoji emote = event.getEmoji();
    	MessageEmbed embed = startEmbed(
    			event,
    			"Custom Emote Name Updated",
    			"Updated by "+editor.getAsMention(),
    			Color.cyan.getRGB())
    			.addField("Name Before", auditLog.getChangeByKey(AuditLogKey.EMOJI_NAME).getOldValue(), true)
    			.addField("Name After", auditLog.getChangeByKey(AuditLogKey.EMOJI_NAME).getNewValue(), true)
    			.build();
    	InputStream emoteStream;
		try {
			emoteStream = emote.getImage().download(512).get();
		} catch (InterruptedException | ExecutionException e) {
			m_logger.error(e.getMessage(), e);
			auditChannel.sendMessage("Error in onEmojiUpdateName: Downloading emote '"+emote.getName()+"' failed. '"+editor.getAsMention()+"' modified this emote. - "+e.getMessage()).queue();
			return;
		}
    	
    	auditChannel.sendFiles(FileUpload.fromData(emoteStream, event.getEmoji().getName() + ".png")).setEmbeds(embed).queue();
    }
    
    @Override
    public void onEmojiUpdateRoles(EmojiUpdateRolesEvent event) {
    	/*
    	TextChannel auditChannel = getAuditChannel(event);
    	if (auditChannel == null) return;
    	
    	MessageEmbed embed = startEmbed(event, "Custom Emote Added", Color.cyan.getRGB()).build();
    	RichCustomEmoji emote = event.getEmoji();
    	InputStream emoteStream;
		try {
			emoteStream = emote.getImage().download(512).get();
		} catch (InterruptedException | ExecutionException e) {
			m_logger.error(e.getMessage(), e);
			auditChannel.sendMessage("Error in onEmojiAdded: Downloading emote '"+emote.getName()+"' failed. '"+emote.getOwner().getAsMention()+"' added this emote. - "+e.getMessage()).queue();
			return;
		}
    	
    	auditChannel.sendFiles(FileUpload.fromData(emoteStream, event.getEmoji().getName() + ".png")).setEmbeds(embed).queue();
    	*/
    }
    
    @Override
    public void onGuildStickerAdded(GuildStickerAddedEvent event) {}
    
    @Override
    public void onGuildStickerRemoved(GuildStickerRemovedEvent event) {}

    @Override
    public void onGuildStickerUpdateName(GuildStickerUpdateNameEvent event) {}
    
    @Override
    public void onGuildStickerUpdateTags(GuildStickerUpdateTagsEvent event) {}
    
    @Override
    public void onGuildStickerUpdateDescription(GuildStickerUpdateDescriptionEvent event) {}
    
    @Override
    public void onGuildStickerUpdateAvailable(GuildStickerUpdateAvailableEvent event) {}

}
