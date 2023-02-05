import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/set_notification_settings.dart';
import 'package:partners_app/domain/entites/notification.dart';
import 'package:partners_app/domain/repositories/notifications/set_notification_settings_params.dart';
import 'package:test/test.dart';

void main() {
  test('SetNotificationSettingsReq', () async {
    final req = SetNotificationSettingsReq(
      executor: MockExecutor(),
      params: SetNotificationSettingsParams(
          settings: SetNotificationSettings(
        events: [NotificationSettingsEvent.mpmOrderCreated],
        campaigns: [1],
      )),
    );

    final response = await req.exec();
    expect(response.campaigns?.length, 3);
    expect(response.events?.length, 3);
    expect(response.events?.first, NotificationSettingsEvent.mpmOrderCreated);
  });
}
