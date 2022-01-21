package com.thiendz.tool.fplautocms;

import com.thiendz.tool.fplautocms.services.ServerService;
import com.thiendz.tool.fplautocms.utils.MsgBoxUtils;
import com.thiendz.tool.fplautocms.utils.OsUtils;
import com.thiendz.tool.fplautocms.utils.consts.Messages;
import com.thiendz.tool.fplautocms.utils.excepts.CmsException;
import com.thiendz.tool.fplautocms.views.DashboardView;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
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
                "-- Version V" + Messages.APP_VER + " -----------------------------------\n" +
                "-- Code by " + Messages.APP_AUTHOR + " -------------------------------\n"
        );
        OsUtils.loadEnvironments(args);
        OsUtils.fixHTTPS();
        DashboardView.start();
        try {
            ServerService.start();
        } catch (CmsException e) {
            log.error("Check tool fail.", e);
            MsgBoxUtils.alert(null, e.toString());
            System.exit(0);
        } catch (IOException e) {
            log.error("Check tool fail.", e);
            MsgBoxUtils.alert(null, Messages.CONNECT_TO_SERVER_ERROR);
            System.exit(0);
        }
    }
}
