package com.thiendz.tool.fplautocms.services;

import com.thiendz.tool.fplautocms.dto.callback.CallbackSolution;
import com.thiendz.tool.fplautocms.models.Course;
import com.thiendz.tool.fplautocms.models.Quiz;
import com.thiendz.tool.fplautocms.models.QuizQuestion;
import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.dto.SolutionResponseDto;
import com.thiendz.tool.fplautocms.utils.*;
import com.thiendz.tool.fplautocms.utils.enums.QuizQuestionType;
import com.thiendz.tool.fplautocms.utils.excepts.CmsException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class SolutionService implements Runnable {

    private static final String URL_POST_BASE = "https://cms.poly.edu.vn/courses/%s/xblock/%s+type@problem+block@%s/handler/xmodule_handler/problem_check";
    private static final int TIME_SLEEP_SOLUTION = 60000;

    private final User user;
    private final Course course;
    private final Quiz quiz;

    private CallbackSolution callbackSolution;

    private int status;
    private double scorePresent;

    public SolutionService(User user, Course course, Quiz quiz) {
        this.user = user;
        this.course = course;
        this.quiz = quiz;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public double getScorePresent() {
        return scorePresent;
    }

    public int getStatus() {
        return status;
    }

    public void setCallbackSolution(CallbackSolution callbackSolution) {
        this.callbackSolution = callbackSolution;
    }

    @Override
    public void run() {
        try {
            start();
            setStatus(1);
        } catch (CmsException ex) {
            setStatus(-1);
        } catch (IOException ex) {
            ex.printStackTrace();
            setStatus(-2);
        }
    }

    public void start() throws CmsException, IOException {
        if (isQuizFinished()) {
            return;
        }
        resetQuizQuestion();
        final String url = String.format(
                URL_POST_BASE,
                course.getId(),
                course.getId().replace("course", "block"),
                quiz.getQuizQuestions().get(0).getKey().split("_")[1]
        );
        scorePresent = quiz.getScore();
        long timeTick = 0;
        do {
            if (DateUtils.getCurrentMilis() - timeTick > TIME_SLEEP_SOLUTION) {
                final HttpClient client = HttpClientBuilder.create()
                        .disableRedirectHandling()
                        .build();
                final Executor executor = Executor.newInstance(client);
                final Request request = Request.Post(url)
                        .setHeader("X-CSRFToken", user.getCsrf_token())
                        .setHeader("Referer", quiz.getUrl())
                        .setHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                        .setHeader("Cookie", user.getCookie())
                        .setHeader("User-Agent", "Auto By ThienDepTrai.")
                        .bodyString(buildParam(), ContentType.create("application/x-www-form-urlencoded", StandardCharsets.UTF_8));
                final String bodyResponseSolution = executor.execute(request)
                        .returnContent()
                        .asString();
                SolutionResponseDto solutionResponseDto = MapperUtils.objectMapper.readValue(bodyResponseSolution, SolutionResponseDto.class);
                scorePresent = solutionResponseDto.getCurrent_score();
                setStatus(0);
                updateStatusQuizQuestion(solutionResponseDto.getContents());
                timeTick = DateUtils.getCurrentMilis();
            }
            ThreadUtils.sleep(1000);
        } while (!isQuizFinished());
    }


    private String buildParam() throws CmsException {
        List<QuizQuestion> quizQuestionList = quiz.getQuizQuestions();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < quizQuestionList.size(); i++) {
            QuizQuestion quizQuestion = quizQuestionList.get(i);
            if (quizQuestion.getListValue() == null) {
                continue;
            }
            String ans = setValue(quizQuestionList.get(i));
            sb.append(ans).append("&");
            if (!quizQuestion.isCorrect()) {
                quiz.getQuizQuestions().get(i).setTestCount(quiz.getQuizQuestions().get(i).getTestCount() + 1);
            }
            quiz.getQuizQuestions().get(i).setSelectValue(ans);
        }
        return makeUpValue(sb.toString());
    }

    private String setValue(QuizQuestion quizQuestion) throws CmsException {
        if (quizQuestion.isCorrect()) {
            return quizQuestion.getSelectValue();
        }
        String keyEncrypt = StringUtils.URLEncoder(quizQuestion.getKey());
        if (quizQuestion.isMultiChoice()) {
            StringBuilder value = new StringBuilder();
            List<List<Integer>> alInt;
            if (quizQuestion.getType() == QuizQuestionType.TEXT) {
                alInt = new Permutation(quizQuestion.getAmountInput(), quizQuestion.getListValue().size()).getResult();
            } else {
                alInt = new Combination(2, quizQuestion.getListValue().size(), true).getResult();
            }
            int index = quizQuestion.getTestCount();
            if (index >= alInt.size()) {
                throw new CmsException("ArrayIndexOutOfBound alInt!");
            }
            if (quizQuestion.getType() == QuizQuestionType.TEXT) {
                value.append(keyEncrypt).append("=");
            }
            alInt.get(index).forEach((i) -> {
                String valueEncrypt = StringUtils.URLEncoder(quizQuestion.getListValue().get(i));
                if (quizQuestion.getType() == QuizQuestionType.TEXT) {
                    value.append(valueEncrypt).append("%2C");
                } else {
                    value.append(keyEncrypt).append("=").append(valueEncrypt).append("&");
                }
            });
            return makeUpValue(value.toString());
        } else {
            List<String> choiceList = quizQuestion.getListValue();
            String valueEncrypt = StringUtils.URLEncoder(choiceList.get(quizQuestion.getTestCount()));
            String res = keyEncrypt + "=" + valueEncrypt + "&";
            return makeUpValue(res);
        }
    }

    private void updateStatusQuizQuestion(String body) throws CmsException {
        Document document = Jsoup.parse(body);
        List<QuizQuestion> quizResults = QuizDetailService.buildQuizQuestions(document, true);
        if (!compareKeyQuizQuestion(quiz.getQuizQuestions(), quizResults)) {
            throw new CmsException("QuizResult != QuizStandard!");
        }
        for (int i = 0; i < quizResults.size(); i++) {
            quiz.getQuizQuestions().get(i).setCorrect(quizResults.get(i).isCorrect());
        }
    }

    private boolean compareKeyQuizQuestion(List<QuizQuestion> quizQuestionList1, List<QuizQuestion> quizQuestionList2) {
        if (quizQuestionList1.size() != quizQuestionList2.size())
            return false;
        for (int i = 0; i < quizQuestionList1.size(); i++) {
            if (!quizQuestionList1.get(i).getKey().equals(quizQuestionList2.get(i).getKey()))
                return false;
        }
        return true;
    }

    private String makeUpValue(String value) {
        int len = value.length();
        if (value.endsWith("&")) {
            return value.substring(0, len - 1);
        }
        if (value.endsWith("%2C")) {
            return value.substring(0, len - 3);
        }
        return value;
    }

    private boolean isQuizFinished() {
        List<QuizQuestion> quizQuestionList = quiz.getQuizQuestions();
        return quizQuestionList.stream()
                .filter(quizQuestion -> quizQuestion.isCorrect() || quizQuestion.getListValue() == null)
                .count() == quizQuestionList.size();
    }

    private void resetQuizQuestion() {
        for (QuizQuestion quizQuestion : quiz.getQuizQuestions()) {
            quizQuestion.setCorrect(false);
        }
    }

    private void setStatus(int status) {
        this.status = status;
        callbackSolution.call(scorePresent, status, quiz);
    }
}

