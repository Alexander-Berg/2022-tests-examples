import { DEFAULT_MCRS_STAGE_PARAMS, HOST, ROUTES, STAGE_STATUSES, TIMEOUTS } from '../../helpers/constants';
import { robotRole } from '../../helpers/roles';
import { app } from '../../page_objects/AppNewForm';

const { page, actions } = app;

fixture`Stage creation`
   // .only
   .beforeEach(async t => {
      await t.useRole(robotRole);

      await actions.launchTestStage(t);
   })
   .page(HOST);

test('Stage: spec validation failed', async t => {
   await actions.editDeployUnitSettings(t, DEFAULT_MCRS_STAGE_PARAMS.deployUnitId);
   await page.stage.formNew.deployUnit.network.clear(t);

   await actions.createTestStage(t);

   await page.stage.status.checkLocation(t);

   await t
      .expect(page.stage.status.stage.status(STAGE_STATUSES.VALIDATION_FAILED).exists)
      .ok('', { timeout: TIMEOUTS.stageReadyAfterSave });

   await actions.deleteTestStage(t);
});
