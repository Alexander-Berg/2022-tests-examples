import 'dart:convert';

import 'package:partners_app/data/api/mock/find_location.dart';
import 'package:partners_app/data/api/mock/get_assortment.dart';
import 'package:partners_app/data/api/mock/get_users_businesses.dart';
import 'package:partners_app/data/api/requests/api_request.dart';

import 'package:test/test.dart';

void main() {
  setUp(() {});
  test('collectionByName', () {
    final map = jsonDecode(GET_ASSORTMENT) as Map<String, dynamic>;
    final offers = map.collectionByName('offers', 'offerId');
    expect(offers.keys.isNotEmpty, true);
  });

  test('collectionByName when id is int', () {
    final map = jsonDecode(FIND_LOCATION) as Map<String, dynamic>;
    final toponyms = map.collectionByName('toponyms');

    expect(toponyms.keys.isNotEmpty, true);
  });

  test('collectionListByName, list to list', () {
    final map = jsonDecode(FIND_LOCATION) as Map<String, dynamic>;
    final toponyms =
        map.collectionListByName('toponyms') as List<Map<String, dynamic>>;

    expect(toponyms.isNotEmpty, true);
    expect(toponyms.first['id'], 11022);
  });

  test('collectionListByName, map to list', () {
    final map = jsonDecode(GET_USERS_BUSINESSES) as Map<String, dynamic>;
    final businesses =
        map.collectionListByName('businesses') as List<Map<String, dynamic>>;

    expect(businesses.isNotEmpty, true);
    expect(businesses.first['id'], 10803544);
  });
}
