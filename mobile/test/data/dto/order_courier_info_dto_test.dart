import 'dart:convert';

import 'package:partners_app/data/api/mock/get_order_courier_info.dart';
import 'package:partners_app/data/api/requests/api_request.dart';
import 'package:partners_app/data/dto/order_courier_info_dto.dart';
import 'package:partners_app/data/mappers/mappers.dart';
import 'package:partners_app/domain/entites/courier_info.dart';
import 'package:test/test.dart';

Map<String, dynamic> result = <String, dynamic>{};

void main() {
  setUp(() {
    final map = jsonDecode(GET_ORDER_COURIER_INFO) as Map<String, dynamic>;
    result = map.resultValue as Map<String, dynamic>;
  });
  test('getOrderCourierInfo.fromJson', () {
    final dto = OrderCourierInfoDTO.fromJson(result);
    expect(dto.middleName, 'Рахимчонович');
  });

  test('getOrderCourierInfo.entity', () {
    final dto = OrderCourierInfoDTO.fromJson(result);
    final entity = dto.entity;
    expect(entity.middleName, 'Рахимчонович');
    expect(entity.codeStatus, ElectronicAcceptCodeStatus.okHide);
  });
}
