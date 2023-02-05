import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/set_order_track_code.dart';
import 'package:partners_app/domain/entites/order.dart';
import 'package:partners_app/domain/repositories/orders/set_order_track_code_params.dart';
import 'package:test/test.dart';

void main() {
  test('SetOrderTrackCodeReq', () async {
    final req = SetOrderTrackCodeReq(
      executor: MockExecutor(),
      params: const SetOrderTrackCodeParams(
        campaignId: 123,
        orderId: 33113138,
        timeZone: 'Europe/Moscow',
        actualDeliveryDate: 1650920400,
        reason: OrderStateChangeReason.pendingCancelled,
        state: OrderState.waitingCourier,
      ),
    );

    final response = await req.exec();
    expect(response.result, 33113138);
    final order = response.orders.values.first;
    expect(order.state, OrderState.waitingCourier);
    final orderItem = response.orderItems.values.first;
    expect(orderItem.offerId, 'strundel.865');
  });
}
