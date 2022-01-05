package com.thiendz.tool.fplautocms.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.thiendz.tool.fplautocms.models.Course;
import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.utils.MapperUtils;
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

public class LoginService {
    private static final String CMS_URL_DASHBOARD = "https://cms.poly.edu.vn/dashboard/";
    private static final String REGEX_CSRF_TOKEN = "csrftoken=(.+?);";
    private final String cookie;
    private Document document;
    private User user;

    public LoginService(String cookie) {
        this.cookie = cookie;
    }

    public User getUser() {
        return user;
    }

    public void login() throws IOException, CmsException {
        parseDocument();
        parseUserInfo();
        parseCourseInfo();
    }

    private void parseDocument() throws IOException {
        String bodyDash = Request
                .Get(CMS_URL_DASHBOARD)
                .setHeader("cookie", cookie)
                .execute()
                .returnContent()
                .asString();
        document = Jsoup.parse(bodyDash);
    }

    private void parseUserInfo() throws CmsException, JsonProcessingException {
        Element elmUserMetaData = document.selectFirst("script[id='user-metadata']");
        if (elmUserMetaData == null) {
            throw new CmsException("script[id='user-metadata'] không tồn tại.");
        }
        user = MapperUtils.objectMapper.readValue(elmUserMetaData.html(), User.class);
        if (user.getUser_id() == null) {
            throw new CmsException("Đăng nhập thất bại, cookie sai hoặc hết hạn.");
        }
        Pattern pattern = Pattern.compile(REGEX_CSRF_TOKEN);
        Matcher matcher = pattern.matcher(cookie);
        if (!matcher.find()) {
            throw new CmsException("Đăng nhập thất bại, không tìm thấy CSRF token.");
        }
        int indexStart = matcher.group().indexOf("=");
        int indexEnd = matcher.group().indexOf(";");
        user.setCsrf_token(matcher.group().substring(indexStart + 1, indexEnd));
        user.setCookie(cookie);
    }

    private void parseCourseInfo() throws CmsException {
        Elements elmsLeanModal = document.select("a[rel='leanModal']");
        if (elmsLeanModal.isEmpty()) {
            throw new CmsException("a[rel='leanModal'] không tồn tại.");
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
}
