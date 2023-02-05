import 'dart:convert';

import 'package:partners_app/data/api/mock/get_assortment_partner_categories.dart';
import 'package:partners_app/data/api/requests/api_request.dart';
import 'package:partners_app/data/dto/responses/partner_categories_dto.dart';
import 'package:test/test.dart';

Map<String, dynamic> flatMap = <String, dynamic>{};

void main() {
  setUp(() {
    final map =
        jsonDecode(GET_ASSORTMENT_PARTNER_CATEGORIES) as Map<String, dynamic>;
    flatMap = map.flat;
  });
  test('PartnerCategoriesDto.fromJson', () {
    final dto = PartnerCategoriesDto.fromJson(flatMap);
    expect(dto.result.isNotEmpty, true);
  });
}
