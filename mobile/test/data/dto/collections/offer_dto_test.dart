import 'dart:convert';

import 'package:partners_app/data/api/mock/get_offer_by_id.dart';
import 'package:partners_app/data/api/requests/api_request.dart';
import 'package:partners_app/data/dto/collections/offer_dto.dart';
import 'package:test/test.dart';

void main() {
  test('OfferDto.fromJson', () {
    final map = jsonDecode(GET_OFFER_BY_ID) as Map<String, dynamic>;
    final offers = map.collectionByName('offers', 'offerId');
    final offer = offers.isNotEmpty
        ? offers.values.first as Map<String, dynamic>
        : <String, dynamic>{};
    final dto = OfferDto.fromJson(offer);
    expect(dto.adult, false);
  });
}
