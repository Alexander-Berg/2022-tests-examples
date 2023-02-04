import { DEFAULT_MCRS_STAGE_PARAMS, HOST, PROJECT_NAME, ROUTES, STAGE_NAME } from '../../helpers/constants';
import { robotRole } from '../../helpers/roles';
import { app } from '../../page_objects/AppNewForm';

const { page, actions } = app;

fixture`NEW FORM TESTS`.skip
   .beforeEach(async t => {
      await t.useRole(robotRole);
      await actions.launchTestStage(t);
   })
   .page(HOST);

test('some templates', async t => {
   return; // skip don't work ((
   const { formNew: form, tabs } = page.stage;

   await actions.createTestStage(t);

   // await tabs.balancers.click(t);
   // await t.expect(page.stage.balancers.body.exists).eql(true);
   // await t.expect(page.stage.balancers.getLocation()).contains(ROUTES.STAGE_BALANCERS);

   await t.navigateTo(ROUTES.STAGE_LOGS);

   // await tabs.config.click(t);
   // await t.expect(page.stage.config.body.exists).eql(true);
   // await t.expect(page.stage.config.getLocation()).contains(ROUTES.STAGE_CONFIG);

   await tabs.formNew.click(t);
   //  await t.expect(form.body.exists).eql(true);
   await t.expect(form.getLocation()).contains(ROUTES.STAGE_CONFIG);
   await page.stage.buttons.editStage.click(t);
   await t.expect(form.getLocation()).contains(ROUTES.STAGE_EDIT);

   await form.buttons.revertChanges.click(t);
   await form.buttons.updateStage.click(t);

   await form.diff.buttons.closeDiff.click(t);
   await form.buttons.updateStage.click(t);

   await form.diff.buttons.continueEdit.click(t);
   // await form.buttons.updateStage.click(t);
   // await actions.updateTestStage(t);

   // await form.diff.buttons.deployStage.click(t);
   await actions.updateTestStage(t);

   await page.stage.status.checkLocation(t);

   await tabs.formNew.click(t);
   await t.expect(form.getLocation()).contains(ROUTES.STAGE_CONFIG);
   await page.stage.buttons.editStage.click(t);
   await t.expect(form.getLocation()).contains(ROUTES.STAGE_EDIT);

   await form.stage.id.readValue(t, STAGE_NAME);
   await form.stage.project.readValue(t, PROJECT_NAME);

   await form.sideTree.stage.link.click(t);

   // edit deploy unit
   await actions.editDeployUnitSettings(t, DEFAULT_MCRS_STAGE_PARAMS.deployUnitId);
   await form.deployUnit.cpu.value.readValue(t, '100');
   await form.deployUnit.cpu.value.typeText(t, '200');
   await form.deployUnit.ram.value.readValue(t, '1');
   await form.deployUnit.ram.value.typeText(t, '2');

   // edit box
   await actions.editBoxSettings(t, DEFAULT_MCRS_STAGE_PARAMS.deployUnitId, DEFAULT_MCRS_STAGE_PARAMS.boxId);
   await form.box.dockerImage.name.typeText(t, 'name');
   await form.box.dockerImage.tag.typeText(t, 'tag');

   // edit workload
   await actions.editWorkloadSettings(
      t,
      DEFAULT_MCRS_STAGE_PARAMS.deployUnitId,
      DEFAULT_MCRS_STAGE_PARAMS.boxId,
      DEFAULT_MCRS_STAGE_PARAMS.workloadId,
   );
   await form.workload.logs.select(t, 'Disabled');
   await form.workload.logs.select(t, 'Enabled');

   // await actions.deleteTestStage(t);
});
