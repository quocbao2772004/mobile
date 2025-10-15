// Khai báo package, nơi chứa tệp này trong cấu trúc thư mục của dự án.
package com.example.newsai;

// Import các lớp (class) cần thiết từ thư viện của Android và các thư viện bên ngoài.
import android.Manifest; // Chứa các hằng số về quyền (permission) của ứng dụng.
import android.content.Intent; // Dùng để khởi tạo và thực hiện việc chuyển đổi giữa các màn hình (Activity).
import android.content.pm.PackageManager; // Cung cấp thông tin về các package đã cài đặt và quyền của chúng.
import android.os.Build; // Cung cấp thông tin về phiên bản Android của thiết bị.
import android.os.Bundle; // Dùng để truyền dữ liệu và lưu trạng thái.
import android.util.Log; // Dùng để ghi lại thông tin gỡ lỗi (debug) trong Logcat.
import android.view.View; // Lớp cơ sở cho tất cả các thành phần giao diện người dùng.
import android.widget.ImageButton; // Một nút bấm hiển thị hình ảnh.
import android.widget.ImageView; // Thành phần để hiển thị hình ảnh.
import android.widget.TextView; // Thành phần để hiển thị văn bản.
import android.widget.Toast; // Dùng để hiển thị một thông báo nhỏ trên màn hình.

import androidx.activity.result.ActivityResultLauncher; // Dùng để nhận kết quả trả về từ một Activity, bao gồm cả yêu cầu quyền.
import androidx.activity.result.contract.ActivityResultContracts; // Hợp đồng (contract) chuẩn cho các yêu cầu kết quả, như yêu cầu quyền.
import androidx.annotation.NonNull; // Annotation chỉ ra rằng một giá trị không được null.
import androidx.appcompat.app.AppCompatActivity; // Lớp cơ sở cho các Activity.
import androidx.core.content.ContextCompat; // Lớp tiện ích để truy cập các tính năng tương thích ngược.
import androidx.recyclerview.widget.LinearLayoutManager; // Quản lý cách sắp xếp các item trong RecyclerView.
import androidx.recyclerview.widget.RecyclerView; // Dùng để hiển thị các danh sách lớn.

// Import các lớp model dữ liệu và các thành phần mạng/giao diện từ dự án.
import com.example.newsai.data.ClusterItem;
import com.example.newsai.data.NewsItem;
import com.example.newsai.network.ApiClient;
import com.example.newsai.network.ApiService;
import com.example.newsai.ui.ClusterAdapter;
import com.example.newsai.ui.NewsAdapter;
import com.google.android.material.bottomsheet.BottomSheetDialog; // Một dialog hiển thị từ dưới đáy màn hình.
import com.google.firebase.messaging.FirebaseMessaging; // Lớp để tương tác với Firebase Cloud Messaging (FCM).

// Import các lớp tiện ích của Java.
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

// Import các lớp của Retrofit.
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

// Khai báo lớp MainActivity, kế thừa từ AppCompatActivity.
public class MainActivity extends AppCompatActivity {

    // --- KHAI BÁO BIẾN TOÀN CỤC (MEMBER VARIABLES) ---
    private RecyclerView rvNews;
    private NewsAdapter newsAdapter;
    private ClusterAdapter clusterAdapter;
    private TextView tvTitle;
    private ImageButton ivMenu;
    private boolean isClusterMode = false;
    // Biến để lưu trạng thái bộ lọc hiện tại. Mặc định là "home".
    private String currentFilter = "home"; // Các giá trị có thể: home, clusters, web, facebook, positive, negative
    // Danh sách để lưu trữ tất cả tin tức đã tải, dùng cho việc lọc phía client.
    private List<NewsItem> allNews = new ArrayList<>();

    // Các biến dùng cho chức năng phân trang (tải thêm khi cuộn).
    private int currentSkip = 0; // Vị trí bắt đầu lấy dữ liệu từ API.
    private final int limit = 20; // Số lượng item lấy trong mỗi lần gọi API.
    private boolean isLoadingMore = false; // Cờ để ngăn việc gọi API tải thêm liên tục khi đang trong quá trình tải.

