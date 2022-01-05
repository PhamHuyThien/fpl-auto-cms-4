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
            dashboardView.getBtnLogin().setEnabled(false);
            dashboardView.getCbbCourse().setEnabled(false);
            dashboardView.getCbbQuiz().setEnabled(false);
            dashboardView.getBtnSolution().setEnabled(false);
            int start = indexQuiz - 1;
            int end = start;
            if (indexQuiz - 1 == user.getCourses().get(indexCourse - 1).getQuizList().size()) {
                start = 0;
                end = indexQuiz - 2;
            }
            List<SolutionService> cmsSolutionList = new ArrayList<>();
            for (int i = start; i <= end; i++) {
                SolutionService solutionService = new SolutionService(
                        user,
                        user.getCourses().get(indexCourse - 1),
                        user.getCourses().get(indexCourse - 1).getQuizList().get(i)
                );
                solutionService.setCallbackSolution((scorePresent, status) -> {
                    //code here...
                    System.out.println(scorePresent + " " + status);
                });
                cmsSolutionList.add(solutionService);
            }
            ThreadUtils threadUtils = new ThreadUtils(cmsSolutionList, cmsSolutionList.size());
            threadUtils.execute();
            threadUtils.await();
        } catch (InputException e) {
            MsgBoxUtils.alert(dashboardView, e.toString());
        }
        dashboardView.getBtnLogin().setEnabled(true);
        dashboardView.getCbbCourse().setEnabled(true);
        dashboardView.getCbbQuiz().setEnabled(true);
        dashboardView.getBtnSolution().setEnabled(true);
    }

    private void checkValidInput() throws InputException {
        if (indexQuiz < 1)
            throw new InputException(Messages.YOU_CHOOSE_QUIZ);
    }
}
