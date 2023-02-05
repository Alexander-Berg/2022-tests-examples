import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/get_order_transfer_options.dart';
import 'package:partners_app/domain/repositories/orders/get_order_transfer_options_params.dart';
import 'package:test/test.dart';

void main() {
  test('GetOrderTransferOptionsReq', () async {
    final req = GetOrderTransferOptionsReq(
      executor: MockExecutor(),
      params: const GetOrderTransferOptionsParams(
        campaignId: 123,
        orderId: 1,
      ),
    );

    final response = await req.exec();
    expect(response.countTotal, 5);
    expect(response.countRemain, 4);
    expect(response.daysMax, 10);
  });
}
