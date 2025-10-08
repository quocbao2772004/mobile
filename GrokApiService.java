// Khai báo package, nơi chứa tệp này trong cấu trúc thư mục của dự án.
package com.example.newsai;

// Import các lớp cần thiết.
import android.os.Handler; // Dùng để gửi và xử lý các đối tượng Message và Runnable gắn với một luồng (thread).
import android.os.Looper; // Dùng để chạy một vòng lặp tin nhắn cho một luồng.
import android.util.Log; // Dùng để ghi lại thông tin gỡ lỗi (debug) trong Logcat.

// Import các lớp để làm việc với JSON.
import org.json.JSONArray; // Đại diện cho một mảng JSON.
import org.json.JSONObject; // Đại diện cho một đối tượng JSON.

import java.io.IOException; // Lớp ngoại lệ cho các lỗi I/O.
import java.util.concurrent.ExecutorService; // Dịch vụ để quản lý và thực thi các tác vụ bất đồng bộ trên một luồng nền.
import java.util.concurrent.Executors; // Lớp tiện ích để tạo các ExecutorService.

// Import các lớp từ thư viện OkHttp3, dùng để thực hiện các yêu cầu HTTP.
import okhttp3.MediaType; // Đại diện cho kiểu media (MIME type) của một request/response.
import okhttp3.OkHttpClient; // Client chính để thực hiện các cuộc gọi HTTP.
import okhttp3.Request; // Đại diện cho một yêu cầu HTTP.
import okhttp3.RequestBody; // Đại diện cho phần thân (body) của một yêu cầu HTTP.
import okhttp3.Response; // Đại diện cho một phản hồi HTTP.

// Khai báo lớp GrokApiService. Lớp này quản lý tất cả logic để gửi tin nhắn đến Grok và nhận phản hồi.
public class GrokApiService {
    // Thẻ (Tag) dùng để lọc log trong Logcat, giúp dễ dàng tìm thấy các thông điệp từ lớp này.
    private static final String TAG = "GrokApiService";
    // URL của API endpoint cho việc hoàn thành chat của Grok.
    private static final String API_URL = "https://api.x.ai/v1/chat/completions";
    // API Key để xác thực với dịch vụ của Grok. **LƯU Ý QUAN TRỌNG**: Để trống API Key trong code và đưa vào qua các biến môi trường hoặc file cấu hình an toàn là một thực hành tốt nhất.
    private static final String API_KEY = "";
    
    // --- KHAI BÁO BIẾN ---
    private final OkHttpClient client; // Client để thực hiện các cuộc gọi mạng.
    private final ExecutorService executorService; // Dịch vụ để chạy các tác vụ mạng trên một luồng nền, tránh làm đơ giao diện người dùng (UI Thread).
    private final Handler mainHandler; // Handler để post kết quả trở lại luồng chính (UI Thread) một cách an toàn.

    // --- INTERFACE CALLBACK ---
    // Định nghĩa một "hợp đồng" để giao tiếp ngược lại với nơi đã gọi service (ví dụ: ChatbotActivity).
    public interface GrokCallback {
        void onSuccess(String response); // Sẽ được gọi khi nhận được phản hồi thành công từ AI.
        void onError(String error); // Sẽ được gọi khi có lỗi xảy ra.
    }

