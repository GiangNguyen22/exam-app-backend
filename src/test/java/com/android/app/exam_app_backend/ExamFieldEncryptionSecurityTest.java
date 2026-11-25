package com.android.app.exam_app_backend;

import com.android.app.exam_app_backend.entity.Answer;
import com.android.app.exam_app_backend.entity.Question;
import com.android.app.exam_app_backend.entity.Subject;
import com.android.app.exam_app_backend.entity.enums.Difficulty;
import com.android.app.exam_app_backend.entity.enums.QuestionType;
import com.android.app.exam_app_backend.repository.AnswerRepository;
import com.android.app.exam_app_backend.repository.QuestionRepository;
import com.android.app.exam_app_backend.repository.SubjectRepository;
import com.android.app.exam_app_backend.security.crypto.AesGcmAttributeConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class ExamFieldEncryptionSecurityTest {

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private AnswerRepository answerRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private EntityManager entityManager;

    private Subject testSubject;

    @BeforeEach
    void setUp() {
        testSubject = subjectRepository.findAll().stream().findFirst().orElseGet(() -> {
            Subject s = new Subject();
            s.setName("Crypto Subject");
            s.setDescription("Subject for crypto tests");
            return subjectRepository.save(s);
        });
    }

    @Test
    @Transactional
    @DisplayName("TASK-T6: Questions and Answers are encrypted with AES-256-GCM in database and decrypted seamlessly by JPA")
    void testQuestionAndAnswerFieldEncryption() {
        String sensitiveQuestion = "Sensitive Question: What is the secret master key?";
        String sensitiveAnswer = "Sensitive Answer: AlphaBravo123456";
        String sensitiveExplanation = "Explanation: This is derived from AES-GCM specification.";

        // 1. Tạo entity và lưu qua JPA
        Question question = new Question();
        question.setSubject(testSubject);
        question.setContent(sensitiveQuestion);
        question.setType(QuestionType.SINGLE);
        question.setDifficulty(Difficulty.MEDIUM);
        question = questionRepository.save(question);

        Answer answer = new Answer();
        answer.setQuestion(question);
        answer.setContent(sensitiveAnswer);
        answer.setExplanation(sensitiveExplanation);
        answer.setIsCorrect(true);
        answer = answerRepository.save(answer);

        // Đẩy toàn bộ thay đổi xuống DB và xóa JPA Cache (Persistence Context)
        entityManager.flush();
        entityManager.clear();

        // 2. Kiểm tra trực tiếp dữ liệu thô trong CSDL bằng Native SQL Query
        String rawQuestionContent = (String) entityManager.createNativeQuery(
                "SELECT content FROM questions WHERE id = :id")
                .setParameter("id", question.getId())
                .getSingleResult();

        assertNotNull(rawQuestionContent);
        // Xác nhận dữ liệu trong DB đã được mã hóa: Bắt đầu bằng tiền tố ENC: và KHÔNG chứa plaintext
        assertTrue(rawQuestionContent.startsWith(AesGcmAttributeConverter.ENCRYPTED_PREFIX),
                "Database content must start with " + AesGcmAttributeConverter.ENCRYPTED_PREFIX);
        assertFalse(rawQuestionContent.contains(sensitiveQuestion),
                "Database raw content MUST NOT contain sensitive question plaintext!");

        // Kiểm tra câu trả lời và giải thích trong bảng answers
        Object[] rawAnswerRow = (Object[]) entityManager.createNativeQuery(
                "SELECT content, explanation FROM answers WHERE id = :id")
                .setParameter("id", answer.getId())
                .getSingleResult();

        String rawAnswerContent = (String) rawAnswerRow[0];
        String rawExplanation = (String) rawAnswerRow[1];

        assertNotNull(rawAnswerContent);
        assertTrue(rawAnswerContent.startsWith(AesGcmAttributeConverter.ENCRYPTED_PREFIX));
        assertFalse(rawAnswerContent.contains(sensitiveAnswer));

        assertNotNull(rawExplanation);
        assertTrue(rawExplanation.startsWith(AesGcmAttributeConverter.ENCRYPTED_PREFIX));
        assertFalse(rawExplanation.contains(sensitiveExplanation));

        // 3. Tải lại qua JPA: Converter phải tự động giải mã minh bạch về đúng plain text
        Question loadedQuestion = questionRepository.findById(question.getId()).orElseThrow();
        assertEquals(sensitiveQuestion, loadedQuestion.getContent(),
                "JPA entity must transparently decrypt content back to original plaintext");

        List<Answer> loadedAnswers = answerRepository.findByQuestionId(question.getId());
        assertFalse(loadedAnswers.isEmpty());
        Answer loadedAnswer = loadedAnswers.get(0);
        assertEquals(sensitiveAnswer, loadedAnswer.getContent());
        assertEquals(sensitiveExplanation, loadedAnswer.getExplanation());
    }

    @Test
    @Transactional
    @DisplayName("TASK-T6: Backward compatibility - Plaintext data stored prior to encryption is loaded without errors")
    void testBackwardCompatibilityWithLegacyPlaintext() {
        String legacyPlaintext = "Legacy unencrypted question before migration";

        // Chèn trực tiếp bản ghi dạng plaintext vào DB không qua converter
        entityManager.createNativeQuery(
                "INSERT INTO questions (subject_id, content, type, difficulty, created_at, updated_at) " +
                        "VALUES (:subjectId, :content, 'SINGLE', 'EASY', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)")
                .setParameter("subjectId", testSubject.getId())
                .setParameter("content", legacyPlaintext)
                .executeUpdate();

        entityManager.flush();
        entityManager.clear();

        // Tìm kiếm câu hỏi vừa chèn bằng content
        List<Question> questions = questionRepository.findAll();
        Question legacyQuestion = questions.stream()
                .filter(q -> legacyPlaintext.equals(q.getContent()))
                .findFirst()
                .orElse(null);

        assertNotNull(legacyQuestion, "Legacy question should be loaded without decryption failure");
        assertEquals(legacyPlaintext, legacyQuestion.getContent());
    }
}
