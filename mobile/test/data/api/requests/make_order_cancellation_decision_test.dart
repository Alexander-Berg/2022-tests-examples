import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/make_order_cancellation_decision.dart';
import 'package:partners_app/domain/entites/order.dart';
import 'package:partners_app/domain/repositories/orders/make_order_cancellation_decision_params.dart';
import 'package:test/test.dart';

void main() {
  test('MakeOrderCancellationDecisionReq', () async {
    final req = MakeOrderCancellationDecisionReq(
      executor: MockExecutor(),
      params: const MakeOrderCancellationDecisionParams(
        campaignId: 123,
        orderId: 1,
        timeZone: 'Asia/Yekaterinburg',
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
