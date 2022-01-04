package com.thiendz.tool.fplautocms.controllers;

import com.thiendz.tool.fplautocms.FplAutoCmsMain;
import com.thiendz.tool.fplautocms.utils.OsUtils;

public class ContactController implements Runnable {
    public static void start() {
        new Thread(new ContactController()).start();
    }

    @Override
    public void run() {
        OsUtils.openTabBrowser(FplAutoCmsMain.APP_CONTACT);
    }
}
