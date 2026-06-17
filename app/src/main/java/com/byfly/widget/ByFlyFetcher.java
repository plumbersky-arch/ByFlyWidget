package com.byfly.widget;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ByFlyFetcher {

    private static final String BASE_URL = "https://issaold.beltelecom.by/";
    private static final String UA = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";

    public static String fetchBalance(String login, String password) throws Exception {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        CookieHandler oldHandler = CookieHandler.getDefault();
        CookieHandler.setDefault(cookieManager);

        try {
            URL loginUrl = new URL(BASE_URL + "main.html");
            HttpURLConnection conn = (HttpURLConnection) loginUrl.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            conn.setRequestProperty("Accept-Language", "ru,ru-RU;q=0.9,en;q=0.5");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Referer", BASE_URL + "main.html");
            conn.setRequestProperty("Origin", BASE_URL.substring(0, BASE_URL.length() - 1));

            String postData = "redirect=%2Fmain.html&oper_user=" +
                    URLEncoder.encode(login, "UTF-8") +
                    "&passwd=" +
                    URLEncoder.encode(password, "UTF-8");

            OutputStream os = conn.getOutputStream();
            os.write(postData.getBytes("UTF-8"));
            os.flush();
            os.close();

            StringBuilder body = readStream(conn);
            int code = conn.getResponseCode();
            conn.disconnect();

            String html = body.toString();

            if (!html.contains("logout")) {
                String snippet = html.length() > 500 ? html.substring(0, 500) : html;
                throw new Exception("Login failed (HTTP " + code + "). Response: " + snippet);
            }

            URL mainUrl = new URL(BASE_URL + "main.html");
            conn = (HttpURLConnection) mainUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(30000);
            conn.setRequestProperty("User-Agent", UA);
            conn.setRequestProperty("Accept", "text/html,*/*");
            conn.setRequestProperty("Accept-Language", "ru,ru-RU;q=0.9");

            body = readStream(conn);
            conn.disconnect();

            html = body.toString();

            return parseBalance(html);
        } finally {
            CookieHandler.setDefault(oldHandler);
        }
    }

    private static String parseBalance(String html) throws Exception {
        String cleaned = html.replaceAll("\\s+", " ");

        Matcher m1 = Pattern.compile(
                "\u0410\u043a\u0442\u0443\u0430\u043b\u044c\u043d\u044b\u0439\\s*\u0431\u0430\u043b\u0430\u043d\u0441" +
                        "\\s*</(?:td|th|div|span|b|strong)[^>]*>\\s*(?:<[^>]*>)*\\s*([\\d\\s\\.,]+)\\s*(?:[\u0440\u0440ub])?",
                Pattern.CASE_INSENSITIVE
        ).matcher(cleaned);

        if (m1.find()) {
            String val = m1.group(1).trim().replaceAll("\\s+", "");
            if (!val.isEmpty()) {
                return val + " \u0440";
            }
        }

        Matcher m2 = Pattern.compile(
                "\u0410\u043a\u0442\u0443\u0430\u043b\u044c\u043d\u044b\u0439\\s*\u0431\u0430\u043b\u0430\u043d\u0441" +
                        "\\s*(?:<[^>]*>)*\\s*([\\-]?\\d+[\\.,]?\\d*)",
                Pattern.CASE_INSENSITIVE
        ).matcher(cleaned);

        if (m2.find()) {
            String val = m2.group(1).trim();
            return val + " \u0440";
        }

        Matcher m3 = Pattern.compile(
                "\u0411\u0430\u043b\u0430\u043d\u0441[:\\s]*(?:<[^>]*>)*\\s*([\\-]?\\d+[\\.,]?\\d*)\\s*[\u0440]",
                Pattern.CASE_INSENSITIVE
        ).matcher(cleaned);

        if (m3.find()) {
            return m3.group(1).trim() + " \u0440";
        }

        Matcher m4 = Pattern.compile(
                "(\u0440\u0443\u0431|[\\u0440])[\\s]*(?:<[^>]*>)*\\s*([\\-]?\\d+[\\.,]?\\d*)",
                Pattern.CASE_INSENSITIVE
        ).matcher(cleaned);

        if (m4.find()) {
            String val = m4.group(2).trim();
            if (!val.isEmpty()) {
                return val + " \u0440";
            }
        }

        throw new Exception("Balance not found in page");
    }

    private static StringBuilder readStream(HttpURLConnection conn) throws Exception {
        BufferedReader reader;
        int respCode = conn.getResponseCode();
        if (respCode >= 200 && respCode < 400) {
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
        } else {
            reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), "UTF-8"));
        }
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }
        reader.close();
        return sb;
    }
}
