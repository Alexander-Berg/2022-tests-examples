import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/get_major_cities.dart';
import 'package:test/test.dart';

void main() {
  test('GetMajorCitiesReq', () async {
    final req = GetMajorCitiesReq(
      executor: MockExecutor(),
    );

    final result = await req.exec();
    final firstEntity = result[0];

    expect(firstEntity.id, 213);
    expect(firstEntity.title, 'Москва');
    expect(firstEntity.subtitle, 'Москва и Московская область, Россия');
    expect(firstEntity.type, 'toponym');
  });
}
