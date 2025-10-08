// Khai báo package, nơi chứa tệp này.
package com.example.newsai.ui;

// Import các lớp cần thiết.
import android.view.LayoutInflater; // Dùng để "thổi phồng" một file layout XML thành một đối tượng View trong code.
import android.view.View; // Lớp cơ sở cho mọi thành phần giao diện.
import android.view.ViewGroup; // Một View đặc biệt có thể chứa các View khác.
import android.widget.ImageView; // Hiển thị hình ảnh.
import android.widget.TextView; // Hiển thị văn bản.

import androidx.annotation.NonNull; // Annotation để chỉ ra rằng một tham số hoặc giá trị trả về không bao giờ được null.
import androidx.recyclerview.widget.RecyclerView; // Lớp chính để hiển thị danh sách cuộn.

import com.bumptech.glide.Glide; // Thư viện tải ảnh.
import com.example.newsai.R; // Lớp chứa ID của tất cả các tài nguyên (layout, drawable, string,...).
import com.example.newsai.data.NewsItem; // Lớp model cho một bài báo.

import java.util.ArrayList;
import java.util.List;

// Khai báo lớp NewsAdapter.
// "extends RecyclerView.Adapter<NewsAdapter.VH>" có nghĩa là:
// "Đây là một Adapter cho RecyclerView, và nó sẽ sử dụng một ViewHolder tên là VH (được định nghĩa ở cuối file)".
public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.VH> {

    // --- INTERFACE ĐỂ XỬ LÝ CLICK ---
    // Định nghĩa một "hợp đồng" tên là OnClick. Bất kỳ lớp nào muốn nhận sự kiện click từ Adapter này
    // đều phải triển khai (implement) phương thức `click(NewsItem item)`.
    public interface OnClick { void click(NewsItem item); }

    // --- KHAI BÁO BIẾN ---
    private final List<NewsItem> items = new ArrayList<>(); // Đây là "kho dữ liệu", một danh sách để chứa tất cả các bài báo sẽ hiển thị.
    private final OnClick onClick; // Biến để lưu trữ đối tượng sẽ lắng nghe sự kiện click.

    // --- CONSTRUCTOR ---
    // Hàm khởi tạo của lớp. Khi một đối tượng NewsAdapter được tạo (ví dụ trong MainActivity),
    // nó phải nhận vào một đối tượng OnClick.
    public NewsAdapter(OnClick onClick) { this.onClick = onClick; }

    // --- PHƯƠNG THỨC submit() ---
    // Dùng để cập nhật dữ liệu cho Adapter.
    public void submit(List<NewsItem> list) {
        items.clear(); // Xóa sạch dữ liệu cũ.
        if (list != null) items.addAll(list); // Nếu danh sách mới không rỗng, thêm tất cả vào.
        notifyDataSetChanged(); // **QUAN TRỌNG**: Báo cho RecyclerView biết rằng "dữ liệu đã thay đổi, hãy vẽ lại toàn bộ danh sách đi!".
    }

    // --- PHƯƠNG THỨC onCreateViewHolder() ---
    // Được gọi khi RecyclerView cần tạo một "hộp trưng bày" (ViewHolder) MỚI.
    // Việc này chỉ xảy ra vài lần đầu tiên, đủ để lấp đầy màn hình và một vài item dự phòng.
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // "Thổi phồng" file layout activity_item_news.xml thành một đối tượng View.
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_news, parent, false);
        // Tạo một ViewHolder (VH) mới với View vừa tạo và trả về.
        return new VH(v);
    }

    // --- PHƯƠNG THỨC onBindViewHolder() ---
    // **QUAN TRỌNG**: Được gọi khi RecyclerView muốn hiển thị dữ liệu tại một vị trí cụ thể.
    // Nó sẽ TÁI SỬ DỤNG một ViewHolder cũ (một "hộp trưng bày" đã bị cuộn ra khỏi màn hình) và đổ dữ liệu mới vào.
    @Override
    public void onBindViewHolder(@NonNull VH h, int p) {
        // Lấy dữ liệu (NewsItem) tại vị trí `p` (position) trong danh sách.
        NewsItem it = items.get(p);

        // --- ĐỔ DỮ LIỆU LÊN VIEW ---

        // 1. Tiêu đề:
        String title = it.getTitle();
        // Nếu bài báo không có tiêu đề, lấy một đoạn nội dung đầu tiên để làm tiêu đề tạm.
        if (title == null || title.trim().isEmpty()) {
            String tc = it.getText_content();
            title = tc == null ? "" : (tc.length() > 120 ? tc.substring(0, 120) + "…" : tc);
        }
        h.title.setText(title); // Gán tiêu đề cho TextView.

        // 2. Hình ảnh:
        String img = null;
        // Lấy URL của ảnh đầu tiên trong danh sách ảnh (nếu có).
        if (it.getImage_contents() != null && !it.getImage_contents().isEmpty()) img = it.getImage_contents().get(0);
        // Dùng Glide để tải ảnh.
        Glide.with(h.img).load(img).placeholder(R.drawable.news1).error(R.drawable.news1).centerCrop().into(h.img);

        // 3. Nguồn tin (Chip):
        // Lấy tên miền từ URL.
        String src = domain(it.getSource_url() != null ? it.getSource_url() : it.getUrl());
        // Nếu không có tên miền (ví dụ tin từ facebook), mặc định là "facebook.com".
        h.chipSource.setText(src.isEmpty() ? "facebook.com" : src);

        // 4. Ngày đăng:
        String d = it.getCrawled_at();
        // Cắt chuỗi ngày tháng để lấy phần ngày (YYYY-MM-DD).
        h.tvDate.setText(d != null && d.length() >= 10 ? d.substring(0, 10) : "");

        // 5. Gán sự kiện Click:
        // `h.itemView` là toàn bộ View của một item.
        // Khi người dùng nhấn vào, nó sẽ gọi phương thức `click()` của đối tượng `onClick`
        // (chính là phương thức `openDetail` trong MainActivity) và truyền vào dữ liệu của item đó.
        h.itemView.setOnClickListener(v -> onClick.click(it));
    }
    
    // --- PHƯƠNG THỨC HỖ TRỢ domain() ---
    // Lấy tên miền từ một URL.
    private String domain(String u) {
        if (u == null || u.isEmpty()) return "";
        try {
            java.net.URI uri = new java.net.URI(u);
            String host = uri.getHost();
            return host != null ? host.replaceFirst("^www\\.", "") : ""; // Xóa "www."
        } catch (Exception e) { return ""; }
    }

    // --- PHƯƠNG THỨC getItemCount() ---
    // Trả về tổng số item có trong danh sách dữ liệu. RecyclerView dựa vào đây để biết cần vẽ bao nhiêu item.
    @Override
    public int getItemCount() { return items.size(); }

    // --- LỚP VIEWHOLDER (VH) ---
    // Đây là một "lớp con" bên trong Adapter, có vai trò như một cái "hộp đựng".
    // Nhiệm vụ của nó là giữ các tham chiếu đến các View con bên trong một item layout (ImageView, TextView,...).
    // Việc này giúp tránh phải gọi `findViewById()` nhiều lần, vốn là một tác vụ tốn kém, giúp việc cuộn danh sách mượt mà hơn.
    protected static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView title;
        TextView chipSource;
        TextView tvDate;
        // Constructor của ViewHolder
        VH(@NonNull View v) {
            super(v); // Gọi constructor của lớp cha.
            // Thực hiện `findViewById` một lần duy nhất tại đây.
            img = v.findViewById(R.id.imgNews);
            title = v.findViewById(R.id.tvNewsTitle);
            chipSource = v.findViewById(R.id.chipSource);
            tvDate = v.findViewById(R.id.tvDate);
        }
    }
}
