// Khai báo package, nơi chứa tệp này.
package com.example.newsai.ui;

// Import các lớp (class) cần thiết.
import android.view.LayoutInflater; // Dùng để "thổi phồng" (inflate) file layout XML thành đối tượng View.
import android.view.View; // Lớp cơ sở của các thành phần giao diện.
import android.view.ViewGroup; // Một View có thể chứa các View khác.
import android.widget.TextView; // Hiển thị văn bản.
import androidx.annotation.NonNull; // Annotation để chỉ ra rằng giá trị không được null.
import androidx.recyclerview.widget.RecyclerView; // Lớp cơ sở để tạo Adapter cho danh sách.
import com.example.newsai.R; // Lớp chứa ID của tất cả các tài nguyên.
import com.example.newsai.data.ClusterArticleItem; // Lớp model (đối tượng) đại diện cho một bài báo trong một cụm tin.
import java.util.ArrayList; // Lớp triển khai của List, có thể thay đổi kích thước.
import java.util.List; // Giao diện đại diện cho một danh sách các đối tượng.

// Khai báo lớp ClusterArticleAdapter.
// Nó kế thừa từ RecyclerView.Adapter và sử dụng một ViewHolder tên là VH.
public class ClusterArticleAdapter extends RecyclerView.Adapter<ClusterArticleAdapter.VH> {

    // --- INTERFACE ĐỂ XỬ LÝ CLICK ---
    // Định nghĩa một "hợp đồng" để Activity có thể lắng nghe sự kiện click vào một item bài báo.
    public interface OnClick {
        void click(ClusterArticleItem item);
    }

    // --- KHAI BÁO BIẾN ---
    private final List<ClusterArticleItem> items = new ArrayList<>(); // Danh sách để chứa tất cả các bài báo trong cụm tin.
    private final OnClick onClick; // Biến để lưu trữ đối tượng sẽ lắng nghe sự kiện click.

    // --- CONSTRUCTOR ---
    // Hàm khởi tạo của lớp, yêu cầu một đối tượng OnClick khi tạo.
    public ClusterArticleAdapter(OnClick onClick) {
        this.onClick = onClick;
    }

    // --- PHƯƠNG THỨC submit() ---
    // Dùng để cập nhật dữ liệu mới cho Adapter.
    public void submit(List<ClusterArticleItem> list) {
        items.clear(); // Xóa sạch dữ liệu cũ.
        if (list != null) items.addAll(list); // Thêm tất cả dữ liệu từ danh sách mới vào.
        notifyDataSetChanged(); // Báo cho RecyclerView biết rằng dữ liệu đã thay đổi và cần vẽ lại.
    }

    // --- PHƯƠNG THỨC onCreateViewHolder() ---
    // Được gọi khi RecyclerView cần tạo một ViewHolder (một "hộp" cho một bài báo) MỚI.
    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // "Thổi phồng" layout từ file item_cluster_article.xml.
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cluster_article, parent, false);
        // Tạo một ViewHolder (VH) mới với View vừa tạo và trả về.
        return new VH(v);
    }

    // --- PHƯƠNG THỨC onBindViewHolder() ---
    // Được gọi khi RecyclerView muốn hiển thị dữ liệu tại một vị trí cụ thể.
    @Override
    public void onBindViewHolder(@NonNull VH h, int p) {
        // Lấy dữ liệu (ClusterArticleItem) tại vị trí `p` (position) trong danh sách.
        ClusterArticleItem article = items.get(p);

        // --- ĐỔ DỮ LIỆU LÊN VIEW ---

        // 1. Hiển thị thứ hạng của bài báo (1, 2, 3...).
        h.tvRank.setText(String.valueOf(article.getRank() + 1));

        // 2. Hiển thị tiêu đề.
        String title = article.getTitle();
        // Nếu tiêu đề rỗng, lấy một đoạn văn bản đầu tiên để làm tiêu đề tạm.
        if (title == null || title.trim().isEmpty()) {
            String text = article.getText();
            title = text != null && text.length() > 80 
                ? text.substring(0, 80) + "..." 
                : (text != null ? text : "Không có tiêu đề");
        }
        h.tvArticleTitle.setText(title);

        // 3. Hiển thị nguồn tin.
        String source = article.getSource();
        // Kiểm tra xem nguồn có phải là "facebook" không (không phân biệt hoa thường).
        h.tvSourceBadge.setText(source != null && source.equalsIgnoreCase("facebook") ? "Facebook" : "Web");

        // 4. Hiển thị một đoạn xem trước của nội dung.
        String text = article.getText();
        if (text != null && text.length() > 120) {
            text = text.substring(0, 120) + "..."; // Cắt bớt nếu quá dài.
        }
        h.tvArticleText.setText(text != null ? text : "");

        // 5. Gán sự kiện click cho toàn bộ item.
        h.itemView.setOnClickListener(v -> {
            if (onClick != null) onClick.click(article); // Gọi hàm click của listener đã đăng ký.
        });
    }

    // --- PHƯƠNG THỨC getItemCount() ---
    // Trả về tổng số bài báo có trong danh sách.
    @Override
    public int getItemCount() {
        return items.size();
    }

    // --- LỚP VIEWHOLDER (VH) ---
    // Lớp "hộp đựng" này giữ các tham chiếu đến các View con bên trong một item layout.
    // Việc này giúp tránh phải gọi `findViewById()` nhiều lần, giúp cuộn danh sách mượt mà hơn.
    protected static class VH extends RecyclerView.ViewHolder {
        TextView tvRank;
        TextView tvArticleTitle;
        TextView tvArticleText;
        TextView tvSourceBadge;

        // Constructor của ViewHolder.
        VH(@NonNull View v) {
            super(v);
            // Thực hiện `findViewById` một lần duy nhất tại đây.
            tvRank = v.findViewById(R.id.tvRank);
            tvArticleTitle = v.findViewById(R.id.tvArticleTitle);
            tvArticleText = v.findViewById(R.id.tvArticleText);
            tvSourceBadge = v.findViewById(R.id.tvSourceBadge);
        }
    }
}
