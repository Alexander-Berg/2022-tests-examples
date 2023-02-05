import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/unsubscribe_from_notifications.dart';
import 'package:test/test.dart';

void main() {
  test('UnsubscribeFromNotificationsReq', () async {
    final req = UnsubscribeFromNotificationsReq(
      executor: MockExecutor(),
      uuid: 'some uuid',
    );

    final response = await req.exec();

    expect(response, 'mob:uuid');
  });
}
