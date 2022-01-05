package com.thiendz.tool.fplautocms.services;

import com.thiendz.tool.fplautocms.models.Course;
import com.thiendz.tool.fplautocms.models.Quiz;
import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.dto.QuizDto;
import com.thiendz.tool.fplautocms.utils.StringUtils;
import com.thiendz.tool.fplautocms.utils.ThreadUtils;
import com.thiendz.tool.fplautocms.utils.excepts.CmsException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CourseService {
    private static final String CMS_QUIZ_BASE = "https://cms.poly.edu.vn/courses/%s/course/";
    private static final String REGEX_CHAPTER_BLOCK = "chapter\\+block\\@([a-z0-9]+?)_contents";
    private static final String REGEX_SEQUENTIAL_BLOCK = "sequential\\+block\\@([a-z0-9]+?)_contents";
    private static final String REGEX_CHAPTER_BLOCK_REPLACE_ALL = "chapter\\+block\\@|_contents";
    private static final String REGEX_SEQUENTIAL_BLOCK_REPLACE_ALL = "sequential\\+block\\@|_contents";
    private static final String BASE_URL_CHANGE = "jump_to/";
    private static final String BASE_URL_CHANGE_TO = "courseware/%s/%s/1?activate_block_id=";

    private final User user;
    private Course course;

    public CourseService(User user, Course course) {
        this.user = user;
        this.course = course;
    }

    public Course getCourse() {
        return course;
    }

    public void render() throws CmsException, IOException {
        parseQuizBase();
        parseQuizReal();
    }

    private void parseQuizBase() throws IOException, CmsException {
        final String url = String.format(CMS_QUIZ_BASE, course.getId());
        final HttpClient client = HttpClientBuilder.create()
                .disableRedirectHandling()
                .build();
        final Executor executor = Executor.newInstance(client);
        final Request request = Request.Get(url).setHeader("cookie", user.getCookie());
        final String bodyQuiz = executor.execute(request)
                .returnContent()
                .asString();
        Document document = Jsoup.parse(bodyQuiz);
        Elements elmsOutlineItemSection = document.select("li[class='outline-item section']");
        List<QuizDto> quizDtoList = elmsOutlineItemSection.stream().map(element -> {
            QuizDto quizDto = new QuizDto();
            List<String> regexChapter = StringUtils.regex(REGEX_CHAPTER_BLOCK, element.html(), String.class);
            List<String> regexSequential = StringUtils.regex(REGEX_SEQUENTIAL_BLOCK, element.html(), String.class);
            Elements elms = element.select("a[class='outline-item focusable']");
            if (regexChapter.isEmpty() || regexSequential.isEmpty() || elms.isEmpty())
                return null;
            quizDto.setChapterId(regexChapter.get(0).replaceAll(REGEX_CHAPTER_BLOCK_REPLACE_ALL, ""));
            quizDto.setSequentialId(regexSequential.get(regexSequential.size() - 1).replaceAll(REGEX_SEQUENTIAL_BLOCK_REPLACE_ALL, ""));
            quizDto.setElement(elms.last());
            return quizDto;
        }).collect(Collectors.toList());
        if (quizDtoList.isEmpty()) {
            throw new CmsException("li[class='outline-item section'] không tồn tại.");
        }
        List<Quiz> quizList = quizDtoList.stream()
                .map(quizDto -> {
                    Quiz quiz = new Quiz();
                    final String urlQuiz = quizDto.getElement().attr("href");
                    final String changeTo = String.format(BASE_URL_CHANGE_TO, quizDto.getChapterId(), quizDto.getSequentialId());
                    final String urlVip = urlQuiz.replaceAll(BASE_URL_CHANGE, changeTo);
                    quiz.setUrl(urlVip);
                    return quiz;
                }).collect(Collectors.toList());
        course.setQuizList(quizList);
    }

    private void parseQuizReal() {
        List<QuizRealService> quizRealServices = course.getQuizList()
                .stream()
                .map(quiz -> new QuizRealService(user, quiz))
                .collect(Collectors.toList());
        ThreadUtils threadUtils = new ThreadUtils(quizRealServices, quizRealServices.size());
        threadUtils.execute();
        threadUtils.await();
        List<Quiz> quizRealList = quizRealServices
                .stream()
                .map(QuizRealService::getQuiz)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        course.setQuizList(quizRealList);
    }
}
