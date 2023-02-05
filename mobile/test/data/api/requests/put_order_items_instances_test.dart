import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/put_order_items_instances.dart';
import 'package:partners_app/domain/entites/order.dart';
import 'package:partners_app/domain/repositories/orders/put_order_items_instances_params.dart';
import 'package:test/test.dart';

void main() {
  test('PutOrderItemsInstancesReq', () async {
    final req = PutOrderItemsInstancesReq(
      executor: MockExecutor(),
      params: const PutOrderItemsInstancesParams(
          campaignId: 123,
          orderId: 1,
          timeZone: 'Asia/Yekaterinburg',
          items: [
            <String, String>{
              'key': 'value',
            },
          ]),
    );

    final response = await req.exec();
    expect(response.result, 1);
    final order = response.orders.values.first;
    expect(order.state, OrderState.newState);
    final orderItem = response.orderItems.values.first;
    expect(orderItem.offerId, 'strundel.731');
  });
}
