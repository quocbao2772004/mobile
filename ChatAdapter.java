// Khai báo package, nơi chứa tệp này.
package com.example.newsai;

// Import các lớp (class) cần thiết.
import android.content.Context; // Cung cấp quyền truy cập vào các tài nguyên và dịch vụ của ứng dụng.
import android.view.LayoutInflater; // Dùng để "thổi phồng" (inflate) file layout XML thành đối tượng View.
import android.view.View; // Lớp cơ sở của các thành phần giao diện.
import android.view.ViewGroup; // Một View có thể chứa các View khác.
import android.widget.LinearLayout; // Layout sắp xếp các View con theo chiều dọc hoặc ngang.
import android.widget.TextView; // Hiển thị văn bản.
import androidx.annotation.NonNull; // Annotation để chỉ ra rằng giá trị không được null.
import androidx.recyclerview.widget.RecyclerView; // Lớp cơ sở để tạo Adapter cho danh sách.
import com.google.android.material.button.MaterialButton; // Một loại nút bấm theo phong cách Material Design.
import java.util.List; // Giao diện đại diện cho một danh sách các đối tượng.

// Khai báo lớp ChatAdapter.
public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    // --- KHAI BÁO HẰNG SỐ ĐỂ PHÂN BIỆT CÁC LOẠI VIEW ---
    private static final int VIEW_TYPE_USER = 1; // Hằng số đại diện cho tin nhắn của người dùng.
    private static final int VIEW_TYPE_BOT = 2; // Hằng số đại diện cho tin nhắn của bot.

    // --- KHAI BÁO BIẾN ---
    private List<ChatMessage> messageList; // Danh sách chứa tất cả các tin nhắn.
    private Context context; // Context của Activity đang sử dụng Adapter này.
    private OnSuggestionClickListener suggestionClickListener; // Biến để lưu trữ listener cho sự kiện click vào gợi ý.

    // --- INTERFACE ĐỂ XỬ LÝ CLICK GỢI Ý ---
    // Định nghĩa một "hợp đồng" để ChatbotActivity có thể lắng nghe sự kiện click vào các nút gợi ý.
    public interface OnSuggestionClickListener {
        void onSuggestionClick(String suggestion);
    }

    // --- CONSTRUCTOR ---
    public ChatAdapter(List<ChatMessage> messageList, Context context) {
        this.messageList = messageList;
        this.context = context;
    }

    // Phương thức để Activity có thể "đăng ký" lắng nghe sự kiện click gợi ý.
    public void setOnSuggestionClickListener(OnSuggestionClickListener listener) {
        this.suggestionClickListener = listener;
    }

    // --- PHƯƠNG THỨC getItemViewType() ---
    // **QUAN TRỌNG**: Đây là phương thức cốt lõi để RecyclerView biết cần sử dụng layout nào.
    // Nó được gọi cho mỗi item trong danh sách.
    @Override
    public int getItemViewType(int position) {
        // Lấy tin nhắn tại vị trí `position`.
        // Dùng toán tử ba ngôi: Nếu `isUser()` trả về true, trả về hằng số VIEW_TYPE_USER. Ngược lại, trả về VIEW_TYPE_BOT.
        return messageList.get(position).isUser() ? VIEW_TYPE_USER : VIEW_TYPE_BOT;
    }

    // --- PHƯƠNG THỨC onCreateViewHolder() ---
    // Được gọi khi RecyclerView cần tạo một ViewHolder MỚI.
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Dựa vào `viewType` đã được xác định ở trên, nó sẽ quyết định inflate layout nào.
        if (viewType == VIEW_TYPE_USER) {
            // Nếu là tin nhắn của người dùng, inflate layout item_message_user.xml.
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_user, parent, false);
            return new UserMessageViewHolder(view); // Trả về ViewHolder cho tin nhắn người dùng.
        } else {
            // Nếu là tin nhắn của bot, inflate layout item_message_bot.xml.
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_bot, parent, false);
            return new BotMessageViewHolder(view); // Trả về ViewHolder cho tin nhắn của bot.
        }
    }

    // --- PHƯƠNG THỨC onBindViewHolder() ---
    // Được gọi khi RecyclerView muốn hiển thị dữ liệu tại một vị trí cụ thể.
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessage message = messageList.get(position); // Lấy dữ liệu tin nhắn tại vị trí `position`.
        // `instanceof` kiểm tra xem `holder` thuộc kiểu ViewHolder nào.
        if (holder instanceof UserMessageViewHolder) {
            // Nếu là UserMessageViewHolder, gọi hàm bind của nó.
            ((UserMessageViewHolder) holder).bind(message);
        } else {
            // Nếu là BotMessageViewHolder, gọi hàm bind của nó, truyền thêm cả listener.
            ((BotMessageViewHolder) holder).bind(message, suggestionClickListener);
        }
    }

    // --- PHƯƠNG THỨC getItemCount() ---
    // Trả về tổng số tin nhắn trong danh sách.
    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // --- LỚP VIEWHOLDER CHO TIN NHẮN CỦA NGƯỜI DÙNG ---
    static class UserMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage; // Chỉ chứa một TextView để hiển thị nội dung tin nhắn.

        UserMessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvUserMessage);
        }

        // Hàm để đổ dữ liệu vào View.
        void bind(ChatMessage message) {
            tvMessage.setText(message.getContent());
        }
    }

    // --- LỚP VIEWHOLDER CHO TIN NHẮN CỦA BOT ---
    static class BotMessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage; // TextView để hiển thị nội dung tin nhắn của bot.
        LinearLayout suggestionsLayout; // Layout để chứa các nút gợi ý được tạo động.

        BotMessageViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvBotMessage);
            suggestionsLayout = itemView.findViewById(R.id.suggestionsLayout);
        }

        // Hàm để đổ dữ liệu vào View, có thêm logic xử lý các gợi ý.
        void bind(ChatMessage message, OnSuggestionClickListener listener) {
            tvMessage.setText(message.getContent());

            // Kiểm tra xem tin nhắn này có chứa gợi ý không.
            if (message.hasSuggestions()) {
                suggestionsLayout.setVisibility(View.VISIBLE); // Hiện layout chứa gợi ý.
                suggestionsLayout.removeAllViews(); // Xóa tất cả các nút gợi ý cũ (nếu có) để tránh trùng lặp khi tái sử dụng ViewHolder.

                // Dùng vòng lặp 'for' để duyệt qua từng chuỗi gợi ý.
                for (String suggestion : message.getSuggestions()) {
                    // --- TẠO NÚT BẤM MỘT CÁCH TỰ ĐỘNG (PROGRAMMATICALLY) ---
                    // Tạo một đối tượng MaterialButton mới.
                    MaterialButton btnSuggestion = new MaterialButton(itemView.getContext(), null,
                            com.google.android.material.R.attr.materialButtonOutlinedStyle);
                    btnSuggestion.setText(suggestion); // Đặt nội dung cho nút.
                    // Tùy chỉnh giao diện cho nút (màu chữ, màu viền, bo góc).
                    btnSuggestion.setTextColor(itemView.getContext().getColor(R.color.blue_primary));
                    btnSuggestion.setStrokeColor(itemView.getContext().getColorStateList(R.color.blue_primary));
                    btnSuggestion.setCornerRadius(50);

                    // Tạo các tham số layout (LayoutParams) để định dạng vị trí và kích thước cho nút.
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, // Chiều rộng vừa với nội dung.
                            LinearLayout.LayoutParams.WRAP_CONTENT  // Chiều cao vừa với nội dung.
                    );
                    params.setMargins(0, 8, 0, 8); // Đặt lề trên và dưới cho nút.
                    btnSuggestion.setLayoutParams(params); // Áp dụng các tham số layout.

                    // Gán sự kiện click cho nút gợi ý vừa tạo.
                    btnSuggestion.setOnClickListener(v -> {
                        if (listener != null) {
                            // Khi nhấn, gọi phương thức onSuggestionClick của listener và truyền vào nội dung gợi ý.
                            listener.onSuggestionClick(suggestion);
                        }
                    });

                    // Thêm nút vừa tạo vào suggestionsLayout.
                    suggestionsLayout.addView(btnSuggestion);
                }
            } else {
                // Nếu tin nhắn không có gợi ý, ẩn layout chứa gợi ý đi.
                suggestionsLayout.setVisibility(View.GONE);
            }
        }
    }
}
