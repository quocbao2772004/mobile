// Khai báo package, nơi chứa tệp này trong cấu trúc thư mục của dự án.
package com.example.newsai;

// Import các lớp (class) cần thiết từ thư viện của Android và các thư viện bên ngoài.
import androidx.annotation.Nullable; // Annotation chỉ ra rằng một giá trị có thể là null.
import androidx.appcompat.app.AppCompatActivity; // Lớp cơ sở cho các Activity.
import android.os.Bundle; // Dùng để truyền dữ liệu và lưu trạng thái.
import android.content.Intent; // Dùng để khởi tạo và thực hiện việc chuyển đổi giữa các màn hình.
import android.text.TextUtils; // Cung cấp các hàm tiện ích để xử lý chuỗi (ví dụ: kiểm tra rỗng).
import android.widget.EditText; // Thành phần để người dùng nhập văn bản.
import android.widget.TextView; // Thành phần để hiển thị văn bản.
import android.widget.Toast; // Dùng để hiển thị một thông báo nhỏ trên màn hình.

// Import các lớp từ Facebook, Google và Firebase SDK, tương tự như LoginActivity.
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;

import com.google.android.gms.auth.api.signin.*;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.*;

import java.util.Arrays; // Dùng để tạo danh sách từ mảng.

// Khai báo lớp SignupActivity, kế thừa từ AppCompatActivity.
public class SignupActivity extends AppCompatActivity {
    // Hằng số để nhận diện yêu cầu đăng nhập Google.
    private static final int RC_SIGN_IN = 1;
    // Đối tượng chính của Firebase Authentication.
    private FirebaseAuth mAuth;
    // Client để tương tác với Google Sign-In API.
    private GoogleSignInClient googleSignInClient;
    // Trình quản lý callback cho việc đăng nhập Facebook.
    private CallbackManager fbCallbackManager;
    // Các ô nhập liệu cho tên, email, mật khẩu và xác nhận mật khẩu.
    private EditText edtName, edtEmail, edtPassword, edtConfirm;

    // --- PHƯƠG THỨC onCreate() ---
    // Được gọi đầu tiên khi Activity được tạo ra.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Kiểm tra và khởi tạo Firebase App nếu nó chưa được khởi tạo.
        if (com.google.firebase.FirebaseApp.getApps(this).isEmpty()) {
            com.google.firebase.FirebaseApp.initializeApp(this);
        }
        // Gắn layout từ file activity_signup.xml vào màn hình này.
        setContentView(R.layout.activity_signup);

        // Lấy một thực thể (instance) của FirebaseAuth.
        mAuth = FirebaseAuth.getInstance();
        
        // Ánh xạ các EditText từ layout XML.
        edtName = findViewById(R.id.edtName);
        edtEmail = findViewById(R.id.edtEmail);
        edtPassword = findViewById(R.id.edtPassword);
        edtConfirm = findViewById(R.id.edtConfirm);

        // Gán sự kiện click cho nút "Đăng ký". Khi nhấn, sẽ gọi hàm registerWithEmail().
        findViewById(R.id.btnRegister).setOnClickListener(v -> registerWithEmail());
        
