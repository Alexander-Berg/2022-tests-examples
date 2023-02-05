import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/get_order_by_id.dart';
import 'package:partners_app/domain/entites/order.dart';
import 'package:partners_app/domain/repositories/orders/get_order_by_id_params.dart';
import 'package:test/test.dart';

void main() {
  test('GetOrderByIdReq', () async {
    final req = GetOrderByIdReq(
      executor: MockExecutor(),
      params: const GetOrderByIdParams(
        campaignId: 123,
        orderId: 1,
        timeZone: 'Asia/Yekaterinburg',
      ),
    );

    final response = await req.exec();
    expect(response.result, 1);
    final order = response.orders.values.first;
    expect(order.state, OrderState.newState);
    final orderItem = response.orderItems.values.first;
    expect(orderItem.offerId, 'strundel.731');
  });
}