    // Trình khởi chạy (Launcher) để yêu cầu quyền gửi thông báo.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                // Callback này sẽ được gọi khi người dùng trả lời hộp thoại yêu cầu quyền.
                if (isGranted) {
                    // Nếu người dùng đồng ý.
                    Log.d("FCM", "Notification permission granted");
                    subscribeToNewsClusters(); // Đăng ký nhận thông báo.
                } else {
                    // Nếu người dùng từ chối.
                    Log.d("FCM", "Notification permission denied");
                    Toast.makeText(this, "Bạn sẽ không nhận được thông báo cụm tin mới", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Ánh xạ các View.
        tvTitle = findViewById(R.id.tvTitle);
        ivMenu = findViewById(R.id.ivMenu);
        rvNews = findViewById(R.id.rvNews);
        rvNews.setLayoutManager(new LinearLayoutManager(this));

        // Khởi tạo các Adapter.
        newsAdapter = new NewsAdapter(this::openDetail);
        clusterAdapter = new ClusterAdapter(this::openClusterDetail);
        rvNews.setAdapter(newsAdapter); // Đặt adapter mặc định là newsAdapter.

        // Hiển thị ngày hiện tại.
        TextView tvDate = findViewById(R.id.tvDate);
        tvDate.setText(new SimpleDateFormat("EEE, dd.MM", Locale.getDefault()).format(new Date()));

        // Gán sự kiện cho nút menu để mở menu bộ lọc.
        ivMenu.setOnClickListener(v -> showFilterMenu());

        // Gán sự kiện cho nút tài khoản để mở màn hình Profile.
        ImageView btnAccount = findViewById(R.id.btnAccount);
        btnAccount.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            startActivity(intent);
        });

