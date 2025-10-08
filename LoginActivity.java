// Khai báo package, nơi chứa tệp này trong cấu trúc thư mục của dự án.
package com.example.newsai;

// Import các lớp (class) cần thiết từ thư viện của Android và các thư viện bên ngoài.
import android.content.Intent; // Dùng để khởi tạo và thực hiện việc chuyển đổi giữa các màn hình (Activity).
import android.os.Bundle; // Dùng để truyền dữ liệu giữa các Activity và lưu trạng thái.
import android.widget.Toast; // Dùng để hiển thị một thông báo nhỏ, ngắn gọn trên màn hình.

import androidx.annotation.Nullable; // Annotation chỉ ra rằng một giá trị có thể là null.
import androidx.appcompat.app.AppCompatActivity; // Lớp cơ sở cho các Activity.

// --- Import các lớp từ Facebook SDK ---
import com.facebook.AccessToken; // Đại diện cho mã thông báo truy cập của người dùng Facebook.
import com.facebook.CallbackManager; // Quản lý các callback (phản hồi) từ Facebook SDK.
import com.facebook.FacebookCallback; // Interface để xử lý kết quả đăng nhập Facebook (thành công, hủy, lỗi).
import com.facebook.FacebookException; // Lớp ngoại lệ cho các lỗi từ Facebook SDK.
import com.facebook.login.LoginManager; // Quản lý các hoạt động đăng nhập và đăng xuất của Facebook.
import com.facebook.login.LoginResult; // Chứa kết quả của một lần đăng nhập Facebook thành công.

// --- Import các lớp từ Google Sign-In SDK ---
import com.google.android.gms.auth.api.signin.*; // Chứa các lớp để thực hiện đăng nhập bằng Google.
import com.google.android.gms.common.api.ApiException; // Lớp ngoại lệ cho các lỗi từ Google API.
import com.google.android.gms.tasks.Task; // Đại diện cho một hoạt động bất đồng bộ của Google Play Services.

// --- Import các lớp từ Firebase Authentication ---
import com.google.firebase.auth.*; // Chứa các lớp cốt lõi để xác thực người dùng với Firebase.

// Import các lớp tiện ích khác.
import java.util.Arrays; // Dùng để tạo danh sách từ mảng một cách nhanh chóng.
import android.widget.TextView; // Thành phần để hiển thị văn bản.
import android.widget.EditText; // Thành phần để người dùng nhập văn bản.
import android.text.TextUtils; // Cung cấp các hàm tiện ích để xử lý chuỗi.

// Khai báo lớp LoginActivity, kế thừa từ AppCompatActivity.
public class LoginActivity extends AppCompatActivity {
    // Hằng số để nhận diện yêu cầu đăng nhập Google khi nhận kết quả trả về.
    private static final int RC_SIGN_IN = 1;
    // Đối tượng chính của Firebase Authentication, dùng để đăng ký, đăng nhập, quản lý người dùng.
    private FirebaseAuth mAuth;
    // Client để tương tác với Google Sign-In API.
    private GoogleSignInClient googleSignInClient;
    // Trình quản lý callback cho việc đăng nhập Facebook.
    private CallbackManager fbCallbackManager;
    // Các ô nhập liệu cho email và mật khẩu.
    private EditText edtEmail, edtPassword;

