package com.thiendz.tool.fplautocms.services;

import com.thiendz.tool.fplautocms.dto.callback.CallbackSolution;
import com.thiendz.tool.fplautocms.models.Course;
import com.thiendz.tool.fplautocms.models.Quiz;
import com.thiendz.tool.fplautocms.models.QuizQuestion;
import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.dto.SolutionResponseDto;
import com.thiendz.tool.fplautocms.utils.*;
import com.thiendz.tool.fplautocms.utils.excepts.CmsException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.fluent.Executor;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.List;

public class SolutionService implements Runnable {

    private static final String URL_POST_BASE = "https://cms.poly.edu.vn/courses/%s/xblock/%s+type@problem+block@%s/handler/xmodule_handler/problem_check";
    private static final int TIME_SLEEP_SOLUTION = 60000;

    private final User user;
    private final Course course;
    private final Quiz quiz;

    private CallbackSolution callbackSolution;

    private double scorePresent;

    public SolutionService(User user, Course course, Quiz quiz) {
        this.user = user;
        this.course = course;
        this.quiz = quiz;
    }

    public void setCallbackSolution(CallbackSolution callbackSolution) {
        this.callbackSolution = callbackSolution;
    }

    @Override
    public void run() {
        try {
            start();
        } catch (CmsException ex) {
            callbackSolution.call(scorePresent, -1);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void start() throws CmsException, IOException {
        //đã đủ điểm
        if (isQuizFinished(quiz)) {
            callbackSolution.call(scorePresent, 1);
            return;
        }
        resetQuizQuestion(quiz);
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
                        .bodyString(buildParam(), ContentType.APPLICATION_FORM_URLENCODED);
                final String bodyResponseSolution = executor.execute(request)
                        .returnContent()
                        .asString();
                SolutionResponseDto solutionResponseDto = MapperUtils.objectMapper.readValue(bodyResponseSolution, SolutionResponseDto.class);
                scorePresent = solutionResponseDto.getCurrent_score();
                callbackSolution.call(scorePresent, 0);
                updateStatusQuizQuestion(solutionResponseDto.getContents(), quiz);
                timeTick = DateUtils.getCurrentMilis();
            }
            ThreadUtils.sleep(100);
        } while (!isQuizFinished(quiz));
        callbackSolution.call(scorePresent, 1);
    }


    private String buildParam() throws CmsException {
        List<QuizQuestion> quizQuestionList = quiz.getQuizQuestions();
        StringBuilder sb = new StringBuilder();
        //ghép các parampost từ quizQuestion, thành paramPost Full
        for (int i = 0; i < quizQuestionList.size(); i++) {
            QuizQuestion quizQuestion = quizQuestionList.get(i);
            //câu hỏi này là câu hỏi tự luận thì bỏ qua
            if (quizQuestion.getListValue() == null) {
                continue;
            }
            String ans = setValue(quizQuestionList.get(i));
            sb.append(ans).append("&");
            if (!quizQuestion.isCorrect()) { //hoàn thành rồi thì bỏ qua tăng test
                quiz.getQuizQuestions().get(i).setTestCount(quiz.getQuizQuestions().get(i).getTestCount() + 1);
            }
            quiz.getQuizQuestions().get(i).setSelectValue(ans); // set đáp án
        }
        return makeUpValue(sb.toString());
    }

    //convert quizQuestion sang paramPost, tự động ++ giá trị tiếp theo cho quizQUestion
    private String setValue(QuizQuestion quizQuestion) throws CmsException {
        //đã hoàn thành thì chỉ lấy getAnswer()
        if (quizQuestion.isCorrect()) {
            return quizQuestion.getSelectValue();
        }
        // đây là kiểu multichoice
        if (quizQuestion.isMultiChoice()) {
            //tạo mới biến global alInt chứa danh sách tổ hợp chập k của n phần tử
            List<List<Integer>> alInt;
            if (quizQuestion.getType().equals("text")) {
                alInt = new Permutation(quizQuestion.getAmountInput(), quizQuestion.getListValue().size()).getResult();
            } else {
                alInt = new Combination(2, quizQuestion.getListValue().size(), true).getResult();
            }

            StringBuilder value = new StringBuilder();
            int index = quizQuestion.getTestCount();
            //nếu vượt quá index tổ hợp
            if (index >= alInt.size()) {
                throw new CmsException("setValue ArrayIndexOutOfBound alInt!");
            }
            //nếu là text: định dạng key=value1,value2...
            if (quizQuestion.getType().equals("text")) {
                value.append(quizQuestion.getKey()).append("=");
            }
            alInt.get(index).forEach((i) -> {
                if (quizQuestion.getType().equals("text")) {
                    // kiểu text chỉ việc append value1,value2...
                    value.append(StringUtils.URLEncoder(quizQuestion.getListValue().get(i))).append("%2C");
                } else {
                    // kiểu checkbox => key[]=value1&key[]=value2.....
                    value.append(StringUtils.URLEncoder(quizQuestion.getKey())).append("=").append(StringUtils.URLEncoder(quizQuestion.getListValue().get(i))).append("&");
                }
            });
            //xóa kí tự nối cuối và return quizQuestion
            return makeUpValue(value.toString());
        } else { // đây là kiểu chọn 1 đáp án
            List<String> choiceList = quizQuestion.getListValue();
            String key = StringUtils.URLEncoder(quizQuestion.getKey());
            String value = StringUtils.URLEncoder(choiceList.get(quizQuestion.getTestCount()));
            //định dạng: key=value
            String res = key + "=" + value + "&";
            return makeUpValue(res);
        }
    }

    //kiểm tra giá trị đầu vào và setCorrect lại cho mỗi quizQuestion
    private void updateStatusQuizQuestion(String body, Quiz quiz) throws CmsException {
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

    private boolean isQuizFinished(Quiz quiz) {
        List<QuizQuestion> quizQuestionList = quiz.getQuizQuestions();
        return quizQuestionList.stream()
                .filter(quizQuestion -> quizQuestion.isCorrect() || quizQuestion.getListValue() == null)
                .count() == quizQuestionList.size();
    }

    private void resetQuizQuestion(Quiz quiz) {
        for (QuizQuestion quizQuestion : quiz.getQuizQuestions()) {
            quizQuestion.setCorrect(false);
        }
    }
}

