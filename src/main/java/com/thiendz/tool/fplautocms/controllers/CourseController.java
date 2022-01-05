package com.thiendz.tool.fplautocms.controllers;

import com.thiendz.tool.fplautocms.models.Course;
import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.services.CourseService;
import com.thiendz.tool.fplautocms.utils.MsgBoxUtils;
import com.thiendz.tool.fplautocms.utils.consts.Messages;
import com.thiendz.tool.fplautocms.utils.excepts.CmsException;
import com.thiendz.tool.fplautocms.utils.excepts.InputException;
import com.thiendz.tool.fplautocms.views.DashboardView;

import java.io.IOException;

public class CourseController implements Runnable {

    private final DashboardView dashboardView;
    private final int courseSelectedIndex;
    private final User user;
    private Course course;

    public static void start(DashboardView dashboardView) {
        new Thread(new CourseController(dashboardView)).start();
    }

    public CourseController(DashboardView dashboardView) {
        this.dashboardView = dashboardView;
        this.courseSelectedIndex = dashboardView.getCbbCourse().getSelectedIndex();
        this.user = dashboardView.getUser();
    }

    @Override
    public void run() {
        try {
            dashboardView.getCbbCourse().setEnabled(false);
            checkValidInput();
            course = dashboardView.getUser().getCourses().get(courseSelectedIndex - 1);
            CourseService courseService = new CourseService(user, course);
            courseService.render();
            course = courseService.getCourse();
            showDashboard();
            dashboardView.getCbbQuiz().setEnabled(true);
            dashboardView.getBtnSolution().setEnabled(true);
            dashboardView.getCbbQuiz().setSelectedIndex(dashboardView.getCbbQuiz().getItemCount() - 1);
            dashboardView.getUser().getCourses().set(courseSelectedIndex, course);
        } catch (InputException e) {
            return;
        } catch (IOException e) {
            MsgBoxUtils.alert(dashboardView, Messages.CONNECT_ERROR);
        } catch (CmsException e) {
            MsgBoxUtils.alert(dashboardView, e.toString());
        } catch (Exception e) {
            MsgBoxUtils.alert(dashboardView, Messages.AN_ERROR_OCCURRED + e);
        }
        dashboardView.getCbbCourse().setEnabled(true);
    }

    private void checkValidInput() throws InputException {
        if (courseSelectedIndex <= 0) {
            throw new InputException(Messages.YOU_CHOOSE_QUIZ);
        }
    }

    private void showDashboard() {
        dashboardView.getCbbQuiz().removeAllItems();
        dashboardView.getCbbQuiz().addItem("Chọn Quiz...");
        course.getQuizList().forEach(quiz -> {
            String name = quiz.getName();
            int score = (int) quiz.getScore();
            int scorePossible = (int) quiz.getScorePossible();
            dashboardView.getCbbQuiz().addItem(name + " - " + score + "/" + scorePossible + " point");
        });
        dashboardView.getCbbQuiz().addItem("Auto giải hết quiz");
    }
}
