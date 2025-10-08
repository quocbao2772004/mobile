"""
    Helper functions for Facebook crawler
    (Các hàm tiện ích hỗ trợ cho việc cào dữ liệu Facebook)
"""
# --- IMPORT CÁC THƯ VIỆN CẦN THIẾT ---
import asyncio  # Thư viện cho lập trình bất đồng bộ, giúp chạy nhiều tác vụ song song
import time     # Thư viện cung cấp các hàm liên quan đến thời gian
from typing import List, Dict, Any # Cung cấp các "type hints" (chỉ dẫn kiểu dữ liệu) để code rõ ràng hơn
from datetime import datetime, timedelta # Các lớp để xử lý ngày giờ và khoảng thời gian

async def sleep(miliseconds: int):
    """
    Tạm dừng chương trình (coroutine) trong một khoảng thời gian tính bằng mili giây.
    Việc này rất quan trọng để tránh gửi quá nhiều yêu cầu tới server và bị chặn.
    
    Args:
        miliseconds: Số mili giây cần tạm dừng.
    """
    # asyncio.sleep nhận vào số giây, nên ta cần chia mili giây cho 1000
    await asyncio.sleep(miliseconds / 1000)

def generate_cookies(cookies: List[Dict[str, Any]], cookie_form: List[str]) -> str: 
    """
    Tạo ra một chuỗi cookie hoàn chỉnh từ một danh sách các đối tượng cookie.
    Chuỗi này sẽ được dùng trong HTTP Header để xác thực phiên đăng nhập.
        
    Args: 
        cookies: Danh sách các dictionary, mỗi dictionary chứa thông tin một cookie (thường lấy từ trình duyệt).
        cookie_form: Danh sách tên của những cookie quan trọng cần lấy (ví dụ: ['c_user', 'xs']).
            
    Returns: 
        Một chuỗi cookie đã được định dạng, ví dụ: "c_user=123; xs=abc"
    """
    # Nếu không có cookie đầu vào thì trả về chuỗi rỗng
    if not cookies:
        return ""
    
    cookie_dict = {}
    # Duyệt qua từng cookie trong danh sách
    for cookie in cookies:
        name = cookie.get('name', '')
        # Nếu tên của cookie này nằm trong danh sách các cookie cần lấy
        if name in cookie_form:
            # Thì thêm cặp name:value vào dictionary
            cookie_dict[name] = cookie.get('value', '')
    
    # Nối các cặp key=value trong dictionary lại thành một chuỗi duy nhất, ngăn cách bởi "; "
    cookie_string = '; '.join([f"{k}={v}" for k, v in cookie_dict.items()])
    return cookie_string


def is_rate_limited(response_text: str) -> bool:
    """
    Kiểm tra xem nội dung trả về từ server có phải là thông báo bị giới hạn yêu cầu (rate limited) hay không.
    
    Args:
        response_text: Nội dung văn bản của phản hồi HTTP.
        
    Returns:
        True nếu bị giới hạn, False nếu không.
    """
    return "Rate limit exceeded" in response_text

def parse_timestamp(timestamp: Any) -> datetime:
    """
    Chuyển đổi một giá trị thời gian ở nhiều định dạng khác nhau 
    thành một đối tượng `datetime` chuẩn của Python.
    
    Args:
        timestamp: Giá trị thời gian, có thể là số (Unix timestamp) hoặc chuỗi (ISO format).
        
    Returns:
        Một đối tượng datetime. Trả về thời gian hiện tại nếu không thể phân tích.
    """
    # Nếu là kiểu số (int hoặc float), coi nó là Unix timestamp (số giây từ 1/1/1970)
    if isinstance(timestamp, (int, float)):
        # datetime.fromtimestamp() chuyển Unix timestamp thành datetime theo múi giờ địa phương
        return datetime.fromtimestamp(timestamp)
    # Nếu là kiểu chuỗi
    elif isinstance(timestamp, str):
        try: 
            # Thử phân tích chuỗi theo định dạng ISO 8601.
            # 'Z' (Zulu time) được thay bằng '+00:00' để tương thích.
            return datetime.fromisoformat(timestamp.replace('Z', '+00:00'))
        except:
            # Nếu lỗi, trả về thời gian hiện tại
            return datetime.now()
    # Nếu nó đã là một đối tượng datetime rồi
    elif isinstance(timestamp, datetime):
        return timestamp 
    # Nếu không phải các kiểu trên, trả về thời gian hiện tại
    return datetime.now()

def get_time_hours_ago(hours: int) -> datetime:
    """
    Lấy về đối tượng `datetime` của một thời điểm trong quá khứ, cách hiện tại `hours` tiếng.
    
    Args:
        hours: Số giờ tính về quá khứ.
    
    Returns:
        Đối tượng datetime tương ứng.
    """
    return datetime.now() - timedelta(hours=hours)

