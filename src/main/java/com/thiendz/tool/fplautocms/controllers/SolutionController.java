package com.thiendz.tool.fplautocms.controllers;

import com.thiendz.tool.fplautocms.models.Course;
import com.thiendz.tool.fplautocms.models.Quiz;
import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.services.SolutionService;
import com.thiendz.tool.fplautocms.utils.*;
import com.thiendz.tool.fplautocms.utils.consts.Messages;
import com.thiendz.tool.fplautocms.utils.excepts.InputException;
import com.thiendz.tool.fplautocms.views.DashboardView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
            int quizNum = NumberUtils.getInt(solutionService.getQuiz().getName());
            String name = quizNum != -1 ? quizNum + ":" : "FT:";
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
        int count = dashboardView.getCbbCourse().getItemCount();
        List<String> cbbCourseName = new ArrayList<>();
        for (int i = 0; i < count; i++)
            cbbCourseName.add(dashboardView.getCbbCourse().getItemAt(i));
        cbbCourseName = cbbCourseName.stream().map(s -> {
            Optional<Quiz> quizOptional = solutionServiceList.stream()
                    .map(SolutionService::getQuiz)
                    .filter(quiz -> s.startsWith(quiz.getName()))
                    .findFirst();
            if (quizOptional.isPresent()) {
                Quiz quiz = quizOptional.get();
                String name = quiz.getName();
                int score = (int) quiz.getScore();
                int scorePossible = (int) quiz.getScorePossible();
                return String.format(Messages.VIEW_DETAIL_QUIZ, name, score, scorePossible);
            }
            return s;
        }).collect(Collectors.toList());
        dashboardView.getCbbQuiz().removeAllItems();
        cbbCourseName.forEach(s -> dashboardView.getCbbCourse().addItem(s));
        dashboardView.getCbbCourse().setSelectedItem(cbbCourseName.get(cbbCourseName.size() - 1));
    }
}
