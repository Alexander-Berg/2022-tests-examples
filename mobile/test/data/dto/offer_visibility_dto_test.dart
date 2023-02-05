import 'dart:convert';

import 'package:partners_app/data/api/mock/set_offer_visibility.dart';
import 'package:partners_app/data/api/requests/api_request.dart';
import 'package:partners_app/data/dto/offer_visibility_dto.dart';
import 'package:partners_app/domain/entites/offer.dart';
import 'package:test/test.dart';

Map<String, dynamic> map = <String, dynamic>{};

void main() {
  test('OfferVisibilityDto.fromJson', () {
    final map = jsonDecode(SET_OFFER_VISIBILITY) as Map<String, dynamic>;
    final result = map.resultValue as Map<String, dynamic>;
    final dto = OfferVisibilityDto.fromJson(result);
    expect(dto.health, OfferHealth.good);
  });
}
