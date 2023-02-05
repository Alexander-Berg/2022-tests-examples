import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/get_orders_list.dart';
import 'package:partners_app/domain/entites/campaign.dart';
import 'package:partners_app/domain/entites/order.dart';
import 'package:partners_app/domain/repositories/orders/get_orders_list_params.dart';
import 'package:test/test.dart';

void main() {
  test('GetMajorCitiesReq', () async {
    final req = GetOrdersListReq(
      executor: MockExecutor(),
      params: const GetOrdersListParams(
        campaignId: 123,
        timeZone: 'Asia/Yekaterinburg',
      ),
    );

    final response = await req.exec();
    expect(response.result.timeZone, 'Asia/Yekaterinburg');
    expect(response.result.placementModel, BusinessModel.express);
    expect(response.result.pager.pageSize, 10);
    expect(response.result.orders.length, 1);
    expect(response.result.searchParams.partnerId, 555);
    final order = response.orders.values.first;
    expect(order.state, OrderState.newState);
    final orderItem = response.orderItems.values.first;
    expect(orderItem.offerId, 'strundel.731');
  });
}
