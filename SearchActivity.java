// Khai báo package, nơi chứa tệp này trong cấu trúc thư mục của dự án.
package com.example.newsai;

// Import các lớp (class) cần thiết từ thư viện của AndroidX.
import android.os.Bundle; // Dùng để truyền dữ liệu và lưu trạng thái.

import androidx.activity.EdgeToEdge; // Một API mới giúp ứng dụng dễ dàng hiển thị nội dung tràn ra các cạnh màn hình (edge-to-edge).
import androidx.appcompat.app.AppCompatActivity; // Lớp cơ sở cho các Activity.
import androidx.core.graphics.Insets; // Đại diện cho các khoảng trống (padding) do các thanh hệ thống (thanh trạng thái, thanh điều hướng) gây ra.
import androidx.core.view.ViewCompat; // Lớp tiện ích để làm việc với các View một cách tương thích ngược.
import androidx.core.view.WindowInsetsCompat; // Lớp xử lý các khoảng trống (insets) của cửa sổ ứng dụng.
import android.content.Intent; // Dùng để khởi tạo và thực hiện việc chuyển đổi giữa các màn hình.
import androidx.activity.OnBackPressedCallback; // Một lớp để xử lý sự kiện khi người dùng nhấn nút "Back" vật lý hoặc cử chỉ quay lại.


// Khai báo lớp SearchActivity, kế thừa từ AppCompatActivity.
public class SearchActivity extends AppCompatActivity {

    // --- PHƯƠNG THỨC onCreate() ---
    // Được gọi đầu tiên khi Activity được tạo ra.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Luôn gọi phương thức của lớp cha đầu tiên.

        // Kích hoạt chế độ hiển thị tràn cạnh (Edge-to-Edge).
        // Giao diện của ứng dụng sẽ được vẽ bên dưới cả thanh trạng thái và thanh điều hướng hệ thống.
        EdgeToEdge.enable(this);

        // Gắn layout từ file activity_search.xml vào màn hình này.
        setContentView(R.layout.activity_search);

        // --- XỬ LÝ SỰ KIỆN NHẤN NÚT "BACK" ---
        // `getOnBackPressedDispatcher()`: Lấy trình điều phối (dispatcher) quản lý các callback cho nút Back.
        // `.addCallback(this, new OnBackPressedCallback(true) { ... })`: Thêm một callback mới để tùy chỉnh hành vi của nút Back.
        // `this`: Callback này sẽ chỉ hoạt động khi SearchActivity đang trong trạng thái started (lifecycle-aware).
        // `new OnBackPressedCallback(true)`: `true` có nghĩa là callback này được kích hoạt.
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            
            // `handleOnBackPressed()`: Phương thức này sẽ được gọi thay cho hành vi mặc định (là đóng Activity)
            // khi người dùng nhấn nút Back.
            @Override
            public void handleOnBackPressed() {
                // Tạo một Intent mới để quay trở về màn hình chính (MainActivity).
                Intent intent = new Intent(SearchActivity.this, MainActivity.class);
                
                // Bắt đầu chuyển màn hình.
                startActivity(intent);
                
                // Đóng màn hình SearchActivity hiện tại lại.
                finish();
            }
        });
    }; // Dấu chấm phẩy ở đây là không cần thiết về mặt cú pháp nhưng không gây lỗi.

}
