package bar.barinade.surveyor.discord.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import bar.barinade.surveyor.discord.serverconfig.service.ServerConfigService;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Component
@Scope("prototype")
public class ServerConfigCommandHandler extends CommandHandlerBase {
	
	private static final Logger m_logger = LoggerFactory.getLogger(ServerConfigCommandHandler.class);
	
	@Autowired
	private ServerConfigService configService;
	
	@Value("${discord.ownerid}")
	private String ownerId;
	
	@Override
	public CommandData[] getCommandsToUpsert() {
		return new CommandData[] {};
	}
	
	// ...
	
	private boolean hasPermission(SlashCommandInteractionEvent event) {
		Member mmbr = event.getMember();
		if (mmbr != null
				&& !mmbr.getId().equals(ownerId)
				&& !mmbr.isOwner()
				&& !mmbr.hasPermission(Permission.ADMINISTRATOR)
				&& !mmbr.hasPermission(Permission.MANAGE_SERVER)) {
			m_logger.info("{} attempted to use config command without having permission", mmbr.getId());
			event.getHook().editOriginal("You must have Manage Server or Administrator permissions to use this command.").queue();;
			return false;
		}
		return true;
	}

}
