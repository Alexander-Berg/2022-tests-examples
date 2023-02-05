import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/subscribe_to_notifications.dart';
import 'package:partners_app/domain/repositories/notifications/subscribe_to_notifications_params.dart';
import 'package:test/test.dart';

void main() {
  test('SubscribeToNotificationsReq', () async {
    final req = SubscribeToNotificationsReq(
      executor: MockExecutor(),
      params: SubscribeToNotificationsParams(
          uuid: 'some uuid',
          appName: 'ru.yandex.mobile.market.partner.inhouse',
          token: 'some token',
          deviceId: 'some deviceId'),
    );

    final response = await req.exec();

    expect(response, 'mob:uuid');
  });
}
