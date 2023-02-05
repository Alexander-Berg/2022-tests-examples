import 'dart:convert';

import 'package:partners_app/data/api/mock/get_assortment.dart';
import 'package:partners_app/data/api/requests/api_request.dart';
import 'package:partners_app/data/dto/responses/assortment_dto.dart';
import 'package:test/test.dart';

Map<String, dynamic> flatMap = <String, dynamic>{};

void main() {
  setUp(() {
    final map = jsonDecode(GET_ASSORTMENT) as Map<String, dynamic>;
    flatMap = <String, dynamic>{
      'result': map.resultValue as Map<String, dynamic>,
      'error': map.resultError,
      'offers': map.collectionByName('offers', 'offerId'),
      'offersPrice': map.collectionByName('offersPrice', 'ssku'),
    };
  });
  test('AssortmentDto.fromJson', () {
    final dto = AssortmentDTO.fromJson(flatMap);
    expect(dto.result.offers.isEmpty, false);
  });
}
