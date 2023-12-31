package br.pizza.bot.events;

import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class RegisterCommands extends ListenerAdapter {

    @Override
    public void onReady(@NotNull ReadyEvent event) {
        event.getJDA().updateCommands().addCommands(Commands.slash("tiktokdownload", "Show the video without a watermark to download")
                .addOption(OptionType.STRING, "url", "Enter the URL of the video to be downloaded", false)
                .addOption(OptionType.ATTACHMENT, "urls", "Enter the .txt with the video URLs to be downloaded", false)).queue();
    }
}
