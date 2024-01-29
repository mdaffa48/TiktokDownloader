package br.pizza.bot.commands;

import br.pizza.bot.Main;
import br.pizza.bot.config.BotConfig;
import br.pizza.bot.utils.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CloseShieldOutputStream;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.CharacterIterator;
import java.text.SimpleDateFormat;
import java.text.StringCharacterIterator;
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

        if (!event.getName().equals("tiktokdownload")) return;

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

        // Create a scheduler
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        event.reply("Downloading...").setEphemeral(true).queue(m -> {
            AtomicInteger successfulDownloadInts = new AtomicInteger();

            // Start the scheduler
            scheduler.scheduleAtFixedRate(() -> {
                // If the url is already empty, stop the scheduler
                if (commandUrls.isEmpty()) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("Successful Downloads: ").append(successfulDownloadInts.get()).append("\n");
                    builder.append("Failed Download: ").append(failedVideos.size()).append("\n");
                    builder.append("Video Amount: ").append(originalSize).append(" videos\n");
                    builder.append("The download is completed!\n\n");
                    builder.append("Failed Video Links:\n");
                    // Give the link of failed videos
                    failedVideos.stream()
                            .filter(video -> !successfulVideos.contains(video))
                            .toList()
                            .forEach(failedVideo -> builder.append("- <").append(failedVideo).append(">").append("\n"));
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

                // Get the file size
                long inputStreamSize = getInputStreamSize(inputStream);
                String fileSize = humanReadableByteCountSI(inputStreamSize);

                FileUpload fileUpload = null;

                // Create the message
                String videoMessage;
                String logMessage;

                if (inputStreamSize > 8388608) {
                    logMessage = "[Download] " + url + " (" + fileSize + ") " + " - Sending message without attachment...";
                    videoMessage = "> Author: " + author + "\n" +
                                    "> Description: " + caption + "[.](" + videoUrl + ")" + "\n" +
                                    "> <" + url + ">";
                } else {
                    logMessage = "[Download] " + url + " (" + fileSize + ") " +" - Sending message with attachment...";
                    videoMessage = "> Author: " + author + "\n" +
                                    "> Description: " + caption + "\n" +
                                    "> <" + url + ">";
                    // Get the new input stream
                    fileUpload = FileUpload.fromData(inputStream, videoId + ".mov");
                }
                // Send log messages
                System.out.println(logMessage);
                MessageCreateAction action = event.getChannel().sendMessage(videoMessage);
                if (fileUpload != null) action.setFiles(fileUpload);
                // Add the action row
                action.addActionRow(
                        Button.link(videoUrl, "Download Video"),
                        Button.link(url, "Go to Link")
                );
                // Send the message
                action.queue(success -> {
                    // Send success message
                    System.out.println("[Download] " + url + " - Successfully send message!");
                    // Close the input stream
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

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

    public static String humanReadableByteCountSI(long bytes) {
        if (-1000 < bytes && bytes < 1000) {
            return bytes + " B";
        }
        CharacterIterator ci = new StringCharacterIterator("kMGTPE");
        while (bytes <= -999_950 || bytes >= 999_950) {
            bytes /= 1000;
            ci.next();
        }
        return String.format("%.1f %cB", bytes / 1000.0, ci.current());
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
