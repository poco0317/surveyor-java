package bar.barinade.surveyor.discord.serverconfig.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import bar.barinade.surveyor.discord.serverconfig.data.ServerConfiguration;

@Repository
public interface ServerConfigurationRepo extends JpaRepository<ServerConfiguration, Long> {

}
