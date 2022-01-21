package com.thiendz.tool.fplautocms.controllers;

import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.services.LoginService;
import com.thiendz.tool.fplautocms.services.ServerService;
import com.thiendz.tool.fplautocms.utils.MsgBoxUtils;
import com.thiendz.tool.fplautocms.utils.consts.Messages;
import com.thiendz.tool.fplautocms.utils.excepts.CmsException;
import com.thiendz.tool.fplautocms.utils.excepts.InputException;
import com.thiendz.tool.fplautocms.views.DashboardView;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
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
        dashboardView.buttonEnabled(false);
        dashboardView.getLbHello().setText(Messages.HI + "..................");
        dashboardView.getLbUserId().setText(Messages.USER_ID + "..............");
        try {
            checkValidLoginInput();
            dashboardView.showProcess(Messages.WAIT_LOGIN);
            LoginService loginService = new LoginService(cookie);
            loginService.login();
            user = loginService.getUser();
            pushAnalysis();
            showDashboard();
            dashboardView.getCbbCourse().setEnabled(true);
            dashboardView.showProcess(Messages.LOGIN_SUCCESS);
            log.info(loginService.getUser().toString());
        } catch (InputException e) {
            log.error(Messages.LOGIN_ERROR, e);
            dashboardView.showProcess(Messages.INVALID_INPUT + e);
            MsgBoxUtils.alertErr(dashboardView, Messages.INVALID_INPUT + e);
        } catch (IOException e) {
            log.error(Messages.LOGIN_ERROR, e);
            dashboardView.showProcess(Messages.CONNECT_ERROR);
            MsgBoxUtils.alertErr(dashboardView, Messages.CONNECT_ERROR);
        } catch (CmsException e) {
            log.error(Messages.LOGIN_ERROR, e);
            dashboardView.showProcess(e.toString());
            MsgBoxUtils.alertErr(dashboardView, e.toString());
        } catch (Exception e) {
            log.error(Messages.LOGIN_ERROR, e);
            dashboardView.showProcess(Messages.AN_ERROR_OCCURRED + e);
            MsgBoxUtils.alertErr(dashboardView, Messages.AN_ERROR_OCCURRED + e);
        }
        dashboardView.getTfCookie().setEnabled(true);
        dashboardView.getBtnLogin().setEnabled(true);
    }

    private void pushAnalysis() {
        try {
            ServerService.serverService.pushAnalysis(user);
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

    private void checkValidLoginInput() throws InputException {
        if (cookie.trim().length() == 0) {
            throw new InputException(Messages.COOKIE_EMPTY);
        }
    }

    private void showDashboard() {
        dashboardView.setUser(user);
        dashboardView.getLbHello().setText(Messages.HI + user.getUsername());
        dashboardView.getLbUserId().setText(Messages.USER_ID + user.getUser_id());
        dashboardView.getCbbCourse().removeAllItems();
        dashboardView.getCbbCourse().addItem(Messages.SELECT_COURSE);
        user.getCourses().forEach(course -> dashboardView.getCbbCourse().addItem(course.getName()));
        dashboardView.getCbbCourse().setEnabled(true);
    }
}
