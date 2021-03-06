package com.thiendz.tool.fplautocms.controllers;

import com.thiendz.tool.fplautocms.dto.GetQuizQuestionDto;
import com.thiendz.tool.fplautocms.models.Course;
import com.thiendz.tool.fplautocms.models.Quiz;
import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.services.ServerService;
import com.thiendz.tool.fplautocms.services.SolutionService;
import com.thiendz.tool.fplautocms.utils.*;
import com.thiendz.tool.fplautocms.utils.consts.Environments;
import com.thiendz.tool.fplautocms.utils.consts.Messages;
import com.thiendz.tool.fplautocms.utils.excepts.InputException;
import com.thiendz.tool.fplautocms.views.DashboardView;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class SolutionController implements Runnable {
    private final DashboardView dashboardView;
    private final User user;
    private Course course;
    private final int indexCourse;
    private final int indexQuiz;

    public static void start(DashboardView dashboardView) {
        new Thread(new SolutionController(dashboardView)).start();
    }

    public SolutionController(DashboardView dashboardView) {
        this.dashboardView = dashboardView;
        this.user = dashboardView.getUser();
        this.indexCourse = dashboardView.getCbbCourse().getSelectedIndex();
        if (indexCourse > 0)
            this.course = user.getCourses().get(indexCourse - 1);
        this.indexQuiz = dashboardView.getCbbQuiz().getSelectedIndex();
    }

    @Override
    public void run() {
        try {
            checkValidInput();
            dashboardView.buttonEnabled(false);
            dashboardView.showProcess(Messages.STARTING_SOLUTION);
            List<SolutionService> solutionServiceList = toSolutionServiceList();
            ThreadUtils threadUtils = new ThreadUtils(solutionServiceList, solutionServiceList.size());
            threadUtils.execute();
            int sec = 0;
            do {
                showProcess(solutionServiceList, ++sec, false);
                ThreadUtils.sleep(1000);
            } while (threadUtils.isTerminating());
            pushQuizQuestionFinish(course, solutionServiceList);
            showProcess(solutionServiceList, ++sec, true);
            updateComboBoxCourse(solutionServiceList);
            MsgBoxUtils.alert(dashboardView, Messages.AUTO_SOLUTION_FINISH);
            askForFeedback();
        } catch (InputException e) {
            MsgBoxUtils.alert(dashboardView, e.toString());
        }
        dashboardView.buttonEnabled(true);
    }

    private List<SolutionService> toSolutionServiceList() {
        int start = indexQuiz - 1;
        int end = start;
        if (indexQuiz - 1 == course.getQuizList().size()) {
            start = 0;
            end = indexQuiz - 2;
        }
        List<SolutionService> solutionServiceList = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            Quiz quiz = course.getQuizList().get(i);
            if (Environments.SKIP_FINAL_TEST) {
                Integer quizNum = NumberUtils.getInt(quiz.getName());
                if (quizNum == null)
                    break;
            }
            SolutionService solutionService = new SolutionService(user, course, quiz);
            if (!Environments.DISABLE_QUIZ_SPEED)
                solutionService.setParamPost(getSpeedParamFromServer(course, quiz));
            solutionService.setResetScoreQuiz(Environments.ENABLE_RESET_QUIZ);
            solutionServiceList.add(solutionService);
        }
        return solutionServiceList;
    }

    private String getSpeedParamFromServer(Course course, Quiz quiz) {
        try {
            GetQuizQuestionDto getQuizQuestionDto = ServerService.serverService.getQuizQuestion(course, quiz);
            if (getQuizQuestionDto.getStatus() != null && getQuizQuestionDto.getStatus() == 1)
                return StringUtils.b64Decode(getQuizQuestionDto.getData().getData_b64());
        } catch (Exception e) {
            log.error(e.toString(), e);
        }
        return null;
    }

    private void pushQuizQuestionFinish(Course course, List<SolutionService> solutionServiceList) {
        solutionServiceList.forEach(solutionService -> {
            try {
                if (solutionService.getStatus() == 1 && solutionService.getParamPost() == null)
                    ServerService.serverService.pushQuizQuestion(course, solutionService.getQuiz());
            } catch (IOException e) {
                log.error(e.toString(), e);
            }
        });
    }

    private void checkValidInput() throws InputException {
        if (indexQuiz < 1)
            throw new InputException(Messages.YOU_CHOOSE_QUIZ);

    }

    private void showProcess(List<SolutionService> solutionServiceList, int time, boolean finish) {
        int len = solutionServiceList.size();
        String timeStr = DateUtils.toStringDate(time);
        String solution = finish ? "Gi???i ho??n t???t m???t " : "??ang gi???i ";
        String content = finish ? " - " + len + " quiz ho??n th??nh!" : "...";
        StringBuilder show = new StringBuilder(solution + timeStr + content + "##");
        for (SolutionService solutionService : solutionServiceList) {
            Integer quizNum = NumberUtils.getInt(solutionService.getQuiz().getName());
            String name = quizNum != null ? quizNum + ":" : "FT:";
            String score = name + NumberUtils.roundReal(solutionService.getScorePresent(), 1) + ":";
            switch (solutionService.getStatus()) {
                case -1:
                    score += "f - ";
                    break;
                case 0:
                    score += "r - ";
                    break;
                case 1:
                    score += "d - ";
                    break;
            }
            show.append(score);
        }
        String newLn = solutionServiceList.size() > 1 ? "## " : "";
        dashboardView.showProcess(show.substring(0, show.length() - 3) + newLn);
    }

    private void updateComboBoxCourse(List<SolutionService> solutionServiceList) {
        int selected = dashboardView.getCbbQuiz().getSelectedIndex();
        int count = dashboardView.getCbbQuiz().getItemCount();
        List<String> cbbQuizName = new ArrayList<>();
        for (int i = 0; i < count; i++)
            cbbQuizName.add(dashboardView.getCbbQuiz().getItemAt(i));
        cbbQuizName = cbbQuizName.stream().map(s -> {
            Optional<SolutionService> solutionServiceOptional = solutionServiceList.stream()
                    .filter(solutionService -> s.startsWith(solutionService.getQuiz().getName()))
                    .findFirst();
            if (solutionServiceOptional.isPresent()) {
                SolutionService solutionService = solutionServiceOptional.get();
                updateQuiz(solutionService.getQuiz());
                String name = solutionService.getQuiz().getName();
                int score = (int) solutionService.getScorePresent();
                int scorePossible = (int) solutionService.getQuiz().getScorePossible();
                return String.format(Messages.VIEW_DETAIL_QUIZ, name, score, scorePossible);
            }
            return s;
        }).collect(Collectors.toList());
        dashboardView.getCbbQuiz().removeAllItems();
        cbbQuizName.forEach(s -> dashboardView.getCbbQuiz().addItem(s));
        dashboardView.getCbbQuiz().setSelectedIndex(selected);
        dashboardView.getUser().getCourses().set(indexCourse - 1, course);
    }

    private void updateQuiz(Quiz quiz) {
        for (int i = 0; i < course.getQuizList().size(); i++) {
            Quiz quiz1 = course.getQuizList().get(i);
            if (quiz1.getName().equals(quiz.getName())) {
                course.getQuizList().set(i, quiz);
                break;
            }
        }
    }

    private void askForFeedback() {
        String feedback = MsgBoxUtils.promptWar(dashboardView, Messages.ASK_FOR_FEEDBACK);
        if (feedback != null && !feedback.trim().equals("")) {
            try {
                ServerService.serverService.feedback(user, feedback);
            } catch (IOException e) {
                log.error(e.toString(), e);
            }
            MsgBoxUtils.alertErr(dashboardView, Messages.THANK_FOR_FEEDBACK);
        }
    }
}
