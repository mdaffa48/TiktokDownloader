package br.pizza.bot.config;

import org.skife.config.Config;

public interface BotConfig {

    @Config("bot.token")
    String getToken();

    @Config("bot.playing")
    String getPlaying();

    @Config("rapidapi.key")
    String getRapidApiKey();
}
