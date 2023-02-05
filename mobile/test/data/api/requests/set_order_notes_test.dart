import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/set_order_notes.dart';
import 'package:partners_app/domain/entites/order.dart';
import 'package:partners_app/domain/repositories/orders/set_order_notes_params.dart';
import 'package:test/test.dart';

void main() {
  test('SetOrderNotesReq', () async {
    final req = SetOrderNotesReq(
      executor: MockExecutor(),
      params: const SetOrderNotesParams(
        campaignId: 123,
        orderId: 1,
        timeZone: 'Asia/Yekaterinburg',
        text: 'Заметка',
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
