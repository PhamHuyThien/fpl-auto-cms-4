package com.thiendz.tool.fplautocms;

import com.thiendz.tool.fplautocms.services.ServerService;
import com.thiendz.tool.fplautocms.utils.MsgBoxUtils;
import com.thiendz.tool.fplautocms.utils.OsUtils;
import com.thiendz.tool.fplautocms.utils.excepts.CmsException;
import com.thiendz.tool.fplautocms.views.DashboardView;

import java.io.IOException;

public class FplAutoCmsMain {
    public static void main(String[] args) {
        System.out.println("" +
                "-----------------------------------------------------\n" +
                "                                                         \n" +
                "  ___ ___ _      _  _   _ _____ ___   ___ __  __ ___ \n" +
                " | __| _ \\ |    /_\\| | | |_   _/ _ \\ / __|  \\/  / __|\n" +
                " | _||  _/ |__ / _ \\ |_| | | || (_) | (__| |\\/| \\__ \\\n" +
                " |_| |_| |____/_/ \\_\\___/  |_| \\___/ \\___|_|  |_|___/\n" +
                "                                                         \n" +
                "-- Version V" + APP_VER + " -----------------------------------\n" +
                "-- Code by " + APP_AUTHOR + " -------------------------------\n"
        );
        OsUtils.fixHTTPS();
        DashboardView.start();
        try {
            ServerService.start();
        } catch (CmsException e) {
            MsgBoxUtils.alert(null, e.toString());
        } catch (IOException e) {
            MsgBoxUtils.alert(null, "Kết nối tới máy chủ thất bại.");
        }
    }

    public static final String APP_NAME = "FPL@utoCMS";
    public static final String APP_VER = "4.0.0";
    public static final String APP_SLOGAN = "10 Quiz 10 Point Easy!";
    public static final String APP_AUTHOR = "ThienDZaii";
    public static final String APP_NICKNAME = "SystemError";
    public static final String APP_CONTACT = "https://fb.com/ThienDz.SystemError";
}
