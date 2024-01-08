package br.pizza.bot.commands;

import br.pizza.bot.Main;
import br.pizza.bot.config.BotConfig;
import br.pizza.bot.utils.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TTKDCommand extends ListenerAdapter {

    private final BotConfig botConfig;

    public TTKDCommand(BotConfig botConfig) {
        this.botConfig = botConfig;
    }


    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        if (event.getName().equals("tiktokdownload")) {

            try {
                OptionMapping multipleUrl = event.getOption("urls");
                OptionMapping singleUrl = event.getOption("url");

                List<String> commandUrls = new ArrayList<>();
                // add the single url to the list
                if (singleUrl != null && Utils.isValidURL(singleUrl.getAsString())) {
                    commandUrls.add(singleUrl.getAsString());
                }
                // parse the attachment into list of urls
                if (multipleUrl != null) {
                    // get the attachment file
                    Message.Attachment attachment = multipleUrl.getAsAttachment();
                    // extract the urls
                    commandUrls.addAll(Utils.extractValidLinksFromAttachment(attachment));
                }

                if (commandUrls.isEmpty()) {
                    event.reply("Please provide at least one TikTok URL.").setEphemeral(true).queue();
                    return;
                }

                final int originalSize = commandUrls.size();

                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                event.reply("Downloading...").setEphemeral(true).queue(m -> {
                    AtomicInteger successfulDownloadInts = new AtomicInteger();

                    scheduler.scheduleAtFixedRate(() -> {
                        if (commandUrls.isEmpty()) {
                            return;
                        }

                        String url = commandUrls.get(0);
                        if (!Utils.isValidURL(url)) {
                            m.editOriginal("The URL `" + url + "` is not valid!").queue();
                            return;
                        }

                        String videoJson = Utils.getVideoJsonInfo(url, botConfig);
                        String videoUrl = Utils.getVideoDownloadLink(videoJson);
                        InputStream inputStream = Utils.getVideoInputStream(videoUrl);
                        String videoId = Utils.getVideoId(videoJson);

                        if (videoJson == null) {
                            m.editOriginal("An error occurred while downloading the video. Please try again later.\nTotal Successful Download: " + successfulDownloadInts.get()).queue();
                            return;
                        } else if (videoUrl.equalsIgnoreCase("Invalid")) {
                            m.editOriginal("An error occurred while downloading the video. Please try again later.\nTotal Successful Download: " + successfulDownloadInts.get()).queue();
                            return;
                        }

                        if (getInputStreamSize(inputStream) > 8388608) {
                            String videoInformationWithoutAttachment =
                                    "> Author: " + Utils.getVideoAuthorName(videoJson) + "\n" +
                                            "> Description: " + Utils.getVideoDescriptionName(videoJson) + "[.](" + videoUrl + ")";

                            event.getChannel().sendMessage(videoInformationWithoutAttachment).addActionRow(
                                    Button.link(videoUrl, "Download Video"),
                                    Button.link(url, "Go to TikTok")
                            ).queue();
                        } else {

                            String videoInformationWithAttachment =
                                    "> Author: " + Utils.getVideoAuthorName(videoJson) + "\n" +
                                            "> Description: " + Utils.getVideoDescriptionName(videoJson);
                            event.getChannel().sendMessage(videoInformationWithAttachment).setFiles(FileUpload.fromData(Utils.getVideoInputStream(videoUrl), videoId + ".mov"))
                                    .addActionRow(
                                            Button.link(videoUrl, "Download Video"),
                                            Button.link(url, "Go to TikTok")
                                    ).queue();
                        }

                        successfulDownloadInts.getAndIncrement();
                        // remove the first element
                        commandUrls.remove(0);
                        // Edit the message
                        m.editOriginal(
                                "Total Downloads: " + successfulDownloadInts.get() + "\n" +
                                        "Remaining: " + commandUrls.size() + "/" + originalSize
                        ).queue();

                        // Check if it's empty
                        if (commandUrls.isEmpty()) {
                            m.editOriginal(
                                    "Successful Downloads: " + successfulDownloadInts.get() + "\n" +
                                            "Video Amount: " + originalSize + " videos" + "\n" +
                                            "Completed!"
                            ).queue();
                        }
                    }, 0, 3L, TimeUnit.SECONDS);

                    /*for (String commandUrl : commandUrls) {

                        if (!Utils.isValidURL(commandUrl)) {
                            m.editOriginal("The URL `" + commandUrl + "` is not valid!").queue();
                            return;
                        }

                        String videoJson = Utils.getVideoJsonInfo(commandUrl, botConfig);
                        String videoUrl = Utils.getVideoDownloadLink(videoJson);
                        InputStream inputStream = Utils.getVideoInputStream(videoUrl);
                        String videoId = Utils.getVideoId(videoJson);

                        // salvarInputStream(Utils.getVideoInputStream(videoUrl), videoId);

                        if (videoJson == null) {
                            m.editOriginal("An error occurred while downloading the video. Please try again later.").queue();
                            return;
                        } else if (videoUrl.equalsIgnoreCase("Invalid")) {
                            m.editOriginal("An error occurred while downloading the video. Please try again later.").queue();
                            return;
                        }

                        if (getInputStreamSize(inputStream) > 8388608) {

                            String videoInformationWithoutAttachment =
                                    "> Author: " + Utils.getVideoAuthorName(videoJson) + "\n" +
                                            "> Description: " + Utils.getVideoDescriptionName(videoJson) + "[.](" + videoUrl + ")";

                            event.getChannel().sendMessage(videoInformationWithoutAttachment).addActionRow(
                                    Button.link(videoUrl, "Download Video"),
                                    Button.link(commandUrl, "Go to TikTok")
                            ).queue();
                        } else {

                            String videoInformationWithAttachment =
                                    "> Author: " + Utils.getVideoAuthorName(videoJson) + "\n" +
                                            "> Description: " + Utils.getVideoDescriptionName(videoJson);
                            event.getChannel().sendMessage(videoInformationWithAttachment).setFiles(FileUpload.fromData(Utils.getVideoInputStream(videoUrl), videoId + ".mov"))
                                    .addActionRow(
                                            Button.link(videoUrl, "Download Video"),
                                            Button.link(commandUrl, "Go to TikTok")
                                    ).queue();
                        }

                        successfulDownloadsInt[0]++;
                        m.editOriginal("Downloading... " + successfulDownloadsInt[0] + " of " + commandUrls.size()).queue();
                    }*/
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static void salvarInputStream(InputStream inputStream, String videoId) {
        try {
            Date date = new Date();
            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy HH-mm");
            String formattedDate = format.format(date);

            Path destino = Path.of(Main.RESOURCES_PATH + "/videos/" + formattedDate + "/" + videoId + ".mov");

            Files.createDirectories(destino.getParent());
            Files.copy(inputStream, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static long getInputStreamSize(InputStream inputStream) {
        try {
            long size = 0;
            byte[] buffer = new byte[1024];

            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                size += bytesRead;
            }

            return size;
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
    }
}
