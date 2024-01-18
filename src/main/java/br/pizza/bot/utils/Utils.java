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

    private static final OkHttpClient client = new OkHttpClient();


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
                        .url("https://instagram301.p.rapidapi.com/postinfo.php?url=" + link)
                        .get()
                        .addHeader("X-RapidAPI-Key", "0b7167575emsh5e451a16348d2cap1021d5jsn713cb56a7ecb")
                        .addHeader("X-RapidAPI-Host", "instagram301.p.rapidapi.com")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    return response.body().string();
                }
            } catch (Exception ex) {
                return null;
            }
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
                        return videoArray.getString(0);
                    }
                }
            } catch (Exception e) {
                return null;
            }
        } else {
            try {
                if (json.equals("null")) return "Invalid";

                JSONObject jsonResponse = new JSONObject(json);
                JSONObject result = jsonResponse.getJSONObject("result");
                return result.getString("video_url");

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
                JSONObject result = jsonResponse.getJSONObject("result");
                JSONObject owner = result.getJSONObject("owner");
                return owner.getString("username");

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
                JSONObject result = jsonResponse.getJSONObject("result");
                JSONObject edges = result.getJSONObject("edge_media_to_caption");
                JSONObject node = edges.getJSONArray("edges")
                        .getJSONObject(0)
                        .getJSONObject("node");

                return node.getString("text");

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
                JSONObject result = jsonResponse.getJSONObject("result");
                return result.getString("id");

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

    public static InputStream getVideoInputStream(String videoUrl) {
        try {
            Request request = new Request.Builder()
                    .url(videoUrl)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                return response.body().byteStream();
            }

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
}
