package com.thiendz.tool.fplautocms.utils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.cert.X509Certificate;

public class OsUtils {

    public static boolean fixHTTPS() {
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
            return true;
        } catch (GeneralSecurityException e) {
            return false;
        }
    }

    public static void openTabBrowser(String url) {
        String path[] = new String[]{
                //coccoc
                "C:\\Users\\" + getUserName() + "\\AppData\\Local\\CocCoc\\Browser\\Application\\browser.exe",
                //chrome x86
                "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe",
                //chrome x64
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                //ie
                "C:\\Program Files\\Internet Explorer\\iexplore.exe"
        };
        for (String s : path) {
            if (shell(s, url, "--new-tab", "--full-screen")) {
                return;
            }
        }
    }

    public static boolean shell(String... shell) {
        if (getOSName().toLowerCase().startsWith("windows")) {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(shell);
            builder.directory(new File(System.getProperty("user.home")));
            try {
                builder.start();
                return true;
            } catch (IOException ex) {
            }
        }
        return false;
    }

    public static String getUserName() {
        return System.getProperty("user.name");
    }

    public static String getScriptDir() {
        return System.getProperty("user.dir");
    }

    public static String getOSName() {
        return System.getProperty("os.name");
    }
}
