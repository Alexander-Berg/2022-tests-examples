import 'dart:convert';

import 'package:partners_app/data/api/mock/get_catalog_departments.dart';
import 'package:partners_app/data/api/requests/api_request.dart';
import 'package:partners_app/data/dto/catalog_departments_dto.dart';
import 'package:partners_app/data/mappers/mappers.dart';
import 'package:test/test.dart';

Map<String, dynamic> flatMap = <String, dynamic>{};

void main() {
  setUp(() {
    final map = jsonDecode(GET_CATALOG_DEPARTMENTS) as Map<String, dynamic>;
    flatMap = map.flat;
  });
  test('getCatalogDepartments.fromJson', () {
    final dto = CatalogDepartmentsDTO.fromJson(flatMap);

    expect(dto.result.isNotEmpty, true);
    expect(dto.result[0], 54440);
    expect(dto.departments.isNotEmpty, true);
    expect(dto.departments[0].name, 'Товары для авто- и мототехники');
  });

  test('getCatalogDepartments.entity', () {
    final dto = CatalogDepartmentsDTO.fromJson(flatMap);

    final entity = dto.entity;

    expect(entity.ids.isNotEmpty, true);
    expect(entity.ids[0], 54440);
    expect(entity.departments.isNotEmpty, true);
    expect(entity.departments[0].name, 'Товары для авто- и мототехники');
  });
}
