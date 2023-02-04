import {
   DEFAULT_MCRS_STAGE_PARAMS,
   DEFAULT_RS_STAGE_PARAMS,
   DISABLED_LOCATIONS,
   HOST,
   NETWORK,
   PROJECT_NAME,
   SECRET,
   STAGE_STATUSES,
} from '../../helpers/constants';
import { robotRole } from '../../helpers/roles';
import { getRawConfig } from '../../helpers/variableGetters';
import { app } from '../../page_objects/AppNewForm';

const { page, actions } = app;
const { formNew: form } = page.stage;

fixture`Deploy Unit Settings (new)`
   // .only
   .beforeEach(async t => {
      await t.useRole(robotRole);
      await actions.launchTestStage(t);
   })
   .page(HOST);

test('Deploy Unit: multiplication', async t => {
   const deployUnit = {
      id: DEFAULT_MCRS_STAGE_PARAMS.deployUnitId,
      cpu: '100',
      ram: '1',
   };

   const deployUnitCloned = {
      id: `${deployUnit.id}_clone`,
      cpu: '200',
      ram: '2',
   };

   const deployUnitAdded = {
      id: 'deployUnit_1',
      cpu: '300',
      ram: '3',
   };

   await actions.editDeployUnitSettings(t, deployUnit.id);

   await form.deployUnit.buttons.clone.click(t);
   // await form.deployUnit.id.typeText(t, deployUnitCloned.id);
   await t.expect(form.sideTree.deployUnit(deployUnitCloned.id).isAdded.exists).ok();

   await form.deployUnit.cpu.value.typeText(t, deployUnitCloned.cpu);
   await form.deployUnit.ram.value.typeText(t, deployUnitCloned.ram);

   await actions.editDeployUnitSettings(t, deployUnit.id);
   await form.deployUnit.id.readValue(t, deployUnit.id);
   await form.deployUnit.cpu.value.readValue(t, deployUnit.cpu);
   await form.deployUnit.ram.value.readValue(t, deployUnit.ram);

   await actions.editDeployUnitSettings(t, deployUnitCloned.id);
   await form.deployUnit.id.readValue(t, deployUnitCloned.id);
   await form.deployUnit.cpu.value.readValue(t, deployUnitCloned.cpu);
   await form.deployUnit.ram.value.readValue(t, deployUnitCloned.ram);

   await form.sideTree.stage.buttons.addDeployUnit.click(t);
   await actions.editDeployUnitSettings(t, deployUnitAdded.id);
   // await form.deployUnit.id.typeText(t, deployUnitAdded.id);
   await t.expect(form.sideTree.deployUnit(deployUnitAdded.id).isAdded.exists).ok();

   await form.deployUnit.cpu.value.typeText(t, deployUnitAdded.cpu);
   await form.deployUnit.ram.value.typeText(t, deployUnitAdded.ram);

   await actions.editDeployUnitSettings(t, deployUnit.id);
   await form.deployUnit.id.readValue(t, deployUnit.id);
   await form.deployUnit.cpu.value.readValue(t, deployUnit.cpu);
   await form.deployUnit.ram.value.readValue(t, deployUnit.ram);

   await actions.editDeployUnitSettings(t, deployUnitCloned.id);
   await form.deployUnit.id.readValue(t, deployUnitCloned.id);
   await form.deployUnit.cpu.value.readValue(t, deployUnitCloned.cpu);
   await form.deployUnit.ram.value.readValue(t, deployUnitCloned.ram);

   await actions.editDeployUnitSettings(t, deployUnitAdded.id);
   await form.deployUnit.id.readValue(t, deployUnitAdded.id);
   await form.deployUnit.cpu.value.readValue(t, deployUnitAdded.cpu);
   await form.deployUnit.ram.value.readValue(t, deployUnitAdded.ram);

   await actions.createTestStage(t);

   await t.expect(page.stage.status.stage.status(STAGE_STATUSES.VALIDATION_FAILED).exists).ok('', { timeout: 120000 });

   await actions.editTestStage(t);

   await actions.editDeployUnitSettings(t, deployUnitAdded.id);
   await form.deployUnit.id.readValue(t, deployUnitAdded.id);
   await form.deployUnit.cpu.value.readValue(t, deployUnitAdded.cpu);
   await form.deployUnit.ram.value.readValue(t, deployUnitAdded.ram);
   await form.deployUnit.network.readValue(t, '');
   await form.deployUnit.buttons.remove.click(t);
   await t.expect(form.sideTree.deployUnit(deployUnitAdded.id).isRemoved.exists).ok();
   await actions.editDeployUnitSettings(t, deployUnitAdded.id);
   await form.deployUnit.buttons.restore.click(t);
   await t.expect(form.sideTree.deployUnit(deployUnitAdded.id).isRemoved.exists).notOk();
   await form.deployUnit.buttons.remove.click(t);
   await t.expect(form.sideTree.deployUnit(deployUnitAdded.id).isRemoved.exists).ok();

   await actions.editDeployUnitSettings(t, deployUnit.id);
   await form.deployUnit.id.readValue(t, deployUnit.id);
   await form.deployUnit.cpu.value.readValue(t, deployUnit.cpu);
   await form.deployUnit.ram.value.readValue(t, deployUnit.ram);
   await form.deployUnit.network.readValue(t, NETWORK);

   await actions.editDeployUnitSettings(t, deployUnitCloned.id);
   await form.deployUnit.id.readValue(t, deployUnitCloned.id);
   await form.deployUnit.cpu.value.readValue(t, deployUnitCloned.cpu);
   await form.deployUnit.ram.value.readValue(t, deployUnitCloned.ram);
   await form.deployUnit.network.readValue(t, NETWORK);

   await actions.updateTestStage(t);

   await t
      .expect(page.stage.status.stage.status(STAGE_STATUSES.VALIDATION_FAILED).exists)
      .notOk('', { timeout: 120000 });
   await t.expect(page.stage.status.stage.status(STAGE_STATUSES.DEPLOYING).exists).ok('', { timeout: 120000 });

   await actions.deleteTestStage(t);
});

