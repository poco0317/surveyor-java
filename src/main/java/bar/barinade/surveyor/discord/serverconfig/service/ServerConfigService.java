package bar.barinade.surveyor.discord.serverconfig.service;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import bar.barinade.surveyor.discord.serverconfig.data.ServerConfiguration;
import bar.barinade.surveyor.discord.serverconfig.repo.ServerConfigurationRepo;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ServerConfigService {
	
	@Autowired
	private ServerConfigurationRepo configRepo;
	
	@Transactional
	public ServerConfiguration getConfig(Long guildId) {
		ServerConfiguration config = configRepo.findById(guildId).orElse(null);
		if (config == null) {
			config = new ServerConfiguration();
			config.setId(guildId);
			config = configRepo.saveAndFlush(config);
		}
		return config;
	}
	
	@Transactional
	public void setOutputChannel(Long guildId, Long channelId) {
		ServerConfiguration config = getConfig(guildId);
		config.setChannelId(channelId);
		configRepo.saveAndFlush(config);
		m_logger.debug("Guild {} set output channel to {}", guildId, channelId);
	}
	
	@Transactional
	public Long getOutputChannel(Long guildId) {
		return getConfig(guildId).getChannelId();
	}
	
	@Transactional
	public void setAuditChannel(Long guildId, Long channelId) {
		ServerConfiguration config = getConfig(guildId);
		config.setAuditId(channelId);
		configRepo.saveAndFlush(config);
		m_logger.debug("Guild {} set claim audit channel to {}", guildId, channelId);
	}
	
	@Transactional
	public Long getAuditChannel(Long guildId) {
		return getConfig(guildId).getAuditId();
	}
	
}
