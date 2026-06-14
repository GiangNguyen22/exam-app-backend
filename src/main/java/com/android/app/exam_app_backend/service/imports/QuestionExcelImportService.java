package com.android.app.exam_app_backend.service.imports;

import com.android.app.exam_app_backend.entity.Answer;
import com.android.app.exam_app_backend.entity.Question;
import com.android.app.exam_app_backend.entity.Subject;
import com.android.app.exam_app_backend.entity.Topic;
import com.android.app.exam_app_backend.entity.User;
import com.android.app.exam_app_backend.entity.enums.AuditResult;
import com.android.app.exam_app_backend.entity.enums.Difficulty;
import com.android.app.exam_app_backend.entity.enums.QuestionType;
import com.android.app.exam_app_backend.exception.ResourceNotFoundException;
import com.android.app.exam_app_backend.payload.QuestionImportErrorResponse;
import com.android.app.exam_app_backend.payload.QuestionImportResponse;
import com.android.app.exam_app_backend.repository.QuestionRepository;
import com.android.app.exam_app_backend.repository.SubjectRepository;
import com.android.app.exam_app_backend.repository.TopicRepository;
import com.android.app.exam_app_backend.repository.UserRepository;
import com.android.app.exam_app_backend.security.PermissionConstants;
import com.android.app.exam_app_backend.service.AuditLogService;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class QuestionExcelImportService {

    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public QuestionExcelImportService(QuestionRepository questionRepository,
                                      SubjectRepository subjectRepository,
                                      TopicRepository topicRepository,
                                      UserRepository userRepository,
                                      AuditLogService auditLogService) {
        this.questionRepository = questionRepository;
        this.subjectRepository = subjectRepository;
        this.topicRepository = topicRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public QuestionImportResponse importFromExcel(MultipartFile file, Authentication authentication) {
        if (file == null || file.isEmpty()) {
            return failure(List.of(error(0, null, "file", "File is empty")));
        }

        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authentication.getName()));

        List<QuestionImportErrorResponse> errors = new ArrayList<>();
        Map<String, DraftQuestion> drafts;

        try (InputStream inputStream = file.getInputStream(); Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getNumberOfSheets() > 0 ? workbook.getSheetAt(0) : null;
            if (sheet == null) {
                return failure(List.of(error(0, null, "sheet", "Excel sheet is empty")));
            }
            drafts = parseSheet(sheet, errors);
        } catch (IOException ex) {
            return failure(List.of(error(0, null, "file", "Cannot read Excel file")));
        }

        if (!errors.isEmpty()) {
            return failure(errors, drafts.size());
        }

        List<Question> questions = new ArrayList<>();
        int importedAnswers = 0;

        for (DraftQuestion draft : drafts.values()) {
            Subject subject = resolveSubject(draft, errors);
            Topic topic = resolveTopic(draft, subject, errors);
            QuestionType type = parseQuestionType(draft.type, draft.firstRowNumber, draft.key, errors);
            Difficulty difficulty = parseDifficulty(draft.difficulty, draft.firstRowNumber, draft.key, errors);

            if (subject == null || type == null || difficulty == null) {
                continue;
            }
            if (draft.content == null || draft.content.isBlank()) {
                errors.add(error(draft.firstRowNumber, draft.key, "content", "content is required"));
                continue;
            }

            List<DraftAnswer> answers = draft.answers.stream()
                    .filter(answer -> answer.content != null && !answer.content.isBlank())
                    .collect(Collectors.toList());
            if (answers.isEmpty()) {
                errors.add(error(draft.firstRowNumber, draft.key, "answer_content", "At least one answer is required"));
                continue;
            }

            long correctCount = answers.stream().filter(answer -> Boolean.TRUE.equals(answer.correct)).count();
            if (type == QuestionType.SINGLE || type == QuestionType.TRUE_FALSE) {
                if (correctCount != 1) {
                    errors.add(error(draft.firstRowNumber, draft.key, "answer_correct", "This type must have exactly one correct answer"));
                    continue;
                }
            } else if (type == QuestionType.MULTI) {
                if (correctCount < 1) {
                    errors.add(error(draft.firstRowNumber, draft.key, "answer_correct", "This type must have at least one correct answer"));
                    continue;
                }
            } else if (correctCount < 1) {
                errors.add(error(draft.firstRowNumber, draft.key, "answer_correct", "At least one answer must be marked correct"));
                continue;
            }

            Question question = new Question();
            question.setSubject(subject);
            question.setTopic(topic);
            question.setContent(draft.content.trim());
            question.setType(type);
            question.setDifficulty(difficulty);
            question.setCreatedBy(currentUser);

            for (DraftAnswer draftAnswer : answers) {
                Answer answer = new Answer();
                answer.setQuestion(question);
                answer.setContent(draftAnswer.content.trim());
                answer.setIsCorrect(Boolean.TRUE.equals(draftAnswer.correct));
                answer.setExplanation(blankToNull(draftAnswer.explanation));
                question.getAnswers().add(answer);
                importedAnswers++;
            }

            questions.add(question);
        }

        if (!errors.isEmpty()) {
            return failure(errors, drafts.size());
        }

        questionRepository.saveAll(questions);
        auditLogService.log(currentUser, PermissionConstants.QUESTION_IMPORT, "question", null, AuditResult.allow,
                "Imported " + questions.size() + " questions from Excel");

        return QuestionImportResponse.builder()
                .success(true)
                .totalGroups(drafts.size())
                .importedQuestions(questions.size())
                .importedAnswers(importedAnswers)
                .errors(List.of())
                .build();
    }

    private Map<String, DraftQuestion> parseSheet(Sheet sheet, List<QuestionImportErrorResponse> errors) {
        DataFormatter formatter = new DataFormatter();
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            errors.add(error(1, null, "header", "Header row is missing"));
            return Map.of();
        }

        Map<String, Integer> headers = new LinkedHashMap<>();
        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            String normalized = normalizeHeader(formatter.formatCellValue(headerRow.getCell(i)));
            if (!normalized.isBlank()) {
                headers.put(normalized, i);
            }
        }

        if (!headers.containsKey("question_key")) {
            errors.add(error(1, null, "question_key", "Missing required column"));
        }
        if (!(headers.containsKey("subject_id") || headers.containsKey("subject_name"))) {
            errors.add(error(1, null, "subject", "Provide subject_id or subject_name"));
        }
        if (!headers.containsKey("content")) {
            errors.add(error(1, null, "content", "Missing required column"));
        }
        if (!headers.containsKey("type")) {
            errors.add(error(1, null, "type", "Missing required column"));
        }
        if (!headers.containsKey("difficulty")) {
            errors.add(error(1, null, "difficulty", "Missing required column"));
        }

        if (!errors.isEmpty()) {
            return Map.of();
        }

        Map<String, DraftQuestion> drafts = new LinkedHashMap<>();
        for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null || isBlankRow(row, formatter)) {
                continue;
            }

            int displayRow = rowIndex + 1;
            String key = readString(row, headers, formatter, "question_key");
            if (key.isBlank()) {
                errors.add(error(displayRow, null, "question_key", "question_key is required"));
                continue;
            }

            DraftQuestion draft = drafts.computeIfAbsent(key, DraftQuestion::new);
            draft.firstRowNumber = draft.firstRowNumber == 0 ? displayRow : draft.firstRowNumber;

            mergeLong(draft::setSubjectId, draft.subjectId, readLong(row, headers, formatter, "subject_id", displayRow, key, errors), displayRow, key, "subject_id", errors);
            mergeString(draft::setSubjectName, draft.subjectName, readString(row, headers, formatter, "subject_name"), displayRow, key, "subject_name", errors);
            mergeLong(draft::setTopicId, draft.topicId, readLong(row, headers, formatter, "topic_id", displayRow, key, errors), displayRow, key, "topic_id", errors);
            mergeString(draft::setTopicName, draft.topicName, readString(row, headers, formatter, "topic_name"), displayRow, key, "topic_name", errors);
            mergeString(draft::setContent, draft.content, readString(row, headers, formatter, "content"), displayRow, key, "content", errors);
            mergeString(draft::setType, draft.type, readString(row, headers, formatter, "type"), displayRow, key, "type", errors);
            mergeString(draft::setDifficulty, draft.difficulty, readString(row, headers, formatter, "difficulty"), displayRow, key, "difficulty", errors);

            String answerContent = readString(row, headers, formatter, "answer_content");
            String answerCorrect = readString(row, headers, formatter, "answer_correct");
            String answerExplanation = readString(row, headers, formatter, "answer_explanation");
            if (!answerContent.isBlank() || !answerCorrect.isBlank() || !answerExplanation.isBlank()) {
                draft.answers.add(new DraftAnswer(displayRow, answerContent, parseBoolean(answerCorrect), answerExplanation));
            }
        }
        return drafts;
    }

    private Subject resolveSubject(DraftQuestion draft, List<QuestionImportErrorResponse> errors) {
        if (draft.subjectId != null) {
            return subjectRepository.findById(draft.subjectId)
                    .orElseGet(() -> {
                        errors.add(error(draft.firstRowNumber, draft.key, "subject_id", "Subject not found"));
                        return null;
                    });
        }
        if (draft.subjectName != null && !draft.subjectName.isBlank()) {
            return subjectRepository.findByNameIgnoreCase(draft.subjectName.trim())
                    .orElseGet(() -> {
                        errors.add(error(draft.firstRowNumber, draft.key, "subject_name", "Subject not found"));
                        return null;
                    });
        }
        errors.add(error(draft.firstRowNumber, draft.key, "subject", "subject_id or subject_name is required"));
        return null;
    }

    private Topic resolveTopic(DraftQuestion draft, Subject subject, List<QuestionImportErrorResponse> errors) {
        if (subject == null) {
            return null;
        }
        if (draft.topicId != null) {
            return topicRepository.findById(draft.topicId)
                    .filter(topic -> topic.getSubject() != null && topic.getSubject().getId().equals(subject.getId()))
                    .orElseGet(() -> {
                        errors.add(error(draft.firstRowNumber, draft.key, "topic_id", "Topic not found or does not belong to subject"));
                        return null;
                    });
        }
        if (draft.topicName != null && !draft.topicName.isBlank()) {
            return topicRepository.findBySubjectIdAndNameIgnoreCase(subject.getId(), draft.topicName.trim())
                    .orElseGet(() -> {
                        errors.add(error(draft.firstRowNumber, draft.key, "topic_name", "Topic not found or does not belong to subject"));
                        return null;
                    });
        }
        return null;
    }

    private QuestionType parseQuestionType(String raw, int rowNumber, String key, List<QuestionImportErrorResponse> errors) {
        try {
            return QuestionType.valueOf(normalizeEnum(raw));
        } catch (Exception ex) {
            errors.add(error(rowNumber, key, "type", "Use SINGLE, MULTI, TRUE_FALSE or FILL_BLANK"));
            return null;
        }
    }

    private Difficulty parseDifficulty(String raw, int rowNumber, String key, List<QuestionImportErrorResponse> errors) {
        try {
            return Difficulty.valueOf(normalizeEnum(raw));
        } catch (Exception ex) {
            errors.add(error(rowNumber, key, "difficulty", "Use EASY, MEDIUM or HARD"));
            return null;
        }
    }

    private String readString(Row row, Map<String, Integer> headers, DataFormatter formatter, String header) {
        Integer index = headers.get(header);
        if (index == null) {
            return "";
        }
        return formatter.formatCellValue(row.getCell(index)).trim();
    }

    private Long readLong(Row row,
                          Map<String, Integer> headers,
                          DataFormatter formatter,
                          String header,
                          int rowNumber,
                          String key,
                          List<QuestionImportErrorResponse> errors) {
        String raw = readString(row, headers, formatter, header);
        if (raw.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(raw);
        } catch (Exception ex) {
            errors.add(error(rowNumber, key, header, "Invalid number"));
            return null;
        }
    }

    private void mergeString(ValueSetter<String> setter,
                             String existing,
                             String candidate,
                             int rowNumber,
                             String key,
                             String field,
                             List<QuestionImportErrorResponse> errors) {
        if (candidate == null || candidate.isBlank()) {
            return;
        }
        if (existing == null || existing.isBlank()) {
            setter.set(candidate.trim());
            return;
        }
        if (!existing.trim().equals(candidate.trim())) {
            errors.add(error(rowNumber, key, field, "Conflicting values in the same question group"));
        }
    }

    private void mergeLong(ValueSetter<Long> setter,
                           Long existing,
                           Long candidate,
                           int rowNumber,
                           String key,
                           String field,
                           List<QuestionImportErrorResponse> errors) {
        if (candidate == null) {
            return;
        }
        if (existing == null) {
            setter.set(candidate);
            return;
        }
        if (!existing.equals(candidate)) {
            errors.add(error(rowNumber, key, field, "Conflicting values in the same question group"));
        }
    }

    private boolean isBlankRow(Row row, DataFormatter formatter) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            if (!formatter.formatCellValue(row.getCell(i)).trim().isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean parseBoolean(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("true") || normalized.equals("1") || normalized.equals("yes") || normalized.equals("y") || normalized.equals("x");
    }

    private String normalizeHeader(String header) {
        return header == null ? "" : header.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }

    private String normalizeEnum(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT).replace(' ', '_').replace('-', '_');
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private QuestionImportErrorResponse error(int rowNumber, String key, String field, String message) {
        return QuestionImportErrorResponse.builder()
                .rowNumber(rowNumber)
                .questionKey(key)
                .field(field)
                .message(message)
                .build();
    }

    private QuestionImportResponse failure(List<QuestionImportErrorResponse> errors) {
        return failure(errors, 0);
    }

    private QuestionImportResponse failure(List<QuestionImportErrorResponse> errors, int totalGroups) {
        return QuestionImportResponse.builder()
                .success(false)
                .totalGroups(totalGroups)
                .importedQuestions(0)
                .importedAnswers(0)
                .errors(errors)
                .build();
    }

    private interface ValueSetter<T> {
        void set(T value);
    }

    private static class DraftQuestion {
        private final String key;
        private int firstRowNumber;
        private Long subjectId;
        private String subjectName;
        private Long topicId;
        private String topicName;
        private String content;
        private String type;
        private String difficulty;
        private final Set<DraftAnswer> answers = new LinkedHashSet<>();

        private DraftQuestion(String key) {
            this.key = key;
        }

        private void setSubjectId(Long subjectId) {
            this.subjectId = subjectId;
        }

        private void setSubjectName(String subjectName) {
            this.subjectName = subjectName;
        }

        private void setTopicId(Long topicId) {
            this.topicId = topicId;
        }

        private void setTopicName(String topicName) {
            this.topicName = topicName;
        }

        private void setContent(String content) {
            this.content = content;
        }

        private void setType(String type) {
            this.type = type;
        }

        private void setDifficulty(String difficulty) {
            this.difficulty = difficulty;
        }
    }

    private static class DraftAnswer {
        private final int rowNumber;
        private final String content;
        private final Boolean correct;
        private final String explanation;

        private DraftAnswer(int rowNumber, String content, Boolean correct, String explanation) {
            this.rowNumber = rowNumber;
            this.content = content;
            this.correct = correct;
            this.explanation = explanation;
        }
    }
}