test('TVM', async t => {
   const deployUnitId = DEFAULT_MCRS_STAGE_PARAMS.deployUnitId;

   const abc = {
      name: 'nodejsgirl-deploy',
      url: 'nodejsgirldeploy',
   };

   const tvmConfig = {
      clientPort: '2',
      blackbox: 'Test',
      source: abc,
      destination: abc,
   };

   const source = {
      1: {
         id: '2015727',
         alias: 's1',
      },
      2: {
         id: '2015729',
         alias: 's2',
      },
   };

   const destination = {
      1: {
         id: '2015731',
         alias: 'd1',
      },
      2: {
         id: '2015733',
         alias: 'd2',
      },
   };

   await actions.editDeployUnitSettings(t, deployUnitId);

   await form.deployUnit.tvm.mode.readValue(t, 'Disabled');
   await form.deployUnit.tvm.mode.change(t, 'Disabled', 'Enabled');

   await form.deployUnit.tvm.buttons.hide.click(t);
   await form.deployUnit.tvm.buttons.show.click(t);

   await form.deployUnit.tvm.clientPort.typeText(t, tvmConfig.clientPort);
   await form.deployUnit.tvm.blackbox.change(t, 'ProdYaTeam', tvmConfig.blackbox);

   await form.deployUnit.tvm.clients.buttons.addClient.click(t);

   await form.deployUnit.tvm.clients.buttons.addClient.click(t);

   await form.deployUnit.tvm.clients.client(3).destination(1).app.typeText(t, destination[2].id);
   await form.deployUnit.tvm.clients.client(3).destination(1).alias.typeText(t, destination[2].alias);

   await form.deployUnit.tvm.clients.client(1).buttons.remove.click(t);

   await form.deployUnit.tvm.clients.client(2).secret.add.click(t);
   await form.deployUnit.tvm.clients.client(2).secret.modal.cancelButton.click(t);

   await form.deployUnit.tvm.clients.client(2).secret.add.click(t);
   await form.deployUnit.tvm.clients.client(2).secret.modal.alias.select(t, SECRET.ALIAS);

   await t.wait(250);

   await form.deployUnit.tvm.clients.client(2).secret.modal.addButton.click(t);
   await form.deployUnit.tvm.clients.client(2).secret.key.select(t, 'TVM_SOURCE_2');

   await form.deployUnit.tvm.clients.client(2).destination(1).app.readValue(t, destination[2].id);
   await form.deployUnit.tvm.clients.client(2).destination(1).alias.readValue(t, destination[2].alias);

   await form.deployUnit.tvm.clients.client(2).source.app.typeText(t, source[2].id);
   await form.deployUnit.tvm.clients.client(2).source.alias.typeText(t, source[2].alias);

   await form.deployUnit.tvm.clients.client(1).secret.alias.selectByGroup(t, SECRET.ALIAS, SECRET.ALIAS);
   await form.deployUnit.tvm.clients.client(1).secret.key.select(t, 'TVM_SOURCE_1');

   await form.deployUnit.tvm.clients.client(1).buttons.addDestination.click(t);

   await form.deployUnit.tvm.clients.client(1).destination(2).alias.typeText(t, destination[1].alias);

   await form.deployUnit.tvm.clients.client(1).destination(1).buttons.deleteDestination.click(t);
   await form.deployUnit.tvm.clients.client(1).destination(1).alias.readValue(t, destination[1].alias);

   await form.deployUnit.tvm.clients.client(1).destination(1).app.typeText(t, destination[1].id);

   await form.deployUnit.tvm.clients.client(1).source.app.typeText(t, source[1].id);
   await form.deployUnit.tvm.clients.client(1).source.alias.typeText(t, source[1].alias);

   await form.deployUnit.tvm.clients.client(1).source.app.typeText(t, source[1].id);
   await form.deployUnit.tvm.clients.client(1).source.alias.typeText(t, source[1].alias);

   await actions.createTestStage(t);

   await actions.editTestStage(t);
   await actions.editDeployUnitSettings(t, deployUnitId);

   // await form.deployUnit.tvm.buttons.show.click(t);

   await form.deployUnit.tvm.mode.readValue(t, 'Enabled');
   await form.deployUnit.tvm.clientPort.readValue(t, tvmConfig.clientPort);
   await form.deployUnit.tvm.blackbox.readValue(t, tvmConfig.blackbox);

   await form.deployUnit.tvm.clients.client(1).source.app.readValue(t, source[1].id);
   await form.deployUnit.tvm.clients.client(1).source.alias.readValue(t, source[1].alias);

   await form.deployUnit.tvm.clients.client(1).destination(1).app.readValue(t, destination[1].id);
   await form.deployUnit.tvm.clients.client(1).destination(1).alias.readValue(t, destination[1].alias);

   await form.deployUnit.tvm.clients.client(1).secret.key.readValue(t, 'TVM_SOURCE_1');

   await form.deployUnit.tvm.clients.client(2).source.app.readValue(t, source[2].id);
   await form.deployUnit.tvm.clients.client(2).source.alias.readValue(t, source[2].alias);

   await form.deployUnit.tvm.clients.client(2).destination(1).app.readValue(t, destination[2].id);
   await form.deployUnit.tvm.clients.client(2).destination(1).alias.readValue(t, destination[2].alias);

   await form.deployUnit.tvm.clients.client(2).secret.key.readValue(t, 'TVM_SOURCE_2');

   // TODO: добавить проверку изменения секрета
   // 1) в первой ревизии выбираем некорректный секрет,
   // 2) во второй - меняем его на корректный,
   // 3) после этого delegation token должен обновиться, а стейдж подняться без ошибок

   await actions.updateTestStage(t);

   await actions.waitForStageValidation(t);
   await actions.waitForReadyStage(t, '2');

   await actions.editTestStage(t);
   await actions.editDeployUnitSettings(t, deployUnitId);

   await form.deployUnit.tvm.mode.change(t, 'Enabled', 'Disabled');

   await actions.updateTestStage(t);

   await actions.editTestStage(t);
   await actions.editDeployUnitSettings(t, deployUnitId);

   // await form.deployUnit.tvm.buttons.show.click(t);

   await form.deployUnit.tvm.mode.readValue(t, 'Disabled');
   await form.deployUnit.tvm.mode.change(t, 'Disabled', 'Enabled');

   await form.deployUnit.tvm.clientPort.readValue(t, tvmConfig.clientPort);
   await form.deployUnit.tvm.blackbox.readValue(t, tvmConfig.blackbox);

   await form.deployUnit.tvm.clients.client(1).source.app.readValue(t, source[1].id);
   await form.deployUnit.tvm.clients.client(1).source.alias.readValue(t, source[1].alias);

   await form.deployUnit.tvm.clients.client(1).destination(1).app.readValue(t, destination[1].id);
   await form.deployUnit.tvm.clients.client(1).destination(1).alias.readValue(t, destination[1].alias);

   await form.deployUnit.tvm.clients.client(2).source.app.readValue(t, source[2].id);
   await form.deployUnit.tvm.clients.client(2).source.alias.readValue(t, source[2].alias);

   await form.deployUnit.tvm.clients.client(2).destination(1).app.readValue(t, destination[2].id);
   await form.deployUnit.tvm.clients.client(2).destination(1).alias.readValue(t, destination[2].alias);

   // await form.deployUnit.tvm.mode.change(t, 'Disabled', 'Enabled');

   await form.deployUnit.tvm.clientPort.clearValue(t);

   await form.deployUnit.tvm.clients.buttons.addClient.click(t);

   await form.deployUnit.tvm.clients.client(1).buttons.remove.click(t);
   await form.deployUnit.tvm.clients.client(1).buttons.remove.click(t);

   await form.deployUnit.tvm.mode.change(t, 'Enabled', 'Disabled');

   await actions.updateTestStage(t);

   await actions.editTestStage(t);
   await actions.editDeployUnitSettings(t, deployUnitId);

   await form.deployUnit.tvm.mode.readValue(t, 'Disabled');
   await form.deployUnit.tvm.mode.change(t, 'Disabled', 'Enabled');

   // await form.deployUnit.tvm.buttons.show.click(t);

   await form.deployUnit.tvm.clientPort.readValue(t, '');
   await form.deployUnit.tvm.blackbox.readValue(t, tvmConfig.blackbox);

   await form.deployUnit.tvm.clients.client(1).source.app.readValue(t, '');
   await form.deployUnit.tvm.clients.client(1).source.alias.readValue(t, '');

   await form.deployUnit.tvm.clients.client(1).destination(1).app.readValue(t, '');
   await form.deployUnit.tvm.clients.client(1).destination(1).alias.readValue(t, '');

   await form.buttons.revertChanges.click(t);

   await actions.deleteTestStage(t);
});

