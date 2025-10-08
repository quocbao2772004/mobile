// Khai báo package, nơi chứa tệp này.
package com.example.newsai;

// Import các lớp cần thiết từ thư viện Android và các tệp khác trong dự án.
import android.content.Intent; // Dùng để nhận dữ liệu từ màn hình trước và thực hiện các hành động như chia sẻ.
import android.net.Uri; // Dùng để xử lý các đường link (URL).
import android.os.Bundle; // Dùng để nhận và lưu trạng thái của Activity.
import android.text.TextUtils; // Chứa các hàm tiện ích để xử lý chuỗi, ví dụ như kiểm tra chuỗi rỗng.
import android.util.Log; // Dùng để ghi log, hữu ích cho việc gỡ lỗi.
import android.widget.TextView; // Hiển thị văn bản.
import android.widget.ImageView; // Hiển thị hình ảnh.
import android.widget.Toast; // Hiển thị một thông báo nhỏ, tạm thời trên màn hình.

import androidx.appcompat.app.AppCompatActivity; // Lớp cơ sở cho các Activity có thanh Action Bar.

import com.bumptech.glide.Glide; // Một thư viện mạnh mẽ và phổ biến để tải và hiển thị hình ảnh từ URL.
import com.example.newsai.data.NewsItem; // Lớp model đại diện cho một bài báo.
import com.example.newsai.network.ApiClient; // Lớp helper để tạo Retrofit client.
import com.example.newsai.network.ApiService; // Interface định nghĩa các API endpoint.

import retrofit2.Call; // Đại diện cho một lời gọi API.
import retrofit2.Callback; // Dùng để xử lý kết quả trả về từ API một cách bất đồng bộ.
import retrofit2.Response; // Đại diện cho phản hồi từ server.

// Khai báo lớp DetailActivity.
public class DetailActivity extends AppCompatActivity {

    // --- KHAI BÁO CÁC HẰNG SỐ (CONSTANTS) ---
    // Đây là các "khóa" (key) dùng để gửi và nhận dữ liệu qua Intent.
    // Việc dùng hằng số giúp tránh lỗi gõ sai chính tả và làm cho code dễ đọc hơn.
    public static final String K_TITLE = "k_title";
    public static final String K_IMAGE = "k_image";
    public static final String K_URL = "k_url";
    public static final String K_SOURCE_URL = "k_source_url";
    public static final String K_CONTENT = "k_content";
    public static final String K_DATE = "k_date";

    // --- PHƯƠNG THỨC onCreate() ---
    // Được gọi khi màn hình được tạo ra.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail); // Gắn layout từ file activity_detail.xml.

        // --- ÁNH XẠ VIEWS ---
        // Kết nối các biến Java với các thành phần trên giao diện XML.
        ImageView img = findViewById(R.id.imgHeader);
        TextView tvCaption = findViewById(R.id.tvCaption);
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvLede = findViewById(R.id.tvLede);
        TextView tvMeta = findViewById(R.id.tvMeta);
        TextView tvContent = findViewById(R.id.tvContent);
        TextView tvSourceLink = findViewById(R.id.tvSourceLink);
        TextView btnShare = findViewById(R.id.btnShare);
        TextView btnBookmark = findViewById(R.id.btnBookmark);

        // Lấy Intent đã khởi tạo màn hình này. Intent này chứa dữ liệu được gửi từ màn hình trước.
        Intent it = getIntent();
        
        // --- LOGIC PHÂN LUỒNG ---
        // Màn hình này có thể được mở theo 2 cách:
        // 1. Từ màn hình chính (MainActivity): Toàn bộ dữ liệu bài báo đã được gửi kèm.
        // 2. Từ màn hình chi tiết cụm tin (ClusterDetailActivity): Chỉ có ID của bài báo được gửi, phải gọi API để lấy đủ thông tin.

        // Kiểm tra xem có `article_id` được gửi qua không.
        String articleId = it.getStringExtra("article_id");
        if (articleId != null) {
            // Nếu có ID, gọi hàm loadArticleById để tải dữ liệu từ server.
            loadArticleById(articleId, img, tvCaption, tvTitle, tvLede, tvMeta, tvContent, tvSourceLink, btnShare, btnBookmark);
            return; // Dừng thực thi hàm onCreate tại đây.
        }
        
