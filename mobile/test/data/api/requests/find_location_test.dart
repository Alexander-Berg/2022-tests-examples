import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/find_location.dart';

import 'package:test/test.dart';

void main() {
  test('FindLocationReq', () async {
    final req = FindLocationReq(
      executor: MockExecutor(),
      query: 'Мо'
    );

    final result = await req.exec();

    expect(result[0].id, 11022);
    expect(result[0].title, 'Моздок');
  });
}
