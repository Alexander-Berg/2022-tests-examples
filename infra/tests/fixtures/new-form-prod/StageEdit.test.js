import { DEFAULT_MCRS_STAGE_PARAMS, HOST } from '../../helpers/constants';
import { robotRole } from '../../helpers/roles';
import { app } from '../../page_objects/AppNewForm';

const { page, actions } = app;
const { formNew: form } = page.stage;

fixture`Stage edition`
   // .only
   .beforeEach(async t => {
      await t.useRole(robotRole);

      await actions.launchTestStage(t);
   })
   .page(HOST);

test('Edit Stage, show/close Diff', async t => {
   const { deployUnitId, boxId, workloadId } = DEFAULT_MCRS_STAGE_PARAMS;

   await actions.createTestStage(t);
   await actions.editTestStage(t);

   await actions.editDeployUnitSettings(t, deployUnitId);

   await form.deployUnit.cpu.value.replaceText(t, '100', '200');
   await form.deployUnit.ram.value.replaceText(t, '1', '2');

   await form.buttons.update.click(t);
   await form.diff.buttons.closeDiff.click(t);

   await form.deployUnit.cpu.value.readValue(t, '200');
   await form.deployUnit.ram.value.readValue(t, '2');

   await form.buttons.update.click(t);
   await form.buttons.cancel.click(t);

   await form.deployUnit.cpu.value.readValue(t, '200');
   await form.deployUnit.ram.value.readValue(t, '2');

   await actions.editBoxSettings(t, deployUnitId, boxId);

   await form.box.resolvConf.change(t, 'default', 'nat64');
   await form.box.bindSkynet.change(t, 'Disabled', 'Enabled');

   await form.buttons.update.click(t);
   await form.diff.buttons.closeDiff.click(t);
   // await form.buttons.cancel.click(t);

   await form.box.resolvConf.readValue(t, 'nat64');
   await form.box.bindSkynet.readValue(t, 'Enabled');

   await actions.editWorkloadSettings(t, deployUnitId, boxId, workloadId);

   await form.workload.logs.change(t, 'Disabled', 'Enabled');

   await form.buttons.update.click(t);
   // await form.diff.buttons.closeDiff.click(t);
   await form.buttons.cancel.click(t);

   await form.workload.logs.readValue(t, 'Enabled');

   await actions.updateTestStage(t);
   await actions.editTestStage(t);

   await actions.editDeployUnitSettings(t, deployUnitId);

   await form.deployUnit.cpu.value.readValue(t, '200');
   await form.deployUnit.ram.value.readValue(t, '2');

   await actions.editBoxSettings(t, deployUnitId, boxId);

   await form.box.resolvConf.readValue(t, 'nat64');
   await form.box.bindSkynet.readValue(t, 'Enabled');

   await actions.editWorkloadSettings(t, deployUnitId, boxId, workloadId);

   await form.workload.logs.readValue(t, 'Enabled');

   await actions.deleteTestStage(t);
});
