"""
Crawl Facebook Page/Group WITHOUT login using GraphQL API
(Cào dữ liệu Trang/Nhóm Facebook KHÔNG CẦN ĐĂNG NHẬP bằng GraphQL API)
Phương pháp này không yêu cầu xác thực - chỉ cần sử dụng proxy.
"""
import asyncio
import json
import base64
from datetime import datetime
from typing import Dict, Any, List, Optional
from urllib.parse import urlencode
import aiohttp

# Import các module tự định nghĩa
from utils.logger import logger
from utils.helpers import sleep
from request.parser import normalize_response

class FacebookCrawlerNoAuth:
    """
    Lớp (class) để cào dữ liệu Facebook mà không cần xác thực (đăng nhập).
    Hoạt động bằng cách gửi yêu cầu trực tiếp đến GraphQL endpoint của Facebook,
    mô phỏng một client không đăng nhập.
    """
    
    def __init__(self, proxy: str = None):
        """
        Hàm khởi tạo cho crawler.
        
        Args:
            proxy: URL của proxy (ví dụ: "http://user:pass@host:port"). 
                   Việc sử dụng proxy là rất quan trọng để tránh bị block IP.
        """
        self.proxy = proxy
        self.graphql_url = "https://www.facebook.com/api/graphql/"
        
    async def crawl_page(
        self,
        source: Dict[str, Any],
        max_pages: int = 20
    ) -> Dict[str, Any]:
        """
        Cào các bài đăng từ một Trang Facebook (Fanpage) hoặc tài khoản cá nhân.
        
        Args:
            source: Dictionary chứa thông tin về nguồn cần cào (id_source, link, name...).
            max_pages: Số lượng trang tối đa cần cào (mỗi trang chứa một vài bài viết).
            
        Returns:
            Dictionary chứa danh sách các bài viết ('data') và thông tin metadata.
        """
        logger.info(f"Bắt đầu cào trang: {source.get('link')}")
        
        result = {
            'data': [],      # Nơi lưu trữ các bài viết đã được xử lý
            'error': None,   # Ghi lại lỗi nếu có
            'source_info': {} # Thông tin về trang (tên, avatar)
        }
        
        cursor = "" # "Con trỏ" để phân trang, ban đầu là rỗng
        count = 0   # Biến đếm số trang đã cào
        
        # Vòng lặp để cào qua nhiều trang
        while count < max_pages:
            count += 1
            logger.info(f"Đang cào trang {count}/{max_pages}")
            
            try:
                # 1. Gửi yêu cầu lấy dữ liệu bài viết của trang
                response_data = await self._request_page_posts(
                    source.get('id_source'),
                    cursor
                )
                
                # Nếu không có dữ liệu trả về, dừng lại
                if not response_data:
                    logger.warning("Không nhận được dữ liệu phản hồi.")
                    break
                
                # 2. Kiểm tra có bị giới hạn yêu cầu (rate limit) không
                response_str = json.dumps(response_data) if isinstance(response_data, dict) else str(response_data)
                if self._is_rate_limited(response_str):
                    logger.warning("Bị giới hạn yêu cầu, tạm nghỉ 10 phút.")
                    await sleep(10 * 60 * 1000) # Nghỉ 10 phút
                    continue
                
                # 3. Phân tích phản hồi (có thể là JSONL - nhiều JSON trên nhiều dòng)
                # Lấy tất cả các object JSON từ phản hồi
                all_objects = response_data.get('_all_objects', [response_data])
                
                # 4. Xử lý và chuẩn hóa từng object JSON
                data_parsed = []
                for obj in all_objects:
                    # normalize_response là hàm để làm sạch và cấu trúc lại JSON từ Facebook
                    normalized = normalize_response(json.dumps(obj))
                    if isinstance(normalized, list):
                        data_parsed.extend(normalized)
                    else:
                        data_parsed.append(normalized)
                
                # 5. Trích xuất danh sách các bài viết "thô" từ dữ liệu đã chuẩn hóa
                list_raw_posts = self._extract_raw_posts(data_parsed)
                
                if not list_raw_posts:
                    logger.info("Không tìm thấy bài viết nào nữa.")
                    break
                
                # 6. Ở trang đầu tiên, trích xuất thông tin của trang (tên, avatar)
                if count == 1 and list_raw_posts:
                    result['source_info'] = self._extract_source_info(list_raw_posts[0], source)
                
                # 7. Phân tích chi tiết từng bài viết thô để lấy thông tin cần thiết
                list_posts = []
                for raw_post in list_raw_posts:
                    post = await self._parse_post(raw_post, source)
                    if post:
                        list_posts.append(post)
                
                logger.info(f"Đã xử lý được {len(list_posts)} bài viết.")
                
                if not list_posts:
                    break
                
                result['data'].extend(list_posts)
                
                # 8. Kiểm tra xem đã cào đến các bài viết cũ hay chưa (nếu có thông tin lần cào cuối)
                last_crawled_at = source.get('last_crawled_at')
                if last_crawled_at:
                    if isinstance(last_crawled_at, str):
                        last_crawled_at = datetime.fromisoformat(last_crawled_at.replace('Z', '+00:00'))
                    
                    if list_posts and list_posts[-1].get('posted_at'):
                        last_post_time = list_posts[-1]['posted_at']
                        if isinstance(last_post_time, str):
                            last_post_time = datetime.fromisoformat(last_post_time.replace('Z', '+00:00'))
                        
                        if last_post_time < last_crawled_at:
                            logger.info("Đã cào tới các bài viết cũ, dừng lại.")
                            break
                
                # 9. Lấy "con trỏ" (cursor) để cào trang tiếp theo
                cursor = self._extract_cursor(data_parsed)
                if not cursor:
                    logger.info("Không còn trang tiếp theo.")
                    break
                
                # 10. Tạm dừng một chút giữa các yêu cầu để tránh bị block
                await sleep(2000)
                
            except Exception as e:
                logger.error(f"Lỗi trong vòng lặp cào dữ liệu: {e}")
                result['error'] = str(e)
                break
        
        logger.info(f"Hoàn tất cào dữ liệu. Tổng cộng: {len(result['data'])} bài viết.")
        return result
    
    # ... (Hàm crawl_group và get_comments có cấu trúc tương tự)
    # Dưới đây là giải thích cho các hàm "private" (bắt đầu bằng dấu _)
    
    async def _request_page_posts(
        self,
        page_id: str,
        cursor: str
    ) -> Optional[Dict]:
        """Gửi yêu cầu HTTP POST tới GraphQL để lấy bài viết của Trang."""
        
        # 'variables' chứa các tham số cho truy vấn GraphQL
        variables = {
            "count": 3,      # Số lượng bài viết muốn lấy mỗi lần
            "cursor": cursor,# Con trỏ đến trang tiếp theo
            "id": page_id,   # ID của trang/người dùng
            "feedLocation": "TIMELINE",
            # ... các tham số khác để mô phỏng yêu cầu từ trình duyệt
        }
        
        # 'doc_id' là một mã hash đại diện cho một truy vấn GraphQL đã được định nghĩa sẵn trên server của Facebook.
        # Thay vì gửi toàn bộ câu truy vấn, client chỉ cần gửi doc_id và variables.
        # Con số này có thể thay đổi và cần được cập nhật nếu API thay đổi.
        body = f"variables={json.dumps(variables)}&doc_id=7724071297644851"
        
        headers = {
            "accept": "*/*",
            "content-type": "application/x-www-form-urlencoded",
            "x-fb-friendly-name": "ProfileCometTimelineFeedRefetchQuery", # Tên gợi nhớ của truy vấn
             # ... các header khác để yêu cầu trông giống từ trình duyệt
        }
        
        try:
            # Sử dụng aiohttp để thực hiện yêu cầu bất đồng bộ
            async with aiohttp.ClientSession() as session:
                async with session.post(
                    self.graphql_url,
                    data=body,
                    headers=headers,
                    proxy=self.proxy,
                    timeout=aiohttp.ClientTimeout(total=120)
                ) as response:
                    text = await response.text()
                    # Phản hồi từ Facebook thường là JSONL (nhiều JSON trên các dòng khác nhau)
                    # nên cần một hàm parse đặc biệt.
                    return self._parse_facebook_response(text)
        except Exception as e:
            logger.error(f"Lỗi khi gửi yêu cầu: {e}")
            return None
    
    def _parse_facebook_response(self, text: str) -> Optional[Dict]:
        """
        Phân tích phản hồi từ GraphQL của Facebook.
        Phản hồi thường có định dạng JSONL (mỗi dòng là một JSON hợp lệ) 
        và đôi khi có tiền tố "for(;;);".
        """
        if not text or not text.strip():
            return None
        
        lines = text.strip().split('\n')
        
        # Trường hợp chỉ có 1 dòng JSON
        if len(lines) == 1:
            try:
                # Bỏ tiền tố "for(;;);" nếu có (đây là một cơ chế bảo mật của FB)
                clean_text = lines[0][8:] if lines[0].startswith('for(;;);') else lines[0]
                return json.loads(clean_text)
            except json.JSONDecodeError as e:
                logger.error(f"Lỗi giải mã JSON: {e}")
                return None

        # Trường hợp có nhiều dòng (JSONL)
        parsed_objects = []
        for line in lines:
            line = line.strip()
            if not line:
                continue
            
            if line.startswith('for(;;);'):
                line = line[8:]
            
            try:
                obj = json.loads(line)
                parsed_objects.append(obj)
            except json.JSONDecodeError:
                logger.error(f"Lỗi giải mã JSON trên dòng: {line[:100]}...")
                continue
        
        # Trả về một cấu trúc thống nhất chứa tất cả các object đã parse
        # để hàm gọi có thể xử lý toàn bộ dữ liệu.
        return {
            "data": parsed_objects[0].get("data") if parsed_objects else None,
            "_all_objects": parsed_objects
        }

    # ... (Các hàm _request_group_posts, _request_comments có cấu trúc tương tự)

    def _is_rate_limited(self, response: Any) -> bool:
        """Kiểm tra xem phản hồi có chứa thông báo bị giới hạn yêu cầu hay không."""
        if isinstance(response, str):
            return "Rate limit exceeded" in response or "rate_limit_exceeded" in response
        return False
    
    def _extract_raw_posts(self, data_parsed: List[Dict]) -> List[Dict]:
        """
        Trích xuất danh sách các bài viết "thô" (dữ liệu JSON gốc) từ dữ liệu đã được parse.
        Hàm này điều hướng qua cấu trúc JSON phức tạp của Facebook để tìm đến mảng chứa các bài viết.
        """
        list_raw_posts = []
        for item in data_parsed:
            data = item.get('data', {})
            # Cấu trúc JSON có thể khác nhau, cần kiểm tra nhiều trường hợp
            if data.get('__typename') == 'User':
                edges = data.get('timeline_list_feed_units', {}).get('edges', [])
                list_raw_posts.extend(edges)
                continue
            
            node = data.get('node', {})
            if node.get('__typename') == 'User':
                edges = node.get('timeline_list_feed_units', {}).get('edges', [])
                list_raw_posts.extend(edges)
        
        return list_raw_posts
    
    def _extract_group_raw_posts(self, data_parsed: List[Dict]) -> List[Dict]:
        """Trích xuất bài viết thô từ dữ liệu của Group (có cấu trúc khác Page)."""
        list_raw_posts = []
        for item in data_parsed:
            node = item.get('data', {}).get('node', {})
            if node.get('__typename') == 'Group':
                edges = node.get('group_feed', {}).get('edges', [])
                list_raw_posts.extend(edges)
        return list_raw_posts

    def _extract_source_info(self, raw_post: Dict, source: Dict) -> Dict[str, str]:
        """Trích xuất thông tin của nguồn (tên, avatar) từ dữ liệu bài viết đầu tiên."""
        try:
            # Điều hướng qua một đường dẫn rất sâu và cụ thể trong JSON
            actor_info = (
                raw_post.get('node', {})
                .get('comet_sections', {})
                .get('content', {})
                # ... nhiều tầng lồng nhau
                .get('actors', [{}])[0]
            )
            return {
                'name': actor_info.get('name'),
                'avatar': actor_info.get('profile_picture', {}).get('uri')
            }
        except Exception as e:
            # Vì cấu trúc JSON có thể thay đổi, cần có xử lý lỗi
            logger.error(f"Lỗi khi trích xuất thông tin nguồn: {e}")
            return {'name': source.get('name', ''), 'avatar': source.get('avatar', '')}
    
    async def _parse_post(self, raw_post: Dict, source: Dict) -> Optional[Dict[str, Any]]:
        """
        Phân tích dữ liệu bài viết thô để chuyển thành một dictionary có cấu trúc sạch sẽ.
        Hàm này gọi một hàm parser bên ngoài để tái sử dụng code.
        """
        from request.parser import parse_post_data
        return parse_post_data(raw_post)
    
    def _parse_comment(self, edge: Dict) -> Optional[Dict[str, Any]]:
        """Phân tích dữ liệu comment thô."""
        try:
            node = edge.get('node', {})
            # Lấy các trường thông tin cần thiết từ JSON của comment
            return {
                'id': node.get('feedback', {}).get('url', '').split('comment_id=')[-1],
                'text': node.get('body', {}).get('text', ''),
                'posted_at': datetime.fromtimestamp(node.get('created_time', 0)).isoformat(),
                'likes': int(node.get('reactors', {}).get('count_reduced', 0)),
                # ...
                'user': {
                    'id': node.get('author', {}).get('id'),
                    'name': node.get('author', {}).get('name'),
                    'avatar': node.get('author', {}).get('profile_picture_depth_0', {}).get('uri'),
                }
            }
        except Exception as e:
            logger.error(f"Lỗi khi phân tích comment: {e}")
            return None
    
    def _extract_cursor(self, data_parsed: List[Dict]) -> Optional[str]:
        """
        Trích xuất "con trỏ" (end_cursor) để dùng cho yêu cầu lấy trang tiếp theo.
        Con trỏ này thường nằm trong các object JSON cuối cùng của phản hồi JSONL.
        """
        # Duyệt ngược từ cuối danh sách các object đã parse
        for i in range(len(data_parsed) - 1, max(0, len(data_parsed) - 8), -1):
            cursor = data_parsed[i].get('data', {}).get('page_info', {}).get('end_cursor')
            if cursor:
                return cursor
        return None
