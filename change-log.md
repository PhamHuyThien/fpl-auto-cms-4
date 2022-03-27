# Change Log
### v4.1.3
Fix kết nối tới server (change to https://poly.thien.biz)  
Update mvn build (mvn clean install)  

### v4.1.2
Fix lỗi hoàn thành bài rồi mà k enable all button.  
Thêm thông báo khi bắt đầu auto giải (ko có nhìn như lag).  
Thêm phản hồi khi người dùng giải xong bài.  
Add env `--skip-final-test`, bỏ làm final test.  
Change `--serve-add` to `--server-address`.  

### v4.1.1
Add env `--disable-quiz-speed`.  

### v4.1.0
Fake request trách việc phát bị phát hiện bởi nhà trường.  
Fix write log use log4j.  
Add log error in logs/error.log.  
Fix connectionTimeout (60s).  
Add env `--server-address`.  
Add env `--enable-reset-quiz`.  
Auto 10 quiz 10 point 10 sec.  

### v4.0.0
Chuyển sang sử dụng maven project.  
Nâng cấp toàn bộ core hệ thống.  
Tăng tốc độ tìm quiz nhanh gấp 10 lần v3.
