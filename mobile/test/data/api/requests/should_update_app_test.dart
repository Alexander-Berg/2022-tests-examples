import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/should_update_app.dart';
import 'package:test/test.dart';

void main() {
  test('ShouldUpdateApp', () async {
    final req = ShouldUpdateAppReq(
      executor: MockExecutor(),
    );

    final response = await req.exec();

    expect(response.status, false);
  });
}