        // Gán sự kiện click cho TextView "Đăng nhập".
        findViewById(R.id.tvLogin).setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class)); // Chuyển sang màn hình Đăng nhập.
            finish(); // Đóng màn hình Đăng ký lại.
        });
        
        // Gán sự kiện click cho nút đóng (dấu X).
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        // --- CẤU HÌNH ĐĂNG NHẬP/ĐĂNG KÝ GOOGLE ---
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail().build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);
        findViewById(R.id.btnGoogle).setOnClickListener(v ->
                startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN));

        // --- CẤU HÌNH ĐĂNG NHẬP/ĐĂNG KÝ FACEBOOK ---
        com.facebook.FacebookSdk.sdkInitialize(getApplicationContext());
        fbCallbackManager = CallbackManager.Factory.create();
        findViewById(R.id.btnFacebook).setOnClickListener(v ->
                LoginManager.getInstance().logInWithReadPermissions(this, Arrays.asList("email", "public_profile")));
        LoginManager.getInstance().registerCallback(fbCallbackManager,
                new com.facebook.FacebookCallback<LoginResult>() {
                    @Override public void onSuccess(LoginResult result) {
                        // Nếu thành công, dùng token để xác thực với Firebase.
                        handleFacebookAccessToken(result.getAccessToken());
                    }
                    @Override public void onCancel() {
                        Toast.makeText(SignupActivity.this, "Facebook hủy", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onError(com.facebook.FacebookException error) {
                        Toast.makeText(SignupActivity.this, "Facebook lỗi", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- HÀM ĐĂNG KÝ TÀI KHOẢN MỚI BẰNG EMAIL ---
    private void registerWithEmail() {
        // Lấy dữ liệu người dùng nhập từ các EditText.
        String name = edtName.getText().toString().trim();
        String email = edtEmail.getText().toString().trim();
        String pass = edtPassword.getText().toString().trim();
        String confirm = edtConfirm.getText().toString().trim();

        // --- KIỂM TRA TÍNH HỢP LỆ CỦA DỮ LIỆU ---
        if (TextUtils.isEmpty(name) || TextUtils.isEmpty(email) ||
            TextUtils.isEmpty(pass) || !pass.equals(confirm) || pass.length() < 6) {
            // Nếu có bất kỳ trường nào rỗng, hoặc mật khẩu không khớp, hoặc mật khẩu ngắn hơn 6 ký tự...
            Toast.makeText(this, "Vui lòng nhập đúng thông tin", Toast.LENGTH_SHORT).show();
            return; // ...thì hiển thị thông báo và dừng hàm.
        }

        // --- TẠO NGƯỜI DÙNG TRÊN FIREBASE ---
        // Gọi hàm `createUserWithEmailAndPassword` để tạo một người dùng mới.
        mAuth.createUserWithEmailAndPassword(email, pass).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                // Nếu việc tạo người dùng thành công.
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    // Cập nhật thông tin hồ sơ của người dùng mới, cụ thể là thêm "Tên hiển thị".
                    user.updateProfile(new UserProfileChangeRequest.Builder()
                            .setDisplayName(name).build());
                }
                Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show();
                goToMain(); // Chuyển đến màn hình chính.
            } else {
                // Nếu việc tạo người dùng thất bại (ví dụ: email đã tồn tại).
                Toast.makeText(this, "Đăng ký thất bại", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // --- PHƯƠNG THỨC onActivityResult() ---
    // Xử lý kết quả trả về từ các luồng đăng nhập Google và Facebook.
    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        fbCallbackManager.onActivityResult(requestCode, resultCode, data); // Chuyển kết quả cho Facebook SDK.
        if (requestCode == RC_SIGN_IN) { // Nếu đây là kết quả từ Google Sign-In.
            try {
                // Lấy thông tin tài khoản Google.
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                if (account != null && account.getIdToken() != null) {
                    // Nếu thành công, dùng ID Token để xác thực với Firebase.
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Toast.makeText(this, "Google đăng ký thất bại", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // --- XÁC THỰC VỚI FIREBASE BẰNG GOOGLE ---
    // Logic này giống hệt LoginActivity: Firebase sẽ tự động tạo người dùng mới nếu đây là lần đầu tiên
    // tài khoản Google này được sử dụng để đăng nhập vào ứng dụng của bạn.
    private void firebaseAuthWithGoogle(String idToken) {
        mAuth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) goToMain();
                    else Toast.makeText(this, "Google đăng ký thất bại", Toast.LENGTH_SHORT).show();
                });
    }

    // --- XÁC THỰC VỚI FIREBASE BẰNG FACEBOOK ---
    // Tương tự Google, Firebase sẽ tự tạo người dùng mới nếu tài khoản Facebook này chưa tồn tại.
    private void handleFacebookAccessToken(AccessToken token) {
        mAuth.signInWithCredential(FacebookAuthProvider.getCredential(token.getToken()))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) goToMain();
                    else Toast.makeText(this, "Facebook đăng ký thất bại", Toast.LENGTH_SHORT).show();
                });
    }

    // --- HÀM TIỆN ÍCH: CHUYỂN ĐẾN MÀN HÌNH CHÍNH ---
    private void goToMain() {
        startActivity(new Intent(this, MainActivity.class));
        // Đóng màn hình SignupActivity lại để người dùng không thể quay lại bằng nút Back.
        finish();
    }
}
