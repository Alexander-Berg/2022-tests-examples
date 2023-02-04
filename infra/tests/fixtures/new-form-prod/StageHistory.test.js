import { DEFAULT_MCRS_STAGE_PARAMS, DISABLED_LOCATIONS, HOST, ROUTES, STAGE_NAME } from '../../helpers/constants';
import { robotRole } from '../../helpers/roles';
import { getRawConfig } from '../../helpers/variableGetters';
import { app } from '../../page_objects/AppNewForm';

const { page, actions } = app;

fixture`Stage History tests (new)`
   // .only
   .skip // Не включать, тесты истории теперь на cypress
   .beforeEach(async t => {
      await t.useRole(robotRole);

      await actions.launchTestStage(t);
      await actions.createTestStage(t);
   })
   .page(HOST);

async function makeRevision2({ t, deployUnitId, podCount, disruptionBudget, clusters }) {
   const { formNew: form } = page.stage;

   await actions.editTestStage(t);

   {
      // first edit
      await actions.editDeployUnitSettings(t, deployUnitId);
      for (const { value } of clusters) {
         await form.deployUnit
            .location(value)
            .podCount.replaceText(t, `${podCount.revision1}`, `${podCount.revision2}`);
      }
      await form.deployUnit.disruptionBudget.replaceText(
         t,
         `${disruptionBudget.revision1}`,
         `${disruptionBudget.revision2}`,
      );
      await form.buttons.update.click(t);
   }

   await form.diff.buttons.continueEdit.click(t);

   {
      // check edited values after "Edit"
      await actions.editDeployUnitSettings(t, deployUnitId);
      for (const { value } of clusters) {
         await form.deployUnit.location(value).podCount.readValue(t, `${podCount.revision2}`);
      }
      await form.deployUnit.disruptionBudget.readValue(t, `${disruptionBudget.revision2}`);
      await form.buttons.update.click(t);
   }

   await form.diff.buttons.closeDiff.click(t);

   {
      // check edited values after "Cancel"
      await actions.editDeployUnitSettings(t, deployUnitId);

      for (const { value } of clusters) {
         await form.deployUnit.location(value).podCount.readValue(t, `${podCount.revision2}`);
      }
      await form.deployUnit.disruptionBudget.readValue(t, `${disruptionBudget.revision2}`);

      await form.buttons.update.click(t);
      await form.diff.buttons.continueEdit.click(t);
   }

   await actions.editDeployUnitSettings(t, deployUnitId);

   for (const { value } of clusters) {
      await form.deployUnit.location(value).podCount.readValue(t, `${podCount.revision2}`);
   }
   await form.deployUnit.disruptionBudget.readValue(t, `${disruptionBudget.revision2}`);

   await actions.updateTestStage(t);

   await actions.editTestStage(t);

   await actions.editDeployUnitSettings(t, deployUnitId);

   // finally checks
   for (const { value } of clusters) {
      await form.deployUnit.location(value).podCount.readValue(t, `${podCount.revision2}`);
   }
   await form.deployUnit.disruptionBudget.readValue(t, `${disruptionBudget.revision2}`);
}

