import 'dart:convert';

import 'package:partners_app/data/api/mock/create_business.dart';
import 'package:partners_app/data/api/requests/api_request.dart';
import 'package:partners_app/data/dto/user_business_creation_result_dto.dart';
import 'package:partners_app/data/mappers/mappers.dart';
import 'package:test/test.dart';

Map<String, dynamic> result = <String, dynamic>{};

void main() {
  setUp(() {
      final map = jsonDecode(CREATE_BUSINESS) as Map<String, dynamic>;
      result = map.resultValue as Map<String, dynamic>;
  });
  test('createBusiness.fromJson', () {
    final dto = UserBusinessCreationResultDTO.fromJson(result);
    expect(dto.businessId, 123);
    expect(dto.name, 'Yandex');
    expect(dto.slug, 'yandex');
  });

  test('createBusiness.entity', () {
    final dto = UserBusinessCreationResultDTO.fromJson(result);
    final entity = dto.entity;
    expect(entity.id, 123);
    expect(entity.name, 'Yandex');
    expect(entity.campaigns.length, 0);
  });
}
