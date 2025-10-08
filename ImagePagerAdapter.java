// Khai báo package.
package com.example.newsai.ui;

// Import các lớp cần thiết.
import android.view.LayoutInflater; // Dùng để "thổi phồng" file layout XML thành đối tượng View.
import android.view.View; // Lớp cơ sở của các thành phần giao diện.
import android.view.ViewGroup; // Một View có thể chứa các View khác.
import android.widget.ImageView; // Để hiển thị hình ảnh.
import androidx.annotation.NonNull; // Annotation để chỉ ra rằng giá trị không được null.
import androidx.recyclerview.widget.RecyclerView; // Lớp cơ sở để tạo Adapter cho danh sách.
import com.bumptech.glide.Glide; // Thư viện tải ảnh từ URL.
import com.example.newsai.R; // Lớp chứa ID của các tài nguyên.
import java.util.List; // Lớp để làm việc với danh sách.

// Khai báo lớp ImagePagerAdapter.
// Nó kế thừa từ RecyclerView.Adapter và sử dụng một ViewHolder tên là ImageViewHolder.
public class ImagePagerAdapter extends RecyclerView.Adapter<ImagePagerAdapter.ImageViewHolder> {
    
    // --- KHAI BÁO BIẾN ---
    // `private final List<String> imageUrls;`
    // - `private`: Chỉ có thể truy cập từ bên trong lớp này.
    // - `final`: Biến này phải được khởi tạo trong constructor và không thể thay đổi sau đó.
    // Đây là danh sách chứa các đường dẫn (URL) của tất cả các ảnh cần hiển thị.
    private final List<String> imageUrls;
    
    // --- CONSTRUCTOR ---
    // Hàm khởi tạo của lớp. Khi tạo một đối tượng ImagePagerAdapter,
    // bạn phải cung cấp cho nó một danh sách các URL ảnh.
    public ImagePagerAdapter(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
    
    // --- PHƯƠNG THỨC onCreateViewHolder() ---
    // Được gọi khi RecyclerView/ViewPager2 cần tạo một ViewHolder (một "trang" hiển thị ảnh) MỚI.
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // "Thổi phồng" layout từ file item_image_pager.xml, đây là layout cho MỘT trang ảnh.
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_pager, parent, false);
        // Tạo một ImageViewHolder mới với View vừa tạo và trả về.
        return new ImageViewHolder(view);
    }
    
    // --- PHƯƠNG THỨC onBindViewHolder() ---
    // Được gọi khi RecyclerView/ViewPager2 muốn hiển thị một ảnh tại một vị trí (trang) cụ thể.
    // Nó sẽ tái sử dụng ViewHolder và đổ dữ liệu mới vào.
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        // Lấy URL của ảnh tại vị trí (trang) hiện tại.
        String imageUrl = imageUrls.get(position);
        
        // Dòng này giúp tránh hiệu ứng "nháy ảnh cũ" khi tái sử dụng ViewHolder.
        // Nó xóa ảnh đang hiển thị trên ViewHolder trước khi tải ảnh mới.
        holder.imageView.setImageDrawable(null);
        
        // Kiểm tra xem URL có hợp lệ không (khác null và không rỗng).
        if (imageUrl != null && !imageUrl.isEmpty()) {
            // Sử dụng thư viện Glide để tải ảnh một cách hiệu quả.
            Glide.with(holder.imageView.getContext()) // Bắt đầu một yêu cầu tải ảnh trong context của ImageView.
                    .load(imageUrl) // Nguồn ảnh cần tải.
                    .placeholder(android.R.color.white) // Hiển thị màu trắng trong khi đang tải ảnh.
                    .error(android.R.color.white) // Hiển thị màu trắng nếu có lỗi xảy ra khi tải.
                    .centerCrop() // Cắt ảnh để vừa với kích thước của ImageView mà không làm biến dạng tỷ lệ.
                    .into(holder.imageView); // ImageView đích để hiển thị ảnh sau khi tải xong.
        } else {
            // Nếu URL không hợp lệ, đặt nền của ImageView thành màu trắng.
            holder.imageView.setBackgroundColor(
                holder.imageView.getContext().getResources().getColor(android.R.color.white)
            );
        }
    }
    
    // --- PHƯƠNG THỨC getItemCount() ---
    // Trả về tổng số trang/ảnh có trong danh sách.
    @Override
    public int getItemCount() {
        // Nếu danh sách imageUrls khác null, trả về kích thước của nó. Nếu không, trả về 0.
        return imageUrls != null ? imageUrls.size() : 0;
    }
    
    // --- LỚP VIEWHOLDER ---
    // Lớp "hộp đựng" này giữ tham chiếu đến ImageView bên trong layout của một trang.
    // Giúp việc truy cập ImageView nhanh hơn và hiệu quả hơn.
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        
        // Constructor của ViewHolder.
        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            // Ánh xạ biến imageView với ImageView có ID là "imageView" trong file layout item_image_pager.xml.
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}