        // --- CẤU HÌNH PHÂN TRANG (LOAD MORE ON SCROLL) ---
        // Gắn một listener để theo dõi sự kiện cuộn của RecyclerView.
        rvNews.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // Kiểm tra các điều kiện để tải thêm:
                // 1. `!recyclerView.canScrollVertically(1)`: Đã cuộn đến cuối danh sách.
                // 2. `!isLoadingMore`: Hiện không có tiến trình tải nào đang chạy.
                // 3. `!isClusterMode`: Chức năng này chỉ áp dụng cho chế độ xem bài báo, không áp dụng cho cụm tin.
                // 4. `currentFilter.equals("home")`: Chỉ tải thêm ở trang chủ.
                if (!recyclerView.canScrollVertically(1)
                        && !isLoadingMore
                        && !isClusterMode
                        && currentFilter.equals("home")) {
                    loadMoreNews(); // Gọi hàm tải thêm dữ liệu.
                }
            }
        });

        // Yêu cầu quyền gửi thông báo và đăng ký nhận tin từ FCM.
        requestNotificationPermission();

        // Tải trang dữ liệu đầu tiên cho trang chủ.
        loadNews();
    }

    // --- HIỂN THỊ MENU BỘ LỌC (BOTTOM SHEET) ---
    private void showFilterMenu() {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_filter, null);
        bottomSheet.setContentView(view);

        // Gán sự kiện cho từng nút trong menu.
        // Nút Trang chủ.
        view.findViewById(R.id.btnHome).setOnClickListener(v -> {
            currentFilter = "home";
            tvTitle.setText("Trang chủ");
            isClusterMode = false;
            rvNews.setAdapter(newsAdapter);
            loadNews(); // Reset và tải lại dữ liệu trang chủ.
            bottomSheet.dismiss(); // Đóng menu.
        });

        // Nút Cụm tin.
        view.findViewById(R.id.btnClusters).setOnClickListener(v -> {
            currentFilter = "clusters";
            tvTitle.setText("Cụm tin");
            isClusterMode = true;
            rvNews.setAdapter(clusterAdapter);
            loadClusters();
            bottomSheet.dismiss();
        });

        // Nút Báo điện tử.
        view.findViewById(R.id.btnWeb).setOnClickListener(v -> {
            currentFilter = "web";
            tvTitle.setText("Báo điện tử");
            isClusterMode = false;
            rvNews.setAdapter(newsAdapter);
            loadNews(); // Tải lại toàn bộ tin tức trước.
            bottomSheet.dismiss();
            filterNewsByType("article"); // Sau đó lọc ra các tin từ "article".
        });

        // Nút Facebook.
        view.findViewById(R.id.btnFacebook).setOnClickListener(v -> {
            currentFilter = "facebook";
            tvTitle.setText("Facebook");
            isClusterMode = false;
            rvNews.setAdapter(newsAdapter);
            loadFaceBookposts(); // Tải lại toàn bộ tin tức.
            bottomSheet.dismiss();
            filterNewsByType("facebook_post"); // Lọc ra các tin từ "facebook_post".
        });

        // Nút Tin tích cực.
        view.findViewById(R.id.btnPositive).setOnClickListener(v -> {
            currentFilter = "positive";
            tvTitle.setText("Tin tích cực");
            isClusterMode = false;
            rvNews.setAdapter(newsAdapter);
            bottomSheet.dismiss();
            // Tải một lượng lớn tin tức rồi lọc phía client theo nhãn "tich cuc".
            loadAllNewsThenFilterBySentiment("tich cuc");
        });

        // Nút Tin tiêu cực.
        view.findViewById(R.id.btnNegative).setOnClickListener(v -> {
            currentFilter = "negative";
            tvTitle.setText("Tin tiêu cực");
            isClusterMode = false;
            rvNews.setAdapter(newsAdapter);
            bottomSheet.dismiss();
            loadAllNewsThenFilterBySentiment("tieu cuc");
        });

        bottomSheet.show(); // Hiển thị menu.
    }
    
    // --- TẢI DỮ LIỆU BÀI VIẾT FACEBOOK ---
    private void loadFaceBookposts() {
        ApiService api = ApiClient.get().create(ApiService.class);
        api.getFacebookPosts().enqueue(new Callback<List<NewsItem>>()
        {
            @Override public void onResponse(Call<List<NewsItem>> call, Response<List<NewsItem>> res)
            {
                if (res.isSuccessful() && res.body() != null)
                {
                    // Lưu dữ liệu vào danh sách và cập nhật adapter.
                    allNews = res.body();
                    newsAdapter.submit(allNews);
                }
                else Log.e("API", "HTTP " + res.code());
            } @Override public void onFailure(Call<List<NewsItem>> call, Throwable t)
        {
            Log.e("API", "FAIL", t);
        }
        });
    }
    
    /** Reset phân trang và bắt đầu tải trang đầu tiên (dùng cho trang chủ và các bộ lọc). */
    private void loadNews() {
        currentSkip = 0; // Đặt lại vị trí bắt đầu.
        allNews = new ArrayList<>(); // Xóa sạch danh sách tin cũ.
        newsAdapter.submit(new ArrayList<>()); // Xóa sạch dữ liệu trên giao diện.
        loadMoreNews(); // Bắt đầu tải trang đầu tiên.
    }

    /** Tải thêm một trang dữ liệu mới (20 bài báo + 20 bài facebook). */
    private void loadMoreNews() {
        isLoadingMore = true; // Đánh dấu là đang tải.
        ApiService api = ApiClient.get().create(ApiService.class);
        List<NewsItem> newBatch = new ArrayList<>(); // Danh sách tạm để chứa dữ liệu mới.
        
        // Gọi API lấy bài báo (articles) với vị trí `currentSkip` và giới hạn `limit`.
        api.getArticles(currentSkip, limit).enqueue(new Callback<List<NewsItem>>() {
            @Override
            public void onResponse(Call<List<NewsItem>> call, Response<List<NewsItem>> r1) {
                if (r1.isSuccessful() && r1.body() != null) {
                    newBatch.addAll(r1.body()); // Thêm kết quả vào danh sách tạm.
                }
                // Sau khi lấy articles xong, gọi tiếp API lấy bài viết facebook với cùng skip/limit.
                api.getFacebookPosts(currentSkip, limit).enqueue(new Callback<List<NewsItem>>() {
                    @Override
                    public void onResponse(Call<List<NewsItem>> call2, Response<List<NewsItem>> r2) {
                        if (r2.isSuccessful() && r2.body() != null) {
                            newBatch.addAll(r2.body()); // Thêm kết quả vào danh sách tạm.
                        }
                        // Sau khi đã có cả 2 loại dữ liệu:
                        currentSkip += limit; // Cập nhật vị trí skip cho lần tải tiếp theo.
                        allNews.addAll(newBatch); // Thêm lô dữ liệu mới vào danh sách tổng.
                        if (newBatch.size() > 0) {
                            // Cần một hàm addAll trong NewsAdapter để chỉ thêm dữ liệu mới vào cuối danh sách
                            // thay vì vẽ lại toàn bộ, giúp hiệu năng tốt hơn.
                            newsAdapter.addAll(newBatch);
                        }
                        isLoadingMore = false; // Tải xong, bỏ đánh dấu.
                    }

                    @Override public void onFailure(Call<List<NewsItem>> call2, Throwable t) {
                        // Kể cả khi API facebook lỗi, vẫn cập nhật dữ liệu từ API articles.
                        currentSkip += limit;
                        allNews.addAll(newBatch);
                        if (newBatch.size() > 0) {
                            newsAdapter.addAll(newBatch);
                        }
                        isLoadingMore = false;
                    }
                });
            }

            @Override public void onFailure(Call<List<NewsItem>> call, Throwable t) {
                // Nếu API articles lỗi, dừng trạng thái loading.
                isLoadingMore = false;
            }
        });
    }

    /** Lọc danh sách `allNews` đã tải theo loại (type). */
    private void filterNewsByType(String type) {
        if (allNews.isEmpty()) {
            return;
        }
        List<NewsItem> filtered = new ArrayList<>();
        for (NewsItem item : allNews) {
            if (type.equals(item.getType())) {
                filtered.add(item);
            }
        }
        newsAdapter.submit(filtered); // Cập nhật adapter với danh sách đã lọc.
    }

    /** Lọc danh sách `allNews` đã tải theo nhãn cảm xúc (sentiment). */
    private void filterNewsBySentiment(String sentiment) {
        if (allNews.isEmpty()) {
            return;
        }
        List<NewsItem> filtered = new ArrayList<>();
        for (NewsItem item : allNews) {
            String itemSentiment = item.getSentiment_label();
            if (sentiment.equals(itemSentiment)) {
                filtered.add(item);
            }
        }
        newsAdapter.submit(filtered);
    }

    /** Tải một lượng lớn dữ liệu (100 articles + 100 facebook posts) rồi thực hiện lọc theo cảm xúc. */
    private void loadAllNewsThenFilterBySentiment(final String sentiment) {
        ApiService api = ApiClient.get().create(ApiService.class);
        List<NewsItem> combined = new ArrayList<>();
        // Lấy 100 articles.
        api.getArticles(0, 100).enqueue(new Callback<List<NewsItem>>() {
            @Override
            public void onResponse(Call<List<NewsItem>> call, Response<List<NewsItem>> r1) {
                if (r1.isSuccessful() && r1.body() != null) {
                    combined.addAll(r1.body());
                }
                // Lấy 100 facebook posts.
                api.getFacebookPosts(0, 100).enqueue(new Callback<List<NewsItem>>() {
                    @Override
                    public void onResponse(Call<List<NewsItem>> call2, Response<List<NewsItem>> r2) {
                        if (r2.isSuccessful() && r2.body() != null) {
                            combined.addAll(r2.body());
                        }
                        allNews = combined; // Cập nhật danh sách tổng.
                        filterNewsBySentiment(sentiment); // Thực hiện lọc.
                    }

                    @Override public void onFailure(Call<List<NewsItem>> call2, Throwable t) {
                        allNews = combined;
                        filterNewsBySentiment(sentiment);
                      }
                });
            }

            @Override public void onFailure(Call<List<NewsItem>> call, Throwable t) {
                Log.e("API", "FAIL", t);
            }
        });
    }

    // --- YÊU CẦU QUYỀN GỬI THÔNG BÁO ---
    private void requestNotificationPermission() {
        // Chỉ áp dụng cho Android 13 (TIRAMISU) trở lên.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Kiểm tra xem quyền đã được cấp hay chưa.
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                subscribeToNewsClusters(); // Nếu đã cấp, đăng ký nhận tin.
            } else {
                // Nếu chưa, khởi chạy hộp thoại yêu cầu quyền.
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Với các phiên bản Android cũ hơn, không cần yêu cầu quyền, cứ đăng ký.
            subscribeToNewsClusters();
        }
    }

    // --- ĐĂNG KÝ NHẬN TIN TỪ CHỦ ĐỀ FCM ---
    private void subscribeToNewsClusters() {
        // Đăng ký thiết bị này vào chủ đề (topic) "news_clusters".
        // Bất cứ khi nào server gửi thông báo đến chủ đề này, thiết bị sẽ nhận được.
        FirebaseMessaging.getInstance().subscribeToTopic("news_clusters")
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d("FCM", "Subscribed to news_clusters topic");
                    } else {
                        Log.e("FCM", "Failed to subscribe to topic", task.getException());
                    }
                });
    }

    // --- TẢI DANH SÁCH CỤM TIN --- (Không thay đổi)
    private void loadClusters() {
        ApiService api = ApiClient.get().create(ApiService.class);
        api.getTopClusters(20).enqueue(new Callback<List<ClusterItem>>() {
            @Override public void onResponse(Call<List<ClusterItem>> call, Response<List<ClusterItem>> res) {
                if (res.isSuccessful() && res.body() != null) clusterAdapter.submit(res.body());
                else Log.e("API", "HTTP " + res.code());
            }
            @Override public void onFailure(Call<List<ClusterItem>> call, Throwable t) {
                Log.e("API", "FAIL", t);
            }
        });
    }

    // --- MỞ MÀN HÌNH CHI TIẾT BÀI BÁO --- (Cập nhật để gửi thêm dữ liệu)
    private void openDetail(NewsItem it) {
        Intent intent = new Intent(this, DetailActivity.class);
        String img = (it.getImage_contents() != null && !it.getImage_contents().isEmpty())
                ? it.getImage_contents().get(0) : null;
        // Gửi thêm các thông tin như sentiment, spam, ngày đăng...
        intent.putExtra(DetailActivity.K_TITLE, it.getTitle());
        intent.putExtra(DetailActivity.K_IMAGE, img);
        intent.putExtra(DetailActivity.K_URL, it.getUrl());
        intent.putExtra(DetailActivity.K_SOURCE_URL, it.getSource_url());
        intent.putExtra(DetailActivity.K_CONTENT, it.getText_content());
        intent.putExtra(DetailActivity.K_DATE, it.getCrawled_at());
        intent.putExtra(DetailActivity.K_SENTIMENT, it.getSentiment_label());
        intent.putExtra(DetailActivity.K_SPAM, it.getSpam_label());
        intent.putExtra(DetailActivity.K_POSTED, it.getPosted_at());
        startActivity(intent);
    }

    // --- MỞ MÀN HÌNH CHI TIẾT CỤM TIN --- (Không thay đổi)
    private void openClusterDetail(ClusterItem cluster) {
        Intent intent = new Intent(this, ClusterDetailActivity.class);
        intent.putExtra("cluster_id", cluster.getCluster_id());
        startActivity(intent);
    }
}
