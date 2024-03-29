package bar.barinade.surveyor.discord.handler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import bar.barinade.surveyor.discord.serverconfig.service.ServerConfigService;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

@Component
@Scope("prototype")
@Slf4j
public class ServerConfigCommandHandler extends CommandHandlerBase {
		
	@Autowired
	private ServerConfigService configService;
	
	@Override
	public CommandData[] getCommandsToUpsert() {
		return new CommandData[] {};
	}
	
	// ...
	
}
