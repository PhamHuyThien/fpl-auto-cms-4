package com.thiendz.tool.fplautocms.services;

import com.thiendz.tool.fplautocms.dto.*;
import com.thiendz.tool.fplautocms.models.Course;
import com.thiendz.tool.fplautocms.models.Quiz;
import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.models.Version;
import com.thiendz.tool.fplautocms.utils.MapperUtils;
import com.thiendz.tool.fplautocms.utils.NumberUtils;
import com.thiendz.tool.fplautocms.utils.OsUtils;
import com.thiendz.tool.fplautocms.utils.StringUtils;
import com.thiendz.tool.fplautocms.utils.consts.Environments;
import com.thiendz.tool.fplautocms.utils.consts.Messages;
import com.thiendz.tool.fplautocms.utils.enums.QuizQuestionType;
import com.thiendz.tool.fplautocms.utils.excepts.CmsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
public class ServerService {

    public static ServerService serverService;

    private static final String SERVER_ADDRESS = Environments.SERVER_ADDRESS;
    private static final String SERVER_API = SERVER_ADDRESS + "/api/index.php";

    private Integer appId;

    public static void start() throws CmsException, IOException {
        serverService = new ServerService();
        serverService.init();
    }

    public GetQuizQuestionDto getQuizQuestion(Course course, Quiz quiz) throws IOException {
        if (Environments.DISABLE_ANALYSIS) {
            GetQuizQuestionDto getQuizQuestionDto = new GetQuizQuestionDto();
            getQuizQuestionDto.setStatus(0);
            getQuizQuestionDto.setMsg("Disable analysis.");
            return getQuizQuestionDto;
        }
        Integer quizNumber = NumberUtils.getInt(quiz.getName());
        String name = StringUtils.md5(course.getId()) + "_" + (quizNumber == null ? "FT" : quizNumber);

        String url = SERVER_API + "?c=get-quiz-question&course_md5_id=" + name;

        String body = postRequest(url, "");
        log.info("Request GET: {}", url);
        log.info("Response: {}", body);

        return MapperUtils.objectMapper.readValue(body, GetQuizQuestionDto.class);
    }

    public Boolean pushQuizQuestion(Course course, Quiz quiz) throws IOException {
        if (Environments.DISABLE_ANALYSIS)
            return false;
        List<String> quizParamPost = quiz.getQuizQuestions().stream()
                .map(quizQuestion -> {
                    String param;
                    if (!quizQuestion.isCorrect() || quizQuestion.getValue() == null)
                        return null;
                    List<String> valueEncryptList = quizQuestion.getValue()
                            .stream().map(StringUtils::URLEncoder)
                            .collect(Collectors.toList());
                    String keyEncrypt = StringUtils.URLEncoder(quizQuestion.getKey());
                    if (quizQuestion.getType() == QuizQuestionType.TEXT)
                        param = keyEncrypt + "=" + String.join("%2C", valueEncryptList);
                    else
                        param = valueEncryptList
                                .stream().map(s -> keyEncrypt + "=" + s)
                                .collect(Collectors.joining("&"));
                    return param;
                }).filter(Objects::nonNull).collect(Collectors.toList());
        Integer quizNumber = NumberUtils.getInt(quiz.getName());
        String name = StringUtils.md5(course.getId()) + "_" + (quizNumber == null ? "FT" : quizNumber);
        String data = String.join("&", quizParamPost);

        if(data.equals(""))
            return false;

        String url = SERVER_API + "?c=push-quiz-question&course_md5_id=" + name;
        String send = "data_b64=" + StringUtils.b64Encode(data);

        String body = postRequest(url, send);
        log.info("Request POST: {}", url);
        log.info("Request send: {}", send);
        log.info("Response: {}", body);

        return MapperUtils.objectMapper.readValue(body, VoidResponseDto.class).getStatus() == 1;
    }

    public CourseSafetyDto getCourse(Course course) throws IOException, CmsException {
        if (Environments.DISABLE_ANALYSIS)
            return new CourseSafetyDto(-1, 9999);

        String courseId = StringUtils.URLEncoder(course.getId());

        String url = SERVER_API + "?c=get-course&id-tool=" + appId;
        String send = "id-course=" + courseId;

        String body = postRequest(url, send);

        log.info("Request POST: {}", url);
        log.info("Request send: {}", send);
        log.info("Response: {}", body);

        CourseSafetyResponseDto courseSafetyResponseDto = MapperUtils.objectMapper.readValue(body, CourseSafetyResponseDto.class);
        if (courseSafetyResponseDto.getStatus() == 0)
            throw new CmsException(Messages.AN_ERROR_OCCURRED);
        return courseSafetyResponseDto.getData();
    }

    public Boolean pushCourse(Course course) throws IOException {
        if (Environments.DISABLE_ANALYSIS)
            return false;

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
        if (Environments.DISABLE_ANALYSIS)
            return false;

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
        if (Environments.DISABLE_ANALYSIS)
            return;

        String url = SERVER_API + "?c=get-tool-info";
        String send = "name=" + Messages.APP_NAME;

        String body = postRequest(url, send);
        log.info("Request POST: {}", url);
        log.info("Request send: {}", send);
        log.info("Response: {}", body);

        CheckAppDto checkAppDto = MapperUtils.objectMapper.readValue(body, CheckAppDto.class);

        if (checkAppDto.getStatus() == 0)
            throw new CmsException(Messages.TOOL_MAINTAIN);

        String strVerOld = Messages.APP_VER;
        String strVerNew = checkAppDto.getData().getVersion();
        if (new Version(strVerNew).compareTo(new Version(strVerOld)) > 0)
            throw new CmsException(String.format(Messages.VERSION_OLD, strVerOld, strVerNew, SERVER_ADDRESS));

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
