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

    @Data
    static class QuizRealService implements Runnable {

        private final User user;
        private Quiz quiz;

        public QuizRealService(User user, Quiz quiz) {
            this.user = user;
            this.quiz = quiz;
        }

        @Override
        public void run() {
            try {
                filter();
            } catch (Exception ex) {
                this.quiz = null;
            }
        }

        //
        public void filter() throws CmsException, IOException {
            final HttpClient client = HttpClientBuilder.create()
                    .disableRedirectHandling()
                    .build();
            final Executor executor = Executor.newInstance(client);
            final Request request = Request.Get(quiz.getUrl()).setHeader("cookie", user.getCookie());
            final String bodyQuiz = executor.execute(request)
                    .returnContent()
                    .asString();
            Document document = Jsoup.parse(bodyQuiz);
            Quiz quizNew = buildQuiz(document);
            quizNew.setUrl(quiz.getUrl());
            this.quiz = quizNew;
        }

        public Quiz buildQuiz(Document document) throws CmsException {
            return buildQuiz(document, false);
        }

        public Quiz buildQuiz(Document document, boolean getStatus) throws CmsException {
            //===========================
            Element elmData = document.selectFirst("div[class='seq_contents tex2jax_ignore asciimath2jax_ignore']");
            if (elmData == null) {
                throw new CmsException("build div[class='seq_contents tex2jax_ignore asciimath2jax_ignore'] is NULL!");
            }
            //tạo lại document (giải mã đoạn mã hóa)
            document = Jsoup.parse(elmData.text());
            elmData = document.selectFirst("div[class='problems-wrapper']");
            if (elmData == null) {
                throw new CmsException("build div[class='problems-wrapper'] is NULL!");
            }
            //===========================
            Quiz quiz = new Quiz();
            //setname
            Element elementNameQuiz = document.selectFirst("h2[class='hd hd-2 unit-title']");
            assert elementNameQuiz != null;
            String name = elementNameQuiz.html();
            quiz.setName(name.contains("_") ? name.substring(0, name.indexOf("_")) : name);
            //set score
            double score = Double.parseDouble(elmData.attr("data-problem-score"));
            quiz.setScore(score);
            //set score posible
            double scorePosible = Double.parseDouble(elmData.attr("data-problem-total-possible"));
            quiz.setScorePossible(scorePosible);
            //set QuizQuestion
            String content = elmData.attr("data-content");
            document = Jsoup.parse(content);
            quiz.setQuizQuestions(buildQuizQuestions(document, getStatus));
            return quiz;
        }

        public List<QuizQuestion> buildQuizQuestions(Document document) throws CmsException {
            return buildQuizQuestions(document, false);
        }

        public List<QuizQuestion> buildQuizQuestions(Document document, boolean getStatus) throws CmsException {
            //kiểu chọn
            Elements elmsPoly = document.select("div[class='poly']");
            //kiểu nhập
            Elements elmsPolyInput = document.select("div[class='poly poly-input']");
            if (elmsPoly.isEmpty() && elmsPolyInput.isEmpty()) {
                throw new CmsException("buildQuizQuestions div[class='poly'] && div[class='poly poly-input'] is Empty!");
            }
            List<QuizQuestion> alQuizQuestions = new ArrayList<>();
            //xử lý kiểu chọn trước
            for (Element elmPoly : elmsPoly) {
                Element elmWraper = elmPoly.nextElementSibling();
                //
                QuizQuestion quizQuestion = new QuizQuestion();
                quizQuestion.setName(Objects.requireNonNull(elmPoly.selectFirst("h3")).text());
                assert elmWraper != null;
                quizQuestion.setType(Objects.requireNonNull(elmWraper.selectFirst("input")).attr("type").equals("radio") ? "radio" : "checkbox");
                quizQuestion.setQuestion(Objects.requireNonNull(elmPoly.selectFirst("pre[class='poly-body']")).text());
                quizQuestion.setKey(Objects.requireNonNull(elmWraper.selectFirst("input")).attr("name"));
                try {
                    quizQuestion.setListValue(buildListValue(elmWraper));
                } catch (CmsException e) {
                    continue;
                }
                quizQuestion.setAmountInput(-1);
                quizQuestion.setMultiChoice(quizQuestion.getType().equals("checkbox"));
                quizQuestion.setCorrect(getStatus && Objects.requireNonNull(elmWraper.selectFirst("span[class='sr']")).text().equals("correct"));
                alQuizQuestions.add(quizQuestion);
            }
            //xử lý kiểu text sau
            for (Element elmPolyInput : elmsPolyInput) {
                Element elmWraper = elmPolyInput.nextElementSibling();

                QuizQuestion quizQuestion = new QuizQuestion();
                quizQuestion.setName(Objects.requireNonNull(elmPolyInput.selectFirst("h3")).text());
                quizQuestion.setType("text");
                quizQuestion.setQuestion(Objects.requireNonNull(elmPolyInput.selectFirst("pre")).text());
                assert elmWraper != null;
                quizQuestion.setKey(Objects.requireNonNull(elmWraper.selectFirst("input")).attr("name"));
                try {
                    quizQuestion.setListValue(buildListValueText(elmPolyInput));
                } catch (CmsException | NullPointerException e) {
                    //not continue;
                }
                quizQuestion.setAmountInput(elmPolyInput.select("input").size());
                quizQuestion.setMultiChoice(quizQuestion.getAmountInput() > 1);
                quizQuestion.setCorrect(getStatus && Objects.requireNonNull(elmWraper.selectFirst("span[class='sr']")).text().equals("correct"));
                alQuizQuestions.add(quizQuestion);
            }
            return alQuizQuestions;
        }

        private List<String> buildListValueText(Element elmPolyInput) throws CmsException {
            Element elmData = elmPolyInput.selectFirst("div[class='data']");
            if (elmData == null) {
                throw new CmsException("buildListValueText div[class='data'] is NULL!");
            }
            ObjectMapper objectMapper = new ObjectMapper();
            QuizQuestionListDto quizQuestionListDto = objectMapper.convertValue(elmData.text(), QuizQuestionListDto.class);
            return quizQuestionListDto.getQuestions().stream().map(QuizQuestionTextDto::getText).collect(Collectors.toList());
        }

        private List<String> buildListValue(Element elmWrapper) throws CmsException {
            Elements elmsInput = elmWrapper.select("input");
            if (elmsInput.isEmpty()) {
                throw new CmsException("buildListValue input is empty");
            }
            return elmsInput.stream().map(element -> element.attr("value")).collect(Collectors.toList());
        }
    }

}
