package com.thiendz.tool.fplautocms.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiendz.tool.fplautocms.data.models.Course;
import com.thiendz.tool.fplautocms.data.models.User;
import com.thiendz.tool.fplautocms.utils.MsgBoxUtils;
import com.thiendz.tool.fplautocms.utils.consts.Messages;
import com.thiendz.tool.fplautocms.utils.excepts.InputException;
import com.thiendz.tool.fplautocms.views.DashboardView;
import com.thiendz.tool.fplautocms.utils.excepts.CmsException;
import org.apache.http.client.fluent.Request;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoginService implements Runnable {
    private static final String CMS_URL_DASHBOARD = "https://cms.poly.edu.vn/dashboard/";
    private static final String REGEX_CSRF_TOKEN = "csrftoken=(.+?);";
    private final DashboardView dashboardView;
    private final String cookie;
    private User user;

    public static void start(DashboardView dashboardView) {
        new Thread(new LoginService(dashboardView)).start();
    }

    public LoginService(DashboardView dashboardView) {
        this.dashboardView = dashboardView;
        this.cookie = dashboardView.getTfCookie().getText();
    }

    @Override
    public void run() {
        dashboardView.getBtnLogin().setEnabled(false);
        try {
            checkValidLoginInput();
            Document document = parseDocument();
            parseUserInfo(document);
            parseCourseInfo(document);
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

    private Document parseDocument() throws IOException {
        String bodyDash = Request
                .Get(CMS_URL_DASHBOARD)
                .setHeader("cookie", cookie)
                .execute()
                .returnContent()
                .asString();
        return Jsoup.parse(bodyDash);
    }

    private void parseUserInfo(Document document) throws CmsException, JsonProcessingException {
        Element elmUserMetaData = document.selectFirst("script[id='user-metadata']");
        if (elmUserMetaData == null) {
            throw new CmsException("script[id='user-metadata'] is NULL!");
        }
        ObjectMapper objectMapper = new ObjectMapper();
        user = objectMapper.readValue(elmUserMetaData.html(), User.class);
        if (user.getUser_id() == null) {
            throw new CmsException("Login fail, Cookie expired.");
        }
        Pattern pattern = Pattern.compile(REGEX_CSRF_TOKEN);
        Matcher matcher = pattern.matcher(cookie);
        if (!matcher.find()) {
            throw new CmsException("Regex 'csrftoken=(.+?);' is null!");
        }
        int indexStart = matcher.group().indexOf("=");
        int indexEnd = matcher.group().indexOf(";");
        user.setCsrf_token(matcher.group().substring(indexStart + 1, indexEnd));
        user.setCookie(cookie);
    }

    private void parseCourseInfo(Document document) throws CmsException {
        Elements elmsLeanModal = document.select("a[rel='leanModal']");
        if (elmsLeanModal.isEmpty()) {
            throw new CmsException("buildCourse a[rel='leanModal'] is empty!");
        }
        List<Course> courses = new ArrayList<>();
        for (Element elmLean : elmsLeanModal) {
            Course course = new Course();
            course.setName(elmLean.attr("data-course-name"));
            course.setId(elmLean.attr("data-course-id"));
            course.setIndex(elmLean.attr("data-dashboard-index"));
            course.setNumber(elmLean.attr("data-course-number"));
            course.setRefundUrl(elmLean.attr("data-course-refund-url"));
            courses.add(course);
        }
        user.setCourses(courses);
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
