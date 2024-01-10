package bar.barinade.surveyor.discord;

import javax.annotation.PostConstruct;
import javax.security.auth.login.LoginException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import bar.barinade.surveyor.discord.handler.BasicMessageHandler;
import bar.barinade.surveyor.discord.handler.CommandHandlerBase;
import bar.barinade.surveyor.discord.handler.ServerConfigCommandHandler;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.Compression;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

@Service
@Slf4j
public class BotManager {
		
	@Value("${discord.token}")
	private String token;
	
	private static JDA jdaBot;
	
	@Autowired
	private ApplicationContext springContext;
	
	public static JDA getJDA() {
		return jdaBot;
	}
	
	@PostConstruct
	private void initialize() throws LoginException, InterruptedException {
		m_logger.info("Initializing BotManager");
		JDABuilder builder = JDABuilder.createDefault(token);
		
		// bunch of stuff we dont need
		builder.disableCache(
				CacheFlag.ACTIVITY,
				CacheFlag.CLIENT_STATUS,
				// CacheFlag.EMOJI,
				CacheFlag.MEMBER_OVERRIDES,
				CacheFlag.ONLINE_STATUS,
				CacheFlag.ROLE_TAGS,
				CacheFlag.VOICE_STATE
				// CacheFlag.STICKER
				);
		
		// even more stuff we dont need
		builder.disableIntents(
				GatewayIntent.DIRECT_MESSAGE_REACTIONS,
				GatewayIntent.DIRECT_MESSAGE_TYPING,
				GatewayIntent.DIRECT_MESSAGES,
				//GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
				GatewayIntent.GUILD_INVITES,
				GatewayIntent.GUILD_MEMBERS,
				GatewayIntent.GUILD_MESSAGE_REACTIONS,
				GatewayIntent.GUILD_MESSAGE_TYPING,
				GatewayIntent.GUILD_MESSAGES,
				GatewayIntent.GUILD_PRESENCES,
				GatewayIntent.GUILD_VOICE_STATES,
				GatewayIntent.GUILD_WEBHOOKS
				);
		
		builder.enableIntents(
				GatewayIntent.GUILD_EMOJIS_AND_STICKERS
				// GatewayIntent.MESSAGE_CONTENT
				);
		
		// stuff that might matter
		builder.setChunkingFilter(ChunkingFilter.NONE);
		builder.setMemberCachePolicy(MemberCachePolicy.NONE);
		builder.setBulkDeleteSplittingEnabled(false);
		builder.setCompression(Compression.ZLIB);
		builder.setAutoReconnect(true);
		
		// fun
		builder.setActivity(Activity.watching("...."));
		
		// how to care about commands
		final CommandHandlerBase configCommands = springContext.getBean(ServerConfigCommandHandler.class);
		final BasicMessageHandler msgCommands = springContext.getBean(BasicMessageHandler.class);
		builder.addEventListeners(configCommands, msgCommands);
		
		// about to finish making the client...
		m_logger.info("Waiting for login");
		JDA result = builder.build();
		jdaBot = result;
		jdaBot.awaitReady();
		m_logger.info("Login finished");

		m_logger.info("Registering commands");
		
		// how to care about slash commands
		// have to do this after trying to log in
		upsertHandlerCommands(result, configCommands);
		
		m_logger.info("BotManager initialize finished");
	}
	
	private static void upsertHandlerCommands(JDA jda, CommandHandlerBase... handlers) {
		/*
		List<Command> cmds = jda.retrieveCommands().complete();
		if (cmds != null) {
			for (Command cmd : cmds) {
				jda.deleteCommandById(cmd.getIdLong()).complete();
			}
		}
		*/
		if (handlers == null) {
			m_logger.error("Passed no handlers to upsertHandlerCommands. No commands upserted.");
		} else {
			long cnt = 0;
			for (CommandHandlerBase handler : handlers) {
				for (final CommandData cmd : handler.getCommandsToUpsert()) {
					jda.upsertCommand(cmd).queue();
					cnt++;
				}
			}
			m_logger.info("Upserted {} commands from {} handlers.", cnt, handlers.length);
		}
	}

}
