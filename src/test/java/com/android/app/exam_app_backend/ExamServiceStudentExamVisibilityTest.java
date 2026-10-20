package com.android.app.exam_app_backend;

import com.android.app.exam_app_backend.entity.Exam;
import com.android.app.exam_app_backend.entity.StudentGroup;
import com.android.app.exam_app_backend.entity.StudentGroupMember;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.payload.ExamResponse;
import com.android.app.exam_app_backend.repository.ExamRepository;
import com.android.app.exam_app_backend.repository.StudentGroupMemberRepository;
import com.android.app.exam_app_backend.repository.StudentGroupRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import com.android.app.exam_app_backend.service.ExamService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;

@SpringBootTest
class ExamServiceStudentExamVisibilityTest {

    @Autowired
    private ExamService examService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ExamRepository examRepository;

    @Autowired
    private StudentGroupRepository studentGroupRepository;

    @Autowired
    private StudentGroupMemberRepository studentGroupMemberRepository;

    @Test
    void shouldReturnExamForStudentAssignedToExamGroup() {
        User student = userRepository.findByUsername("student1").orElseThrow();

        StudentGroup group = new StudentGroup();
        group.setName("Assigned Group " + System.currentTimeMillis());
        group.setDescription("For student exam visibility test");
        group = studentGroupRepository.save(group);

        StudentGroupMember member = new StudentGroupMember();
        member.setGroup(group);
        member.setUser(student);
        studentGroupMemberRepository.save(member);

        Exam exam = new Exam();
        exam.setCode("EXAM-" + System.currentTimeMillis());
        exam.setTitle("Exam assigned to group");
        exam.setDurationMinutes(30);
        exam.setScorePerQuestion(BigDecimal.ONE);
        exam.setMaxAttempts(1);
        exam.setGroups(new HashSet<>(List.of(group)));
        Exam savedExam = examRepository.save(exam);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "student1",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        ExamResponse response = examService.getExamForStudent(savedExam.getId(), auth);

        Assertions.assertNotNull(response);
        Assertions.assertEquals(savedExam.getId(), response.getId());
        Assertions.assertTrue(response.getGroupIds().contains(group.getId()));
    }

    @Test
    void shouldRejectExamWhenStudentIsNotInAssignedGroup() {
        User student = userRepository.findByUsername("student1").orElseThrow();

        StudentGroup anotherGroup = new StudentGroup();
        anotherGroup.setName("Other Group " + System.currentTimeMillis());
        anotherGroup.setDescription("Not assigned to student");
        anotherGroup = studentGroupRepository.save(anotherGroup);

        Exam exam = new Exam();
        exam.setCode("EXAM-OTHER-" + System.currentTimeMillis());
        exam.setTitle("Exam not assigned to this student");
        exam.setDurationMinutes(30);
        exam.setScorePerQuestion(BigDecimal.ONE);
        exam.setMaxAttempts(1);
        exam.setGroups(new HashSet<>(List.of(anotherGroup)));
        Long examId = examRepository.save(exam).getId();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "student1",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        Assertions.assertThrows(ResponseStatusException.class,
                () -> examService.getExamForStudent(examId, auth));
    }

    @Test
    void shouldNotReturnScheduledExamWithNoAssignedGroupsForStudent() {
        User student = userRepository.findByUsername("student1").orElseThrow();

        StudentGroup group = new StudentGroup();
        group.setName("Target Group " + System.currentTimeMillis());
        group.setDescription("Used to be in assigned group");
        group = studentGroupRepository.save(group);

        StudentGroupMember member = new StudentGroupMember();
        member.setGroup(group);
        member.setUser(student);
        studentGroupMemberRepository.save(member);

        Exam exam = new Exam();
        exam.setCode("EXAM-UNASSIGNED-" + System.currentTimeMillis());
        exam.setTitle("Exam without any assigned group");
        exam.setDurationMinutes(45);
        exam.setScorePerQuestion(BigDecimal.ONE);
        exam.setMaxAttempts(1);
        exam.setStartTime(java.time.LocalDateTime.now().minusMinutes(5));
        exam.setEndTime(java.time.LocalDateTime.now().plusMinutes(30));
        exam.setGroups(new HashSet<>());
        examRepository.save(exam);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "student1",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        List<ExamResponse> scheduledExams = examService.getScheduledExams(auth);

        Assertions.assertFalse(scheduledExams.stream().anyMatch(e -> e.getId().equals(exam.getId())));
    }

    @Test
    void shouldReturnScheduledExamOnceWhenStudentBelongsToMultipleAssignedGroups() {
        User student = userRepository.findByUsername("student1").orElseThrow();

        StudentGroup groupA = new StudentGroup();
        groupA.setName("Group A " + System.currentTimeMillis());
        groupA.setDescription("Assigned group A");
        groupA = studentGroupRepository.save(groupA);

        StudentGroup groupB = new StudentGroup();
        groupB.setName("Group B " + System.currentTimeMillis());
        groupB.setDescription("Assigned group B");
        groupB = studentGroupRepository.save(groupB);

        studentGroupMemberRepository.saveAll(List.of(
                createMember(student, groupA),
                createMember(student, groupB)
        ));

        Exam exam = new Exam();
        exam.setCode("EXAM-MULTI-GROUP-" + System.currentTimeMillis());
        exam.setTitle("Exam assigned to multiple groups");
        exam.setDurationMinutes(60);
        exam.setScorePerQuestion(BigDecimal.ONE);
        exam.setMaxAttempts(1);
        exam.setStartTime(java.time.LocalDateTime.now().minusMinutes(5));
        exam.setEndTime(java.time.LocalDateTime.now().plusMinutes(60));
        exam.setGroups(new HashSet<>(List.of(groupA, groupB)));
        Exam savedExam = examRepository.save(exam);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "student1",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        List<ExamResponse> scheduledExams = examService.getScheduledExams(auth);

        Assertions.assertEquals(1, scheduledExams.stream().filter(e -> e.getId().equals(savedExam.getId())).count());
    }

    @Test
    void shouldReturnUpcomingExamAssignedToStudentGroupEvenBeforeStartTime() {
        User student = userRepository.findByUsername("student1").orElseThrow();

        StudentGroup group = new StudentGroup();
        group.setName("Future Exam Group " + System.currentTimeMillis());
        group.setDescription("Upcoming exam assignment");
        group = studentGroupRepository.save(group);

        studentGroupMemberRepository.save(createMember(student, group));

        Exam exam = new Exam();
        exam.setCode("EXAM-UPCOMING-" + System.currentTimeMillis());
        exam.setTitle("Upcoming assigned exam");
        exam.setDurationMinutes(30);
        exam.setScorePerQuestion(BigDecimal.ONE);
        exam.setMaxAttempts(1);
        exam.setStartTime(java.time.LocalDateTime.now().plusDays(1));
        exam.setEndTime(java.time.LocalDateTime.now().plusDays(1).plusHours(2));
        exam.setGroups(new HashSet<>(List.of(group)));
        Exam savedExam = examRepository.save(exam);

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "student1",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );

        List<ExamResponse> upcoming = examService.getMyValidAndUpcomingExams(auth);

        Assertions.assertTrue(upcoming.stream().anyMatch(e -> e.getId().equals(savedExam.getId())));
    }

    private StudentGroupMember createMember(User user, StudentGroup group) {
        StudentGroupMember member = new StudentGroupMember();
        member.setUser(user);
        member.setGroup(group);
        return member;
    }
}
