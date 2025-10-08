// Khai báo package, nơi chứa tệp này trong cấu trúc thư mục của dự án.
package com.example.newsai;

// Import các lớp (class) cần thiết từ thư viện của Android.
import android.os.Bundle; // Dùng để truyền dữ liệu và lưu trạng thái.
import android.widget.EditText; // Thành phần để người dùng nhập văn bản.
import android.widget.ImageButton; // Một nút bấm hiển thị hình ảnh thay vì văn bản.
import android.widget.Toast; // Dùng để hiển thị một thông báo nhỏ trên màn hình.
import androidx.appcompat.app.AppCompatActivity; // Lớp cơ sở cho các Activity.
import androidx.recyclerview.widget.LinearLayoutManager; // Quản lý cách sắp xếp các item trong RecyclerView theo chiều dọc hoặc ngang.
import androidx.recyclerview.widget.RecyclerView; // Dùng để hiển thị các danh sách lớn một cách hiệu quả.
import java.util.ArrayList; // Lớp triển khai của List, có thể thay đổi kích thước.
import java.util.List; // Giao diện đại diện cho một danh sách các đối tượng.

// Khai báo lớp ChatbotActivity, kế thừa từ AppCompatActivity.
public class ChatbotActivity extends AppCompatActivity {
    // --- KHAI BÁO BIẾN TOÀN CỤC (MEMBER VARIABLES) ---
    private RecyclerView recyclerView; // Biến đại diện cho danh sách hiển thị cuộc trò chuyện.
    private ChatAdapter chatAdapter; // Adapter để kết nối dữ liệu (danh sách tin nhắn) với RecyclerView.
    private List<ChatMessage> messageList; // Danh sách để lưu trữ tất cả các tin nhắn trong cuộc trò chuyện.
    private EditText edtMessage; // Ô để người dùng nhập tin nhắn.
    private ImageButton btnSend; // Nút để gửi tin nhắn.
    private GrokApiService grokApiService; // Đối tượng dịch vụ để giao tiếp với Grok AI.

    // --- PHƯƠNG THỨC onCreate() ---
    // Được gọi đầu tiên khi Activity được tạo ra.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Gắn layout từ file activity_chatbot.xml vào màn hình này.
        setContentView(R.layout.activity_chatbot);

