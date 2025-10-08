// Dòng này khai báo package, là một cách để tổ chức các tệp Java.
// Tất cả các tệp trong cùng một package có thể dễ dàng truy cập lẫn nhau.
package com.example.newsai;

// Phần này import (nhập) các lớp (class) cần thiết từ thư viện Android và các tệp khác trong dự án.
// Ví dụ: Intent để chuyển màn hình, Bundle để lưu trạng thái, RecyclerView để hiển thị danh sách,...
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.newsai.data.ClusterItem;
import com.example.newsai.data.NewsItem;
import com.example.newsai.network.ApiClient;
import com.example.newsai.network.ApiService;
import com.example.newsai.ui.ClusterAdapter;
import com.example.newsai.ui.NewsAdapter;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Khai báo lớp MainActivity. "extends AppCompatActivity" có nghĩa là nó kế thừa tất cả các chức năng
// cơ bản của một màn hình trong Android.
public class MainActivity extends AppCompatActivity {

    // --- KHAI BÁO BIẾN TOÀN CỤC (MEMBER VARIABLES) ---
    // Đây là các biến sẽ được sử dụng ở nhiều nơi trong file này.

    private RecyclerView rvNews; // Biến đại diện cho danh sách cuộn (RecyclerView) trên giao diện.
    private NewsAdapter newsAdapter; // Biến để quản lý cách hiển thị danh sách các bài báo.
    private ClusterAdapter clusterAdapter; // Biến để quản lý cách hiển thị danh sách các cụm tin.
    private TextView tvTitle; // Biến đại diện cho dòng chữ tiêu đề ("Mới và Hot" hoặc "Cụm tin").
    private ImageButton ivMenu; // Biến đại diện cho icon menu để chuyển đổi chế độ xem.
    private boolean isClusterMode = false; // Một "công tắc" (biến cờ) để biết đang ở chế độ xem cụm tin hay không. Mặc định là không.

    // --- PHƯƠNG THỨC onCreate() ---
    // Đây là phương thức quan trọng nhất, được gọi ĐẦU TIÊN và CHỈ MỘT LẦN khi màn hình được tạo ra.
    // Mọi thiết lập ban đầu đều nằm ở đây.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Luôn gọi hàm của lớp cha trước tiên.
        setContentView(R.layout.activity_main); // Gắn layout từ file activity_main.xml vào màn hình này.

        // --- ÁNH XẠ VIEW ---
        // Kết nối các biến Java đã khai báo ở trên với các thành phần giao diện (View) trong file XML.
        tvTitle = findViewById(R.id.tvTitle);
        ivMenu = findViewById(R.id.ivMenu);
        rvNews = findViewById(R.id.rvNews);

        // --- CẤU HÌNH RECYCLERVIEW ---
        // Thiết lập cách các item trong danh sách sẽ được sắp xếp. Ở đây là một danh sách dọc.
        rvNews.setLayoutManager(new LinearLayoutManager(this));
        
        // --- KHỞI TẠO ADAPTER ---
        // Tạo ra các đối tượng Adapter. Adapter là cầu nối giữa dữ liệu (List<NewsItem>) và giao diện (RecyclerView).
        // `this::openDetail` là một cách viết tắt để truyền phương thức openDetail vào làm hành động khi người dùng click vào một item.
        newsAdapter = new NewsAdapter(this::openDetail);
        clusterAdapter = new ClusterAdapter(this::openClusterDetail);

        // Mặc định, gán newsAdapter cho RecyclerView để hiển thị danh sách bài báo trước tiên.
        rvNews.setAdapter(newsAdapter);

        // --- HIỂN THỊ NGÀY HIỆN TẠI ---
        TextView tvDate = findViewById(R.id.tvDate);
        // Định dạng ngày tháng năm hiện tại theo kiểu "Thứ, dd.MM" (ví dụ: "T4, 08.10") và hiển thị nó.
        tvDate.setText(new SimpleDateFormat("EEE, dd.MM", Locale.getDefault()).format(new Date()));

