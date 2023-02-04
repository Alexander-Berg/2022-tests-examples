import {
   DEFAULT_MCRS_STAGE_PARAMS,
   DISABLED_LOCATIONS,
   HOST,
   ROUTES,
   STAGE_NAME,
   TIMEOUTS,
} from '../../helpers/constants';
import { robotRole } from '../../helpers/roles';
import { getRawConfig } from '../../helpers/variableGetters';
import { app } from '../../page_objects/AppNewForm';

const { page, actions } = app;

const { status, formNew: form, tabs } = page.stage;

fixture.skip`Stage Status`
   // .only
   .beforeEach(async t => {
      await t.useRole(robotRole);
   })
   .page(HOST);

test.skip('Stage Status: ...', async t => {
   // Этот тест теперь есть на Cypress: cypress/integration/userStories/stage/StageStatus.ts DEPLOY-6004

   const deployUnitId = DEFAULT_MCRS_STAGE_PARAMS.deployUnitId;

   const config = await getRawConfig();
   const clusters = config.clusters.filter(v => !DISABLED_LOCATIONS.includes(v.value));

   const clusterList = clusters
      .map(v => v.value)
      .sort()
      .slice(0, 2);

   const location1 = {
      cluster: clusterList[0],
      podCount: '1',
   };

   const location2 = {
      cluster: clusterList[1],
      podCount: '11',
   };

   await actions.launchTestStage(t);

   await actions.editDeployUnitSettings(t, deployUnitId);

   await form.deployUnit.disruptionBudget.typeText(t, '11');

   await form.deployUnit.location(location1.cluster).podCount.typeText(t, location1.podCount);
   await form.deployUnit.location(location2.cluster).podCount.typeText(t, location2.podCount);

   for (const { value } of clusters) {
      if (!clusterList.includes(value)) {
         await form.deployUnit.location(value).enabled.uncheck(t);
      }
   }

   await form.deployUnit.ram.value.typeText(t, '300');
   await form.deployUnit.ram.unit.select(t, 'MB');

   await actions.createTestStage(t);

   // для дебага
   // await t.navigateTo(ROUTES.STAGE_STATUS);

   await t.expect(status.body.exists).ok();

   await t.expect(status.stage.revision('1').ready.exists).ok('', { timeout: TIMEOUTS.stageReadyAfterSave });

   await status.pods.location.select(t, location1.cluster);

   await t
      .expect(status.pods.pod(location1.cluster).revision('1').ready.exists)
      // .expect(status.pods.pod(location1.cluster).revision('1').status('ready').exists)
      .ok('', { timeout: TIMEOUTS.stageReadyAfterSave });

   await t
      .expect(status.pods.pod(location1.cluster).revision('1').openedRow.exists)
      .notOk('', { timeout: TIMEOUTS.stageReadyAfterSave });

   await t.click(status.pods.pod(location1.cluster).revision('1').N);

   await t
      .expect(status.pods.pod(location1.cluster).revision('1').openedRow.exists)
      .ok('', { timeout: TIMEOUTS.stageReadyAfterSave });

   await t.click(status.pods.pod(location1.cluster).revision('1').N);

   await t
      .expect(status.pods.pod(location1.cluster).revision('1').openedRow.exists)
      .notOk('', { timeout: TIMEOUTS.stageReadyAfterSave });

   await status.pods.location.select(t, location2.cluster);

   await t
      .expect(status.pods.pagination.page('2').exists)
      .ok('', { timeout: TIMEOUTS.slow })
      .click(status.pods.pagination.page('2'));

   // TODO Неверно работает пагинация DEPLOY-6019
   await status.pods.pagination.limit.change(t, '10', '100');
   await status.pods.pagination.limit.change(t, '100', '50');
   await status.pods.pagination.limit.change(t, '50', '20');

   await status.pods.filter.status.select(t, 'status is ready');
   await status.pods.filter.revision.select(t, 'current revision: 1');

   await tabs.config.click(t);
   await t.expect(form.getLocation()).contains(ROUTES.STAGE_CONFIG);

   await page.stage.buttons.editStage.click(t);
   await t.expect(form.getLocation()).contains(ROUTES.STAGE_EDIT);

   await form.stage.id.readValue(t, STAGE_NAME);

   await tabs.status.click(t);
   await page.stage.status.checkLocation(t);

   await status.pods.location.select(t, location2.cluster);

   await status.pods.filter.status.readValue(t, 'status is ready');
   await status.pods.filter.revision.readValue(t, 'current revision: 1');

   await status.pods.pagination.limit.readValue(t, '20');

   // await status.stage.revision('1').xrayLink.click(t);

   await actions.deleteTestStage(t);
});
