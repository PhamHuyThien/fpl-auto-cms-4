package com.thiendz.tool.fplautocms.controllers;

import com.thiendz.tool.fplautocms.utils.OsUtils;
import com.thiendz.tool.fplautocms.utils.consts.Messages;

public class ContactController implements Runnable {
    public static void start() {
        new Thread(new ContactController()).start();
    }

    @Override
    public void run() {
        OsUtils.openTabBrowser(Messages.APP_CONTACT);
    }
}
