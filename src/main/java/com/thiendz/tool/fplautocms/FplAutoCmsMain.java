package com.thiendz.tool.fplautocms;

import com.thiendz.tool.fplautocms.utils.OsUtils;
import com.thiendz.tool.fplautocms.views.DashboardView;

public class FplAutoCmsMain {
    public static void main(String[] args) {
        OsUtils.fixHTTPS();
        DashboardView.start();
    }

    public static final String APP_NAME = "FPL@utoCMS";
    public static final String APP_VER = "4.0.0";
    public static final String APP_SLOGAN = "10 Quiz 10 Point Easy!";
    public static final String APP_AUTHOR = "ThienDZaii";
    public static final String APP_NICKNAME = "SystemError";
    public static final String APP_CONTACT = "https://fb.com/ThienDz.SystemError";
}
