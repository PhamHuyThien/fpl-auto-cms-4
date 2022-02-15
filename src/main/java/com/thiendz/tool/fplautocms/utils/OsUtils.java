package com.thiendz.tool.fplautocms.utils;

import com.thiendz.tool.fplautocms.dto.IpInfoDto;
import com.thiendz.tool.fplautocms.dto.KeyValueDto;
import com.thiendz.tool.fplautocms.utils.consts.Environments;
import com.thiendz.tool.fplautocms.utils.enums.OsType;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;
import java.util.Locale;

@Slf4j
public class OsUtils {

    public static void loadEnvironments(String[] args) {
        for (String arg : args) {
            KeyValueDto keyValueDto = KeyValueDto.map(arg);
            switch (keyValueDto.getKey()) {
                case "-d":
                case "--disable-analysis":
                    Environments.DISABLE_ANALYSIS = true;
                    log.info("DISABLE_ANALYSIS = true");
                    break;
                case "-sa":
                case "--server-address":
                    Environments.SERVER_ADDRESS = keyValueDto.getValue();
                    log.info("SERVER_ADDRESS = {}", keyValueDto.getValue());
                    break;
                case "-erq":
                case "--enable-reset-quiz":
                    Environments.ENABLE_RESET_QUIZ = true;
                    log.info("ENABLE_RESET_QUIZ = true");
                    break;
                case "-dqs":
                case "--disable-quiz-speed":
                    Environments.DISABLE_QUIZ_SPEED = true;
                    log.info("DISABLE_QUIZ_SPEED = true");
                    break;
                case "-sft":
                case "--skip-final-test":
                    Environments.SKIP_FINAL_TEST = true;
                    log.info("SKIP_FINAL_TEST = true");
                    break;
                case "-s":
                case "--save-log":
                    break;
            }
        }
    }

    public static IpInfoDto getIpInfo() throws IOException {
        final String url = "https://ipinfo.io/json";
        final HttpClient client = HttpClientBuilder.create()
                .disableRedirectHandling()
                .build();
        Executor executor = Executor.newInstance(client);
        Request request = Request.Get(url).setHeader("Content-Type", "application/json; charset=utf-8");
        String body = executor.execute(request)
                .returnContent()
                .asString();
        return MapperUtils.objectMapper.readValue(body, IpInfoDto.class);
    }

    public static void fixHTTPS() {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
        };
        try {
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        } catch (GeneralSecurityException ignored) {
        }
    }

    public static void openTabBrowser(String url) {
        OsType osType = getOperatingSystemType();
        Runtime rt = Runtime.getRuntime();
        try {
            if (osType == OsType.Windows) {
                rt.exec("rundll32 url.dll,FileProtocolHandler " + url);
            } else if (osType == OsType.MacOS) {
                rt.exec("open " + url);
            } else if (osType == OsType.Linux) {
                String[] browsers = {"epiphany", "firefox", "mozilla", "konqueror", "netscape", "opera", "links", "lynx"};
                StringBuilder cmd = new StringBuilder();
                for (int i = 0; i < browsers.length; i++)
                    cmd.append(i == 0 ? "" : " || ").append(browsers[i]).append(" \"").append(url).append("\" ");
                rt.exec(new String[]{"sh", "-c", cmd.toString()});
            }
        } catch (Exception ignored) {
        }
    }

    public static String getUserName() {
        return System.getProperty("user.name");
    }

    public static String getScriptDir() {
        return System.getProperty("user.dir");
    }

    public static OsType getOperatingSystemType() {
        String OS = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
        if ((OS.contains("mac")) || (OS.contains("darwin"))) {
            return OsType.MacOS;
        } else if (OS.contains("win")) {
            return OsType.Windows;
        } else if (OS.contains("nux") || OS.contains("nix")) {
            return OsType.Linux;
        } else {
            return OsType.Other;
        }
    }
}
