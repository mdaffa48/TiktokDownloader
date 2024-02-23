package br.pizza.bot.utils;

import br.pizza.bot.config.BotConfig;
import net.dv8tion.jda.api.entities.Message;
import okhttp3.*;
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

    private static final OkHttpClient client = new OkHttpClient();
    public record VideoStream(
            InputStream stream,
            long size
    ) {

    };


    public static String getVideoJsonInfo(String link, BotConfig botConfig, boolean tiktok) {
        if (tiktok) {
            try {
                Request request = new Request.Builder()
                        .url("https://tiktok-downloader-download-tiktok-videos-without-watermark.p.rapidapi.com/vid/index?url=" + link)
                        .get()
                        .addHeader("X-RapidAPI-Key", botConfig.getRapidApiKey())
                        .addHeader("X-RapidAPI-Host", "tiktok-downloader-download-tiktok-videos-without-watermark.p.rapidapi.com")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    return response.body().string();
                }
            } catch (Exception e) {
                return null;
            }
        } else {
            try {
                Request request = new Request.Builder()
                        .url("https://instagram-scraper-api2.p.rapidapi.com/v1/post_info?code_or_id_or_url=" + link)
                        .get()
                        .addHeader("X-RapidAPI-Key", "0b7167575emsh5e451a16348d2cap1021d5jsn713cb56a7ecb")
                        .addHeader("X-RapidAPI-Host", "instagram-scraper-api2.p.rapidapi.com")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    return response.body().string();
                }
            } catch (Exception ex) {
                return null;
            }
        }
    }

    //Short url
    public static String shortVideoUrl(String url) {
        try {
            //Make API post request
            RequestBody formBody = new FormBody.Builder()
                    .add("url", url)
                    .build();
            Request request = new Request.Builder()
                    .url("https://cleanuri.com/api/v1/shorten")
                    .post(formBody)
                    .header("Accept", "application/json")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                JSONObject jsonResponse = new JSONObject(response.body().string());
                return jsonResponse.getString("result_url");
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getVideoDownloadLink(String json, boolean tiktok) {
        if (tiktok) {
            try {
                if (json.equals("null")) return "Invalid";

                JSONObject jsonResponse = new JSONObject(json);

                if (jsonResponse.has("video")) {
                    JSONArray videoArray = jsonResponse.getJSONArray("video");
                    if (videoArray.length() > 0) {
                        String url = videoArray.getString(0);
                        return url;
                    }
                }
            } catch (Exception e) {
                return null;
            }

        } else {
            try {
                if (json.equals("null")) return "Invalid";
                JSONObject jsonResponse = new JSONObject(json);
                String url = jsonResponse.getJSONObject("data").getString("video_url");
                return url;

            } catch (Exception ex) {
                return null;
            }
        }
        return null;
    }

    public static String getVideoAuthorName(String json, boolean tiktok) {
        if (tiktok) {
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
        } else {
            try {
                if (json.equals("null")) return "Invalid";

                JSONObject jsonResponse = new JSONObject(json);
                JSONObject user = jsonResponse.getJSONObject("data").getJSONObject("user");
                return user.getString("username");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        return null;
    }

    public static String getVideoDescriptionName(String json, boolean tiktok) {
        if (tiktok) {
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
        } else {
            try {
                if (json.equals("null")) return "Invalid";

                JSONObject jsonResponse = new JSONObject(json);
                JSONObject desc = jsonResponse.getJSONObject("data").getJSONObject("caption");
                return desc.getString("text");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        return null;
    }


    public static String getVideoId(String json, String videoUrl, boolean tiktok) {
        if (tiktok) {
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
        } else {
            try {
                if (json.equals("null")) return "Invalid";

                JSONObject jsonResponse = new JSONObject(json);
                JSONObject desc = jsonResponse.getJSONObject("data").getJSONObject("caption");
                return Long.toString(desc.getLong("id"));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static String getInstagramShortcode(String url) {
        String replace = url.replace("https://", "");
        String[] split = replace.split("/");
        return split[2];
    }


    public static VideoStream getVideoInputStream(String videoUrl) {
        try {

            Request request = new Request.Builder()
                    .url(videoUrl)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            return new VideoStream(response.body().byteStream(), Long.parseLong(response.header("Content-Length"), 10));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String isValidURL(String url) {
        String tiktokRegex = "^^(http|https)://www\\.tiktok\\.com(/[a-zA-Z0-9-_\\.?&%+=\"]?)*?(/.*)?$";
        Pattern tiktokPattern = Pattern.compile(tiktokRegex);
        Matcher tiktokMatcher = tiktokPattern.matcher(url);

        String instagramRegex = "^(http|https)://www\\.instagram\\.com(/[a-zA-Z0-9-_\\.?&%+=\"]?)*?(/.*)?$";
        Pattern instagramPattern = Pattern.compile(instagramRegex);
        Matcher instagramMatcher = instagramPattern.matcher(url);

        if (tiktokMatcher.matches()) return "tiktok";
        if (instagramMatcher.matches()) return "instagram";

        return null;
    }

    public static boolean isValidResult(String url) {
        if (url == null) return false;
        if (url.equalsIgnoreCase("tiktok")) return true;
        if (url.equalsIgnoreCase("instagram")) return true;
        return false;
    }

    public static List<String> extractValidLinksFromAttachment(Message.Attachment attachment) {
        List<String> links = new ArrayList<>();
        try (InputStream inputStream = attachment.retrieveInputStream().get()) {
            byte[] bytes = inputStream.readAllBytes();
            String text = new String(bytes);
            String[] words = text.split("\\s+");
            for (String word : words) {
                if (isValidResult(isValidURL(word))) {
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

    public static OkHttpClient getClient() {
        return client;
    }

}
