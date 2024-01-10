package bar.barinade.surveyor.discord.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component
@Scope("prototype")
public class BasicMessageHandler extends ListenerAdapter {
	
	private static final Logger m_logger = LoggerFactory.getLogger(BasicMessageHandler.class);
	
	
	@Value("${discord.ownerid}")
	private String ownerId;
	
	@Override
	public void onMessageReceived(MessageReceivedEvent event) {
		
		// must be in a server
		if (!event.isFromGuild()) {
			return;
		}
		
		// ...
	}
	
	private boolean hasPermission(MessageReceivedEvent event) {
		Member mmbr = event.getMember();
		if (mmbr != null
				&& !mmbr.getId().equals(ownerId)
				&& !mmbr.isOwner()
				&& !mmbr.hasPermission(Permission.ADMINISTRATOR)
				&& !mmbr.hasPermission(Permission.MANAGE_SERVER)) {
			m_logger.info("{} attempted to use config command without having permission", mmbr.getId());
			// event.getChannel().sendMessage("You must have Manage Server or Administrator permissions to use this command.").queue();
			return false;
		}
		return true;
	}
}
