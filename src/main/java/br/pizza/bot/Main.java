package br.pizza.bot;

import br.pizza.bot.commands.TTKDCommand;
import br.pizza.bot.config.BotConfig;
import br.pizza.bot.events.RegisterCommands;
import com.jagrosh.jdautilities.commons.waiter.EventWaiter;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import org.skife.config.ConfigurationObjectFactory;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;

public class Main {

    public static final String RESOURCES_PATH = Paths.get("").toAbsolutePath() + "/resources";

    public static void main(String[] args) throws FileNotFoundException {
        final ScheduledExecutorService threadPool = Executors
                .newScheduledThreadPool(Math.max(2, ForkJoinPool.getCommonPoolParallelism()));

        final BotConfig botConfig = createConfig(BotConfig.class, RESOURCES_PATH + "/bot-config.properties")
                .orElseThrow(() -> new FileNotFoundException("bot config file not found"));

        final JDA jda = createJDA(threadPool, botConfig);
        EventWaiter eventWaiter = new EventWaiter();

        System.out.println("---\n[AglerrBot] Systems initialized with the bot " + jda.getSelfUser().getAsTag() + ".\n---");
        jda.addEventListener(eventWaiter);
        jda.addEventListener(new RegisterCommands());
        jda.addEventListener(new TTKDCommand(botConfig));
    }


    static <T> Optional<T> createConfig(Class<T> configClass, String configPath) {
        try (FileInputStream configFile = new FileInputStream(configPath)) {
            final Properties configProps = new Properties();
            configProps.load(configFile);

            final ConfigurationObjectFactory configFactory = new ConfigurationObjectFactory(configProps);
            return Optional.of(configFactory.build(configClass));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }


    private static JDA createJDA(ScheduledExecutorService threadPool, BotConfig botConfig) {
        final JDABuilder jdaBuilder = JDABuilder
                .createDefault(botConfig.getToken())
                .setActivity(Activity.playing(botConfig.getPlaying()))
                .setGatewayPool(threadPool)
                .setCallbackPool(threadPool)
                .setRateLimitPool(threadPool)
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableIntents(GatewayIntent.getIntents(GatewayIntent.ALL_INTENTS));
        return jdaBuilder.build();
    }
}
