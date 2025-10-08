// Khai báo package, nơi chứa tệp này.
package com.example.newsai.network;

// Import các lớp cần thiết từ thư viện OkHttp3 và Retrofit2.
import okhttp3.OkHttpClient; // Lớp client để thực hiện các yêu cầu HTTP. Retrofit sử dụng nó "ngầm" bên dưới.
import okhttp3.logging.HttpLoggingInterceptor; // Một công cụ để ghi lại (log) chi tiết các yêu cầu và phản hồi mạng. Rất hữu ích cho việc gỡ lỗi.
import retrofit2.Retrofit; // Lớp chính của thư viện Retrofit.
import retrofit2.converter.moshi.MoshiConverterFactory; // Bộ chuyển đổi để biến đổi dữ liệu JSON trả về từ server thành các đối tượng Java (POJO).

// Khai báo lớp ApiClient.
public class ApiClient {
    // --- KHAI BÁO BIẾN STATIC ---
    // `private static Retrofit instance;`
    // - `static`: Biến này thuộc về chính lớp ApiClient, chứ không thuộc về một đối tượng cụ thể nào. Sẽ chỉ có MỘT `instance` cho toàn bộ ứng dụng.
    // - `private`: Chỉ có thể truy cập biến này từ bên trong lớp ApiClient.
    // - Ban đầu, giá trị của nó là `null`.
    private static Retrofit instance;

    // --- PHƯƠƠNG THỨC get() ---
    // `public static Retrofit get()`
    // - `public`: Có thể gọi phương thức này từ bất kỳ đâu trong dự án.
    // - `static`: Có thể gọi trực tiếp thông qua tên lớp (ApiClient.get()) mà không cần tạo đối tượng ApiClient.
    // Đây là phương thức duy nhất để lấy được đối tượng Retrofit.
    public static Retrofit get() {
        // --- KIỂM TRA SINGLETON ---
        // Kiểm tra xem instance đã được khởi tạo hay chưa.
        if (instance == null) {
            // Nếu đây là lần đầu tiên gọi hàm get(), instance sẽ là null và khối lệnh này sẽ được thực thi.
            // Những lần gọi sau, instance đã có giá trị nên khối lệnh này sẽ được bỏ qua.

            // --- CẤU HÌNH LOGGING ---
            // Tạo một đối tượng HttpLoggingInterceptor để theo dõi các yêu cầu mạng.
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            // Đặt mức độ log là BODY, nghĩa là nó sẽ in ra toàn bộ thông tin của yêu cầu và phản hồi
            // (headers, body,...) trong Logcat của Android Studio.
            log.setLevel(HttpLoggingInterceptor.Level.BODY);

            // --- CẤU HÌNH OKHTTPCLIENT ---
            // Tạo một OkHttpClient tùy chỉnh.
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(log) // Thêm interceptor vừa tạo vào client. Giờ đây mọi yêu cầu đi qua client này đều sẽ được log lại.
                    .build(); // Hoàn thành việc xây dựng client.

            // --- CẤU HÌNH VÀ TẠO RETROFIT ---
            // Bắt đầu quá trình xây dựng (build) đối tượng Retrofit và gán nó vào biến `instance`.
            instance = new Retrofit.Builder()
                    // 1. .baseUrl("https://db.dinhmanhhung.net/")
                    // Đặt URL gốc cho tất cả các API endpoint. Mọi đường dẫn tương đối trong ApiService sẽ được nối vào sau URL này.
                    // Ví dụ: nếu trong ApiService bạn định nghĩa `@GET("articles")`, Retrofit sẽ gọi đến `https://db.dinhmanhhung.net/articles`.
                    .baseUrl("https://db.dinhmanhhung.net/")
                    
                    // 2. .addConverterFactory(MoshiConverterFactory.create())
                    // Chỉ định bộ chuyển đổi. Khi Retrofit nhận dữ liệu JSON từ server, nó sẽ dùng Moshi
                    // để tự động "phân tích" chuỗi JSON đó và "đúc" thành các đối tượng Java tương ứng (ví dụ: List<NewsItem>).
                    .addConverterFactory(MoshiConverterFactory.create())
                    
                    // 3. .client(client)
                    // Gắn OkHttpClient đã được cấu hình (với logging) vào Retrofit.
                    .client(client)
                    
                    // 4. .build()
                    // Hoàn tất việc xây dựng và tạo ra đối tượng Retrofit.
                    .build();
        }
        // Trả về đối tượng Retrofit (hoặc là vừa được tạo, hoặc là đã tồn tại từ lần gọi trước).
        return instance;
    }
}
