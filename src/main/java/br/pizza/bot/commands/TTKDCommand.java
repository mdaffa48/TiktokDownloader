package br.pizza.bot.commands;

import br.pizza.bot.Main;
import br.pizza.bot.config.BotConfig;
import br.pizza.bot.utils.Utils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
                Message.Attachment attachment = event.getOption("urls").getAsAttachment();
                List<String> commandUrls = Utils.extractValidLinksFromAttachment(attachment);

                if (commandUrls.isEmpty()) {
                    event.reply("Please provide at least one TikTok URL.").setEphemeral(true).queue();
                    return;
                }

                event.reply("Downloading...").setEphemeral(true).queue(m -> {

                    int successfulDownloadsInt = 0;

                    StringBuilder message = new StringBuilder();
                    for (String commandUrl : commandUrls) {

                        if (!Utils.isValidURL(commandUrl)) {
                            m.editOriginal("The URL `" + commandUrl + "` is not valid!").queue();
                            return;
                        }

                        String videoJson = Utils.getVideoJsonInfo(commandUrl, botConfig);
                        String videoUrl = Utils.getVideoDownloadLink(videoJson);
                        InputStream inputStream = Utils.getVideoInputStream(videoUrl);
                        String videoId = Utils.getVideoId(videoJson);

                        salvarInputStream(Utils.getVideoInputStream(videoUrl), videoId);

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

                            event.getChannel().sendMessage(videoInformationWithoutAttachment).addActionRow(Button.link(videoUrl, "Download Video")).queue();
                        } else {

                            String videoInformationWithAttachment =
                                    "> Author: " + Utils.getVideoAuthorName(videoJson) + "\n" +
                                            "> Description: " + Utils.getVideoDescriptionName(videoJson);
                            event.getChannel().sendMessage(videoInformationWithAttachment).setFiles(FileUpload.fromData(Utils.getVideoInputStream(videoUrl), videoId + ".mov")).addActionRow(Button.link(videoUrl, "Download Video")).queue();
                        }

                        successfulDownloadsInt++;
                        m.editOriginal("Downloading... " + successfulDownloadsInt + " of " + commandUrls.size()).queue();
                    }
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