        // --- GÁN SỰ KIỆN ONCLICK ---
        // Lắng nghe sự kiện khi người dùng nhấn vào icon menu.
        ivMenu.setOnClickListener(v -> toggleMode()); // Khi nhấn, gọi hàm toggleMode().
        
        // Lắng nghe sự kiện khi người dùng nhấn vào icon tài khoản.
        ImageView btnAccount = findViewById(R.id.btnAccount);
        btnAccount.setOnClickListener(v -> {
            // Tạo một Intent để "dịch chuyển" từ màn hình hiện tại (MainActivity) đến ProfileActivity.
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent); // Thực hiện việc chuyển màn hình.
        });

        // --- TẢI DỮ LIỆU BAN ĐẦU ---
        // Gọi hàm để lấy dữ liệu từ API và hiển thị lên danh sách ngay khi mở ứng dụng.
        loadAllNews();
    }

    // --- PHƯƠNG THỨC toggleMode() ---
    // Được gọi khi nhấn vào icon menu để chuyển đổi giữa 2 chế độ xem.
    private void toggleMode() {
        // Đảo ngược giá trị của biến isClusterMode (true thành false, false thành true).
        isClusterMode = !isClusterMode;
        
        if (isClusterMode) { // Nếu đang ở chế độ cụm tin
            tvTitle.setText("Cụm tin"); // Đổi tiêu đề.
            rvNews.setAdapter(clusterAdapter); // Đổi adapter để hiển thị cụm tin.
            loadClusters(); // Tải dữ liệu cụm tin.
        } else { // Nếu đang ở chế độ bài báo
            tvTitle.setText("Mới và Hot"); // Đổi tiêu đề.
            rvNews.setAdapter(newsAdapter); // Đổi adapter để hiển thị bài báo.
            loadAllNews(); // Tải dữ liệu bài báo.
        }
    }

    // --- PHƯƠNG THỨC loadAllNews() ---
    // Tải danh sách bài báo từ 2 nguồn: articles và facebook posts.
    private void loadAllNews() {
        // Lấy đối tượng ApiService đã được cấu hình sẵn trong ApiClient.
        ApiService api = ApiClient.get().create(ApiService.class);
        // Gọi API để lấy danh sách articles. enqueue() để thực hiện yêu cầu một cách bất đồng bộ (không làm đơ app).
        api.getArticles().enqueue(new Callback<List<NewsItem>>() {
            // onResponse được gọi khi có phản hồi từ server (thành công hoặc thất bại).
            @Override public void onResponse(Call<List<NewsItem>> call, Response<List<NewsItem>> r1) {
                // Tạo một danh sách 'all' và thêm kết quả từ API articles vào.
                List<NewsItem> all = (r1.isSuccessful() && r1.body()!=null) ? new java.util.ArrayList<>(r1.body()) : new java.util.ArrayList<>();
                // Gọi tiếp API thứ hai để lấy facebook posts.
                api.getFacebookPosts().enqueue(new Callback<List<NewsItem>>() {
                    @Override public void onResponse(Call<List<NewsItem>> call, Response<List<NewsItem>> r2) {
                        // Nếu thành công, thêm kết quả từ API facebook posts vào danh sách 'all'.
                        if (r2.isSuccessful() && r2.body()!=null) all.addAll(r2.body());
                        // Cập nhật toàn bộ dữ liệu (cả articles và facebook posts) lên giao diện.
                        newsAdapter.submit(all);
                    }
                    // onFailure của API thứ hai.
                    @Override public void onFailure(Call<List<NewsItem>> call, Throwable t) {
                        // Dù API thứ hai lỗi, vẫn hiển thị dữ liệu từ API đầu tiên.
                        newsAdapter.submit(all);
                    }
                });
            }
            // onFailure được gọi khi có lỗi mạng (không kết nối được server).
            @Override public void onFailure(Call<List<NewsItem>> call, Throwable t) { Log.e("API","FAIL", t); }
        });
    }

    // --- PHƯƠNG THỨC loadFacebookPosts() ---
    // Phương thức này hiện không được sử dụng trực tiếp trong luồng chính, nhưng nó vẫn tồn tại
    // để có thể tải riêng các bài viết từ Facebook nếu cần.
    private void loadFacebookPosts() {
        ApiService api = ApiClient.get().create(ApiService.class);
        api.getFacebookPosts().enqueue(new Callback<List<NewsItem>>() {
            @Override
            public void onResponse(Call<List<NewsItem>> call, Response<List<NewsItem>> res) {
                if (res.isSuccessful() && res.body() != null) {
                    newsAdapter.submit(res.body());
                } else {
                    Log.e("API", "HTTP " + res.code());
                }
            }

            @Override
            public void onFailure(Call<List<NewsItem>> call, Throwable t) {
                Log.e("API", "FAIL", t);
            }
        });
    }
    
    // --- PHƯƠNG THỨC loadClusters() ---
    // Tải danh sách các cụm tin.
    private void loadClusters() {
        ApiService api = ApiClient.get().create(ApiService.class);
        // Gọi API getTopClusters với tham số là 20 (lấy 20 cụm tin đầu).
        api.getTopClusters(20).enqueue(new Callback<List<ClusterItem>>() {
            @Override public void onResponse(Call<List<ClusterItem>> call, Response<List<ClusterItem>> res) {
                // Nếu gọi API thành công và có dữ liệu trả về...
                if (res.isSuccessful() && res.body() != null) {
                    // ...thì cập nhật dữ liệu đó lên giao diện thông qua clusterAdapter.
                    clusterAdapter.submit(res.body());
                }
                else Log.e("API", "HTTP " + res.code()); // Ghi log lỗi nếu có.
            }
            @Override public void onFailure(Call<List<ClusterItem>> call, Throwable t) {
                Log.e("API", "FAIL", t); // Ghi log lỗi mạng.
            }
        });
    }

    // --- PHƯƠNG THỨC openDetail() ---
    // Được gọi khi người dùng nhấn vào một bài báo trong danh sách.
    private void openDetail(NewsItem it) {
        // Tạo Intent để chuyển sang màn hình DetailActivity.
        Intent intent = new Intent(this, DetailActivity.class);
        // Lấy URL ảnh đầu tiên trong danh sách ảnh, nếu có.
        String img = (it.getImage_contents()!=null && !it.getImage_contents().isEmpty())
                ? it.getImage_contents().get(0) : null;
        
        // --- ĐÓNG GÓI DỮ LIỆU ---
        // Dùng putExtra để "gửi kèm" dữ liệu của bài báo sang cho màn hình DetailActivity.
        // Giống như bạn gửi một bưu kiện và ghi thông tin bên ngoài.
        intent.putExtra(DetailActivity.K_TITLE, it.getTitle());
        intent.putExtra(DetailActivity.K_IMAGE, img);
        intent.putExtra(DetailActivity.K_URL, it.getUrl());
        intent.putExtra(DetailActivity.K_SOURCE_URL, it.getSource_url());
        intent.putExtra(DetailActivity.K_CONTENT, it.getText_content());
        intent.putExtra(DetailActivity.K_DATE, it.getCrawled_at());
        
        // Bắt đầu chuyển màn hình.
        startActivity(intent);
    }
    
    // --- PHƯƠNG THỨC openClusterDetail() ---
    // Được gọi khi người dùng nhấn vào một cụm tin trong danh sách.
    private void openClusterDetail(ClusterItem cluster) {
        // Tạo Intent để chuyển sang màn hình ClusterDetailActivity.
        Intent intent = new Intent(this, ClusterDetailActivity.class);
        // Gửi kèm ID của cụm tin đó. Màn hình chi tiết sẽ dùng ID này để tải dữ liệu.
        intent.putExtra("cluster_id", cluster.getCluster_id());
        startActivity(intent); // Bắt đầu chuyển màn hình.
    }
}