    // --- PHƯƠNG THỨC onCreate() ---
    // Được gọi đầu tiên khi Activity được tạo ra.
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Kiểm tra và khởi tạo Firebase App nếu nó chưa được khởi tạo.
        if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(this);
        }

        // Gắn layout từ file activity_login.xml vào màn hình này.
        setContentView(R.layout.activity_login);
        
        // Khởi tạo Facebook SDK.
        com.facebook.FacebookSdk.sdkInitialize(getApplicationContext());
        com.facebook.appevents.AppEventsLogger.activateApp(getApplication());
        
        // Lấy một thực thể (instance) của FirebaseAuth.
        mAuth = FirebaseAuth.getInstance();
        
        // Ánh xạ các EditText từ layout XML.
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        
        // Gán sự kiện click cho nút Đăng nhập bằng Email.
        findViewById(R.id.btnLogin).setOnClickListener(v -> loginWithEmail());
        
        // Gán sự kiện click cho TextView "Đăng ký".
        TextView signbtn = findViewById(R.id.tvSignup);
        signbtn.setOnClickListener(v -> {
            // Khi nhấn, tạo một Intent để chuyển sang màn hình SignupActivity.
            Intent intent = new Intent(LoginActivity.this, SignupActivity.class);
            startActivity(intent);
        });
        
        // Gán sự kiện click cho nút đóng (dấu X).
        findViewById(R.id.close_btn).setOnClickListener(v -> finish()); // `finish()` sẽ đóng Activity hiện tại.
        
        // --- CẤU HÌNH ĐĂNG NHẬP GOOGLE ---
        // Tạo đối tượng cấu hình cho Google Sign-In.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Yêu cầu ID Token, cần thiết để xác thực với Firebase.
                .requestEmail() // Yêu cầu quyền truy cập email của người dùng.
                .build();
        // Tạo GoogleSignInClient với các cấu hình đã định nghĩa.
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        // Gán sự kiện click cho nút Đăng nhập bằng Google.
        findViewById(R.id.btnGoogle).setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            // Mở màn hình chọn tài khoản Google của hệ thống. Kết quả sẽ được trả về trong onActivityResult.
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });

        // --- CẤU HÌNH ĐĂNG NHẬP FACEBOOK ---
        // Tạo một trình quản lý callback.
        fbCallbackManager = CallbackManager.Factory.create();
        // Gán sự kiện click cho nút Đăng nhập bằng Facebook.
        findViewById(R.id.btnFacebook).setOnClickListener(v -> {
            // Bắt đầu luồng đăng nhập, yêu cầu quyền đọc email và thông tin công khai.
            LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile"));
            // Đăng ký một callback để xử lý kết quả đăng nhập.
            LoginManager.getInstance().registerCallback(fbCallbackManager, new FacebookCallback<LoginResult>() {
                @Override
                public void onSuccess(LoginResult loginResult) {
                    // Nếu đăng nhập thành công, gọi hàm để xác thực với Firebase.
                    handleFacebookAccessToken(loginResult.getAccessToken());
                }
                @Override
                public void onCancel() {
                    // Nếu người dùng hủy đăng nhập.
                    Toast.makeText(LoginActivity.this, "Facebook login canceled", Toast.LENGTH_SHORT).show();
                }
                @Override
                public void onError(FacebookException error) {
                    // Nếu có lỗi xảy ra.
                    Toast.makeText(LoginActivity.this, "Facebook login error", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    // --- PHƯƠNG THỨC onActivityResult() ---
    // Được gọi khi một Activity mà bạn khởi tạo (như màn hình chọn tài khoản Google) trả về kết quả.
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Chuyển kết quả cho Facebook CallbackManager xử lý.
        fbCallbackManager.onActivityResult(requestCode, resultCode, data);

        // Kiểm tra xem kết quả này có phải là từ luồng đăng nhập Google không.
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Lấy thông tin tài khoản Google đã đăng nhập.
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null && account.getIdToken() != null) {
                    // Nếu thành công và có ID Token, dùng token này để xác thực với Firebase.
                    firebaseAuthWithGoogle(account.getIdToken());
                } else {
                    Toast.makeText(this, "Google Sign-In thất bại", Toast.LENGTH_SHORT).show();
                }
            } catch (ApiException e) {
                // Xử lý nếu có lỗi xảy ra trong quá trình đăng nhập Google.
                Toast.makeText(this, "Google Sign-In thất bại", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- XÁC THỰC VỚI FIREBASE BẰNG GOOGLE ---
    private void firebaseAuthWithGoogle(String idToken) {
        // Tạo một đối tượng "chứng chỉ" từ ID Token của Google.
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        // Dùng "chứng chỉ" này để đăng nhập vào Firebase.
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Nếu đăng nhập Firebase thành công, chuyển đến màn hình chính.
                goToMain();
            } else {
                Toast.makeText(this, "Google login failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- XÁC THỰC VỚI FIREBASE BẰNG FACEBOOK ---
    private void handleFacebookAccessToken(AccessToken token) {
        // Tạo một đối tượng "chứng chỉ" từ Access Token của Facebook.
        AuthCredential credential = FacebookAuthProvider.getCredential(token.getToken());
        // Dùng "chứng chỉ" này để đăng nhập vào Firebase.
        mAuth.signInWithCredential(credential).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Nếu đăng nhập Firebase thành công, chuyển đến màn hình chính.
                goToMain();
            } else {
                Toast.makeText(this, "Facebook login failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- ĐĂNG NHẬP BẰNG EMAIL VÀ MẬT KHẨU ---
    private void loginWithEmail() {
        String email = edtEmail.getText().toString().trim(); // Lấy email và xóa khoảng trắng thừa.
        String password = edtPassword.getText().toString().trim(); // Lấy mật khẩu.
        
        // --- KIỂM TRA DỮ LIỆU ĐẦU VÀO ---
        if (TextUtils.isEmpty(email)) {
            edtEmail.setError("Vui lòng nhập email");
            edtEmail.requestFocus();
            return; // Dừng hàm tại đây.
        }
        
        if (TextUtils.isEmpty(password)) {
            edtPassword.setError("Vui lòng nhập mật khẩu");
            edtPassword.requestFocus();
            return; // Dừng hàm tại đây.
        }
        
        // --- ĐĂNG NHẬP FIREBASE ---
        // Gọi hàm của Firebase Auth để đăng nhập.
        mAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this, task -> {
                if (task.isSuccessful()) {
                    // Nếu tác vụ thành công.
                    Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show();
                    goToMain(); // Chuyển đến màn hình chính.
                } else {
                    // Nếu tác vụ thất bại, phân tích lỗi để đưa ra thông báo phù hợp.
                    String errorMsg = "Đăng nhập thất bại";
                    if (task.getException() != null) {
                        String exception = task.getException().getMessage();
                        if (exception != null) {
                            if (exception.contains("no user record")) {
                                errorMsg = "Tài khoản không tồn tại";
                            } else if (exception.contains("password is invalid")) {
                                errorMsg = "Mật khẩu không đúng";
                            }
                        }
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
    }

    // --- HÀM TIỆN ÍCH: CHUYỂN ĐẾN MÀN HÌNH CHÍNH ---
    private void goToMain() {
        FirebaseUser user = mAuth.getCurrentUser(); // Lấy thông tin người dùng hiện tại.
        if (user != null) {
            // Lấy email hoặc tên hiển thị để chào mừng.
            String nameOrEmail = (user.getEmail() != null ? user.getEmail() : user.getDisplayName());
            Toast.makeText(this, "Xin chào: " + nameOrEmail, Toast.LENGTH_SHORT).show();
            // Tạo Intent và chuyển đến MainActivity.
            startActivity(new Intent(this, MainActivity.class));
            // Đóng màn hình LoginActivity lại để người dùng không thể quay lại bằng nút Back.
            finish();
        }
    }
}
