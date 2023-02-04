import { DEFAULT_MCRS_STAGE_PARAMS, HOST, PROJECT_NAME, STAGE_STATUSES, TIMEOUTS } from '../../helpers/constants';
import { robotRole } from '../../helpers/roles';
import { app } from '../../page_objects/AppNewForm';

const { page, actions } = app;

fixture`Workload settings tests (new)`
   // .only
   .beforeEach(async t => {
      await t.useRole(robotRole);

      await actions.launchTestStage(t);
   })
   .page(HOST);

test('Workload: unistat', async t => {
   const { deployUnitId, boxId, workloadId } = DEFAULT_MCRS_STAGE_PARAMS;
   const unistat = {
      rev1: {
         url: '/unistat-rev1',
         port: '111',
      },
      rev2: {
         url: '/unistat-rev2',
         port: '222',
      },
   };

   await page.stage.formNew.workload.buttons.addUnistat.click(t);

   const yasmForm = page.stage.formNew.workload.yasm;

   await yasmForm.url.typeText(t, unistat.rev1.url);
   await yasmForm.port.typeText(t, unistat.rev1.port);

   await actions.createTestStage(t);

   await actions.editTestStage(t);
   await actions.editWorkloadSettings(t, deployUnitId, boxId, workloadId);

   await yasmForm.url.readValue(t, unistat.rev1.url);
   await yasmForm.port.readValue(t, unistat.rev1.port);

   await yasmForm.url.typeText(t, unistat.rev2.url);
   await yasmForm.port.typeText(t, unistat.rev2.port);

   await actions.updateTestStage(t);

   await actions.editTestStage(t);
   await actions.editWorkloadSettings(t, deployUnitId, boxId, workloadId);

   await yasmForm.url.readValue(t, unistat.rev2.url);
   await yasmForm.port.readValue(t, unistat.rev2.port);

   await yasmForm.url.clearValue(t);
   await yasmForm.port.clearValue(t);

   await actions.updateTestStage(t);

   await actions.editTestStage(t);
   await actions.editWorkloadSettings(t, deployUnitId, boxId, workloadId);

   await yasmForm.url.readValue(t, '');
   await yasmForm.port.readValue(t, '');

   await page.stage.tabs.status.click(t);

   await actions.waitForStageValidation(t);

   // await actions.waitForReadyStage(t, '3');

   await actions.deleteTestStage(t);
});

test('Workload: Yasm tags (itype, tags)', async t => {
   const { deployUnitId, boxId, workloadId } = DEFAULT_MCRS_STAGE_PARAMS;
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

   await page.stage.formNew.workload.buttons.addUnistat.click(t);

   const yasmForm = page.stage.formNew.workload.yasm;
   const yasmTagsForm = yasmForm.yasmTags;

   await yasmTagsForm.itype.readValue(t, PROJECT_NAME);
   await yasmTagsForm.buttons.addTag.exists(t);
   await yasmTagsForm.tag(1).buttons.removeTag.notExists(t);

   await yasmTagsForm.itype.clear(t);

   await yasmTagsForm.buttons.addTag.notExists(t);
   await yasmTagsForm.tag(1).buttons.removeTag.notExists(t);

   await yasmTagsForm.itype.select(t, 'base');
   await yasmTagsForm.buttons.addTag.exists(t);

   await yasmTagsForm.itype.clear(t);
   await yasmTagsForm.buttons.addTag.notExists(t);

   await yasmTagsForm.itype.select(t, yasmTags.itype);
   await yasmTagsForm.buttons.addTag.exists(t);

   await yasmTagsForm.buttons.addTag.click(t);
   await yasmTagsForm.buttons.addTag.click(t);
   await yasmTagsForm.buttons.addTag.click(t);
   await yasmTagsForm.tag(1).buttons.removeTag.exists(t);

   // выбираем теги: первый - из саджеста, два - руками

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

   // REVISION 1 -> 2

   await actions.editTestStage(t);
   await actions.editWorkloadSettings(t, deployUnitId, boxId, workloadId);

   const rev1index0 = await yasmTagsForm.actions.getTagIndex(yasmTags.rev1[0].key);
   const rev1index2 = await yasmTagsForm.actions.getTagIndex(yasmTags.rev1[2].key);

   await yasmTagsForm.tag(rev1index0).value.readValue(t, yasmTags.rev1[0].value);
   await yasmTagsForm.tag(rev1index2).value.readValue(t, yasmTags.rev1[2].value);

   // меняем теги: первый - из саджеста, второй - руками
   await yasmTagsForm.tag(rev1index0).value.change(t, yasmTags.rev1[0].value, yasmTags.rev2[0].value);
   await yasmTagsForm.tag(rev1index2).key.typeText(t, yasmTags.rev2[1].key);
   await yasmTagsForm.tag(rev1index2).value.typeText(t, yasmTags.rev2[1].value);

   await yasmTagsForm.itype.clear(t);
   await yasmTagsForm.itype.readValue(t, '');
   await yasmTagsForm.buttons.addTag.notExists(t);
   await yasmTagsForm.tag(1).buttons.removeTag.notExists(t);
   await yasmTagsForm.tag(2).buttons.removeTag.notExists(t);

   await actions.updateTestStage(t);

   // REVISION 2 -> 3

   await actions.editTestStage(t);
   await actions.editWorkloadSettings(t, deployUnitId, boxId, workloadId);

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

   // REVISION 3 -> 4

   await actions.editTestStage(t);
   await actions.editWorkloadSettings(t, deployUnitId, boxId, workloadId);

   await yasmTagsForm.itype.readValue(t, yasmTags.itype);
   await yasmTagsForm.tag(1).buttons.removeTag.notExists(t);

   await yasmTagsForm.itype.clear(t);
   await yasmTagsForm.itype.readValue(t, '');
   await yasmTagsForm.buttons.addTag.notExists(t);

   await actions.updateTestStage(t);

   // REVISION 4

   await actions.editTestStage(t);
   await actions.editWorkloadSettings(t, deployUnitId, boxId, workloadId);

   await yasmTagsForm.itype.readValue(t, '');
   await yasmTagsForm.buttons.addTag.notExists(t);

   await page.stage.tabs.status.click(t);

   await actions.waitForStageValidation(t);

   // await actions.waitForReadyStage(t, '4');

   await actions.deleteTestStage(t);
});
