package com.thiendz.tool.fplautocms.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.thiendz.tool.fplautocms.models.Quiz;
import com.thiendz.tool.fplautocms.models.QuizQuestion;
import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.dto.QuizQuestionListDto;
import com.thiendz.tool.fplautocms.dto.QuizQuestionTextDto;
import com.thiendz.tool.fplautocms.utils.MapperUtils;
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

@Data
public class QuizRealService implements Runnable {

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

    public static Quiz buildQuiz(Document document) throws CmsException {
        return buildQuiz(document, false);
    }

    public static Quiz buildQuiz(Document document, boolean getStatus) throws CmsException {
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

    public static List<QuizQuestion> buildQuizQuestions(Document document) throws CmsException {
        return buildQuizQuestions(document, false);
    }

    public static List<QuizQuestion> buildQuizQuestions(Document document, boolean getStatus) throws CmsException {
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
            } catch (CmsException | NullPointerException | JsonProcessingException e) {
                //not continue;
            }
            quizQuestion.setAmountInput(elmPolyInput.select("input").size());
            quizQuestion.setMultiChoice(quizQuestion.getAmountInput() > 1);
            quizQuestion.setCorrect(getStatus && Objects.requireNonNull(elmWraper.selectFirst("span[class='sr']")).text().equals("correct"));
            alQuizQuestions.add(quizQuestion);
        }
        return alQuizQuestions;
    }

    private static List<String> buildListValueText(Element elmPolyInput) throws CmsException, JsonProcessingException {
        Element elmData = elmPolyInput.selectFirst("div[class='data']");
        if (elmData == null) {
            throw new CmsException("buildListValueText div[class='data'] is NULL!");
        }
        QuizQuestionListDto quizQuestionListDto = MapperUtils.objectMapper.readValue(elmData.text(), QuizQuestionListDto.class);
        return quizQuestionListDto.getQuestions().stream().map(QuizQuestionTextDto::getText).collect(Collectors.toList());
    }

    private static List<String> buildListValue(Element elmWrapper) throws CmsException {
        Elements elmsInput = elmWrapper.select("input");
        if (elmsInput.isEmpty()) {
            throw new CmsException("buildListValue input is empty");
        }
        return elmsInput.stream().map(element -> element.attr("value")).collect(Collectors.toList());
    }
}