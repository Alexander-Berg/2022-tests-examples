import 'dart:convert';

import 'package:partners_app/data/api/mock/get_assortment_stock.dart';
import 'package:partners_app/data/api/requests/api_request.dart';
import 'package:partners_app/data/dto/assortment_stock.dart';
import 'package:test/test.dart';

Map<String, dynamic> map = <String, dynamic>{};

void main() {
  setUp(() {
    map = jsonDecode(GET_ASSORTMENT_STOCK) as Map<String, dynamic>;
  });
  test('AssortmentStockDto.fromJson', () {
    final result = (map.resultValue as List).cast<Map<String, dynamic>>();
    final dto = AssortmentStockDto.fromJson(result.first);
    expect(dto.name, 'MyHo');
  });
}
