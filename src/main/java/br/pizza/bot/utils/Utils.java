package br.pizza.bot.utils;

import br.pizza.bot.config.BotConfig;
import net.dv8tion.jda.api.entities.Message;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {


    public static String getVideoJsonInfo(String link, BotConfig botConfig) {
        try {
            OkHttpClient client = new OkHttpClient();

            Request request = new Request.Builder()
                    .url("https://tiktok-downloader-download-tiktok-videos-without-watermark.p.rapidapi.com/vid/index?url=" + link)
                    .get()
                    .addHeader("X-RapidAPI-Key", botConfig.getRapidApiKey())
                    .addHeader("X-RapidAPI-Host", "tiktok-downloader-download-tiktok-videos-without-watermark.p.rapidapi.com")
                    .build();


            try (Response response = client.newCall(request).execute()) {
                String responseBody = response.body().string();
                return responseBody;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getVideoDownloadLink(String json) {

        try {
            if (json.equals("null")) return "Invalid";

            JSONObject jsonResponse = new JSONObject(json);

            if (jsonResponse.has("video")) {
                JSONArray videoArray = jsonResponse.getJSONArray("video");
                if (videoArray.length() > 0) {
                    return videoArray.getString(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getVideoAuthorName(String json) {

        try {
            if (json.equals("null")) return "Invalid";

            JSONObject jsonResponse = new JSONObject(json);

            if (jsonResponse.has("author")) {
                JSONArray videoArray = jsonResponse.getJSONArray("author");
                if (videoArray.length() > 0) {
                    return videoArray.getString(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getVideoDescriptionName(String json) {

        try {
            if (json.equals("null")) return "Invalid";

            JSONObject jsonResponse = new JSONObject(json);

            if (jsonResponse.has("description")) {
                JSONArray videoArray = jsonResponse.getJSONArray("description");
                if (videoArray.length() > 0) {
                    return videoArray.getString(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }


    public static String getVideoId(String json) {

        try {
            if (json.equals("null")) return "Invalid";

            JSONObject jsonResponse = new JSONObject(json);

            if (jsonResponse.has("videoid")) {
                JSONArray videoArray = jsonResponse.getJSONArray("videoid");
                if (videoArray.length() > 0) {
                    return videoArray.getString(0);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static InputStream getVideoInputStream(String videoUrl) {
        try {
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url(videoUrl)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    return responseBody.byteStream();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean isValidURL(String url) {
        String regex = "^(http|https)://www\\.tiktok\\.com(/[a-zA-Z0-9-_\\.?&%+=\"]?)*?(/.*)?$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);
        return matcher.matches();
    }

    public static List<String> extractValidLinksFromAttachment(Message.Attachment attachment) {
        List<String> links = new ArrayList<>();
        try (InputStream inputStream = attachment.retrieveInputStream().get()) {
            byte[] bytes = inputStream.readAllBytes();
            String text = new String(bytes);
            String[] words = text.split("\\s+");
            for (String word : words) {
                if (isValidURL(word)) {
                    links.add(word);
                }
            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return links;
    }

    private static boolean isTextFile(Message.Attachment attachment) {
        String fileName = attachment.getFileName();
        return fileName.endsWith(".txt") || fileName.endsWith(".log");
    }
}
