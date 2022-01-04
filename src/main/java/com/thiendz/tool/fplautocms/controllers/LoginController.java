package com.thiendz.tool.fplautocms.controllers;

import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.services.LoginService;
import com.thiendz.tool.fplautocms.utils.MsgBoxUtils;
import com.thiendz.tool.fplautocms.utils.consts.Messages;
import com.thiendz.tool.fplautocms.utils.excepts.CmsException;
import com.thiendz.tool.fplautocms.utils.excepts.InputException;
import com.thiendz.tool.fplautocms.views.DashboardView;

import java.io.IOException;

public class LoginController implements Runnable {
    private final DashboardView dashboardView;
    private final String cookie;
    private User user;

    public static void start(DashboardView dashboardView) {
        new Thread(new LoginController(dashboardView)).start();
    }

    public LoginController(DashboardView dashboardView) {
        this.dashboardView = dashboardView;
        this.cookie = dashboardView.getTfCookie().getText();
    }

    @Override
    public void run() {
        dashboardView.getBtnLogin().setEnabled(false);
        try {
            checkValidLoginInput();
            LoginService loginService = new LoginService(cookie);
            loginService.login();
            this.user = loginService.getUser();
            showDashboard();
            MsgBoxUtils.alert(dashboardView, Messages.LOGIN_SUCCESS);
        } catch (InputException e) {
            MsgBoxUtils.alert(dashboardView, Messages.INVALID_INPUT + e);
        } catch (IOException e) {
            e.printStackTrace();
            MsgBoxUtils.alert(dashboardView, Messages.CONNECT_ERROR);
        } catch (CmsException e) {
            MsgBoxUtils.alert(dashboardView, e.toString());
        } catch (Exception e) {
            MsgBoxUtils.alert(dashboardView, Messages.AN_ERROR_OCCURRED + e);
        }
        dashboardView.getBtnLogin().setEnabled(true);
    }

    private void checkValidLoginInput() throws InputException {
        if (cookie.trim().length() == 0) {
            throw new InputException("cookie input not empty.");
        }
    }

    private void showDashboard() {
        dashboardView.setUser(user);
        dashboardView.getLbHello().setText("Hello: " + user.getUsername());
        dashboardView.getLbUserId().setText("User ID: " + user.getUser_id());
        dashboardView.getCbbCourse().removeAllItems();
        dashboardView.getCbbCourse().addItem("Select Course...");
        user.getCourses().forEach(course -> {
            dashboardView.getCbbCourse().addItem(course.getName());
        });
        dashboardView.getCbbCourse().setEnabled(true);
    }
}
