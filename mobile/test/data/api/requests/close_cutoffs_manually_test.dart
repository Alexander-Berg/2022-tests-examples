import 'package:partners_app/data/api/executors/mock_executor.dart';
import 'package:partners_app/data/api/requests/close_cutoffs_manually.dart';
import 'package:partners_app/domain/entites/campaign.dart';
import 'package:partners_app/domain/repositories/user/close_cutoffs_manually_params.dart';

import 'package:test/test.dart';

void main() {
  test('CloseCutoffsManuallyReq', () async {
    final req = CloseCutoffsManuallyReq(
      executor: MockExecutor(),
      params: const CloseCutoffsManuallyParams(
          campaignId: 1001410791, businessId: 11111),
    );

    final result = await req.exec();

    expect(result.id, 1001410791);
    expect(result.name, 'Экспрессович');
    expect(result.status, CampaignStatus.failed);
    expect(result.isEnabled, false);
    expect(result.newbie, false);
    expect(result.partnerId, 11103659);
    expect(result.type, CampaignType.supplier);
    expect(result.model, BusinessModel.express);
  });
}
