# FPL@utoCMS.4

[FPL@utoCMS](https://github.com/PhamHuyThien/fpl-auto-cms-4) là một công cụ miễn phí mã nguồn mở giúp các sĩ tử vượt qua
các bài tập trên hệ thống CMS của trường [FPT Polytechnic](https://caodang.fpt.edu.vn/) một cách dễ dàng nhất.  
Được phát triển dựa trên [FPL@utoCMS.3 core](https://github.com/PhamHuyThien/fpl-auto-cms), nâng cấp hiệu năng, hỗ trợ
đa nền tảng, maintain lại toàn bộ source code.

## Hướng dẫn sử dụng

Chi tiết cách cài đặt môi trường, khởi động ứng dụng, sử dụng phần mềm
... [xem tại đây](https://www.youtube.com/watch?v=kJQZ7rn1YXg).

## Tải về

Jar FPL@utoCMS có sẵn trong [releases github](https://github.com/PhamHuyThien/fpl-auto-cms-4/releases).

## Dành cho nhà phát triển

### Danh sách args

Cài đặt lại server test:

```cmd
--serve-add={LINK_TO_SERVER}
```

Auto lại quiz bất chấp đạt 10 điểm:

```cmd
--enable-reset-quiz
```

Tắt chế độ giải quiz nhanh 1s:
```cmd
--disable-quiz-speed
```


Tắt chế độ theo dõi người dùng:

```cmd
--disable-analysis
```

### Cách sử dụng args

```cmd
java -jar FPLautoCMS_v4.jar -erq
```

## Lưu ý:

Để cải thiện sản phẩm mặc định chúng tôi theo dõi người dùng.  
Để tắt tính năng này hãy sử dụng `--disable-analysis`.  
Nếu tắt tính năng theo dõi người dùng đồng nghĩa với việc sẽ mất kết nối hoàn toàn tới server (bao gồm các chức năng mở rộng hỗ trợ đính kèm sẽ không được hỗ trợ).

## Lich sử thay đổi

Lịch sử thay đổi qua từng phiên bản được đề cập
trong [change-log.md](https://github.com/PhamHuyThien/fpl-auto-cms-4/blob/master/change-log.md).

## Cộng đồng

Chúng tôi luôn tìm kiếm những người đóng góp, bạn có thể đóng góp, thắc mắc:

- [Trang thảo luận](https://www.facebook.com/210874576940463)
- [Tác giả](https://fb.com/thiendz.systemerror)