test('Deploy Unit Settings: Yasm tags (itype, tags)', async t => {
   const { deployUnitId } = DEFAULT_MCRS_STAGE_PARAMS;
   const yasmTags = {
      itype: 'deploy',
      rev1: [
         {
            key: 'geo',
            value: 'sas',
         },
         {
            key: 'deletedKey',
            value: 'deletedValue',
         },
         {
            key: 'custom-tag-name-1',
            value: 'custom-tag-value-1',
         },
      ],
      rev2: [
         {
            key: 'geo',
            value: 'man',
         },
         {
            key: 'custom-tag-name-2',
            value: 'custom-tag-value-2',
         },
      ],
   };

   const yasmTagsForm = form.deployUnit.yasmTags;

   await actions.editDeployUnitSettings(t, deployUnitId);

   await yasmTagsForm.itype.readValue(t, PROJECT_NAME);

   await yasmTagsForm.buttons.addTag.exists(t);
   await yasmTagsForm.tag(1).buttons.removeTag.notExists(t);

   // await yasmTagsForm.itype.select(t, 'base');
   // await yasmTagsForm.buttons.addTag.exists(t);

   await yasmTagsForm.itype.clear(t);
   await yasmTagsForm.buttons.addTag.notExists(t);

   await yasmTagsForm.itype.select(t, yasmTags.itype);
   await yasmTagsForm.buttons.addTag.exists(t);

   await yasmTagsForm.buttons.addTag.click(t);
   await yasmTagsForm.buttons.addTag.click(t);
   await yasmTagsForm.buttons.addTag.click(t);
   await yasmTagsForm.tag(1).buttons.removeTag.exists(t);

   // выбираем теги: первый - из садджеста, два - руками

   await yasmTagsForm.tag(1).key.select(t, yasmTags.rev1[0].key);
   await yasmTagsForm.tag(1).value.select(t, yasmTags.rev1[0].value);

   await yasmTagsForm.tag(2).key.typeText(t, yasmTags.rev1[1].key);
   await yasmTagsForm.tag(2).value.typeText(t, yasmTags.rev1[1].value);

   await yasmTagsForm.tag(3).key.typeText(t, yasmTags.rev1[2].key);
   await yasmTagsForm.tag(3).value.typeText(t, yasmTags.rev1[2].value);

   await yasmTagsForm.tag(2).key.readValue(t, yasmTags.rev1[1].key);
   await yasmTagsForm.tag(2).value.readValue(t, yasmTags.rev1[1].value);
   await yasmTagsForm.tag(2).buttons.removeTag.click(t);

   await yasmTagsForm.itype.clear(t);
   await yasmTagsForm.itype.readValue(t, '');
   await yasmTagsForm.buttons.addTag.notExists(t);

   await yasmTagsForm.tag(1).key.readValue(t, yasmTags.rev1[0].key);
   await yasmTagsForm.tag(1).value.readValue(t, yasmTags.rev1[0].value);
   await yasmTagsForm.tag(1).buttons.removeTag.notExists(t);

   await yasmTagsForm.tag(2).key.readValue(t, yasmTags.rev1[2].key);
   await yasmTagsForm.tag(2).value.readValue(t, yasmTags.rev1[2].value);
   await yasmTagsForm.tag(2).buttons.removeTag.notExists(t);

   await yasmTagsForm.itype.select(t, yasmTags.itype);
   await yasmTagsForm.buttons.addTag.exists(t);
   await yasmTagsForm.tag(1).buttons.removeTag.exists(t);
   await yasmTagsForm.tag(2).buttons.removeTag.exists(t);

   await actions.createTestStage(t);
   await actions.editTestStage(t);

   // REVISION 1 -> 2

   await actions.editDeployUnitSettings(t, deployUnitId);

   const rev1index0 = await yasmTagsForm.actions.getTagIndex(yasmTags.rev1[0].key);
   const rev1index2 = await yasmTagsForm.actions.getTagIndex(yasmTags.rev1[2].key);

   await yasmTagsForm.tag(rev1index0).value.readValue(t, yasmTags.rev1[0].value);
   await yasmTagsForm.tag(rev1index2).value.readValue(t, yasmTags.rev1[2].value);

   // меняем теги: первый - из садджеста, второй - руками
   await yasmTagsForm.tag(rev1index0).value.change(t, yasmTags.rev1[0].value, yasmTags.rev2[0].value);
   await yasmTagsForm.tag(rev1index2).key.typeText(t, yasmTags.rev2[1].key);
   await yasmTagsForm.tag(rev1index2).value.typeText(t, yasmTags.rev2[1].value);

   await yasmTagsForm.itype.clear(t);
   await yasmTagsForm.itype.readValue(t, '');
   await yasmTagsForm.buttons.addTag.notExists(t);
   await yasmTagsForm.tag(1).buttons.removeTag.notExists(t);
   await yasmTagsForm.tag(2).buttons.removeTag.notExists(t);

   await actions.updateTestStage(t);
   await actions.editTestStage(t);

   // REVISION 2 -> 3

   await actions.editDeployUnitSettings(t, deployUnitId);

   const rev2index0 = await yasmTagsForm.actions.getTagIndex(yasmTags.rev2[0].key);
   const rev2index1 = await yasmTagsForm.actions.getTagIndex(yasmTags.rev2[1].key);

   await yasmTagsForm.itype.readValue(t, '');
   await yasmTagsForm.buttons.addTag.notExists(t);
   await yasmTagsForm.tag(1).buttons.removeTag.notExists(t);
   await yasmTagsForm.tag(2).buttons.removeTag.notExists(t);

   await yasmTagsForm.tag(rev2index0).key.readValue(t, yasmTags.rev2[0].key);
   await yasmTagsForm.tag(rev2index0).value.readValue(t, yasmTags.rev2[0].value);

   await yasmTagsForm.tag(rev2index1).key.readValue(t, yasmTags.rev2[1].key);
   await yasmTagsForm.tag(rev2index1).value.readValue(t, yasmTags.rev2[1].value);

   await yasmTagsForm.itype.select(t, yasmTags.itype);
   await yasmTagsForm.buttons.addTag.exists(t);

   await yasmTagsForm.tag(1).buttons.removeTag.click(t);
   await yasmTagsForm.tag(1).buttons.removeTag.click(t);
   await yasmTagsForm.tag(1).buttons.removeTag.notExists(t);

   await actions.updateTestStage(t);
   await actions.editTestStage(t);

   // REVISION 3 -> 4

   await actions.editDeployUnitSettings(t, deployUnitId);

   await yasmTagsForm.itype.readValue(t, yasmTags.itype);
   await yasmTagsForm.tag(1).buttons.removeTag.notExists(t);

   await yasmTagsForm.itype.clear(t);
   await yasmTagsForm.itype.readValue(t, '');
   await yasmTagsForm.buttons.addTag.notExists(t);

   await actions.updateTestStage(t);
   await actions.editTestStage(t);

   // REVISION 4

   await actions.editDeployUnitSettings(t, deployUnitId);

   await yasmTagsForm.itype.readValue(t, '');
   await yasmTagsForm.buttons.addTag.notExists(t);

   await page.stage.tabs.status.click(t);

   await actions.waitForStageValidation(t);

   // await actions.waitForReadyStage(t, '4');

   await actions.deleteTestStage(t);
});

