import 'dart:convert';

import 'package:partners_app/data/api/mock/get_users_businesses.dart';
import 'package:partners_app/data/api/requests/api_request.dart';
import 'package:partners_app/data/dto/responses/user_businesses_dto.dart';
import 'package:partners_app/data/mappers/mappers.dart';
import 'package:test/test.dart';

Map<String, dynamic> flatMap = <String, dynamic>{};

void main() {
  setUp(() {
    final map = jsonDecode(GET_USERS_BUSINESSES) as Map<String, dynamic>;
    flatMap = map.flat;
  });
  test('getUserBusinesses.fromJson', () {
    final dto = UserBusinessesDTO.fromJson(flatMap);
    expect(dto.result.isNotEmpty, true);
  });

  test('getUserBusinesses.entitie', () {
    final dto = UserBusinessesDTO.fromJson(flatMap);
    final entity = dto.entity;
    expect(entity.ids.isNotEmpty, true);
  });
}
