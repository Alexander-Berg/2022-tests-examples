import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/create_business.dart';
import 'package:partners_app/domain/repositories/user/create_business_params.dart';

import 'package:test/test.dart';

void main() {
  test('CreateBusinessReq', () async {
    final req = CreateBusinessReq(
      executor: MockExecutor(),
      params: const CreateBusinessParams(
        contact: CreateBusinessParamsContact(
          email: 'email@yandex.ru',
          firstName: 'Василий',
          lastName: 'Пупкин',
          phone: '+71234567890',
        ),
        business: CreateBusinessParamsBusiness(
          assortment: '1000',
          domain: 'yandex',
          name: 'yandex',
          regionId: 123,
          category: CreateBusinessParamsCategory(
            id: 1,
            name: 'Велосипеды',
          ),
        ),
      ),
    );

    final result = await req.exec();

    expect(result.businessId, 123);
    expect(result.name, 'Yandex');
    expect(result.slug, 'yandex');
  });
}
