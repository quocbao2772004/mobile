// Khai báo package, nơi chứa tệp này trong cấu trúc thư mục của dự án.
package com.example.newsai;

// Import các lớp (class) cần thiết từ thư viện của Android và Firebase.
import androidx.appcompat.app.AppCompatActivity; // Lớp cơ sở cho các Activity.
import android.os.Bundle; // Dùng để truyền dữ liệu và lưu trạng thái.
import android.os.Handler; // Dùng để lên lịch thực thi một đoạn code sau một khoảng thời gian trễ.
import android.os.Looper; // Liên quan đến luồng (thread) chính của ứng dụng.
import android.content.Intent; // Dùng để khởi tạo và thực hiện việc chuyển đổi giữa các màn hình.

import com.google.firebase.auth.FirebaseAuth; // Lớp chính của Firebase Authentication.
import com.google.firebase.auth.FirebaseUser; // Lớp đại diện cho người dùng đang đăng nhập.

// Khai báo lớp SplashActivity, kế thừa từ AppCompatActivity.
public class SplashActivity extends AppCompatActivity {

    // --- PHƯƠNG THỨC onCreate() ---
    // Được gọi đầu tiên khi Activity được tạo ra.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Luôn gọi phương thức của lớp cha đầu tiên.
        
        // Gắn layout từ file activity_splash.xml vào màn hình này.
        // Layout này thường chỉ chứa logo hoặc tên ứng dụng.
        setContentView(R.layout.activity_splash);

        // --- TẠO MỘT TÁC VỤ TRÌ HOÃN (DELAYED TASK) ---
        // `new Handler(Looper.getMainLooper())` tạo một Handler gắn với luồng giao diện chính (UI Thread).
        // `.postDelayed(() -> { ... }, 3000)`: Lên lịch để đoạn code bên trong cặp ngoặc nhọn `{...}`
        // được thực thi sau một khoảng thời gian trễ là 3000 mili giây (tức là 3 giây).
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            
            // --- LOGIC ĐIỀU HƯỚNG SAU 3 GIÂY ---

            /*
            // Đoạn code bị comment này là cách làm đơn giản nhất:
            // Luôn chuyển đến màn hình Login sau 3 giây.
            Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            */

            // --- LOGIC KIỂM TRA ĐĂNG NHẬP ---
            // Lấy thông tin người dùng hiện tại đang đăng nhập vào Firebase.
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            
            // Kiểm tra xem đối tượng `user` có tồn tại (khác null) hay không.
            if (user != null) {
                // Nếu `user` khác null, có nghĩa là người dùng đã đăng nhập từ phiên trước.
                // Chuyển thẳng đến màn hình chính (MainActivity).
                startActivity(new Intent(this, MainActivity.class));
            } else {
                // Nếu `user` là null, có nghĩa là chưa có ai đăng nhập.
                // Chuyển đến màn hình đăng nhập (LoginActivity).
                startActivity(new Intent(this, LoginActivity.class));
            }
            
            // `finish()`: Đóng màn hình SplashActivity lại.
            // Việc này rất quan trọng để khi người dùng nhấn nút "Back" từ màn hình chính hoặc màn hình đăng nhập,
            // họ sẽ không quay lại màn hình chờ này nữa.
            finish();

        }, 3000); // 3000 mili giây = 3 giây.
    }
}
