import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/get_order_items_drop_schema.dart';
import 'package:partners_app/domain/entites/order.dart';
import 'package:partners_app/domain/repositories/orders/get_order_items_drop_schema_params.dart';
import 'package:test/test.dart';

void main() {
  test('SetOrderNotesReq', () async {
    final req = GetOrderItemsDropSchemaReq(
      executor: MockExecutor(),
      params: const GetOrderItemsDropSchemaParams(
        campaignId: 123,
        orderId: 1,
      ),
    );

    final response = await req.exec();
    expect(response.orderId, 123);

    expect(response.disabledReasons?.length, 1);
    final disabledReason = response.disabledReasons?.first;
    expect(disabledReason, OrderItemDisableReasons.notAllowedColor);

    expect(response.itemRemovalPermissions?.length, 1);
    final removalPermission = response.itemRemovalPermissions?.first;
    expect(removalPermission?.disabledReasons?.length, 1);

    final permissionReason = removalPermission?.disabledReasons?.first;
    expect(permissionReason,
        OrderItemDisableReasons.deliveryServiceDoesNotSupport);
  });
}
