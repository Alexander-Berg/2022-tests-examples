import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/update_order_items.dart';
import 'package:partners_app/domain/entites/order.dart';
import 'package:partners_app/domain/repositories/orders/update_order_items_params.dart';
import 'package:test/test.dart';

void main() {
  test('UpdateOrderItemsReq', () async {
    final req = UpdateOrderItemsReq(
      executor: MockExecutor(),
      params: const UpdateOrderItemsParams(
        campaignId: 123,
        orderId: 1,
        timeZone: 'Asia/Yekaterinburg',
        reason: OrderItemsChangeReason.userRequestedRemove,
        items: [
          UpdateOrderItemsParamsItem(id: 1, count: 4),
        ],
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
