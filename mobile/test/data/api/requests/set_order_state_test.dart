import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/set_order_state.dart';
import 'package:partners_app/domain/entites/order.dart';
import 'package:partners_app/domain/repositories/orders/set_order_state_params.dart';
import 'package:test/test.dart';

void main() {
  test('SetOrderStateReq', () async {
    final req = SetOrderStateReq(
      executor: MockExecutor(),
      params: const SetOrderStateParams(
        campaignId: 123,
        orderId: 1,
        timeZone: 'Asia/Yekaterinburg',
        actualDeliveryDate: 0,
        reason: OrderStateChangeReason.cancelledCourierNotFound,
        state: OrderState.cancelled,
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
