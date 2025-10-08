// Khai báo package, nơi chứa tệp này trong cấu trúc thư mục của dự án.
package com.example.newsai;

// Import các lớp (class) cần thiết từ thư viện của Android và các thư viện bên ngoài.
import android.content.Intent; // Dùng để khởi tạo và thực hiện việc chuyển đổi giữa các màn hình (Activity).
import android.net.Uri; // Dùng để xử lý các đường link (URL), ví dụ như mở một trang web.
import android.os.Bundle; // Dùng để truyền dữ liệu giữa các Activity và lưu trạng thái.
import android.text.TextUtils; // Cung cấp các hàm tiện ích để xử lý chuỗi, ví dụ như kiểm tra chuỗi rỗng.
import android.util.Log; // Dùng để ghi lại thông tin gỡ lỗi (debug) trong Logcat.
import android.view.View; // Lớp cơ sở cho tất cả các thành phần giao diện người dùng.
import android.widget.ImageView; // Thành phần để hiển thị hình ảnh.
import android.widget.LinearLayout; // Một layout dùng để sắp xếp các thành phần con theo một hàng ngang hoặc dọc.
import android.widget.TextView; // Thành phần để hiển thị văn bản.
import android.widget.Toast; // Dùng để hiển thị một thông báo nhỏ, ngắn gọn trên màn hình.
import androidx.appcompat.app.AppCompatActivity; // Lớp cơ sở cho các Activity có hỗ trợ Action Bar và các tính năng tương thích ngược.
import androidx.recyclerview.widget.RecyclerView; // Dùng để hiển thị các danh sách lớn một cách hiệu quả. (Ở đây chưa dùng tới)
import androidx.viewpager2.widget.ViewPager2; // Một thành phần giao diện cho phép người dùng lật qua lại giữa các trang hoặc slide.
import com.bumptech.glide.Glide; // Một thư viện mạnh mẽ để tải, cache và hiển thị hình ảnh từ URL.
import com.example.newsai.data.ClusterItem; // Lớp model (đối tượng) đại diện cho dữ liệu của một cụm tin.
import com.example.newsai.data.NewsItem; // Lớp model đại diện cho dữ liệu của một bài báo.
import com.example.newsai.network.ApiClient; // Lớp helper để tạo và cấu hình Retrofit client.
import com.example.newsai.network.ApiService; // Interface định nghĩa các endpoint của API.
import com.example.newsai.ui.ImagePagerAdapter; // Adapter tùy chỉnh để cung cấp dữ liệu (ảnh) cho ViewPager2.
import java.util.List; // Lớp để làm việc với các danh sách (List).
import retrofit2.Call; // Đại diện cho một lời gọi API đang chờ được thực thi.
import retrofit2.Callback; // Dùng để xử lý kết quả trả về từ API một cách bất đồng bộ.
import retrofit2.Response; // Đại diện cho phản hồi từ server.

// Khai báo lớp ClusterDetailActivity, kế thừa từ AppCompatActivity.
public class ClusterDetailActivity extends AppCompatActivity {

    // --- KHAI BÁO BIẾN TOÀN CỤC (MEMBER VARIABLES) ---
    private LinearLayout articlesContainer; // Layout sẽ chứa danh sách các bài báo con. Ta sẽ thêm View vào đây bằng code.
    private String clusterId; // Biến để lưu ID của cụm tin đang được xem.
    private ViewPager2 viewPager; // Biến đại diện cho slide ảnh ở đầu màn hình.
    private RecyclerView dotsIndicator; // Biến này được khai báo nhưng chưa được sử dụng, có thể dùng để làm thanh chỉ thị trang cho slide ảnh.

    // --- PHƯƠNG THỨC onCreate() ---
    // Được gọi đầu tiên khi Activity được tạo ra. Nơi để khởi tạo giao diện và dữ liệu ban đầu.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState); // Luôn gọi phương thức của lớp cha đầu tiên.
        setContentView(R.layout.activity_cluster_detail); // Gắn layout từ file activity_cluster_detail.xml vào màn hình này.

        // Lấy Intent đã khởi động Activity này.
        Intent intent = getIntent();
        // Lấy dữ liệu "cluster_id" đã được gửi từ màn hình trước (MainActivity).
        clusterId = intent.getStringExtra("cluster_id");

        // --- ÁNH XẠ VIEWS ---
        // Kết nối các biến đã khai báo với các thành phần giao diện trong file XML.
        viewPager = findViewById(R.id.viewPager);
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvSummary = findViewById(R.id.tvSummary);
        TextView tvMeta = findViewById(R.id.tvMeta);
        articlesContainer = findViewById(R.id.articlesContainer);