        // Nếu không có article_id, nghĩa là dữ liệu đã được gửi đầy đủ.
        // Lấy dữ liệu từ Intent bằng các "khóa" đã định nghĩa ở trên.
        String title = it.getStringExtra(K_TITLE);
        String image = it.getStringExtra(K_IMAGE);
        String url = it.getStringExtra(K_URL);
        String sourceUrl = it.getStringExtra(K_SOURCE_URL);
        String content = it.getStringExtra(K_CONTENT);
        String date = it.getStringExtra(K_DATE);

        // Gọi hàm để hiển thị dữ liệu đã nhận lên giao diện.
        displayArticle(img, tvCaption, tvTitle, tvLede, tvMeta, tvContent, tvSourceLink, btnShare, btnBookmark,
                      title, image, url, sourceUrl, content, date);
    }
    
    // --- PHƯƠNG THỨC loadArticleById() ---
    // Tải thông tin chi tiết của một bài báo từ server bằng ID.
    private void loadArticleById(String articleId, ImageView img, TextView tvCaption, TextView tvTitle, 
                                 TextView tvLede, TextView tvMeta, TextView tvContent, TextView tvSourceLink, 
                                 TextView btnShare, TextView btnBookmark) {
        ApiService api = ApiClient.get().create(ApiService.class);
        // Gọi API getArticleById với ID đã nhận.
        api.getArticleById(articleId).enqueue(new Callback<NewsItem>() {
            // onResponse được gọi khi có phản hồi từ server.
            @Override
            public void onResponse(Call<NewsItem> call, Response<NewsItem> res) {
                // Nếu gọi API thành công và có dữ liệu trả về...
                if (res.isSuccessful() && res.body() != null) {
                    NewsItem article = res.body(); // Lấy đối tượng NewsItem từ phản hồi.
                    // Lấy URL ảnh đầu tiên trong danh sách ảnh.
                    String img_url = (article.getImage_contents() != null && !article.getImage_contents().isEmpty())
                            ? article.getImage_contents().get(0) : null;
                    // Sau khi có đủ dữ liệu, gọi hàm displayArticle để hiển thị.
                    displayArticle(img, tvCaption, tvTitle, tvLede, tvMeta, tvContent, tvSourceLink, btnShare, btnBookmark,
                                  article.getTitle(), img_url, article.getUrl(), article.getSource_url(), 
                                  article.getText_content(), article.getCrawled_at());
                } else {
                    // Nếu không thành công, thông báo cho người dùng và đóng màn hình.
                    Toast.makeText(DetailActivity.this, "Không tìm thấy bài viết", Toast.LENGTH_SHORT).show();
                    finish(); // `finish()` dùng để đóng Activity hiện tại.
                }
            }
            
            // onFailure được gọi khi có lỗi kết nối mạng.
            @Override
            public void onFailure(Call<NewsItem> call, Throwable t) {
                Log.e("API", "FAIL", t);
                Toast.makeText(DetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
    
    // --- PHƯƠNG THỨC displayArticle() ---
    // Chịu trách nhiệm lấy dữ liệu và "đổ" lên các thành phần giao diện.
    private void displayArticle(ImageView img, TextView tvCaption, TextView tvTitle, TextView tvLede, 
                                TextView tvMeta, TextView tvContent, TextView tvSourceLink, 
                                TextView btnShare, TextView btnBookmark,
                                String title, String image, String url, String sourceUrl, 
                                String content, String date) {
        // 1. Ảnh và chú thích:
        // Dùng thư viện Glide để tải ảnh từ URL.
        // .placeholder(): Ảnh hiển thị trong lúc đang tải.
        // .error(): Ảnh hiển thị nếu tải lỗi.
        // .centerCrop(): Cắt ảnh cho vừa với ImageView.
        Glide.with(img).load(image).placeholder(R.drawable.news1).error(R.drawable.news1).centerCrop().into(img);
        // Lấy tên miền từ URL nguồn để làm chú thích ảnh.
        tvCaption.setText(domain(sourceUrl != null ? sourceUrl : url));

        // 2. Tiêu đề:
        tvTitle.setText(safe(title));

        // 3. Đoạn sapo (lede): Lấy một phần đầu của nội dung để làm đoạn tóm tắt.
        tvLede.setText(makeLede(content));

        // 4. Thông tin meta: Hiển thị ngày tháng.
        tvMeta.setText(safe(formatDate(date)));

        // 5. Nội dung chính:
        tvContent.setText(safe(content));

        // 6. Link nguồn:
        tvSourceLink.setText(url != null ? url : "");
        // Gán sự kiện click: khi nhấn vào sẽ gọi hàm openUrl để mở trình duyệt.
        tvSourceLink.setOnClickListener(v -> openUrl(url));

        // 7. Nút Chia sẻ:
        btnShare.setOnClickListener(v -> {
            Intent share = new Intent(Intent.ACTION_SEND); // Tạo một Intent hành động chia sẻ.
            share.setType("text/plain"); // Kiểu dữ liệu là văn bản thuần túy.
            // Đính kèm tiêu đề và URL vào nội dung chia sẻ.
            share.putExtra(Intent.EXTRA_SUBJECT, title);
            share.putExtra(Intent.EXTRA_TEXT, (title == null ? "" : title) + "\n" + (url == null ? "" : url));
            // Mở hộp thoại chia sẻ của hệ thống.
            startActivity(Intent.createChooser(share, "Chia sẻ bài viết"));
        });

        // 8. Nút Đánh dấu (Bookmark):
        // Hiện tại chỉ là một chức năng giả (demo), hiển thị thông báo khi nhấn.
        btnBookmark.setOnClickListener(v -> Toast.makeText(this, "Đã lưu (demo)", Toast.LENGTH_SHORT).show());
    }

    // --- CÁC PHƯƠNG THỨC HỖ TRỢ (HELPER METHODS) ---

    // Mở một URL trong trình duyệt web của điện thoại.
    private void openUrl(String u) {
        if (TextUtils.isEmpty(u)) { // Kiểm tra xem URL có rỗng không.
            Toast.makeText(this, "Không có đường dẫn bài gốc", Toast.LENGTH_SHORT).show();
            return;
        }
        // Tạo Intent với hành động xem (ACTION_VIEW) và dữ liệu là URL đã được phân tích.
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u)));
    }

    // Đảm bảo chuỗi không bao giờ là null. Nếu null, trả về chuỗi rỗng "".
    private String safe(String s) { return s == null ? "" : s; }

    // Tạo đoạn sapo (lede) từ nội dung bài viết.
    private String makeLede(String content) {
        if (content == null) return "";
        String c = content.trim(); // Xóa khoảng trắng thừa.
        // Nếu nội dung quá dài, cắt lấy 500 ký tự đầu và thêm dấu "…"
        if (c.length() > 500) c = c.substring(0, 500) + "…";
        return c;
    }

    // Định dạng lại chuỗi ngày tháng.
    private String formatDate(String d) {
        if (d == null || d.length() < 10) return "";
        // Dữ liệu ngày tháng từ API có dạng "2025-10-06T22:31:56.869000".
        // Hàm này cắt lấy 10 ký tự đầu tiên để được "2025-10-06".
        return d.substring(0, 10);
    }

    // Lấy tên miền (domain) từ một URL đầy đủ.
    private String domain(String u) {
        try {
            if (u == null || u.isEmpty()) return "";
            // Sử dụng lớp java.net.URI để phân tích URL.
            java.net.URI uri = new java.net.URI(u);
            String host = uri.getHost(); // Lấy phần host (ví dụ: www.example.com).
            // Xóa tiền tố "www." nếu có.
            return host != null ? host.replaceFirst("^www\\.", "") : "";
        } catch (Exception e) {
            // Bắt lỗi nếu URL không hợp lệ.
            return "";
        }
    }
}
