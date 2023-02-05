import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/get_notification_settings.dart';
import 'package:partners_app/domain/entites/notification.dart';
import 'package:test/test.dart';

void main() {
  test('GetNotificationSettingsReq', () async {
    final req = GetNotificationSettingsReq(
      executor: MockExecutor(),
    );

    final response = await req.exec();
    expect(response.campaigns?.length, 3);
    expect(response.events?.length, 3);
    expect(response.events?.first, NotificationSettingsEvent.mpmOrderCreated);
  });
}