test('Update Stage: History new', async t => {
   const deployUnitId = DEFAULT_MCRS_STAGE_PARAMS.deployUnitId;
   const boxId = DEFAULT_MCRS_STAGE_PARAMS.boxId;
   const workloadId = DEFAULT_MCRS_STAGE_PARAMS.workloadId;

   const config = await getRawConfig();
   const clusters = config.clusters.filter(v => !DISABLED_LOCATIONS.includes(v.value));
   const countClusters = clusters.length;

   const podCount = {
      revision1: 1,
      revision2: 2,
   };

   const disruptionBudget = {
      revision1: countClusters * podCount.revision1 - 1,
      revision2: countClusters * podCount.revision2 - 1,
   };

   const message = {
      revision2: 'testcafe update stage',
      revision3: 'apply revision 1 as is',
      revision4: 'apply revision 2 with changes',
   };

   const { formNew: form, tabs, history } = page.stage;

   await makeRevision2({ t, deployUnitId, podCount, disruptionBudget, clusters });

   // history
   await tabs.history.click(t);
   await t.expect(history.table.revision('2').actions.currentRevision.exists).ok();
   await t.expect(history.table.revision('2').message(message.revision2).exists).ok();
   await t.expect(history.table.revision('1').message('—').exists).ok();

   await history.table.revision('1').row.click(t);
   await t.expect(history.getLocation()).eql(`${ROUTES.STAGE_HISTORY}/1`);
   await t.expect(history.diff.message.empty.exists).ok();
   await history.diff.buttons.backToHistory.click(t);

   await history.table.revision('2').row.click(t);
   await t.expect(history.getLocation()).eql(`${ROUTES.STAGE_HISTORY}/2`);
   await t.expect(history.diff.message.text(message.revision2).exists).ok();

   await history.diff.selector(1).select(t, 'Revision 1');

   await t.expect(history.getLocation()).eql(`${ROUTES.STAGE_HISTORY}/1`);
   await t.expect(history.diff.message.empty.exists).ok();
   await history.diff.buttons.apply.click(t);
   await history.diff.buttons.applyAsIs.click(t);
   await t.expect(history.getLocation()).eql(`${ROUTES.STAGE_HISTORY}/1/applyAsIs`);
   await form.buttons.cancel.click(t);

   await t.expect(history.getLocation()).eql(`${ROUTES.STAGE_HISTORY}/1`);
   await t.expect(history.diff.message.empty.exists).ok();
   await history.diff.selector(1).readValue(t, 'Revision 1');
   await history.diff.buttons.apply.click(t);
   await history.diff.buttons.applyAsIs.click(t);

   await t.expect(history.getLocation()).eql(`${ROUTES.STAGE_HISTORY}/1/applyAsIs`);
   await form.diff.description.typeText(t, message.revision3);

   // await t.expect(history.diff.title(`Revision ${revision}`).exists).eql(true, '', { timeout: 120000 }); // history acceptance bug fix DEPLOY-3472
   await actions.deployHistoryTestStage(t, '3');

   await actions.editTestStage(t);
   await actions.editDeployUnitSettings(t, deployUnitId);

   for (const { value } of clusters) {
      await form.deployUnit.location(value).podCount.readValue(t, `${podCount.revision1}`);
   }

   await form.deployUnit.disruptionBudget.readValue(t, `${disruptionBudget.revision1}`);

   await tabs.history.click(t);
   await t.expect(history.table.revision('3').actions.currentRevision.exists).ok();
   await t.expect(history.table.revision('3').message(message.revision3).exists).ok();

   await history.table.revision('3').row.click(t);
   await t.expect(history.getLocation()).contains(`${ROUTES.STAGE_HISTORY}/3`);
   await t.expect(history.diff.currentRevision.exists).ok();
   await history.diff.selector(1).select(t, 'Revision 2');
   await t.expect(history.getLocation()).contains(`${ROUTES.STAGE_HISTORY}/2`);
   await t.expect(history.diff.message.text(message.revision2).exists).ok();
   await t.expect(history.diff.currentRevision.exists).notOk();

   await history.diff.buttons.apply.click(t);
   await history.diff.buttons.applyWithChanges.click(t);

   await t.expect(history.getLocation()).eql(`${ROUTES.STAGE_HISTORY}/2/apply`);
   await form.buttons.cancel.click(t);
   await t.expect(history.getLocation()).eql(`${ROUTES.STAGE_HISTORY}/2`);

   await history.diff.buttons.apply.click(t);
   await history.diff.buttons.applyWithChanges.click(t);

   await t.expect(history.getLocation()).eql(`${ROUTES.STAGE_HISTORY}/2/apply`);

   await actions.editWorkloadSettings(t, deployUnitId, boxId, workloadId);

   await form.workload.logs.change(t, 'Disabled', 'Enabled');

   await form.buttons.update.click(t);

   // TODO: routes
   // /stages/nodejsgirl-e2e/history/2/edit/stage-nodejsgirl-e2e/du-DeployUnitMCRS/box-box/wl-workload
   // await t.expect(history.getLocation()).contains(`${ROUTES.STAGE_HISTORY}/2/deploy`);
   // await history.diff.buttons.backToEdit.click(t);

   await form.diff.buttons.closeDiff.click(t);
   await t.expect(history.getLocation()).contains(`${ROUTES.STAGE_HISTORY}/2/apply`);

   // await actions.editWorkloadSettings(t, deployUnitId, boxId, workloadId);
   await form.workload.logs.readValue(t, 'Enabled');

   await form.buttons.update.click(t);

   // TODO: routes
   // /stages/nodejsgirl-e2e/history/2/edit/stage-nodejsgirl-e2e/du-DeployUnitMCRS/box-box/wl-workload
   // await t.expect(history.getLocation()).contains(`${ROUTES.STAGE_HISTORY}/2/deploy`);

   await form.diff.description.typeText(t, message.revision4);
   await actions.deployHistoryTestStage(t, '4');

   await actions.editTestStage(t);
   await actions.editDeployUnitSettings(t, deployUnitId);

   for (const { value } of clusters) {
      await form.deployUnit.location(value).podCount.readValue(t, `${podCount.revision2}`);
   }
   await form.deployUnit.disruptionBudget.readValue(t, `${disruptionBudget.revision2}`);

   await actions.editWorkloadSettings(t, deployUnitId, boxId, workloadId);
   await form.workload.logs.readValue(t, 'Enabled');

   await tabs.history.click(t);
   await history.table.revision('4').row.click(t);
   await t.expect(history.getLocation()).contains(`${ROUTES.STAGE_HISTORY}/4`);
   await t.expect(history.diff.currentRevision.exists).ok();
   await t.expect(history.diff.message.text(message.revision4).exists).ok();
   await history.diff.buttons.compareRevisions.click(t);
   await t.expect(history.getLocation()).contains(`${ROUTES.STAGE_HISTORY}/3/diff/4`);

   // // revision 3 <-> revision 4

   // inserted fields
   await history.diff.viewer.fieldDeleted(t, 'revision', '3');
   await history.diff.viewer.fieldDeleted(t, 'description', message.revision3);
   await history.diff.viewer.fieldDeleted(t, 'replica_count', String(podCount.revision1));
   await history.diff.viewer.fieldDeleted(t, 'max_unavailable', String(disruptionBudget.revision1));

   // deleted fields
   await history.diff.viewer.fieldInserted(t, 'revision', '4');
   await history.diff.viewer.fieldInserted(t, 'description', message.revision4);
   await history.diff.viewer.fieldInserted(t, 'replica_count', String(podCount.revision2));
   await history.diff.viewer.fieldInserted(t, 'max_unavailable', String(disruptionBudget.revision2));
   await history.diff.viewer.fieldInserted(t, 'transmit_logs', 'true');

   // TODO: bugfix/rearrange_revisions#DEPLOY-4700
   await history.diff.buttons.rearrangeRevisions.click(t);
   await t.expect(history.getLocation()).contains(`${ROUTES.STAGE_HISTORY}/4/diff/3`);
   await history.diff.buttons.rearrangeRevisions.click(t);
   await t.expect(history.getLocation()).contains(`${ROUTES.STAGE_HISTORY}/3/diff/4`);

   await history.diff.viewer.fieldDeleted(t, 'revision', '3');
   await history.diff.viewer.fieldDeleted(t, 'description', message.revision3);
   await history.diff.viewer.fieldInserted(t, 'revision', '4');
   await history.diff.viewer.fieldInserted(t, 'description', message.revision4);
   await history.diff.selector(2).selectWithoutTextCheck(t, 'Revision 1');
   await t.expect(history.getLocation()).contains(`${ROUTES.STAGE_HISTORY}/1/diff/3`);

   await history.diff.viewer.fieldDeleted(t, 'revision', '1');

   await history.diff.viewer.fieldInserted(t, 'revision', '3');
   // await history.diff.viewer.fieldInserted(t, 'revision_info', ''); // TODO: null bug
   await history.diff.viewer.fieldInserted(t, 'description', message.revision3);

   await history.diff.options.viewType.select(t, 'split'); ///
   await history.diff.options.viewType.select(t, 'unified'); ///
   await history.diff.options.allExpanded.check(t);

   await history.diff.viewer.field(t, 'meta', '');
   await history.diff.viewer.field(t, 'acl', '');
   await history.diff.viewer.field(t, 'id', STAGE_NAME);

   await history.diff.info('1').click(t);
   await t.expect(history.diff.popup.title('Revision 1').exists).ok();
   await t.expect(history.diff.popup.message.empty.exists).ok();
   await history.diff.popup.buttons.close.click(t);

   await history.diff.info('2').click(t);
   await t.expect(history.diff.popup.title('Revision 3').exists).ok();
   await t.expect(history.diff.popup.message.text(message.revision3).exists).ok(); // TODO: bugfix/revision_popup_description#DEPLOY-4699
   await history.diff.popup.buttons.showYaml.click(t);

   await history.diff.selector(1).readValue(t, 'Revision 3');
   await t.expect(history.diff.message.text(message.revision3).exists).ok();
   await history.diff.buttons.backToHistory.click(t);
   await t.expect(history.table.revision('1').message('—').exists).ok();

   await actions.deleteTestStage(t);
});
