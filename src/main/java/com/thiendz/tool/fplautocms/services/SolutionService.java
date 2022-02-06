package com.thiendz.tool.fplautocms.services;

import com.thiendz.tool.fplautocms.models.Course;
import com.thiendz.tool.fplautocms.models.Quiz;
import com.thiendz.tool.fplautocms.models.QuizQuestion;
import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.dto.SolutionResponseDto;
import com.thiendz.tool.fplautocms.utils.*;
import com.thiendz.tool.fplautocms.utils.consts.UserAgent;
import com.thiendz.tool.fplautocms.utils.enums.QuizQuestionType;
import com.thiendz.tool.fplautocms.utils.excepts.CmsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class SolutionService implements Runnable {

    private static final String URL_POST_BASE = "https://cms.poly.edu.vn/courses/%s/xblock/%s+type@problem+block@%s/handler/xmodule_handler/problem_check";
    private static final int TIME_SLEEP_SOLUTION = 60000;

    private final User user;
    private final Course course;
    private final Quiz quiz;
    private String paramPost;
    private boolean resetScoreQuiz;

    private int status;
    private Double scorePresent;

    public SolutionService(User user, Course course, Quiz quiz) {
        this.user = user;
        this.course = course;
        this.quiz = quiz;
        this.scorePresent = quiz.getScore();
    }

    public void setResetScoreQuiz(boolean resetScoreQuiz) {
        this.resetScoreQuiz = resetScoreQuiz;
    }

    public void setParamPost(String paramPost) {
        this.paramPost = paramPost;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public String getParamPost() {
        return paramPost;
    }

    public double getScorePresent() {
        return scorePresent;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public void run() {
        try {
            start();
            setStatus(1);
            log.info("Quiz [{}] - score: [{}] solution finish.", quiz.getName(), scorePresent);
        } catch (CmsException | IOException | NullPointerException e) {
            setStatus(-1);
            log.error(e.toString(), e);
        }
    }

    public void start() throws CmsException, IOException, NullPointerException {
        if (isQuizFinished()) {
            return;
        }
        long timeTick = 0;
        resetQuizQuestion();
        final String url = String.format(
                URL_POST_BASE,
                course.getId(),
                course.getId().replace("course", "block"),
                quiz.getQuizQuestions().get(0).getKey().split("_")[1]
        );
        do {
            if (DateUtils.getCurrentMilis() - timeTick > TIME_SLEEP_SOLUTION) {
                String bodyParam = paramPost == null ? buildParam() : paramPost;
                RequestConfig requestConfig = RequestConfig.custom()
                        .setConnectTimeout(60000)
                        .build();
                HttpClient client = HttpClientBuilder.create()
                        .setDefaultRequestConfig(requestConfig)
                        .disableRedirectHandling()
                        .build();
                Executor executor = Executor.newInstance(client);
                Request request = Request.Post(url)
                        .setHeader("X-CSRFToken", user.getCsrf_token())
                        .setHeader("Referer", quiz.getUrl())
                        .setHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                        .setHeader("Cookie", user.getCookie())
                        .setHeader("User-Agent", UserAgent.USER_AGENT_DEFAULT)
                        .bodyString(bodyParam, ContentType.create("application/x-www-form-urlencoded", StandardCharsets.UTF_8));
                String bodyResponseSolution = executor.execute(request).returnContent().asString();
                SolutionResponseDto solutionResponseDto = MapperUtils.objectMapper.readValue(bodyResponseSolution, SolutionResponseDto.class);
                setScorePresent(solutionResponseDto.getCurrent_score());
                updateStatusQuizQuestion(solutionResponseDto);
                log.info("Quiz: {}", quiz.getName());
                log.info("Request POST: {}", url);
                log.info("Request Send: {}", bodyParam);
                log.info("Response: {}", bodyResponseSolution);
                log.info("Score: {}", scorePresent);
                timeTick = DateUtils.getCurrentMilis();
                setStatus(0);
            }
            ThreadUtils.sleep(1000);
        } while (!isQuizFinished());
    }


    private String buildParam() throws CmsException {
        List<QuizQuestion> quizQuestionList = quiz.getQuizQuestions();
        List<String> paramList = new ArrayList<>();
        for (int i = 0; i < quizQuestionList.size(); i++) {
            QuizQuestion quizQuestion = quizQuestionList.get(i);
            if (quizQuestion.getListValue() == null)
                continue;
            List<String> selectNextValueList = selectNextValue(quizQuestion);
            List<String> selectNextValueEncryptList = selectNextValueList
                    .stream().map(StringUtils::URLEncoder)
                    .collect(Collectors.toList());
            String param;
            String keyEncrypt = StringUtils.URLEncoder(quizQuestion.getKey());
            if (quizQuestion.getType() == QuizQuestionType.TEXT)
                param = keyEncrypt + "=" + String.join("%2C", selectNextValueEncryptList);
            else
                param = selectNextValueEncryptList
                        .stream().map(s -> keyEncrypt + "=" + s)
                        .collect(Collectors.joining("&"));
            if (!quizQuestion.isCorrect()) {
                quiz.getQuizQuestions().get(i).setTest(quizQuestion.getTest() + 1);
                quiz.getQuizQuestions().get(i).setValue(selectNextValueList);
            }
            paramList.add(param);
        }
        return String.join("&", paramList);
    }

    private List<String> selectNextValue(QuizQuestion quizQuestion) throws CmsException {
        if (quizQuestion.isCorrect())
            return quizQuestion.getValue();
        int next = quizQuestion.getTest();
        List<String> nextValueList = new ArrayList<>();
        if (quizQuestion.isMultiChoice()) {
            List<List<Integer>> selectList;
            if (quizQuestion.getType() == QuizQuestionType.TEXT)
                selectList = new Permutation(quizQuestion.getInput(), quizQuestion.getListValue().size()).getResult();
            else
                selectList = new Combination(2, quizQuestion.getListValue().size(), true).getResult();
            if (next > selectList.size() - 1)
                throw new CmsException("[M]Câu trả lời nằm ngoài danh sách đáp án có sẵn.");
            selectList.get(next).forEach(select -> nextValueList.add(quizQuestion.getListValue().get(select)));
        } else {
            if (next > quizQuestion.getListValue().size() - 1)
                throw new CmsException("[S]Câu trả lời nằm ngoài danh sách đáp án có sẵn.");
            nextValueList.add(quizQuestion.getListValue().get(next));
        }
        return nextValueList;
    }


    private void updateStatusQuizQuestion(SolutionResponseDto solutionResponseDto) throws CmsException {
        if (solutionResponseDto.getContents() == null)
            throw new CmsException(solutionResponseDto.getSuccess());
        Document document = Jsoup.parse(solutionResponseDto.getContents());
        List<QuizQuestion> quizResults = QuizDetailService.buildQuizQuestions(document, true);
        if (!compareKeyQuizQuestion(quiz.getQuizQuestions(), quizResults))
            throw new CmsException("Dữ liệu trả về không khớp dữ liệu tại máy.");
        for (int i = 0; i < quizResults.size(); i++)
            quiz.getQuizQuestions().get(i).setCorrect(quizResults.get(i).isCorrect());
    }

    private boolean compareKeyQuizQuestion(List<QuizQuestion> quizQuestionList1, List<QuizQuestion> quizQuestionList2) {
        if (quizQuestionList1.size() != quizQuestionList2.size())
            return false;
        for (int i = 0; i < quizQuestionList1.size(); i++)
            if (!quizQuestionList1.get(i).getKey().equals(quizQuestionList2.get(i).getKey()))
                return false;
        return true;
    }

    private boolean isQuizFinished() {
        List<QuizQuestion> quizQuestionList = quiz.getQuizQuestions();
        boolean maxScore = false;
        if (!resetScoreQuiz)
            maxScore = quiz.getScore() == quiz.getScorePossible() && quiz.getScore() != 0;
        boolean quizFinish = quizQuestionList.stream()
                .filter(quizQuestion -> quizQuestion.isCorrect() || quizQuestion.getListValue() == null)
                .count() == quizQuestionList.size();
        return maxScore || quizFinish;
    }

    private void resetQuizQuestion() {
        for (int i = 0; i < quiz.getQuizQuestions().size(); i++) {
            quiz.getQuizQuestions().get(i).setCorrect(false);
            quiz.getQuizQuestions().get(i).setTest(0);
            quiz.getQuizQuestions().get(i).setValue(null);
        }
    }

    private void setStatus(int status) {
        this.status = status;
    }

    private void setScorePresent(Double scorePresent) {
        this.scorePresent = scorePresent;
        quiz.setScore(scorePresent);
    }
}