    // --- CONSTRUCTOR ---
    // Hàm khởi tạo của lớp.
    public GrokApiService() {
        // Tạo một OkHttpClient với thời gian chờ (timeout) tùy chỉnh.
        client = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS) // Thời gian chờ kết nối là 30 giây.
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)    // Thời gian chờ đọc dữ liệu là 30 giây.
                .build();
        // Tạo một ExecutorService chỉ với một luồng duy nhất để đảm bảo các yêu cầu được thực hiện tuần tự.
        executorService = Executors.newSingleThreadExecutor();
        // Tạo một Handler gắn với luồng chính của ứng dụng.
        mainHandler = new Handler(Looper.getMainLooper());
    }

    // --- PHƯƠNG THỨC CHÍNH ĐỂ GỬI TIN NHẮN ---
    public void sendMessage(String userMessage, GrokCallback callback) {
        // `execute()`: Giao tác vụ xây dựng yêu cầu và gọi mạng cho luồng nền.
        executorService.execute(() -> {
            try {
                // --- BƯỚC 1: XÂY DỰNG BODY CỦA YÊU CẦU (REQUEST BODY) DẠNG JSON ---
                JSONObject requestBody = new JSONObject();
                requestBody.put("model", "grok-4-fast-non-reasoning"); // Chỉ định model AI sẽ sử dụng.
                requestBody.put("temperature", 0.7); // Độ "sáng tạo" của câu trả lời (0.0-1.0).
                requestBody.put("max_tokens", 1000); // Số lượng token tối đa trong câu trả lời.
                
                // Tạo một mảng JSON để chứa lịch sử cuộc trò chuyện.
                JSONArray messages = new JSONArray();
                
                // Tạo tin nhắn hệ thống (system message) để định hình vai trò và hành vi của AI.
                JSONObject systemMsg = new JSONObject();
                systemMsg.put("role", "system");
                systemMsg.put("content", "Bạn là NewsBot, một trợ lý AI chuyên về tin tức và sự kiện thời sự. Hãy trả lời bằng tiếng Việt một cách thân thiện và chính xác.");
                messages.put(systemMsg); // Thêm tin nhắn hệ thống vào mảng.
                
                // Tạo tin nhắn của người dùng.
                JSONObject userMsg = new JSONObject();
                userMsg.put("role", "user");
                userMsg.put("content", userMessage); // Nội dung là tin nhắn người dùng gửi lên.
                messages.put(userMsg); // Thêm tin nhắn người dùng vào mảng.
                
                requestBody.put("messages", messages); // Đặt mảng tin nhắn vào body của yêu cầu.

                // Tạo đối tượng RequestBody của OkHttp từ chuỗi JSON.
                RequestBody body = RequestBody.create(
                    requestBody.toString(),
                    MediaType.parse("application/json; charset=utf-8")
                );
                
                // --- BƯỚC 2: XÂY DỰNG YÊU CẦU HTTP (HTTP REQUEST) ---
                Request request = new Request.Builder()
                        .url(API_URL) // Đặt URL đích.
                        .addHeader("Authorization", "Bearer " + API_KEY) // Thêm header Authorization với API Key.
                        .addHeader("Content-Type", "application/json") // Thêm header chỉ định kiểu nội dung là JSON.
                        .post(body) // Chỉ định phương thức là POST và gắn body đã tạo vào.
                        .build();
                
                // --- BƯỚC 3: THỰC THI YÊU CẦU VÀ XỬ LÝ PHẢN HỒI ---
                // `execute()`: Thực hiện cuộc gọi mạng một cách đồng bộ (synchronous) trên luồng nền.
                Response response = client.newCall(request).execute();
                
                // Nếu phản hồi thành công (HTTP code 2xx)...
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string(); // Đọc nội dung phản hồi.
                    Log.d(TAG, "Response: " + responseBody);
                    
                    // --- BƯỚC 4: PHÂN TÍCH PHẢN HỒI JSON ---
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray choices = jsonResponse.getJSONArray("choices"); // Lấy mảng 'choices'.
                    if (choices.length() > 0) {
                        JSONObject firstChoice = choices.getJSONObject(0); // Lấy lựa chọn đầu tiên.
                        JSONObject message = firstChoice.getJSONObject("message"); // Lấy đối tượng 'message' bên trong.
                        String content = message.getString("content"); // Lấy nội dung câu trả lời của AI.
                        
                        // --- BƯỚC 5: TRẢ KẾT QUẢ VỀ LUỒNG CHÍNH ---
                        // Dùng mainHandler.post() để đảm bảo callback.onSuccess() được chạy trên UI Thread.
                        mainHandler.post(() -> callback.onSuccess(content));
                    } else {
                        mainHandler.post(() -> callback.onError("Không có phản hồi từ Grok"));
                    }
                } else { // Nếu phản hồi thất bại...
                    String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                    Log.e(TAG, "API Error: " + response.code() + " - " + errorBody);
                    mainHandler.post(() -> callback.onError("Lỗi API: " + response.code()));
                }
                
            } catch (IOException e) { // Bắt lỗi mạng (ví dụ: không có internet).
                Log.e(TAG, "Network error", e);
                mainHandler.post(() -> callback.onError("Lỗi kết nối: " + e.getMessage()));
            } catch (Exception e) { // Bắt các lỗi khác (ví dụ: lỗi phân tích JSON).
                Log.e(TAG, "Error parsing response", e);
                mainHandler.post(() -> callback.onError("Lỗi xử lý: " + e.getMessage()));
            }
        });
    }

    // Phương thức để tắt ExecutorService khi không cần dùng đến nữa, giúp giải phóng tài nguyên.
    public void shutdown() {
        executorService.shutdown();
    }
}
