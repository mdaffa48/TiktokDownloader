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
import java.util.stream.Collectors;
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
                if (singleUrl != null && Utils.isValidResult(Utils.isValidURL(singleUrl.getAsString()))) {
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

                List<String> failedVideos = new ArrayList<>();
                List<String> successfulVideos = new ArrayList<>();
                final int originalSize = commandUrls.size();

                ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
                event.reply("Downloading...").setEphemeral(true).queue(m -> {
                    AtomicInteger successfulDownloadInts = new AtomicInteger();

                    scheduler.scheduleAtFixedRate(() -> {
                        if (commandUrls.isEmpty()) {
                            StringBuilder builder = new StringBuilder();
                            builder.append("Successful Downloads: ").append(successfulDownloadInts.get()).append("\n");
                            builder.append("Failed Download: ").append(failedVideos.size()).append("\n");
                            builder.append("Video Amount: ").append(originalSize).append(" videos\n");
                            builder.append("The download is completed!\n\n");
                            builder.append("Failed Video Links:\n");
                            List<String> filteredVideos = failedVideos.stream()
                                    .filter(video -> !successfulVideos.contains(video))
                                    .toList();
                            for (String failedVideo : filteredVideos) {
                                builder.append("<").append(failedVideo).append(">").append("\n");
                            }
                            // Edit the original message
                            m.editOriginal(builder.toString()).queue();
                            // Stop the scheduler
                            scheduler.shutdown();
                            return;
                        }

                        String url = commandUrls.get(0);
                        String result = Utils.isValidURL(url);
                        boolean tiktok = result.equalsIgnoreCase("tiktok");
                        if (!Utils.isValidResult(result)) {
                            m.editOriginal("The URL `" + url + "` is not valid!").queue();
                            return;
                        }

                        String videoJson = Utils.getVideoJsonInfo(url, botConfig, tiktok);
                        if (videoJson == null) {
                            failedVideos.add(url);
                            return;
                        }

                        String videoUrl = Utils.getVideoDownloadLink(videoJson, tiktok);
                        InputStream inputStream = Utils.getVideoInputStream(videoUrl);

                        String videoId = Utils.getVideoId(videoJson, videoUrl, tiktok);
                        String author = Utils.getVideoAuthorName(videoJson, tiktok);
                        String caption = Utils.getVideoDescriptionName(videoJson, tiktok);

                        long inputStreamSize = getInputStreamSize(inputStream);
                        FileUpload fileUpload = FileUpload.fromData(inputStream, videoId + ".mov");
                        System.out.println("[Download] " + url + " (" + inputStreamSize + " bytes)");
                        if (inputStreamSize > 8388608) {
                            System.out.println("[Download] " + url + " - Sending message without attachment...");
                            String videoInformationWithoutAttachment =
                                    "> Author: " + author + "\n" +
                                            "> Description: " + caption + "[.](" + videoUrl + ")" + "\n" +
                                            "<" + url + ">";

                            event.getChannel().sendMessage(videoInformationWithoutAttachment).addActionRow(
                                    Button.link(videoUrl, "Download Video"),
                                    Button.link(url, "Go to TikTok")
                            ).queue(message -> {
                                // Send success message
                                System.out.println("[Download] " + url + " - Successfully send message!");
                                // Close the input stream
                                try {
                                    inputStream.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        } else {
                            System.out.println("[Download] " + url + " - Sending message with attachment...");
                            String videoInformationWithAttachment =
                                    "> Author: " + author + "\n" +
                                            "> Description: " + caption + "\n" +
                                            "<" + url + ">";
                            event.getChannel().sendMessage(videoInformationWithAttachment).setFiles(fileUpload)
                                    .addActionRow(
                                            Button.link(videoUrl, "Download Video"),
                                            Button.link(url, "Go to TikTok")
                                    ).queue(message -> {
                                        // Send success message
                                        System.out.println("[Download] " + url + " - Successfully send message!");
                                        // Close the input stream
                                        try {
                                            inputStream.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    });
                        }

                        successfulDownloadInts.getAndIncrement();
                        successfulVideos.add(url);
                        // remove the first element
                        commandUrls.remove(0);
                        // Edit the message
                        m.editOriginal(
                                "Total Downloads: " + successfulDownloadInts.get() + "\n" +
                                        "Failed Download: " + failedVideos.size() + "\n" +
                                        "Remaining: " + commandUrls.size() + "/" + originalSize
                        ).queue();

                    }, 0, 3L, TimeUnit.SECONDS);

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
