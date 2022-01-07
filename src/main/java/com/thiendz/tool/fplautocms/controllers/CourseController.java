package com.thiendz.tool.fplautocms.controllers;

import com.thiendz.tool.fplautocms.dto.CourseSafetyDto;
import com.thiendz.tool.fplautocms.models.Course;
import com.thiendz.tool.fplautocms.models.User;
import com.thiendz.tool.fplautocms.services.QuizService;
import com.thiendz.tool.fplautocms.services.ServerService;
import com.thiendz.tool.fplautocms.utils.MsgBoxUtils;
import com.thiendz.tool.fplautocms.utils.OsUtils;
import com.thiendz.tool.fplautocms.utils.consts.Messages;
import com.thiendz.tool.fplautocms.utils.excepts.CmsException;
import com.thiendz.tool.fplautocms.utils.excepts.InputException;
import com.thiendz.tool.fplautocms.views.DashboardView;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
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
                    checkValidInput();
                    dashboardView.getTfCookie().setEnabled(false);
                    dashboardView.getBtnLogin().setEnabled(false);
                    dashboardView.getCbbCourse().setEnabled(false);
                    dashboardView.getCbbQuiz().setEnabled(false);
                    dashboardView.getBtnSolution().setEnabled(false);
                    dashboardView.showProcess("Đang tải dữ liệu khóa học...");
                    QuizService quizService = new QuizService(user, course);
                    quizService.render();
                    course = quizService.getCourse();
                    pushCourse();
                    checkQuizSafety();
                    dashboardView.showProcess("Tải dữ liệu khóa học hoàn tất.");
                    dashboardView.getUser().getCourses().set(courseSelectedIndex - 1, course);
                    dashboardView.getCbbQuiz().setEnabled(true);
                    dashboardView.getBtnSolution().setEnabled(true);
                    log.info(course.toString());
                } catch (InputException e) {
                    log.info(e.toString());
                    return;
                } catch (IOException e) {
                    log.info(e.toString());
                    MsgBoxUtils.alert(dashboardView, Messages.CONNECT_ERROR);
                } catch (CmsException e) {
                    log.info(e.toString());
                    MsgBoxUtils.alert(dashboardView, e.toString());
                } catch (Exception e) {
                    log.info(e.toString());
                    MsgBoxUtils.alert(dashboardView, Messages.AN_ERROR_OCCURRED + e);
                }
                dashboardView.getTfCookie().setEnabled(true);
                dashboardView.getBtnLogin().setEnabled(true);
                dashboardView.getCbbCourse().setEnabled(true);
            }
            showDashboard();
        }
    }

    private void pushCourse() {
        try {
            ServerService.serverService.pushCourse(course);
        } catch (IOException e) {
            log.error(e.toString());
        }
    }

    private void checkQuizSafety() {
        try {
            CourseSafetyDto courseSafetyDto = ServerService.serverService.getCourse(course);
            if (courseSafetyDto.getSafety() < 3) {
                MsgBoxUtils.alertWar(dashboardView, "Số lương quiz môn trên server chưa đủ độ an toàn\nSố lượng quiz tìm thấy có thể bị thiếu do mạng lag...");
            } else if (course.getQuizList().size() < courseSafetyDto.getTotal()) {
                MsgBoxUtils.alertWar(dashboardView, "Không đủ số lượng quiz vui lòng khởi động lại tool và thử lại.");
                System.exit(0);
            }
        } catch (IOException e) {
            log.error(e.toString());
            MsgBoxUtils.alertWar(dashboardView, "Không thể kết nối tới Server!\nSố lượng quiz tìm thấy có thể bị thiếu do mạng lag...");
        } catch (CmsException e) {
            log.error(e.toString());
            MsgBoxUtils.alertWar(dashboardView, e.toString());
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
            String autoSolutionAll = "Auto giải " + course.getQuizList().size() + " quiz (VIP)";
            dashboardView.getCbbQuiz().addItem(autoSolutionAll);
            dashboardView.getCbbQuiz().setSelectedItem(autoSolutionAll);
        }
    }
}
