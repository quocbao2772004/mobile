// Khai báo package.
package com.example.newsai.network;

// Import các lớp model dữ liệu.
import com.example.newsai.data.NewsItem;
import com.example.newsai.data.ClusterItem;
import com.example.newsai.data.ClusterArticleItem;
import java.util.List;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import retrofit2.http.Query;

// Khai báo interface ApiService.
public interface ApiService {

    // --- LẤY DANH SÁCH CÁC BÀI BÁO ---
    // @GET("articles"): Yêu cầu HTTP GET đến endpoint "articles".
    // URL đầy đủ sẽ là: https://db.dinhmanhhung.net/articles
    // Call<List<NewsItem>>: Mong đợi server trả về một danh sách các đối tượng NewsItem.
    @GET("articles")
    Call<List<NewsItem>> getArticles();

    // --- LẤY DANH SÁCH CÁC BÀI VIẾT FACEBOOK ---
    // @GET("facebook_posts"): Yêu cầu HTTP GET đến endpoint "facebook_posts".
    // URL đầy đủ sẽ là: https://db.dinhmanhhung.net/facebook_posts
    // Call<List<NewsItem>>: Các bài viết facebook cũng có cấu trúc tương tự NewsItem.
    @GET("facebook_posts")
    Call<List<NewsItem>> getFacebookPosts();
    
    // --- LẤY MỘT BÀI BÁO CỤ THỂ BẰNG ID ---
    // @GET("articles/{article_id}"): Endpoint này có một phần động là {article_id}.
    // @Path("article_id") String articleId: Chú thích này báo cho Retrofit hãy lấy giá trị của biến `articleId`
    // và thay thế vào vị trí `{article_id}` trong URL.
    // Ví dụ: nếu gọi `getArticleById("123-abc")`, URL sẽ là: https://db.dinhmanhhung.net/articles/123-abc
    // Call<NewsItem>: Mong đợi server trả về thông tin của MỘT đối tượng NewsItem duy nhất.
    @GET("articles/{article_id}")
    Call<NewsItem> getArticleById(@Path("article_id") String articleId);
    
    // --- CÁC ENDPOINT LIÊN QUAN ĐẾN CỤM TIN (CLUSTER) ---

    // LẤY DANH SÁCH CÁC CỤM TIN HÀNG ĐẦU
    // @GET("clusters/top"): Yêu cầu HTTP GET đến endpoint "clusters/top".
    // @Query("n") int n: Chú thích này sẽ thêm một tham số truy vấn (query parameter) vào cuối URL.
    // Ví dụ: nếu gọi `getTopClusters(20)`, URL sẽ là: https://db.dinhmanhhung.net/clusters/top?n=20
    // Call<List<ClusterItem>>: Mong đợi server trả về một danh sách các đối tượng ClusterItem.
    @GET("clusters/top")
    Call<List<ClusterItem>> getTopClusters(@Query("n") int n);
    
    // LẤY MỘT CỤM TIN CỤ THỂ BẰNG ID
    // Tương tự như getArticleById, sử dụng @Path để thay thế {cluster_id} trong URL.
    // Ví dụ: nếu gọi `getClusterById("xyz-789")`, URL sẽ là: https://db.dinhmanhhung.net/clusters/xyz-789
    // Call<ClusterItem>: Mong đợi server trả về thông tin của MỘT đối tượng ClusterItem.
    @GET("clusters/{cluster_id}")
    Call<ClusterItem> getClusterById(@Path("cluster_id") String clusterId);
    
    // LẤY DANH SÁCH CÁC BÀI BÁO THUỘC MỘT CỤM TIN
    // Sử dụng @Path để xác định cụm tin nào cần lấy bài báo.
    // Ví dụ: nếu gọi `getClusterArticles("xyz-789")`, URL sẽ là: https://db.dinhmanhhung.net/clusters/xyz-789/articles
    // Call<List<ClusterArticleItem>>: Mong đợi server trả về một danh sách các bài báo trong cụm tin đó.
    @GET("clusters/{cluster_id}/articles")
    Call<List<ClusterArticleItem>> getClusterArticles(@Path("cluster_id") String clusterId);
}
