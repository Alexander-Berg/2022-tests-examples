import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/get_order_cancel_reasons.dart';
import 'package:partners_app/domain/entites/order.dart';
import 'package:test/test.dart';

void main() {
  test('GetOrderCancelReasonsReq', () async {
    final req = GetOrderCancelReasonsReq(
      executor: MockExecutor(),
    );

    final response = await req.exec();
    final status = response.first.status;
    final substatusItem = response.first.substatuses.first;

    expect(status, OrderStatus.pickup);
    expect(substatusItem.substatus, OrderSubstatus.shopFailed);
    expect(substatusItem.text, 'Магазин не может выполнить заказ');
  });
}
