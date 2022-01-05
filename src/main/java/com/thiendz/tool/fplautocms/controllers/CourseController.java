package com.thiendz.tool.fplautocms.controllers;

import com.thiendz.tool.fplautocms.models.Course;
import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.services.QuizService;
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
        courseSelectedIndex = dashboardView.getCbbCourse().getSelectedIndex();
        user = dashboardView.getUser();
        if (courseSelectedIndex > 0) {
            course = dashboardView.getUser().getCourses().get(courseSelectedIndex - 1);
        }
    }

    @Override
    public void run() {
        if (courseSelectedIndex > 0) {
            if (user.getCourses().get(courseSelectedIndex - 1).getQuizList() == null) {
                try {
                    dashboardView.getBtnLogin().setEnabled(false);
                    dashboardView.getCbbCourse().setEnabled(false);
                    dashboardView.getCbbQuiz().setEnabled(false);
                    dashboardView.getBtnSolution().setEnabled(false);
                    checkValidInput();
                    QuizService quizService = new QuizService(user, course);
                    quizService.render();
                    course = quizService.getCourse();
                    dashboardView.getUser().getCourses().set(courseSelectedIndex - 1, course);
                    dashboardView.getCbbQuiz().setEnabled(true);
                    dashboardView.getBtnSolution().setEnabled(true);
                    dashboardView.getCbbQuiz().setSelectedIndex(dashboardView.getCbbQuiz().getItemCount() - 1);
                } catch (InputException e) {
                    return;
                } catch (IOException e) {
                    MsgBoxUtils.alert(dashboardView, Messages.CONNECT_ERROR);
                } catch (CmsException e) {
                    MsgBoxUtils.alert(dashboardView, e.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                    MsgBoxUtils.alert(dashboardView, Messages.AN_ERROR_OCCURRED + e);
                }
                dashboardView.getBtnLogin().setEnabled(true);
                dashboardView.getCbbCourse().setEnabled(true);
                dashboardView.getCbbQuiz().setEnabled(true);
                dashboardView.getBtnSolution().setEnabled(true);
            }
            showDashboard();
        }
    }

    private void checkValidInput() throws InputException {
        if (courseSelectedIndex < 1) {
            throw new InputException(Messages.YOU_CHOOSE_QUIZ);
        }
    }

    private void showDashboard() {
        dashboardView.getCbbQuiz().removeAllItems();
        dashboardView.getCbbQuiz().addItem("Chọn Quiz...");
        if (course != null && course.getQuizList() != null && !course.getQuizList().isEmpty()) {
            course.getQuizList().forEach(quiz -> {
                String name = quiz.getName();
                int score = (int) quiz.getScore();
                int scorePossible = (int) quiz.getScorePossible();
                dashboardView.getCbbQuiz().addItem(name + " - " + score + "/" + scorePossible + " point");
            });
            dashboardView.getCbbQuiz().addItem("Auto giải " + course.getQuizList().size() + " quiz");
        }
    }
}
