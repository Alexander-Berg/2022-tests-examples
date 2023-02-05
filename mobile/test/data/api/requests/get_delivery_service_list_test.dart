import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/get_delivery_service_list.dart';
import 'package:test/test.dart';

void main() {
  test('GetDeliveryServiceListReq', () async {
    final req = GetDeliveryServiceListReq(
      executor: MockExecutor(),
      campaignId: 111111
    );

    final result = await req.exec();
    final firstEntity = result.first;

    expect(firstEntity.id, 122);
    expect(firstEntity.name, '4BIZ');
  });
}
