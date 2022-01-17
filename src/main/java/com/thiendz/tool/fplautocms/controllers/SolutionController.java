package com.thiendz.tool.fplautocms.controllers;

import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.services.SolutionService;
import com.thiendz.tool.fplautocms.utils.*;
import com.thiendz.tool.fplautocms.utils.consts.Messages;
import com.thiendz.tool.fplautocms.utils.excepts.InputException;
import com.thiendz.tool.fplautocms.views.DashboardView;

import java.util.ArrayList;
import java.util.List;

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
            int start = indexQuiz - 1;
            int end = start;
            if (indexQuiz - 1 == user.getCourses().get(indexCourse - 1).getQuizList().size()) {
                start = 0;
                end = indexQuiz - 2;
            }
            List<SolutionService> solutionServiceList = new ArrayList<>();
            for (int i = start; i <= end; i++) {
                SolutionService solutionService = new SolutionService(
                        user,
                        user.getCourses().get(indexCourse - 1),
                        user.getCourses().get(indexCourse - 1).getQuizList().get(i)
                );
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
}
