# api_mongo.py

# --- IMPORT CÁC THƯ VIỆN CẦN THIẾT ---
from fastapi import FastAPI, Query
from fastapi.middleware.cors import CORSMiddleware
from motor.motor_asyncio import AsyncIOMotorClient # Driver bất đồng bộ cho MongoDB
from typing import Any, Dict, List
from dotenv import load_dotenv # Thư viện để tải biến môi trường từ file .env
from datetime import datetime
import os
from config import MONGO_URI, DB_NAME # Import chuỗi kết nối và tên DB từ file config

# --- CẤU HÌNH VÀ KHỞI TẠO ---

# Tải các biến môi trường từ file .env vào os.environ
# Giúp quản lý các thông tin nhạy cảm như chuỗi kết nối DB một cách an toàn.
load_dotenv()

# Khởi tạo một client để kết nối tới MongoDB.
# AsyncIOMotorClient là phiên bản bất đồng bộ, hoạt động tốt với FastAPI.
client = AsyncIOMotorClient(MONGO_URI)
# Chọn cơ sở dữ liệu (database) cụ thể để làm việc từ client đã kết nối.
db = client[DB_NAME]

# Tạo một instance của ứng dụng FastAPI. Đây là đối tượng chính của API.
app = FastAPI(title="News Crawler API")

# Cấu hình CORS (Cross-Origin Resource Sharing) Middleware.
# Dòng này cho phép các yêu cầu từ bất kỳ nguồn gốc (domain) nào,
# sử dụng bất kỳ phương thức (GET, POST,...) và header nào.
# Rất hữu ích khi phát triển frontend và backend trên các domain khác nhau.
app.add_middleware(
    CORSMiddleware, 
    allow_origins=["*"], 
    allow_methods=["*"], 
    allow_headers=["*"]
)

# --- CÁC HÀM TIỆN ÍCH VÀ HẰNG SỐ ---

# PROJECTION trong MongoDB dùng để chỉ định các trường (fields) bạn muốn trả về trong kết quả truy vấn.
# Việc này giúp giảm lượng dữ liệu truyền tải từ DB về ứng dụng, tăng hiệu suất.
# Ở đây, chúng ta chỉ muốn lấy các trường được liệt kê. `1` có nghĩa là "lấy trường này".
PROJECTION = {"title": 1, "text_content": 1, "url": 1, "image_contents": 1, "crawled_at": 1, "type": 1}

def serialize(doc: Dict[str, Any]) -> Dict[str, Any]:
    """
    Hàm helper để chuyển đổi một document từ MongoDB thành định dạng JSON-friendly.
    MongoDB trả về dữ liệu có kiểu đặc biệt (ObjectId, datetime) không tương thích trực tiếp với JSON.
    """
    # Chuyển đổi _id từ kiểu ObjectId của MongoDB thành một chuỗi (string).
    doc["_id"] = str(doc["_id"])
    
    # Duyệt qua các cặp key-value trong document.
    for k, v in list(doc.items()):
        # Nếu giá trị là một đối tượng datetime
        if isinstance(v, datetime):
            # Chuyển nó thành chuỗi theo định dạng ISO (ví dụ: "2025-10-08T15:30:00").
            doc[k] = v.isoformat()
    return doc

# --- ĐỊNH NGHĨA CÁC API ENDPOINTS ---

@app.get("/health")
def health():
    """Endpoint đơn giản để kiểm tra xem API có đang hoạt động hay không (health check)."""
    return {"ok": True}

@app.get("/articles")
async def get_all_articles(
    skip: int = Query(0, ge=0), 
    limit: int = Query(20, ge=1, le=100)
):
    """
    Endpoint để lấy danh sách các bài báo, có hỗ trợ phân trang (pagination).
    - skip: Bỏ qua bao nhiêu bài báo đầu tiên.
    - limit: Giới hạn số lượng bài báo trả về trong một lần gọi.
    - Query() của FastAPI giúp validate các tham số: ge=0 (lớn hơn hoặc bằng 0), le=100 (nhỏ hơn hoặc bằng 100).
    """
    # Lấy ra collection 'web_articles' (tương đương một bảng trong SQL).
    col = db["web_articles"]
    # Thực hiện truy vấn find():
    # {} -> không có điều kiện lọc, lấy tất cả document.
    # PROJECTION -> chỉ lấy các trường đã định nghĩa.
    # sort -> sắp xếp theo 'crawled_at' giảm dần (-1), tức là bài mới nhất lên đầu.
    # .skip().limit() -> thực hiện phân trang.
    cursor = col.find({}, PROJECTION, sort=[("crawled_at", -1)]).skip(skip).limit(limit)
    # Dùng `await` để thực thi truy vấn và chuyển con trỏ (cursor) thành một danh sách (list).
    docs = await cursor.to_list(length=limit)
    # Trả về danh sách các document đã được serialize.
    return [serialize(d) for d in docs]

@app.get("/latest_articles")
async def latest_articles(n: int = Query(10, ge=1, le=1000)):
    """Endpoint để lấy `n` bài báo mới nhất."""
    col = db["web_articles"]
    # Tương tự như trên nhưng dùng `limit=n` để giới hạn số lượng kết quả ngay từ đầu.
    cursor = col.find({}, PROJECTION, sort=[("crawled_at", -1)], limit=n)
    docs = await cursor.to_list(length=n)
    return [serialize(d) for d in docs]

@app.get("/facebook_posts")
async def get_all_fb_posts(
    skip: int = Query(0, ge=0), 
    limit: int = Query(20, ge=1, le=100)
):
    """Endpoint để lấy danh sách các bài đăng Facebook, có hỗ trợ phân trang."""
    col = db["fb_posts"]
    # Truy vấn này không dùng PROJECTION, tức là sẽ lấy tất cả các trường của document.
    cursor = col.find({}, sort=[("crawled_at", -1)]).skip(skip).limit(limit)
    docs = await cursor.to_list(length=limit)
    return [serialize(d) for d in docs]

@app.get("/latest_facebook_posts")
async def latest_facebook_posts(n: int = Query(10, ge=1, le=1000)):
    """Endpoint để lấy `n` bài đăng Facebook mới nhất."""
    col = db["fb_posts"]
    cursor = col.find({}, sort=[("crawled_at", -1)], limit=n)
    docs = await cursor.to_list(length=n)
    return [serialize(d) for d in docs]
