package com.thiendz.tool.fplautocms.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.thiendz.tool.fplautocms.data.models.Course;
import com.thiendz.tool.fplautocms.data.models.Quiz;
import com.thiendz.tool.fplautocms.data.models.QuizQuestion;
import com.thiendz.tool.fplautocms.data.models.User;
import com.thiendz.tool.fplautocms.dto.QuizDto;
import com.thiendz.tool.fplautocms.dto.QuizQuestionListDto;
import com.thiendz.tool.fplautocms.dto.QuizQuestionTextDto;
import com.thiendz.tool.fplautocms.utils.MsgBoxUtils;
import com.thiendz.tool.fplautocms.utils.StringUtils;
import com.thiendz.tool.fplautocms.utils.ThreadUtils;
import com.thiendz.tool.fplautocms.utils.consts.Messages;
import com.thiendz.tool.fplautocms.utils.excepts.InputException;
import com.thiendz.tool.fplautocms.views.DashboardView;
import com.thiendz.tool.fplautocms.utils.excepts.CmsException;
import lombok.Data;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class CourseService implements Runnable {
    private static final String CMS_QUIZ_BASE = "https://cms.poly.edu.vn/courses/%s/course/";
    private static final String REGEX_CHAPTER_BLOCK = "chapter\\+block\\@([a-z0-9]+?)_contents";
    private static final String REGEX_SEQUENTIAL_BLOCK = "sequential\\+block\\@([a-z0-9]+?)_contents";
    private static final String REGEX_CHAPTER_BLOCK_REPLACE_ALL = "chapter\\+block\\@|_contents";
    private static final String REGEX_SEQUENTIAL_BLOCK_REPLACE_ALL = "sequential\\+block\\@|_contents";
    private static final String BASE_URL_CHANGE = "jump_to/";
    private static final String BASE_URL_CHANGE_TO = "courseware/%s/%s/1?activate_block_id=";

    private final DashboardView dashboardView;
    private final int courseSelectedIndex;
    private final User user;
    private Course course;

    public static void start(DashboardView dashboardView) {
        new Thread(new CourseService(dashboardView)).start();
    }

    public CourseService(DashboardView dashboardView) {
        this.dashboardView = dashboardView;
        this.courseSelectedIndex = dashboardView.getCbbCourse().getSelectedIndex();
        this.user = dashboardView.getUser();
    }

    @Override
    public void run() {
        try {
            dashboardView.getCbbCourse().setEnabled(false);
            checkValidInput();
            parseQuizBase();
            parseQuizReal();
            showDashboard();
            dashboardView.getCbbQuiz().setEnabled(true);
            dashboardView.getBtnSolution().setEnabled(true);
            dashboardView.getCbbQuiz().setSelectedIndex(dashboardView.getCbbQuiz().getItemCount() - 1);
            dashboardView.getUser().getCourses().set(courseSelectedIndex, course);
        } catch (InputException e) {
            return;
        } catch (IOException e) {
            MsgBoxUtils.alert(dashboardView, Messages.CONNECT_ERROR);
        } catch (CmsException e) {
            MsgBoxUtils.alert(dashboardView, e.toString());
        } catch (Exception e) {
            MsgBoxUtils.alert(dashboardView, Messages.AN_ERROR_OCCURRED + e);
        }
        dashboardView.getCbbCourse().setEnabled(true);
    }

    private void checkValidInput() throws InputException {
        if (courseSelectedIndex <= 0) {
            throw new InputException(Messages.YOU_CHOOSE_QUIZ);
        }
        course = dashboardView.getUser().getCourses().get(courseSelectedIndex - 1);
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
            throw new CmsException("li[class='outline-item section'] is empty!");
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

    private void showDashboard() {
        dashboardView.getCbbQuiz().removeAllItems();
        dashboardView.getCbbQuiz().addItem("Select Quiz...");
        course.getQuizList().forEach(quiz -> {
            dashboardView.getCbbQuiz().addItem(quiz.getName() + " - " + ((int) quiz.getScore()) + "/" + ((int) quiz.getScorePossible()) + " point");
        });
        dashboardView.getCbbQuiz().addItem("Auto all quiz");
    }

}
