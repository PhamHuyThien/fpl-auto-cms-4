package com.thiendz.tool.fplautocms.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.thiendz.tool.fplautocms.models.Quiz;
import com.thiendz.tool.fplautocms.models.QuizQuestion;
import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.dto.QuizQuestionTextDto;
import com.thiendz.tool.fplautocms.utils.MapperUtils;
import com.thiendz.tool.fplautocms.utils.StringUtils;
import com.thiendz.tool.fplautocms.utils.enums.QuizQuestionType;
import com.thiendz.tool.fplautocms.utils.excepts.CmsException;
import lombok.extern.slf4j.Slf4j;
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
import java.util.stream.Stream;

@Slf4j
public class QuizDetailService implements Runnable {

    private final User user;
    private Quiz quiz;

    public QuizDetailService(User user, Quiz quiz) {
        this.user = user;
        this.quiz = quiz;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    @Override
    public void run() {
        try {
            filter();
            log.info("Request GET: {}", quiz.getUrl());
            log.info("Quiz: {} => {}", quiz.getName(), quiz.toString());
        } catch (Exception ex) {
            this.quiz = null;
            log.error("Check quiz thất bại: {}", ex.toString());
        }
    }

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
            throw new CmsException("div[class='seq_contents tex2jax_ignore asciimath2jax_ignore'] không tồn tại.");
        }
        //tạo lại document (giải mã đoạn mã hóa)
        document = Jsoup.parse(elmData.text());
        elmData = document.selectFirst("div[class='problems-wrapper']");
        if (elmData == null) {
            throw new CmsException("div[class='problems-wrapper'] không tồn tại.");
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
        double scorePossible = Double.parseDouble(elmData.attr("data-problem-total-possible"));
        quiz.setScorePossible(scorePossible);
        //set QuizQuestion
        String content = elmData.attr("data-content");
        document = Jsoup.parse(content);
        quiz.setQuizQuestions(buildQuizQuestions(document, getStatus));
        return quiz;
    }

    public static List<QuizQuestion> buildQuizQuestions(Document document, boolean getStatus) throws CmsException {
        //kiểu chọn
        Elements elmsPoly = document.select("div[class='poly']");
        //kiểu nhập
        Elements elmsPolyInput = document.select("div[class='poly poly-input']");
        if (elmsPoly.isEmpty() && elmsPolyInput.isEmpty()) {
            throw new CmsException("div[class='poly'] && div[class='poly poly-input'] không có phần tử nào.");
        }
        List<QuizQuestion> alQuizQuestions = new ArrayList<>();
        //xử lý kiểu chọn trước
        for (Element elmPoly : elmsPoly) {
            Element elmWrapper = elmPoly.nextElementSibling();
            //
            QuizQuestion quizQuestion = new QuizQuestion();
            quizQuestion.setName(Objects.requireNonNull(elmPoly.selectFirst("h3")).text());
            assert elmWrapper != null;
            quizQuestion.setType(Objects.requireNonNull(elmWrapper.selectFirst("input")).attr("type").equals("radio") ? QuizQuestionType.RADIO : QuizQuestionType.CHECKBOX);
            quizQuestion.setQuestion(Objects.requireNonNull(elmPoly.selectFirst("pre[class='poly-body']")).text());
            quizQuestion.setKey(Objects.requireNonNull(elmWrapper.selectFirst("input")).attr("name"));
            try {
                quizQuestion.setListValue(buildListValue(elmWrapper));
            } catch (CmsException e) {
                continue;
            }
            quizQuestion.setAmountInput(-1);
            quizQuestion.setMultiChoice(quizQuestion.getType() == QuizQuestionType.CHECKBOX);
            quizQuestion.setCorrect(getStatus && Objects.requireNonNull(elmWrapper.selectFirst("span[class='sr']")).text().equals("correct"));
            alQuizQuestions.add(quizQuestion);
        }
        //xử lý kiểu text sau
        for (Element elmPolyInput : elmsPolyInput) {
            Element elmWrapper = elmPolyInput.nextElementSibling();

            QuizQuestion quizQuestion = new QuizQuestion();
            quizQuestion.setName(Objects.requireNonNull(elmPolyInput.selectFirst("h3")).text());
            quizQuestion.setType(QuizQuestionType.TEXT);
            quizQuestion.setQuestion(Objects.requireNonNull(elmPolyInput.selectFirst("pre")).text());
            assert elmWrapper != null;
            quizQuestion.setKey(Objects.requireNonNull(elmWrapper.selectFirst("input")).attr("name"));
            try {
                quizQuestion.setListValue(buildListValueText(elmPolyInput));
            } catch (CmsException | JsonProcessingException e) {
                //not continue;
            }
            quizQuestion.setAmountInput(elmPolyInput.select("input").size());
            quizQuestion.setMultiChoice(quizQuestion.getAmountInput() > 1);
            quizQuestion.setCorrect(getStatus && Objects.requireNonNull(elmWrapper.selectFirst("span[class='sr']")).text().equals("correct"));
            alQuizQuestions.add(quizQuestion);
        }
        return alQuizQuestions;
    }

    private static List<String> buildListValueText(Element elmPolyInput) throws CmsException, JsonProcessingException {
        Element elmData = elmPolyInput.selectFirst("div[class='data']");
        if (elmData == null) {
            throw new CmsException("div[class='data'] không tồn tại.");
        }
        QuizQuestionTextDto[] questionTextDtoList = MapperUtils.objectMapper.readValue(elmData.text(), QuizQuestionTextDto[].class);
        return Stream.of(questionTextDtoList)
                .map(quizQuestionTextDto -> StringUtils.convertVIToEN(quizQuestionTextDto.getText()))
                .collect(Collectors.toList());
    }

    private static List<String> buildListValue(Element elmWrapper) throws CmsException {
        Elements elmsInput = elmWrapper.select("input");
        if (elmsInput.isEmpty()) {
            throw new CmsException("input[] không tồn tại.");
        }
        return elmsInput.stream().map(element -> element.attr("value")).collect(Collectors.toList());
    }
}
