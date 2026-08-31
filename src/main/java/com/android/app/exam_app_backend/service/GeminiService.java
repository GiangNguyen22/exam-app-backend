package com.android.app.exam_app_backend.service;

import com.android.app.exam_app_backend.payload.AiExplainRequest;
import com.android.app.exam_app_backend.payload.AiExplainResponse;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.ThinkingConfig;
import com.google.genai.types.ThinkingLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Sinh lời giải thích cho câu hỏi trắc nghiệm bằng Google Gen AI (Gemini).
 * Sử dụng Google Gen AI Java SDK (com.google.genai:google-genai).
 */
@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final Client client;
    private final String apiKey;
    private final String model;

    public GeminiService(Client client,
                         String geminiApiKey,
                         @Value("${app.ai.gemini.model:gemini-3.5-flash-lite}") String model) {
        this.client = client;
        this.apiKey = geminiApiKey;
        this.model = model;
    }

    /**
     * Tạo lời giải thích cho một câu hỏi trắc nghiệm.
     * Nếu chưa cấu hình API key hoặc có lỗi gọi AI, trả về null để UI xử lý linh hoạt.
     */
    public AiExplainResponse explain(AiExplainRequest request) {

        long totalStart = System.nanoTime();

        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        long promptStart = System.nanoTime();

        String prompt = buildPrompt(request);

        long promptTime = System.nanoTime() - promptStart;

        try {
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .thinkingConfig(
                            ThinkingConfig.builder()
                                    .thinkingLevel(ThinkingLevel.Known.MINIMAL)
                                    .build()
                    )
                    .maxOutputTokens(400)
                    .responseMimeType("text/plain")
                    .build();

            long geminiStart = System.nanoTime();

            GenerateContentResponse response =
                    client.models.generateContent(model, prompt, config);

            long geminiTime = System.nanoTime() - geminiStart;

            String text = response.text();

            long totalTime = System.nanoTime() - totalStart;

            log.info(
                    "AI timing - prompt: {} ms, Gemini: {} ms, total service: {} ms",
                    promptTime / 1_000_000,
                    geminiTime / 1_000_000,
                    totalTime / 1_000_000
            );

            if (text == null || text.isBlank()) {
                return null;
            }

            return AiExplainResponse.builder()
                    .explanation(text.trim())
                    .build();

        } catch (Exception ex) {
            log.error("Failed to call Gemini API: {}", ex.getMessage());
            return null;
        }
    }

    private String buildPrompt(AiExplainRequest req) {
        boolean correct = req.isCorrect();
        String analysisStatus = correct
                ? "Học sinh trả lời ĐÚNG."
                : "Học sinh trả lời SAI.";

        String requirements;
        if (correct) {
            requirements =
                "- Giải thích ngắn gọn trong 2-3 câu, củng cố kiến thức của bạn.\n" +
                "- Nêu thêm 1 điểm liên quan hữu ích.\n";
        } else {
            requirements =
                "- Giải thích ĐẦY ĐỦ và chi tiết, ít nhất 4-5 câu, theo cấu trúc sau:\n" +
                "  1. Nêu đáp án đúng và tại sao nó đúng.\n" +
                "  2. Chỉ ra lựa chọn của bạn sai ở đâu / thiếu gì.\n" +
                "  3. Giải thích nguyên nhân dẫn đến nhầm lẫn.\n" +
                "  4. Đưa ra mẹo hoặc cách ghi nhớ để tránh sai lần sau.\n" +
                "- QUAN TRỌNG: \"Đáp án đúng\" được cung cấp bên trên CHỈ LÀ dữ liệu từ hệ thống, có thể sai.\n" +
                "  Bạn phải độc lập đánh giá dựa trên kiến thức chuẩn của mình. Nếu nhận thấy \"Đáp án đúng\" hệ thống\n" +
                "  cung cấp mâu thuẫn với kiến thức thực tế chính xác, hãy lịch sự nêu rõ: \"Có vẻ đáp án mẫu của hệ thống\n" +
                "  không chính xác\", chỉ ra đáp án đúng thật sự là gì, và khẳng định lựa chọn đúng về mặt kiến thức.\n" +
                "  TUYỆT ĐỐI không hợp thức hóa hay bao biện cho một đáp án sai.\n";
        }

        return String.format(
                "Bạn là một giảng viên dạy tại một trường đại học, chuyên môn cao, giảng dạy dễ hiểu bằng tiếng Việt.\n" +
                "\n" +
                "Hãy giải thích cho câu hỏi trắc nghiệm sau đây bằng tiếng Việt, khoa học và dễ hiểu.\n" +
                "\n" +
                "%s\n" +
                "\n" +
                "Câu hỏi: %s\n" +
                "Đáp án đúng: %s\n" +
                "Học sinh đã chọn: %s\n" +
                "Môn học: %s\n" +
                "\n" +
                "Yêu cầu:\n" +
                "- Xưng hô: luôn gọi học sinh là \"Bạn\", không dùng \"Em\" hay \"bạn học sinh\".\n" +
                "- TRẢ LỜI THẲNG vào nội dung giải thích, KHÔNG được mở đầu bằng lời chào\n" +
                "  (như \"Chào bạn\", \"Xin chào\", \"Để thầy giải thích\", \"Chào bạn để thầy\") hay phần giới thiệu bản thân.\n" +
                "- Không dùng từ xưng hô ngôi thứ nhất cho bản thân như \"thầy\", \"tôi\".\n" +
                "- Cấu trúc: câu đầu tiên phải bắt đầu ngay bằng nội dung về đáp án/kiến thức.\n" +
                "%s" +
                "- Không lan man, không lặp lại nội dung câu hỏi.",
                analysisStatus,
                nullSafe(req.getQuestion()),
                nullSafe(req.getCorrectAnswer()),
                nullSafe(req.getStudentAnswer()),
                nullSafe(req.getSubject()),
                requirements
        );
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "(không cung cấp)" : value;
    }
}