        // Bắt đầu quá trình tải dữ liệu từ API.
        loadClusterDetail();
    }

    // --- TẢI DỮ LIỆU CHI TIẾT CỦA CỤM TIN ---
    private void loadClusterDetail() {
        // Nếu không có clusterId (vì lý do nào đó), thì không làm gì cả.
        if (clusterId == null) return;

        // Lấy đối tượng ApiService để thực hiện các lời gọi API.
        ApiService api = ApiClient.get().create(ApiService.class);
        // Gọi API getClusterById với ID đã nhận được. enqueue() để chạy bất đồng bộ.
        api.getClusterById(clusterId).enqueue(new Callback<ClusterItem>() {
            // onResponse được gọi khi server có phản hồi.
            @Override
            public void onResponse(Call<ClusterItem> call, Response<ClusterItem> res) {
                // Nếu lời gọi thành công (HTTP 200) và có dữ liệu trả về...
                if (res.isSuccessful() && res.body() != null) {
                    ClusterItem cluster = res.body(); // Lấy đối tượng ClusterItem từ phản hồi.
                    // Gọi hàm để hiển thị thông tin này lên giao diện.
                    displayClusterInfo(cluster);
                }
            }

            // onFailure được gọi khi có lỗi mạng.
            @Override
            public void onFailure(Call<ClusterItem> call, Throwable t) {
                Log.e("API", "FAIL load cluster", t); // Ghi lại lỗi vào Logcat.
            }
        });
    }
    
    // --- HIỂN THỊ THÔNG TIN CỤM TIN LÊN GIAO DIỆN ---
    private void displayClusterInfo(ClusterItem cluster) {
        // Ánh xạ các TextView một lần nữa (để chắc chắn).
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvSummary = findViewById(R.id.tvSummary);
        TextView tvMeta = findViewById(R.id.tvMeta);

        // Gán dữ liệu từ đối tượng 'cluster' lên các TextView.
        tvTitle.setText(cluster.getTitle() != null ? cluster.getTitle() : "");
        tvSummary.setText(cluster.getSummary() != null ? cluster.getSummary() : "");
        tvMeta.setText((cluster.getPrimary_source() != null ? cluster.getPrimary_source() : "")
                + " • " + cluster.getArticle_count() + " bài viết");

        // --- CẤU HÌNH SLIDE ẢNH (ViewPager2) ---
        List<String> images = cluster.getImage_contents(); // Lấy danh sách URL ảnh.
        if (images != null && !images.isEmpty()) { // Nếu có ảnh...
            viewPager.setVisibility(View.VISIBLE); // ...thì hiện slide lên.
            ImagePagerAdapter pagerAdapter = new ImagePagerAdapter(images); // Tạo một adapter mới.
            viewPager.setAdapter(pagerAdapter); // Gắn adapter vào ViewPager2.
        } else {
            viewPager.setVisibility(View.GONE); // Nếu không có ảnh thì ẩn đi.
        }

        // --- TẢI CÁC BÀI BÁO CON ---
        List<String> articleIds = cluster.getArticle_ids(); // Lấy danh sách ID các bài báo con.
        if (articleIds != null && !articleIds.isEmpty()) {
            // Gọi hàm để tải chi tiết của từng bài báo dựa trên danh sách ID này.
            loadArticlesByIds(articleIds);
        }
    }

    // --- TẢI CÁC BÀI BÁO DỰA TRÊN DANH SÁCH ID ---
    private void loadArticlesByIds(List<String> articleIds) {
        Log.d("ClusterDetail", "Loading " + articleIds.size() + " articles by IDs");
        articlesContainer.removeAllViews(); // Xóa sạch các bài báo cũ đang hiển thị (nếu có).
        
        ApiService api = ApiClient.get().create(ApiService.class);
        
        // Dùng vòng lặp 'for' để duyệt qua từng ID trong danh sách.
        for (int i = 0; i < articleIds.size(); i++) {
            String articleId = articleIds.get(i);
            int rank = i; // Lưu lại thứ hạng của bài báo để hiển thị số thứ tự.
            
            // Với mỗi ID, gọi API getArticleById.
            api.getArticleById(articleId).enqueue(new Callback<NewsItem>() {
                @Override
                public void onResponse(Call<NewsItem> call, Response<NewsItem> res) {
                    if (res.isSuccessful() && res.body() != null) {
                        NewsItem article = res.body();
                        // Khi tải thành công, gọi hàm để tạo và thêm View của bài báo này vào layout.
                        addArticleView(article, rank);
                    }
                }

                @Override
                public void onFailure(Call<NewsItem> call, Throwable t) {
                    Log.e("ClusterDetail", "Failed to load article " + articleId, t);
                }
            });
        }
    }

    // --- TẠO VÀ THÊM MỘT VIEW BÀI BÁO VÀO DANH SÁCH ---
    private void addArticleView(NewsItem article, int rank) {
        // "Thổi phồng" (inflate) layout từ file item_cluster_article.xml để tạo ra một View mới.
        View itemView = getLayoutInflater().inflate(R.layout.item_cluster_article, articlesContainer, false);
        
        // Ánh xạ các thành phần con bên trong itemView vừa tạo.
        TextView tvRank = itemView.findViewById(R.id.tvRank);
        TextView tvArticleTitle = itemView.findViewById(R.id.tvArticleTitle);
        TextView tvArticleText = itemView.findViewById(R.id.tvArticleText);
        TextView tvSourceBadge = itemView.findViewById(R.id.tvSourceBadge);
        ImageView imgArticle = itemView.findViewById(R.id.imgArticle);
        
        // Gán dữ liệu từ đối tượng 'article' lên các thành phần giao diện.
        tvRank.setText(String.valueOf(rank + 1)); // Hiển thị số thứ tự (bắt đầu từ 1).
        tvArticleTitle.setText(article.getTitle() != null ? article.getTitle() : "");
        
        // Hiển thị một đoạn xem trước của nội dung.
        String text = article.getText_content();
        if (text != null && !text.isEmpty()) {
            tvArticleText.setVisibility(View.VISIBLE);
            // Giới hạn độ dài văn bản xem trước là 200 ký tự.
            String preview = text.length() > 200 ? text.substring(0, 200) + "..." : text;
            tvArticleText.setText(preview);
        } else {
            tvArticleText.setVisibility(View.GONE); // Ẩn đi nếu không có nội dung.
        }
        
        // Tải ảnh cho bài báo.
        List<String> images = article.getImage_contents();
        if (images != null && !images.isEmpty()) {
            String imageUrl = images.get(0); // Lấy ảnh đầu tiên.
            Glide.with(this)
                    .load(imageUrl)
                    .centerCrop()
                    .into(imgArticle);
            imgArticle.setVisibility(View.VISIBLE);
        } else {
            imgArticle.setVisibility(View.GONE); // Ẩn đi nếu không có ảnh.
        }
        
        // Xác định và hiển thị nguồn tin (Web hay Facebook).
        String type = article.getType();
        if (type != null && type.equals("facebook_post")) {
            tvSourceBadge.setText("Facebook");
        } else {
            tvSourceBadge.setText("Web");
        }
        
        // Gán sự kiện click cho toàn bộ View của bài báo này.
        itemView.setOnClickListener(v -> openArticleDetail(article));
        
        // **QUAN TRỌNG**: Thêm View vừa được tạo và đổ dữ liệu vào LinearLayout `articlesContainer`.
        articlesContainer.addView(itemView);
    }

    // --- MỞ MÀN HÌNH CHI TIẾT BÀI BÁO ---
    private void openArticleDetail(NewsItem article) {
        // Tạo một Intent để chuyển sang DetailActivity.
        Intent intent = new Intent(this, DetailActivity.class);
        
        // Đóng gói tất cả dữ liệu cần thiết của bài báo vào Intent để gửi đi.
        // Sử dụng các hằng số (key) đã định nghĩa trong DetailActivity để đảm bảo tính nhất quán.
        if (article.getTitle() != null) {
            intent.putExtra(DetailActivity.K_TITLE, article.getTitle());
        }
        if (article.getText_content() != null) {
            intent.putExtra(DetailActivity.K_CONTENT, article.getText_content());
        }
        // ... các trường dữ liệu khác ...
        
        // Gửi kèm URL ảnh đầu tiên nếu có.
        List<String> images = article.getImage_contents();
        if (images != null && !images.isEmpty() && images.get(0) != null) {
            intent.putExtra(DetailActivity.K_IMAGE, images.get(0));
        }
        
        // Bắt đầu chuyển màn hình.
        startActivity(intent);
    }

    // --- MỞ LINK BÀI VIẾT GỐC TRONG TRÌNH DUYỆT --- (Phương thức này hiện không được sử dụng)
    private void openArticleLink(String link) {
        if (TextUtils.isEmpty(link)) {
            Toast.makeText(this, "Không có đường dẫn bài viết", Toast.LENGTH_SHORT).show();
            return;
        }
        // Tạo Intent với hành động ACTION_VIEW để mở link trong trình duyệt.
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
    }
}