test('Update Stage: Deploy Unit settings, location, MCRS/RS, multiplication', async t => {
   const deployUnitMCRS = DEFAULT_MCRS_STAGE_PARAMS.deployUnitId;
   const deployUnitRS = DEFAULT_RS_STAGE_PARAMS.deployUnitId;
   const antiaffinity = {
      perRack: '2',
      perNode: '1',
   };
   const cpu = {
      value: '101',
      unit: 'VCPU',
   };
   const ram = {
      value: '300',
      unit: 'MB',
   };
   const disk = {
      type: 'SSD',
      size: '2',
      bandwidth: '5',
   };
   const endpointSetPort = '8080';

   const config = await getRawConfig();
   const clusters = config.clusters.filter(v => !DISABLED_LOCATIONS.includes(v.value));
   const startPartLength = (clusters.length - (clusters.length % 2)) / 2;

   const clustersRS = clusters.slice(0, startPartLength);
   const clustersMCRS = clusters.slice(startPartLength);

   await actions.editDeployUnitSettings(t, deployUnitMCRS);

   await form.deployUnit.disruptionBudget.typeText(t, '1');

   for (const { value } of clustersRS) {
      await form.deployUnit.location(value).enabled.uncheck(t);
   }

   for (const { value } of clustersMCRS) {
      await form.deployUnit.location(value).podCount.typeText(t, '2');

      await form.deployUnit.location(value).antiaffinity.perRack.checkbox.checked(t);
      await form.deployUnit.location(value).antiaffinity.perRack.maxPods.typeText(t, antiaffinity.perRack);

      await form.deployUnit.location(value).antiaffinity.perNode.checkbox.unchecked(t);
      await form.deployUnit.location(value).antiaffinity.perNode.checkbox.check(t);
      await form.deployUnit.location(value).antiaffinity.perNode.maxPods.typeText(t, antiaffinity.perNode);
   }

   await form.deployUnit.cpu.value.typeText(t, cpu.value);
   await form.deployUnit.cpu.unit.select(t, cpu.unit);

   await form.deployUnit.ram.value.typeText(t, ram.value);
   await form.deployUnit.ram.unit.select(t, ram.unit);

   await form.deployUnit.endpointSet(0).port.replaceText(t, '80', endpointSetPort);

   await form.deployUnit.formTabs.selectDisks(t);
   await form.deployUnit.disk.type.change(t, 'HDD', disk.type);
   await form.deployUnit.disk.size.typeText(t, disk.size);
   await form.deployUnit.disk.bandwidth.guarantee.readValue(t, '1');
   await form.deployUnit.disk.bandwidth.guarantee.typeText(t, disk.bandwidth);

   await form.deployUnit.buttons.clone.click(t);

   await form.deployUnit.id.typeText(t, deployUnitRS);

   for (const { value } of clustersMCRS) {
      await form.deployUnit.location(value).enabled.uncheck(t);
   }

   let i = 2;
   for (const { value } of clustersRS) {
      await form.deployUnit.location(value).enabled.check(t);
      i++;
   }

   await form.deployUnit.primitive.change(t, 'Multi-cluster replica set', 'Per-cluster replica set');

   await form.deployUnit.antiaffinity.perRack.checkbox.checked(t);
   await form.deployUnit.antiaffinity.perRack.maxPods.typeText(t, antiaffinity.perRack);

   await form.deployUnit.antiaffinity.perNode.checkbox.unchecked(t);
   await form.deployUnit.antiaffinity.perNode.checkbox.check(t);
   await form.deployUnit.antiaffinity.perNode.maxPods.typeText(t, antiaffinity.perNode);

   i = 2; // fix firefox scroll bug (sidebar validation)
   for (const { value } of clustersRS) {
      await form.deployUnit.location(value).podCount.typeText(t, String(i));
      await form.deployUnit.location(value).disruptionBudget.typeText(t, String(i - 1));
      i++;
   }

   await actions.createTestStage(t);
   await t.click(page.stage.status.deployUnit(deployUnitRS).tab, { timeout: 180000 });
   await t.click(page.stage.status.deployUnit(deployUnitMCRS).tab, { timeout: 180000 });

   await actions.editTestStage(t);

   await actions.editDeployUnitSettings(t, deployUnitMCRS);

   await form.deployUnit.disruptionBudget.readValue(t, '1');

   for (const { value } of clustersMCRS) {
      await form.deployUnit.location(value).podCount.readValue(t, '2');
      await form.deployUnit.location(value).antiaffinity.perRack.checkbox.checked(t);
      await form.deployUnit.location(value).antiaffinity.perRack.maxPods.readValue(t, antiaffinity.perRack);

      await form.deployUnit.location(value).antiaffinity.perNode.checkbox.checked(t);
      await form.deployUnit.location(value).antiaffinity.perNode.maxPods.readValue(t, antiaffinity.perNode);
   }

   await form.deployUnit.cpu.value.readValue(t, cpu.value);
   await form.deployUnit.cpu.unit.readValue(t, cpu.unit);

   await form.deployUnit.ram.value.readValue(t, ram.value);
   await form.deployUnit.ram.unit.readValue(t, ram.unit);

   await form.deployUnit.endpointSet(0).port.readValue(t, endpointSetPort);

   await form.deployUnit.formTabs.selectDisks(t);
   await form.deployUnit.disk.type.readValue(t, disk.type);
   await form.deployUnit.disk.size.readValue(t, disk.size);
   await form.deployUnit.disk.bandwidth.guarantee.readValue(t, disk.bandwidth);

   await actions.editDeployUnitSettings(t, deployUnitRS);

   await form.deployUnit.antiaffinity.perRack.checkbox.checked(t);
   await form.deployUnit.antiaffinity.perRack.maxPods.typeText(t, antiaffinity.perRack);

   await form.deployUnit.antiaffinity.perNode.checkbox.checked(t);
   await form.deployUnit.antiaffinity.perNode.maxPods.typeText(t, antiaffinity.perNode);

   i = 2;
   for (const { value } of clustersRS) {
      await form.deployUnit.location(value).podCount.readValue(t, String(i));
      await form.deployUnit.location(value).disruptionBudget.readValue(t, String(i - 1));
      i++;
   }

   await form.deployUnit.cpu.value.readValue(t, cpu.value);
   await form.deployUnit.cpu.unit.readValue(t, cpu.unit);

   await form.deployUnit.ram.value.readValue(t, ram.value);
   await form.deployUnit.ram.unit.readValue(t, ram.unit);

   await form.deployUnit.endpointSet(0).port.readValue(t, endpointSetPort);

   await form.deployUnit.formTabs.selectDisks(t);
   await form.deployUnit.disk.type.readValue(t, disk.type);
   await form.deployUnit.disk.size.readValue(t, disk.size);
   await form.deployUnit.disk.bandwidth.guarantee.readValue(t, disk.bandwidth);

   await page.stage.tabs.status.click(t);

   await actions.waitForStageValidation(t);
   await actions.waitForReadyStage(t, '1');

   await actions.deleteTestStage(t);
});
