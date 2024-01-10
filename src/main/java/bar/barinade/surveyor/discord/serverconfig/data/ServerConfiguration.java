package bar.barinade.surveyor.discord.serverconfig.data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "server_configs")
@Getter @Setter @EqualsAndHashCode
public class ServerConfiguration {
	
	@Id
	@Column(name = "guild_id")
	private Long id;
	
	@Column(name = "channel_id", nullable = true)
	private Long channelId;
	
	@Column(name = "audit_id", nullable = true)
	private Long auditId;

}
