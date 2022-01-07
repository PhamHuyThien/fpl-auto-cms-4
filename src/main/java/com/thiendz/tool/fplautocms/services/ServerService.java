package com.thiendz.tool.fplautocms.services;

import com.thiendz.tool.fplautocms.FplAutoCmsMain;
import com.thiendz.tool.fplautocms.dto.CheckAppDto;
import com.thiendz.tool.fplautocms.dto.CourseSafetyDto;
import com.thiendz.tool.fplautocms.dto.CourseSafetyResponseDto;
import com.thiendz.tool.fplautocms.dto.VoidResponseDto;
import com.thiendz.tool.fplautocms.models.Course;
import com.thiendz.tool.fplautocms.dto.IpInfoDto;
import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.models.Version;
import com.thiendz.tool.fplautocms.utils.MapperUtils;
import com.thiendz.tool.fplautocms.utils.OsUtils;
import com.thiendz.tool.fplautocms.utils.StringUtils;
import com.thiendz.tool.fplautocms.utils.excepts.CmsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;

@Slf4j
public class ServerService {

    public static ServerService serverService;

    private static final String SERVER_ADDRESS = "https://poly.11x7.xyz";
    private static final String SERVER_API = SERVER_ADDRESS + "/api/index.php";

    private Integer appId;

    public static void start() throws CmsException, IOException {
        serverService = new ServerService();
        serverService.init();
    }

    public CourseSafetyDto getCourse(Course course) throws IOException, CmsException {
        String courseId = StringUtils.URLEncoder(course.getId());

        String url = SERVER_API + "?c=get-course&id-tool=" + appId;
        String send = "id-course=" + courseId;

        String body = postRequest(url, send);

        log.info("Request POST: {}", url);
        log.info("Request send: {}", send);
        log.info("Response: {}", body);

        CourseSafetyResponseDto courseSafetyResponseDto = MapperUtils.objectMapper.readValue(body, CourseSafetyResponseDto.class);
        if (courseSafetyResponseDto.getStatus() == 0)
            throw new CmsException("Có lỗi xảy ra trong quá trình xử lý.");
        return courseSafetyResponseDto.getData();
    }

    public Boolean pushCourse(Course course) throws IOException {
        String courseId = StringUtils.URLEncoder(course.getId());
        int totalQuiz = course.getQuizList().size();

        String url = SERVER_API + "?c=push-course&id-tool=" + appId;
        String send = "id-course=" + courseId + "&total-quiz=" + totalQuiz;

        String body = postRequest(url, send);

        log.info("Request POST: {}", url);
        log.info("Request send: {}", send);
        log.info("Response: {}", body);

        return MapperUtils.objectMapper.readValue(body, VoidResponseDto.class).getStatus() == 1;
    }

    public Boolean pushAnalysis(User user) throws IOException {
        IpInfoDto ipInfo = OsUtils.getIpInfo();
        String url = SERVER_API + "?c=push-analysis&id-tool=" + appId;
        String send = String.format("user=%s&ip=%s&city=%s&region=%s&country=%s&timezone=%s",
                user.getUsername(),
                ipInfo.getIp(),
                ipInfo.getCity(),
                ipInfo.getRegion(),
                ipInfo.getCountry(),
                ipInfo.getTimezone()
        );

        String body = postRequest(url, send);

        log.info("Request POST: {}", url);
        log.info("Request send: {}", send);
        log.info("Response: {}", body);

        return MapperUtils.objectMapper.readValue(body, VoidResponseDto.class).getStatus() == 1;
    }

    public void init() throws IOException, CmsException {

        String url = SERVER_API + "?c=get-tool-info";
        String send = "name=" + FplAutoCmsMain.APP_NAME;

        String body = postRequest(url, send);

        log.info("Request POST: {}", url);
        log.info("Request send: {}", send);
        log.info("Response: {}", body);

        CheckAppDto checkAppDto = MapperUtils.objectMapper.readValue(body, CheckAppDto.class);

        if (checkAppDto.getStatus() == 0)
            throw new CmsException("Tool đang được bảo trì, quay lại sau nhé!");

        String strVerOld = FplAutoCmsMain.APP_VER;
        String strVerNew = checkAppDto.getData().getVersion();
        if (new Version(strVerNew).compareTo(new Version(strVerOld)) > 0)
            throw new CmsException("FPLAutoCMS v" + strVerOld + " đã lỗi thời!\nĐã có phiên bản v" + strVerNew + "!\nTruy cập " + SERVER_ADDRESS + " để tải bản mới nhất!");

        appId = checkAppDto.getData().getId();
    }

    private String postRequest(String url, String data) throws IOException {
        HttpClient client = HttpClientBuilder.create()
                .disableRedirectHandling()
                .build();
        Executor executor = Executor.newInstance(client);
        Request request = Request.Post(url).bodyString(data, ContentType.APPLICATION_FORM_URLENCODED);
        return executor.execute(request)
                .returnContent()
                .asString();
    }
}
