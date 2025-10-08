// Khai báo package, nơi chứa tệp này.
package com.example.newsai.ui;

// Import các lớp (class) cần thiết.
import android.view.LayoutInflater; // Dùng để "thổi phồng" (inflate) file layout XML thành đối tượng View.
import android.view.View; // Lớp cơ sở của các thành phần giao diện.
import android.view.ViewGroup; // Một View có thể chứa các View khác.
import android.widget.ImageView; // Hiển thị hình ảnh.
import android.widget.TextView; // Hiển thị văn bản.
import androidx.annotation.NonNull; // Annotation để chỉ ra rằng giá trị không được null.
import androidx.recyclerview.widget.RecyclerView; // Lớp cơ sở để tạo Adapter cho danh sách.
import com.bumptech.glide.Glide; // Thư viện để tải và hiển thị hình ảnh từ URL.
import com.example.newsai.R; // Lớp chứa ID của tất cả các tài nguyên.
import com.example.newsai.data.ClusterItem; // Lớp model (đối tượng) đại diện cho một cụm tin.
import java.util.ArrayList; // Lớp triển khai của List, có thể thay đổi kích thước.
import java.util.List; // Giao diện đại diện cho một danh sách các đối tượng.

// Khai báo lớp ClusterAdapter.
// Nó kế thừa từ RecyclerView.Adapter và sử dụng một ViewHolder tên là VH.
public class ClusterAdapter extends RecyclerView.Adapter<ClusterAdapter.VH> {

    // --- INTERFACE ĐỂ XỬ LÝ CLICK ---
    // Định nghĩa một "hợp đồng" để MainActivity có thể lắng nghe sự kiện click vào một cụm tin.
    public interface OnClick {
        void click(ClusterItem item);
    }

    // --- KHAI BÁO BIẾN ---
    private final List<ClusterItem> items = new ArrayList<>(); // Danh sách để chứa tất cả các cụm tin.
    private final OnClick onClick; // Biến để lưu trữ đối tượng sẽ lắng nghe sự kiện click.

    // --- CONSTRUCTOR ---
    // Hàm khởi tạo của lớp, yêu cầu một đối tượng OnClick khi tạo.
    public ClusterAdapter(OnClick onClick) {
        this.onClick = onClick;
    }

    // --- PHƯƠNG THỨC submit() ---
    // Dùng để cập nhật dữ liệu mới cho Adapter.
    public void submit(List<ClusterItem> list) {
        items.clear(); // Xóa sạch dữ liệu cũ.
        if (list != null) items.addAll(list); // Thêm tất cả dữ liệu từ danh sách mới vào.
        notifyDataSetChanged(); // Báo cho RecyclerView biết rằng dữ liệu đã thay đổi và cần vẽ lại.
    }

    // --- PHƯƠNG THỨC onCreateViewHolder() ---
    // Được gọi khi RecyclerView cần tạo một ViewHolder MỚI.
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // "Thổi phồng" layout từ file activity_item_news.xml.
        // **Lưu ý**: Cả NewsAdapter và ClusterAdapter đều dùng chung layout này.
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.activity_item_news, parent, false);
        // Tạo một ViewHolder (VH) mới với View vừa tạo và trả về.
        return new VH(v);
    }

    // --- PHƯƠNG THỨC onBindViewHolder() ---
    // Được gọi khi RecyclerView muốn hiển thị dữ liệu tại một vị trí cụ thể.
    @Override
    public void onBindViewHolder(@NonNull VH h, int p) {
        // Lấy dữ liệu (ClusterItem) tại vị trí `p` (position) trong danh sách.
        ClusterItem cluster = items.get(p);

        // --- ĐỔ DỮ LIỆU CỦA CỤM TIN LÊN VIEW ---

        // 1. Hiển thị tiêu đề của cụm tin.
        h.title.setText(cluster.getTitle() != null ? cluster.getTitle() : "");

        // (Phần code này lấy tóm tắt nhưng không gán vào TextView nào trong layout `activity_item_news.xml`,
        // có thể là code còn sót lại hoặc dự định cho một layout khác)
        String summary = cluster.getSummary();
        if (summary != null && summary.length() > 100) {
            summary = summary.substring(0, 100) + "...";
        }

        // 2. Hiển thị hình ảnh đại diện cho cụm tin.
        String img = null;
        // Lấy URL của ảnh đầu tiên trong danh sách ảnh của cụm tin (nếu có).
        if (cluster.getImage_contents() != null && !cluster.getImage_contents().isEmpty()) {
            img = cluster.getImage_contents().get(0);
        }
        
        // Xóa ảnh cũ trước khi tải ảnh mới để tránh hiệu ứng "nháy ảnh" khi cuộn nhanh.
        h.img.setImageDrawable(null);
        
        // Kiểm tra xem URL ảnh có hợp lệ không.
        if (img != null && !img.isEmpty()) {
            // Dùng Glide để tải ảnh.
            Glide.with(h.img)
                    .load(img)
                    .placeholder(android.R.color.white) // Hiển thị nền trắng trong lúc tải.
                    .error(android.R.color.white) // Hiển thị nền trắng nếu tải lỗi.
                    .centerCrop() // Cắt ảnh cho vừa với ImageView.
                    .into(h.img);
        } else {
            // Nếu cụm tin không có ảnh, đặt nền của ImageView thành màu trắng.
            h.img.setBackgroundColor(
                h.img.getContext().getResources().getColor(android.R.color.white)
            );
        }

        // 3. Hiển thị nguồn tin chính của cụm tin.
        String src = cluster.getPrimary_source();
        h.chipSource.setText(src != null ? src : "");

        // 4. Hiển thị ngày tạo và tổng số bài báo trong cụm tin.
        String date = formatDate(cluster.getCreated_at());
        h.tvDate.setText(date + " • " + cluster.getArticle_count() + " bài");

        // 5. Gán sự kiện click cho toàn bộ item.
        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.click(cluster); // Gọi hàm click của listener đã đăng ký (openClusterDetail trong MainActivity).
        });
    }

    // --- PHƯƠNG THỨC HỖ TRỢ formatDate() ---
    // Định dạng lại chuỗi ngày tháng để chỉ lấy phần ngày (YYYY-MM-DD).
    private String formatDate(String d) {
        if (d == null || d.length() < 10) return "";
        return d.substring(0, 10);
    }

    // --- PHƯƠNG THỨC getItemCount() ---
    // Trả về tổng số cụm tin có trong danh sách.
    @Override
    public int getItemCount() {
        return items.size();
    }

    // --- LỚP VIEWHOLDER (VH) ---
    // Lớp "hộp đựng" này giữ các tham chiếu đến các View con.
    // Nó giống hệt với ViewHolder trong NewsAdapter vì chúng dùng chung một file layout.
    protected static class VH extends RecyclerView.ViewHolder {
        ImageView img;
        TextView title;
        TextView chipSource;
        TextView tvDate;

        // Constructor của ViewHolder.
        VH(@NonNull View v) {
            super(v);
            // Thực hiện `findViewById` một lần duy nhất tại đây.
            img = v.findViewById(R.id.imgNews);
            title = v.findViewById(R.id.tvNewsTitle);
            chipSource = v.findViewById(R.id.chipSource);
            tvDate = v.findViewById(R.id.tvDate);
        }
    }
}
