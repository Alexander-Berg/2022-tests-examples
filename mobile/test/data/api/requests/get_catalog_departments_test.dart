import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/get_catalog_departments.dart';

import 'package:test/test.dart';

void main() {
  test('GetCatalogDepartmentsReq', () async {
    final req = GetCatalogDepartmentsReq(
      executor: MockExecutor(),
    );

    final result = await req.exec();
    
    expect(result.result[0], 54440);
    expect(result.departments[0].name, 'Товары для авто- и мототехники');
  });
}