        // Ánh xạ các thành phần giao diện từ file XML.
        recyclerView = findViewById(R.id.recyclerViewChat);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);
        ImageButton btnBack = findViewById(R.id.btnBack);

        // Khởi tạo danh sách tin nhắn.
        messageList = new ArrayList<>();
        // Khởi tạo adapter với danh sách tin nhắn.
        chatAdapter = new ChatAdapter(messageList, this);

        // --- CẤU HÌNH RECYCLERVIEW ---
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        // `setStackFromEnd(true)`: Khi có tin nhắn mới, RecyclerView sẽ tự động cuộn xuống dưới cùng.
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdapter);

        // Khởi tạo dịch vụ Grok AI.
        grokApiService = new GrokApiService();

        // --- TẠO TIN NHẮN CHÀO MỪNG BAN ĐẦU ---
        // Tạo một đối tượng ChatMessage cho tin nhắn chào mừng từ bot.
        ChatMessage welcomeMsg = new ChatMessage(
            "Xin chào, mình là NewsBot được hỗ trợ bởi Grok AI. Tôi có thể giúp bạn về tin tức, bạn có vấn đề thắc mắc nào về tin: \"Cuộc xung đột giữa Israel và phong trào Hamas: Hơn 1.100 người thiệt mạng trong cuộc chiến đẫm máu\"",
            false // `false` có nghĩa đây là tin nhắn của bot.
        );
        // Thêm các câu hỏi gợi ý cho người dùng.
        welcomeMsg.setSuggestions(new String[]{
            "Cuộc xung đột này bắt đầu từ khi nào?",
            "Nguyên nhân của xung đột là gì?"
        });
        messageList.add(welcomeMsg); // Thêm tin nhắn chào mừng vào danh sách.
        chatAdapter.notifyDataSetChanged(); // Báo cho adapter biết dữ liệu đã thay đổi để vẽ lại.

        // Gán sự kiện click cho nút gửi tin nhắn. Khi nhấn, gọi hàm sendMessage().
        btnSend.setOnClickListener(v -> sendMessage());
        // Gán sự kiện click cho nút quay lại. Khi nhấn, đóng màn hình hiện tại.
        btnBack.setOnClickListener(v -> finish());

        // Xử lý sự kiện khi người dùng nhấn vào một trong các câu hỏi gợi ý.
        chatAdapter.setOnSuggestionClickListener(suggestion -> {
            edtMessage.setText(suggestion); // Đặt nội dung gợi ý vào ô nhập liệu.
            sendMessage(); // Tự động gửi tin nhắn.
        });
    }

    // --- PHƯƠNG THỨC onDestroy() ---
    // Được gọi khi Activity sắp bị hủy.
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Kiểm tra xem grokApiService có tồn tại không.
        if (grokApiService != null) {
            // Tắt ExecutorService bên trong grokApiService để giải phóng tài nguyên và tránh rò rỉ bộ nhớ.
            grokApiService.shutdown();
        }
    }

    // --- HÀM XỬ LÝ VIỆC GỬI TIN NHẮN ---
    private void sendMessage() {
        // Lấy nội dung văn bản từ ô nhập liệu và xóa khoảng trắng thừa.
        String messageText = edtMessage.getText().toString().trim();
        // Chỉ xử lý nếu tin nhắn không rỗng.
        if (!messageText.isEmpty()) {
            // --- BƯỚC 1: HIỂN THỊ TIN NHẮN CỦA NGƯỜI DÙNG ---
            messageList.add(new ChatMessage(messageText, true)); // Thêm tin nhắn vào danh sách (`true` = tin của người dùng).
            chatAdapter.notifyItemInserted(messageList.size() - 1); // Thông báo cho adapter có một mục mới được thêm vào cuối.
            recyclerView.scrollToPosition(messageList.size() - 1); // Cuộn xuống tin nhắn mới nhất.
            edtMessage.setText(""); // Xóa trắng ô nhập liệu.

            // Vô hiệu hóa nút gửi để người dùng không thể gửi liên tục trong khi chờ AI trả lời.
            btnSend.setEnabled(false);

            // --- BƯỚC 2: HIỂN THỊ TRẠNG THÁI "ĐANG SUY NGHĨ..." ---
            ChatMessage thinkingMsg = new ChatMessage("Đang suy nghĩ...", false); // Tạo tin nhắn chờ.
            messageList.add(thinkingMsg); // Thêm tin nhắn chờ vào danh sách.
            int thinkingPosition = messageList.size() - 1; // Lưu lại vị trí của tin nhắn chờ.
            chatAdapter.notifyItemInserted(thinkingPosition); // Cập nhật giao diện.
            recyclerView.scrollToPosition(thinkingPosition); // Cuộn xuống.

            // --- BƯỚC 3: GỬI YÊU CẦU ĐẾN GROK AI ---
            // Gọi hàm sendMessage của grokApiService và truyền vào một callback để xử lý kết quả.
            grokApiService.sendMessage(messageText, new GrokApiService.GrokCallback() {
                // onSuccess được gọi (trên UI Thread) khi nhận được phản hồi thành công.
                @Override
                public void onSuccess(String response) {
                    // Xóa tin nhắn "Đang suy nghĩ..." khỏi danh sách.
                    messageList.remove(thinkingPosition);
                    chatAdapter.notifyItemRemoved(thinkingPosition); // Cập nhật giao diện.

                    // Thêm câu trả lời thực sự từ Grok vào danh sách.
                    messageList.add(new ChatMessage(response, false));
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);

                    // Kích hoạt lại nút gửi.
                    btnSend.setEnabled(true);
                }

                // onError được gọi (trên UI Thread) khi có lỗi xảy ra.
                @Override
                public void onError(String error) {
                    // Xóa tin nhắn "Đang suy nghĩ...".
                    messageList.remove(thinkingPosition);
                    chatAdapter.notifyItemRemoved(thinkingPosition);

                    // Hiển thị thông báo lỗi ngắn gọn cho người dùng.
                    Toast.makeText(ChatbotActivity.this, error, Toast.LENGTH_SHORT).show();

                    // Thêm một tin nhắn lỗi vào cuộc trò chuyện để thông báo rõ ràng hơn.
                    messageList.add(new ChatMessage(
                        "Xin lỗi, hiện tại tôi gặp sự cố kết nối với Grok AI. Vui lòng thử lại sau.",
                        false
                    ));
                    chatAdapter.notifyItemInserted(messageList.size() - 1);
                    recyclerView.scrollToPosition(messageList.size() - 1);

                    // Kích hoạt lại nút gửi.
                    btnSend.setEnabled(true);
                }
            });
        }
    }
}
