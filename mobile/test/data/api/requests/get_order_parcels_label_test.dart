import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/get_order_parcels_label.dart';
import 'package:partners_app/domain/repositories/orders/orders_params.dart';
import 'package:test/test.dart';

void main() {
  test('GetOrderParcelsLabelReq', () async {
    final req = GetOrderParcelsLabelReq(
      executor: MockExecutor(),
      params:
          const GetOrderParcelsLabelParams(campaignId: 11111, orderId: 22222),
    );

    final result = await req.execBinary();

    expect(result.isNotEmpty, true);
  });
}
