import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/get_order_conversation.dart';
import 'package:partners_app/domain/repositories/orders/get_order_conversation_params.dart';
import 'package:test/test.dart';

void main() {
  test('GetOrderConversationReq', () async {
    final req = GetOrderConversationReq(
      executor: MockExecutor(),
      params: const GetOrderConversationParams(
        campaignId: 123,
        orderId: 1,
        buyerUid: 123,
      ),
    );

    final response = await req.exec();
    expect(response.inviteHash, 'hash-value');
    expect(response.serviceId, 123456);
  });
}
