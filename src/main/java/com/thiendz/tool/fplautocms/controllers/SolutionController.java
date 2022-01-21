package com.thiendz.tool.fplautocms.controllers;

import com.thiendz.tool.fplautocms.models.Course;
import com.thiendz.tool.fplautocms.models.Quiz;
import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.services.ServerService;
import com.thiendz.tool.fplautocms.services.SolutionService;
import com.thiendz.tool.fplautocms.utils.*;
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
    private final int indexCourse;
    private final int indexQuiz;

    public static void start(DashboardView dashboardView) {
        new Thread(new SolutionController(dashboardView)).start();
    }

    public SolutionController(DashboardView dashboardView) {
        this.dashboardView = dashboardView;
        this.user = dashboardView.getUser();
        this.indexCourse = dashboardView.getCbbCourse().getSelectedIndex();
        this.indexQuiz = dashboardView.getCbbQuiz().getSelectedIndex();
    }

    @Override
    public void run() {
        try {
            checkValidInput();
            dashboardView.buttonEnabled(false);
            Course course = user.getCourses().get(indexCourse - 1);
            int start = indexQuiz - 1;
            int end = start;
            if (indexQuiz - 1 == course.getQuizList().size()) {
                start = 0;
                end = indexQuiz - 2;
            }
            List<SolutionService> solutionServiceList = new ArrayList<>();
            for (int i = start; i <= end; i++) {
                Quiz quiz = course.getQuizList().get(i);
                SolutionService solutionService = new SolutionService(user, course, quiz);
                solutionServiceList.add(solutionService);
            }
            ThreadUtils threadUtils = new ThreadUtils(solutionServiceList, solutionServiceList.size());
            threadUtils.execute();
            int sec = 0;
            do {
                showProcess(solutionServiceList, ++sec, false);
                ThreadUtils.sleep(1000);
            } while (threadUtils.isTerminating());
            solutionServiceList.forEach(solutionService -> {
                try {
                    if (solutionService.getStatus() == 1)
                        ServerService.serverService.pushQuizQuestion(course, solutionService.getQuiz());
                } catch (IOException e) {
                    log.error("Push quizQuestion error.", e);
                }
            });
            showProcess(solutionServiceList, ++sec, true);
            updateComboBoxCourse(solutionServiceList);
            MsgBoxUtils.alert(dashboardView, Messages.AUTO_SOLUTION_FINISH);
        } catch (InputException e) {
            MsgBoxUtils.alert(dashboardView, e.toString());
        }
        dashboardView.buttonEnabled(true);
    }

    private void checkValidInput() throws InputException {
        if (indexQuiz < 1)
            throw new InputException(Messages.YOU_CHOOSE_QUIZ);

    }

    private void showProcess(List<SolutionService> solutionServiceList, int time, boolean finish) {
        int len = solutionServiceList.size();
        String timeStr = DateUtils.toStringDate(time);
        String solution = finish ? "Giải hoàn tất mất " : "Đang giải ";
        String content = finish ? " - " + len + " quiz hoàn thành!" : "...";
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
    }
}
