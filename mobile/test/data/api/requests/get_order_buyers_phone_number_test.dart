import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/get_order_buyers_phone_number.dart';
import 'package:partners_app/domain/repositories/orders/orders_params.dart';
import 'package:test/test.dart';

void main() {
  test('GetOrderBuyersPhoneNumberReq', () async {
    final req = GetOrderBuyersPhoneNumberReq(
        executor: MockExecutor(),
        params: const GetOrderBuyersPhoneNumberParams(
            campaignId: 11111, orderId: 22222));

    final result = await req.exec();

    expect(result, '+71234567890');
  });
}