def normalize_url(url: str) -> str:
    """
    "Chuẩn hóa" một URL Facebook để đảm bảo nó có định dạng đầy đủ.
    Ví dụ: '/groups/123' -> 'https://www.facebook.com/groups/123'
    
    Args:
        url: Chuỗi URL đầu vào.
        
    Returns:
        Chuỗi URL đã được chuẩn hóa.
    """
    if not url:
        return ""
    
    # Nếu url chưa có phần "http", thêm nó vào
    if not url.startswith('http'):
        # lstrip('/') để xóa dấu / thừa ở đầu (nếu có)
        url = 'https://www.facebook.com/' + url.lstrip('/')
    return url 

def extract_post_id(url: str) -> str:
    """
    Trích xuất ID của bài viết/video/ảnh từ các định dạng URL khác nhau của Facebook.
    ID là một chuỗi số duy nhất để định danh nội dung.
    
    Các dạng URL có thể xử lý:
        - https://www.facebook.com/groups/123/posts/789/
        - https://www.facebook.com/photo/?fbid=456&set=a.123
        - https://www.facebook.com/story.php?story_fbid=123&id=456
        - https://www.facebook.com/permalink.php?story_fbid=123&id=456
        - https://www.facebook.com/photo.php?fbid=123&id=456
        - https://www.facebook.com/user.name/videos/123/
        - https://www.facebook.com/watch?v=123
        - ... và nhiều dạng khác có chứa chuỗi số ID.
        
    Args:
        url: Chuỗi URL của Facebook.
        
    Returns:
        Chuỗi ID của bài viết, hoặc chuỗi rỗng nếu không tìm thấy.
    """
    if not url:
        return ""

    # Tách URL thành các phần dựa trên dấu '/'
    parts = url.split('/')
    
    # Cách 1: Tìm ID dựa vào các từ khóa phổ biến
    for i, part in enumerate(parts):
        # Nếu một phần của URL là một trong các từ khóa này, ID thường nằm ngay sau nó
        if part in ['posts', 'permalink.php', 'videos', 'photos', 'reels']:
            if i + 1 < len(parts):
                # Lấy phần tiếp theo và loại bỏ các tham số query (phần sau dấu '?')
                return parts[i+1].split('?')[0]

    # Cách 2: Nếu cách trên không thành công, tìm ID là chuỗi số cuối cùng trong URL
    # Duyệt ngược từ cuối URL
    for part in reversed(parts):
        # Tách các tham số query (ví dụ: fbid=123&id=456)
        sub_parts = part.split('?')[-1].split('&')
        for sp in sub_parts:
            # Tìm các tham số như fbid, story_fbid, v
            if sp.startswith('fbid=') or sp.startswith('story_fbid=') or sp.startswith('v='):
                return sp.split('=')[1]

        # Nếu một phần của URL chỉ chứa toàn số, đó có thể là ID
        if part.isdigit():
            return part 
            
    return ""

def safe_get(data: Dict, *keys, default = None):
    """
    Lấy giá trị từ một cấu trúc dữ liệu lồng nhau (nested dictionary/list) một cách an toàn.
    Hàm này sẽ không gây lỗi nếu một key hoặc index trên đường đi không tồn tại.
    
    Ví dụ:
        thay vì viết: `name = data['user']['profile']['name']` (có thể gây lỗi)
        ta viết: `name = safe_get(data, 'user', 'profile', 'name', default='Không có')`
        
    Args:
        data: Dictionary hoặc list gốc.
        *keys: Các key (cho dict) hoặc index (cho list) liên tiếp để truy cập vào giá trị.
        default: Giá trị trả về nếu không tìm thấy. Mặc định là None.
        
    Returns:
        Giá trị tìm thấy, hoặc giá trị `default` nếu không tìm thấy.
    """
    try:
        result = data 
        for key in keys:
            # Nếu đang duyệt dict, dùng .get() để an toàn
            if isinstance(result, dict):
                result = result.get(key)
            # Nếu đang duyệt list và key là một số nguyên (index)
            elif isinstance(result, list) and isinstance(key, int):
                # Kiểm tra index có hợp lệ không
                if len(result) > key:
                    result = result[key]
                else:
                    result = None # Index ngoài phạm vi
            else:
                return default # Không thể duyệt tiếp
            
            # Nếu ở bước nào đó kết quả là None, dừng lại và trả về default
            if result is None:
                return default
        return result 
    except (KeyError, IndexError, TypeError):
        # Bắt các lỗi có thể xảy ra và trả về default
        return default
            
# --- KHỐI MÃ ĐỂ KIỂM TRA NHANH ---
# Khối này chỉ chạy khi bạn thực thi file python này trực tiếp (python ten_file.py)
# Nó sẽ không chạy nếu file này được import vào một file khác.
if __name__ == "__main__":
    # In ra kết quả của việc chuyển đổi Unix timestamp 3753 thành đối tượng datetime
    # Kết quả sẽ là '1970-01-01 08:02:33' nếu bạn ở múi giờ GMT+7
    print(parse_timestamp(3753))
    
    # Ví dụ kiểm tra hàm extract_post_id
    test_url = "https://www.facebook.com/groups/123456/posts/789101112/"
    print(f"URL: {test_url}")
    print(f"Post ID: {extract_post_id(test_url)}")
