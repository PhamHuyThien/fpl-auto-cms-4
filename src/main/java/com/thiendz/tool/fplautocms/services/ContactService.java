package com.thiendz.tool.fplautocms.services;

import com.thiendz.tool.fplautocms.FplAutoCmsMain;
import com.thiendz.tool.fplautocms.utils.OsUtils;

public class ContactService implements Runnable {
    public static void start() {
        new Thread(new ContactService()).start();
    }

    @Override
    public void run() {
        OsUtils.openTabBrowser(FplAutoCmsMain.APP_CONTACT);
    }
}
