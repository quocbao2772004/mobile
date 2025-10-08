package com.example.newsai;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignIn;


public class ProfileActivity extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private EditText editName, editEmail, editPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        
        // Khởi tạo views
        editName = findViewById(R.id.editName);
        editEmail = findViewById(R.id.editEmail);
        editPhone = findViewById(R.id.editPhone);
        ImageView imageView = findViewById(R.id.imgAvatar);
        
        // Kiểm tra nếu chưa đăng nhập thì quay về LoginActivity
        if (currentUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }
        
        // Hiển thị thông tin user
        loadUserInfo();
        
        // Nút cập nhật thông tin
        findViewById(R.id.btnUpdate).setOnClickListener(v -> updateUserInfo());
        
        // Nút đăng xuất
        findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());
        
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                finish();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Reload thông tin mỗi khi quay lại màn hình
        if (currentUser != null) {
            currentUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    currentUser = mAuth.getCurrentUser();
                    loadUserInfo();
                }
            });
        }
    }
    
    private void loadUserInfo() {
        if (currentUser != null) {
            if (currentUser.getDisplayName() != null) {
                editName.setText(currentUser.getDisplayName());
            }
            if (currentUser.getEmail() != null) {
                editEmail.setText(currentUser.getEmail());
                editEmail.setEnabled(false); // Email không thể sửa
            }
            if (currentUser.getPhoneNumber() != null && !currentUser.getPhoneNumber().isEmpty()) {
                editPhone.setText(currentUser.getPhoneNumber());
            }
        }
    }
    
    private void updateUserInfo() {
        String name = editName.getText().toString().trim();
        String phone = editPhone.getText().toString().trim();
        
        // Validate
        if (TextUtils.isEmpty(name)) {
            editName.setError("Vui lòng nhập tên");
            editName.requestFocus();
            return;
        }
        
        // Cập nhật tên
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();
        
        currentUser.updateProfile(profileUpdates)
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    // Reload user từ server
                    currentUser.reload().addOnCompleteListener(reloadTask -> {
                        if (reloadTask.isSuccessful()) {
                            currentUser = mAuth.getCurrentUser();
                            loadUserInfo();
                            Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Reload thất bại", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    String errorMsg = "Cập nhật thất bại";
                    if (task.getException() != null) {
                        errorMsg += ": " + task.getException().getMessage();
                    }
                    Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
    }
    
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Đăng xuất")
            .setMessage("Bạn có chắc muốn đăng xuất?")
            .setPositiveButton("Đăng xuất", (dialog, which) -> {
                // Đăng xuất Firebase
                mAuth.signOut();

                // Đăng xuất Google nếu có
                GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build();
                com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(this, gso).signOut();
                
                Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                
                // Chuyển về LoginActivity
                Intent intent = new Intent(this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            })
            .setNegativeButton("Hủy", null)
            .show();
    }
}
