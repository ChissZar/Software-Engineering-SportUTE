# Đối soát thu và hoàn tiền thủ công

## Quy trình sử dụng

1. Khách tạo phiếu online: khoản tiền ở trạng thái **Chờ xác nhận**, chưa được tính là đã thu. QR/tài khoản minh họa trên màn hình không dùng để chuyển tiền thật; khách cần liên hệ lễ tân xác nhận thông tin nhận tiền.
2. Lễ tân kiểm tra giao dịch tiền vào thực tế trên ngân hàng, đối chiếu khách và khoản chờ rồi bấm **Xác nhận đã nhận tiền**. Phiếu đủ mức cọc được chuyển sang **Đã xác nhận**.
3. Khi hủy phiếu chưa sử dụng, cả hủy nhanh, hủy theo chính sách và hủy từ tài khoản khách đều tính khoản chờ hoàn theo chính sách của phiếu. Không tự chuyển tiền.
4. Lễ tân thực sự trả tiền cho khách ngoài ứng dụng, sau đó mới bấm **Xác nhận đã hoàn tiền**. Khoản chờ hoàn chưa làm giảm thu ròng; khoản đã xác nhận hoàn mới làm giảm thu ròng.

Ví dụ: đã xác nhận thu cọc 90.000đ, phí hủy 10% → tạo khoản chờ hoàn 81.000đ. Sau khi xác nhận hoàn, thu ròng còn 9.000đ.

Nếu đã có khoản hoàn hoặc khoản chờ hoàn từ trước, số đó được trừ khỏi khoản cần tạo thêm, tránh hoàn trùng. Không hủy phiếu đang sử dụng/đã hoàn tất; hủy lặp và xác nhận cùng giao dịch lần hai bị từ chối.

**Hủy khoản chờ** chỉ thay đổi trạng thái sổ theo dõi; không đảo ngược giao dịch ngân hàng. Không nhập một giao dịch hoàn mới nếu khoản chờ hoàn tương ứng đã tồn tại.

## Giới hạn cần hiểu

- Hệ thống không nhận thông báo từ ngân hàng, không tự xác minh ảnh chứng từ và không tự chuyển khoản hoàn tiền.
- QR và thông tin nhận tiền hiện là minh họa. MoMo/VNPAY chưa tích hợp và không còn cho chọn trên bước thanh toán.
- Nếu khách đã chuyển tiền nhưng hệ thống chưa xác nhận, cần liên hệ lễ tân đối soát **trước khi hủy**. Hệ thống không thể tự nhận biết tiền về muộn; trường hợp đó phải kiểm tra ngân hàng và xử lý trực tiếp, không coi trạng thái VOID là bằng chứng chưa nhận tiền.
- Lần thay đổi này giữ nguyên thứ tự tạo phiếu/QR và không thêm webhook hoặc cổng thanh toán.

## Kiểm tra và khôi phục

Các kiểm thử tự động bổ sung nằm trong `ManualReconciliationTest`: hủy nhanh và hủy theo chính sách cho cùng kết quả; không tạo khoản hoàn từ khai báo chưa xác nhận; trừ khoản đã/chờ hoàn; chặn hủy khi đang sử dụng và chặn hủy lặp.

Kiểm tra tích hợp dùng database riêng, chạy chu trình đặt → xác nhận cọc → hủy → xác nhận hoàn, đối chiếu lịch sử khách/nhân viên và thu ròng, rồi rollback toàn bộ giao dịch thử. Các file hỗ trợ và log nằm trong `target/`, có thể xóa sau khi kiểm tra.

Bản sao các file trước thay đổi được lưu tại:

```text
C:/Users/Minh Triet/.codex/visualizations/2026/10/19/01a1556a-7345-7f10-9ea2-e95d9158a5ed/sportute-before-manual-reconciliation.zip
```

Khi hoàn tác, chọn phần thay đổi cần khôi phục; không chép đè toàn bộ file nếu đã sửa tiếp sau mốc này. Không cần nâng cấp schema hoặc reset database để dùng thay đổi đối soát này.
