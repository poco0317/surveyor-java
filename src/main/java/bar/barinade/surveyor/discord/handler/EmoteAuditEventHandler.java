package bar.barinade.surveyor.discord.handler;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.emoji.EmojiAddedEvent;
import net.dv8tion.jda.api.events.emoji.EmojiRemovedEvent;
import net.dv8tion.jda.api.events.emoji.update.EmojiUpdateNameEvent;
import net.dv8tion.jda.api.events.emoji.update.EmojiUpdateRolesEvent;
import net.dv8tion.jda.api.events.sticker.GuildStickerAddedEvent;
import net.dv8tion.jda.api.events.sticker.GuildStickerRemovedEvent;
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateAvailableEvent;
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateDescriptionEvent;
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateNameEvent;
import net.dv8tion.jda.api.events.sticker.update.GuildStickerUpdateTagsEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

@Component
@Scope("prototype")
@Slf4j
public class EmoteAuditEventHandler extends ListenerAdapter {
	

    @Override
    public void onEmojiAdded(EmojiAddedEvent event) {}
    
    @Override
    public void onEmojiRemoved(EmojiRemovedEvent event) {}

    @Override
    public void onEmojiUpdateName(EmojiUpdateNameEvent event) {}
    
    @Override
    public void onEmojiUpdateRoles(EmojiUpdateRolesEvent event) {}
    
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
