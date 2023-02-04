import { DEFAULT_MCRS_STAGE_PARAMS, HOST, SERVICE, STAGE_STATUSES, TIMEOUTS } from '../../helpers/constants';
import { defaultOS } from '../../../ui/src/old-code/store/reducers/sandbox.js';

import { app } from '../../page_objects/AppNewForm';
import { robotRole } from '../../helpers/roles';

const { page, actions } = app;

fixture`Box settings tests (new)`
   // .only
   .beforeEach(async t => {
      await t.useRole(robotRole);
      await actions.launchTestStage(t);
   })
   .page(HOST);

// TODO cpu, ram, thread, env

// TODO update from old form to new
test('Box: resolv.conf / update diff', async t => {
   const deployUnit = DEFAULT_MCRS_STAGE_PARAMS.deployUnitId;
   const box1 = 'box'; // default
   const box2 = 'box_clone'; // nat64
   const box3 = 'box_clone_1'; // keep
   const workloadInitialName = 'workload';
   // const box1workload = 'Box1-Workload1';
   const box2workload = 'box_1_workload';
   const box3workload = 'box_2_workload';

   await actions.createTestStage(t);
   await actions.editTestStage(t);

   await actions.editBoxSettings(t, deployUnit, box1);
   await page.stage.formNew.box.buttons.clone.click(t);
   await page.stage.formNew.box.buttons.clone.click(t);

   await page.stage.formNew.sideTree.deployUnit(deployUnit).box(box2).link.click(t);
   await page.stage.formNew.box.resolvConf.change(t, 'default', 'nat64');

   await page.stage.formNew.sideTree.deployUnit(deployUnit).box(box2).workload(workloadInitialName).link.click(t);
   await page.stage.formNew.workload.id.typeText(t, box2workload);
   await page.stage.formNew.workload.commands.start.tab.click(t);
   await page.stage.formNew.workload.commands.start.exec.value.typeText(t, '/simple_http_server 82 "Hello (82)!"');
   await page.stage.formNew.workload.commands.readiness.tab.click(t);
   await page.stage.formNew.workload.commands.readiness.tcp.port.replaceText(t, '80', '82');

   await page.stage.formNew.sideTree.deployUnit(deployUnit).box(box3).link.click(t);
   await page.stage.formNew.box.resolvConf.change(t, 'default', 'keep');

   await page.stage.formNew.sideTree.deployUnit(deployUnit).box(box3).workload(workloadInitialName).link.click(t);
   await page.stage.formNew.workload.id.typeText(t, box3workload);
   await page.stage.formNew.workload.commands.start.tab.click(t);
   await page.stage.formNew.workload.commands.start.exec.value.typeText(t, '/simple_http_server 83 "Hello (83)!"');
   await page.stage.formNew.workload.commands.readiness.tab.click(t);
   await page.stage.formNew.workload.commands.readiness.tcp.port.replaceText(t, '80', '83');

   await actions.updateTestStage(t);

   // await page.stage.form.buttons.updateStage.click(t);
   // await page.stage.form.diff.viewer.fieldNotInserted(t, 'resolv_conf', 'default');
   // await page.stage.form.diff.viewer.fieldInserted(t, 'resolv_conf', 'nat64');
   // await page.stage.form.diff.viewer.fieldInserted(t, 'resolv_conf', 'keep');
   // await actions.deployTestStage(t, '2');

   // await page.stage.tabs.config.click(t);
   // await t.expect(page.stage.config.deployUnit(deployUnit).box(box1).resolvConf('default').exists).eql(false);
   // await t.expect(page.stage.config.deployUnit(deployUnit).box(box2).resolvConf('nat64').exists).eql(true);
   // await t.expect(page.stage.config.deployUnit(deployUnit).box(box3).resolvConf('keep').exists).eql(true);

   await actions.editTestStage(t);
   await page.stage.formNew.sideTree.deployUnit(deployUnit).box(box1).link.click(t);
   await page.stage.formNew.box.resolvConf.readValue(t, 'default');
   await page.stage.formNew.sideTree.deployUnit(deployUnit).box(box2).link.click(t);
   await page.stage.formNew.box.resolvConf.readValue(t, 'nat64');
   await page.stage.formNew.sideTree.deployUnit(deployUnit).box(box3).link.click(t);
   await page.stage.formNew.box.resolvConf.readValue(t, 'keep');

   await page.stage.tabs.status.click(t);

   await actions.waitForStageValidation(t);
   // await actions.waitForReadyStage(t, '2');

   await actions.deleteTestStage(t);
});

// checked
test('Box: juggler test', async t => {
   const { deployUnitId, boxId } = DEFAULT_MCRS_STAGE_PARAMS;
   const juggler = {
      // https://sandbox.yandex-team.ru/resource/1222970334/view
      port: '31579',
      url: 'rbtorrent:00c863bb32fbb9fcd4d22cd3d61df5f29bc846f7',
   };

   await page.stage.formNew.sideTree.deployUnit(deployUnitId).box(boxId).link.click(t);

   const boxFormNew = page.stage.formNew.box;

   await boxFormNew.juggler.mode.change(t, 'Disabled', 'Enabled');
   await boxFormNew.juggler.settings.port.typeText(t, juggler.port);
   await boxFormNew.juggler.settings.buttons.addJugglerBundle.click(t);
   await boxFormNew.juggler.settings.buttons.addJugglerBundle.click(t);
   await boxFormNew.juggler.settings.bundle(1).url.typeText(t, juggler.url);
   // await t.expect(boxFormNew.juggler.settings.error.exists).eql(true);
   await boxFormNew.juggler.settings.bundle(2).buttons.deleteBundle.click(t);
   // await t.expect(boxFormNew.juggler.settings.error.exists).eql(false);

   await actions.createTestStage(t);

   // await t.expect(page.stage.config.deployUnit(deployUnitId).box(boxId).juggler.port(juggler.port).exists).eql(true);
   // await t
   //    .expect(page.stage.config.deployUnit(deployUnitId).box(boxId).juggler.check(1).url(juggler.url).exists)
   //    .eql(true);

   await actions.editTestStage(t);
   await page.stage.formNew.sideTree.deployUnit(deployUnitId).box(boxId).link.click(t);
   await boxFormNew.juggler.mode.readValue(t, 'Enabled');
   await boxFormNew.juggler.settings.port.readValue(t, juggler.port);
   await boxFormNew.juggler.settings.bundle(1).url.readValue(t, juggler.url);

   await page.stage.tabs.status.click(t);

   await actions.waitForStageValidation(t);
   // await actions.waitForReadyStage(t, '1');

   await actions.deleteTestStage(t);
});

//
// checked
test.skip('Box: static resources test => Cypress tests', async t => {
   // const { deployUnitId, boxId, workloadId } = DEFAULT_MCRS_STAGE_PARAMS;
   // const boxCloneId = `${boxId}_clone`;
   // const workloadCloneId = `${workloadId}_clone`;
   // const staticResources = [
   //    {
   //       id: 'FileChecker',
   //       url: 'rbtorrent:8b218e1e8a05c51864ef25ffb99df0d037ce91ad',
   //    },
   //    { id: 'TestStaticResource', url: 'rbtorrent:65bf053ae37cc856992f351242322f3f142a151b' },
   // ];
   // const boxStaticResources = {
   //    [boxId]: [
   //       {
   //          id: staticResources[0].id,
   //          mountPoint: '/box-checker/',
   //       },
   //       {
   //          id: staticResources[1].id,
   //          mountPoint: '/box-data/',
   //       },
   //    ],
   //    [boxCloneId]: [
   //       {
   //          id: staticResources[0].id,
   //          mountPoint: '/box-clone-checker/',
   //       },
   //    ],
   // };
   // await actions.editDeployUnitSettings(t, deployUnitId);
   // await page.stage.formNew.deployUnit.formTabs.selectDisks(t);
   // await page.stage.formNew.deployUnit.disk.staticResources.buttons.addResource.click(t);
   // await page.stage.formNew.deployUnit.disk.staticResources.buttons.addResource.click(t);
   // await page.stage.formNew.deployUnit.disk.staticResources.card(1).id.typeText(t, staticResources[0].id);
   // await page.stage.formNew.deployUnit.disk.staticResources.card(1).url.typeText(t, staticResources[0].url);
   // await page.stage.formNew.deployUnit.disk.staticResources.card(2).id.typeText(t, staticResources[1].id);
   // await page.stage.formNew.deployUnit.disk.staticResources.card(2).url.typeText(t, staticResources[1].url);
   // const boxForm = page.stage.formNew.box;
   // // Box #1
   // await actions.editBoxSettings(t, deployUnitId, boxId);
   // // await page.stage.formNew.sideTree.deployUnit(deployUnitId).box(boxId).link.click(t);
   // await boxForm.formTabs.selectResources(t);
   // await boxForm.staticResources.buttons.addResource.click(t);
   // await boxForm.staticResources.buttons.addResource.click(t);
   // await boxForm.buttons.clone.click(t);
   // await actions.editBoxSettings(t, deployUnitId, boxId);
   // // await page.stage.formNew.sideTree.deployUnit(deployUnitId).box(boxId).link.click(t);
   // await boxForm.formTabs.selectResources(t);
   // // Resource #1
   // await boxForm.staticResources.resource(1).id.select(t, boxStaticResources[boxId][0].id);
   // await boxForm.staticResources.resource(1).mountPoint.typeText(t, boxStaticResources[boxId][0].mountPoint);
   // // Resource #2
   // await boxForm.staticResources.resource(2).id.select(t, boxStaticResources[boxId][1].id);
   // await boxForm.staticResources.resource(2).mountPoint.typeText(t, boxStaticResources[boxId][1].mountPoint);
   // // Rename workload
   // await actions.editWorkloadSettings(t, deployUnitId, boxCloneId, workloadId);
   // // await page.stage.formNew.sideTree.deployUnit(deployUnitId).box(boxCloneId).workload(workloadId).link.click(t);
   // await page.stage.formNew.workload.id.typeText(t, workloadCloneId);
   // // Box #2
   // await actions.editBoxSettings(t, deployUnitId, boxCloneId);
   // // await page.stage.formNew.sideTree.deployUnit(deployUnitId).box(boxCloneId).link.click(t);
   // await boxForm.formTabs.selectResources(t);
   // // Resource #1
   // await boxForm.staticResources.resource(2).id.select(t, boxStaticResources[boxCloneId][0].id);
   // await boxForm.staticResources.resource(2).mountPoint.typeText(t, boxStaticResources[boxCloneId][0].mountPoint);
   // // Resource #1
   // await boxForm.staticResources.resource(1).buttons.deleteStaticResource.click(t);
   // await actions.createTestStage(t);
   // await actions.editTestStage(t);
   // // Box #1
   // await actions.editBoxSettings(t, deployUnitId, boxId);
   // // await page.stage.formNew.sideTree.deployUnit(deployUnitId).box(boxId).link.click(t);
   // await boxForm.formTabs.selectResources(t);
   // // Resource #1
   // await boxForm.staticResources.resource(1).id.readValue(t, boxStaticResources[boxId][0].id);
   // await boxForm.staticResources.resource(1).mountPoint.readValue(t, boxStaticResources[boxId][0].mountPoint);
   // // Resource #2
   // await boxForm.staticResources.resource(2).id.readValue(t, boxStaticResources[boxId][1].id);
   // await boxForm.staticResources.resource(2).mountPoint.readValue(t, boxStaticResources[boxId][1].mountPoint);
   // // Box #2
   // await actions.editBoxSettings(t, deployUnitId, boxCloneId);
   // // await page.stage.formNew.sideTree.deployUnit(deployUnitId).box(boxCloneId).link.click(t);
   // await boxForm.formTabs.selectResources(t);
   // // Resource #1
   // await boxForm.staticResources.resource(1).id.readValue(t, boxStaticResources[boxCloneId][0].id);
   // await boxForm.staticResources.resource(1).mountPoint.readValue(t, boxStaticResources[boxCloneId][0].mountPoint);
   // // await actions.deleteTestStage(t);
});

test.skip('Boxes, Layers, Docker (new) => Cypress tests', async t => {
   // const testID = new Date().getTime();

   // // const defaultOS = ['xenial-app', 'bionic-app']
   // // в тестах ui наверно есть смысл использовать самые неэкзотические, т.е. xenial-app и м.б. bionic-app (c)
   // const layers = defaultOS;

   // const { deployUnitId, boxId, workloadId } = DEFAULT_MCRS_STAGE_PARAMS;

   // const box2 = {
   //    id: 'box_1',
   // };

   // const box3 = {
   //    id: 'box_1_clone',
   // };

   // const box4 = {
   //    id: 'box_1_clone_1',
   //    docker: {
   //       name: 'deploy.ui/deploy-ui',
   //       tag: '0.0.100',
   //    },
   // };

   // const workload2 = {
   //    id: 'workload_1',
   //    start: `/simple_http_server 8228 'Test #${testID}'`,
   //    liveness: {
   //       type: 'HTTP',
   //       port: '8228',
   //       answer: `Test #${testID}`,
   //    },
   //    readiness: {
   //       type: 'HTTP',
   //       port: '8228',
   //       answer: `Test #${testID}`,
   //    },
   // };

   // const workload3 = {
   //    id: 'workload_1_clone',
   //    start: `/simple_http_server 8338 'Test #${testID}'`,
   //    liveness: {
   //       type: 'TCP',
   //       port: '8338',
   //    },
   //    readiness: {
   //       type: 'TCP',
   //       port: '8338',
   //    },
   // };

   // const workload4 = {
   //    id: 'workload_1_clone_1',
   //    start: '/bin/bash /run.sh',
   //    init: ['bash -c \'printf "$RUN" > ./run.sh\'', 'bash -c \'printf "init test" > ./init-test\''],
   //    literal: {
   //       name: 'RUN',
   //       value: 'export CONSOLE_ENV=ext-test; cd /opt/app; /usr/bin/supervisord -c /etc/supervisor/supervisord.conf',
   //    },
   //    liveness: {
   //       type: 'TCP',
   //       port: '80',
   //    },
   //    readiness: {
   //       type: 'TCP',
   //       port: '80',
   //    },
   // };

   // const user = 'root';
   // const group = 'root';

   // await actions.editDeployUnitSettings(t, deployUnitId);

   // await page.stage.formNew.deployUnit.formTabs.selectDisks(t);

   // await page.stage.formNew.deployUnit.disk.layers.buttons.addLayer.click(t);
   // await page.stage.formNew.deployUnit.disk.layers.buttons.addLayer.click(t);

   // await page.stage.formNew.deployUnit.disk.layers.card(4).id.typeText(t, layers[1].title);
   // await page.stage.formNew.deployUnit.disk.layers.card(4).url.typeText(t, layers[1].value);

   // await page.stage.formNew.deployUnit.disk.layers.card(3).id.typeText(t, layers[0].title);
   // await page.stage.formNew.deployUnit.disk.layers.card(3).url.typeText(t, layers[0].value);

   // await page.stage.formNew.deployUnit.disk.layers.card(1).buttons.deleteLayer.click(t);

   // await page.stage.formNew.sideTree.deployUnit(deployUnitId).buttons.addBox.click(t);

   // await actions.editBoxSettings(t, deployUnitId, boxId);

   // await page.stage.formNew.box.buttons.remove.click(t);

   // await actions.editWorkloadSettings(t, deployUnitId, box2.id, workloadId);

   // await page.stage.formNew.workload.id.typeText(t, workload2.id);

   // await page.stage.formNew.workload.commands.start.tab.click(t);
   // await page.stage.formNew.workload.commands.start.exec.value.typeText(t, workload2.start);
   // await page.stage.formNew.workload.commands.start.exec.buttons.showAdvancedSettings.click(t);
   // await page.stage.formNew.workload.commands.start.exec.user.typeText(t, user);
   // await page.stage.formNew.workload.commands.start.exec.group.typeText(t, group);

   // await page.stage.formNew.workload.commands.liveness.tab.click(t);
   // await page.stage.formNew.workload.commands.liveness.type.select(t, workload2.liveness.type);
   // await page.stage.formNew.workload.commands.liveness.http.port.typeText(t, workload2.liveness.port);
   // await page.stage.formNew.workload.commands.liveness.http.answer.expected.typeText(t, workload2.liveness.answer);

   // await page.stage.formNew.workload.commands.readiness.tab.click(t);
   // await page.stage.formNew.workload.commands.readiness.type.select(t, workload2.readiness.type);
   // await page.stage.formNew.workload.commands.readiness.http.port.typeText(t, workload2.readiness.port);
   // await page.stage.formNew.workload.commands.readiness.http.answer.expected.typeText(t, workload2.readiness.answer);

   // await page.stage.formNew.workload.logs.select(t, 'Disabled');

   // await actions.editBoxSettings(t, deployUnitId, box2.id);

   // await page.stage.formNew.box.formTabs.selectResources(t);

   // // new box without default layers
   // await page.stage.formNew.box.layers.buttons.addLayer.click(t);
   // await page.stage.formNew.box.layers.buttons.addLayer.click(t);

   // await page.stage.formNew.box.layers.layer(1).id.select(t, layers[0].title);

   // await page.stage.formNew.box.buttons.clone.click(t);

   // await page.stage.formNew.box.formTabs.selectResources(t);
   // await page.stage.formNew.box.layers.layer(1).id.select(t, layers[1].title);

   // await actions.editWorkloadSettings(t, deployUnitId, box3.id, workload2.id);

   // await page.stage.formNew.workload.id.typeText(t, workload3.id);

   // await page.stage.formNew.workload.commands.start.tab.click(t);
   // await page.stage.formNew.workload.commands.start.exec.value.typeText(t, workload3.start);

   // await page.stage.formNew.workload.commands.liveness.tab.click(t);
   // await page.stage.formNew.workload.commands.liveness.type.select(t, workload3.liveness.type);
   // await page.stage.formNew.workload.commands.liveness.tcp.port.typeText(t, workload3.liveness.port);

   // await page.stage.formNew.workload.commands.readiness.tab.click(t);
   // await page.stage.formNew.workload.commands.readiness.type.select(t, workload3.readiness.type);
   // await page.stage.formNew.workload.commands.readiness.tcp.port.typeText(t, workload3.readiness.port);

   // await actions.editBoxSettings(t, deployUnitId, box3.id);

   // await page.stage.formNew.box.buttons.clone.click(t);

   // await page.stage.formNew.box.formTabs.selectResources(t);
   // await page.stage.formNew.box.dockerImage.enabled.select(t, 'Enabled');
   // await page.stage.formNew.box.dockerImage.name.typeText(t, box4.docker.name);
   // await page.stage.formNew.box.dockerImage.tag.typeText(t, box4.docker.tag);

   // await page.stage.formNew.box.layers.layer(2).buttons.deleteLayer.click(t);
   // await page.stage.formNew.box.layers.layer(1).buttons.deleteLayer.click(t);

   // await actions.editWorkloadSettings(t, deployUnitId, box4.id, workload3.id);

   // await page.stage.formNew.workload.id.typeText(t, workload4.id);

   // await page.stage.formNew.workload.commands.start.tab.click(t);
   // await page.stage.formNew.workload.commands.start.exec.value.typeText(t, workload4.start);

   // await page.stage.formNew.workload.commands.init.tab(0).click(t);
   // await page.stage.formNew.workload.commands.init.exec(0).value.typeText(t, workload4.init[0]);
   // await page.stage.formNew.workload.commands.init.exec(0).buttons.showAdvancedSettings.click(t);
   // await page.stage.formNew.workload.commands.init.exec(0).user.typeText(t, user);
   // await page.stage.formNew.workload.commands.init.exec(0).group.typeText(t, group);

   // await page.stage.formNew.workload.commands.buttons.addInitCommand.click(t);
   // await page.stage.formNew.workload.commands.init.tab(1).click(t);
   // await page.stage.formNew.workload.commands.init.buttons.deleteCommand(1).click(t);
   // await page.stage.formNew.workload.commands.buttons.addInitCommand.click(t);

   // await page.stage.formNew.workload.commands.init.tab(1).click(t);

   // await page.stage.formNew.workload.commands.init.exec(1).value.typeText(t, workload4.init[1]);
   // await page.stage.formNew.workload.commands.init.exec(1).buttons.showAdvancedSettings.click(t);
   // await page.stage.formNew.workload.commands.init.exec(1).user.typeText(t, user);
   // await page.stage.formNew.workload.commands.init.exec(1).group.typeText(t, group);

   // await page.stage.formNew.workload.commands.liveness.tab.click(t);
   // await page.stage.formNew.workload.commands.liveness.tcp.port.replaceText(
   //    t,
   //    workload3.liveness.port,
   //    workload4.liveness.port,
   // );

   // await page.stage.formNew.workload.commands.readiness.tab.click(t);
   // await page.stage.formNew.workload.commands.readiness.tcp.port.replaceText(
   //    t,
   //    workload3.readiness.port,
   //    workload4.readiness.port,
   // );

   // await page.stage.formNew.workload.variables.actions.addLiteralVariable(
   //    t,
   //    workload4.literal.name,
   //    workload4.literal.value,
   // );

   // await actions.createTestStage(t);

   // await actions.editTestStage(t);

   // await actions.editBoxSettings(t, deployUnitId, box2.id);

   // await page.stage.formNew.box.formTabs.selectResources(t);
   // await page.stage.formNew.box.layers.layer(1).id.readValue(t, layers[0].title);

   // await actions.editBoxSettings(t, deployUnitId, box3.id);

   // await page.stage.formNew.box.formTabs.selectResources(t);
   // await page.stage.formNew.box.layers.layer(1).id.readValue(t, layers[1].title);

   // await actions.editBoxSettings(t, deployUnitId, box4.id);

   // await page.stage.formNew.box.formTabs.selectResources(t);
   // await page.stage.formNew.box.dockerImage.enabled.readValue(t, 'Enabled');
   // await page.stage.formNew.box.dockerImage.name.readValue(t, box4.docker.name);
   // await page.stage.formNew.box.dockerImage.tag.readValue(t, box4.docker.tag);

   // await actions.editWorkloadSettings(t, deployUnitId, box2.id, workload2.id);

   // await page.stage.formNew.workload.commands.start.tab.click(t);
   // await page.stage.formNew.workload.commands.start.exec.value.readValue(t, workload2.start);
   // await page.stage.formNew.workload.commands.start.exec.buttons.showAdvancedSettings.click(t);
   // await page.stage.formNew.workload.commands.start.exec.user.readValue(t, user);
   // await page.stage.formNew.workload.commands.start.exec.group.readValue(t, group);

   // await page.stage.formNew.workload.commands.liveness.tab.click(t);
   // await page.stage.formNew.workload.commands.liveness.type.readValue(t, workload2.liveness.type);
   // await page.stage.formNew.workload.commands.liveness.http.port.readValue(t, workload2.liveness.port);
   // await page.stage.formNew.workload.commands.liveness.http.answer.expected.readValue(t, workload2.liveness.answer);

   // await page.stage.formNew.workload.commands.readiness.tab.click(t);
   // await page.stage.formNew.workload.commands.readiness.type.readValue(t, workload2.readiness.type);
   // await page.stage.formNew.workload.commands.readiness.http.port.readValue(t, workload2.readiness.port);
   // await page.stage.formNew.workload.commands.readiness.http.answer.expected.readValue(t, workload2.readiness.answer);

   // await actions.editWorkloadSettings(t, deployUnitId, box3.id, workload3.id);

   // await page.stage.formNew.workload.commands.start.tab.click(t);
   // await page.stage.formNew.workload.commands.start.exec.value.readValue(t, workload3.start);

   // await page.stage.formNew.workload.commands.liveness.tab.click(t);
   // await page.stage.formNew.workload.commands.liveness.type.readValue(t, workload3.liveness.type);
   // await page.stage.formNew.workload.commands.liveness.tcp.port.readValue(t, workload3.liveness.port);

   // await page.stage.formNew.workload.commands.readiness.tab.click(t);
   // await page.stage.formNew.workload.commands.readiness.type.readValue(t, workload3.readiness.type);
   // await page.stage.formNew.workload.commands.readiness.tcp.port.readValue(t, workload3.readiness.port);

   // await actions.editWorkloadSettings(t, deployUnitId, box4.id, workload4.id);

   // await page.stage.formNew.workload.commands.start.tab.click(t);
   // await page.stage.formNew.workload.commands.start.exec.value.readValue(t, workload4.start);

   // await page.stage.formNew.workload.commands.init.tab(0).click(t);

   // await page.stage.formNew.workload.commands.init.exec(0).value.readValue(t, workload4.init[0]);
   // await page.stage.formNew.workload.commands.init.exec(0).buttons.showAdvancedSettings.click(t);
   // await page.stage.formNew.workload.commands.init.exec(0).user.readValue(t, user);
   // await page.stage.formNew.workload.commands.init.exec(0).group.readValue(t, group);

   // await page.stage.formNew.workload.commands.init.tab(1).click(t);

   // await page.stage.formNew.workload.commands.init.exec(1).value.readValue(t, workload4.init[1]);
   // await page.stage.formNew.workload.commands.init.exec(1).buttons.showAdvancedSettings.click(t);
   // await page.stage.formNew.workload.commands.init.exec(1).user.readValue(t, user);
   // await page.stage.formNew.workload.commands.init.exec(1).group.readValue(t, group);

   // await page.stage.formNew.workload.commands.liveness.tab.click(t);
   // await page.stage.formNew.workload.commands.liveness.type.readValue(t, workload4.liveness.type);
   // await page.stage.formNew.workload.commands.liveness.tcp.port.readValue(t, workload4.liveness.port);

   // await page.stage.formNew.workload.commands.readiness.tab.click(t);
   // await page.stage.formNew.workload.commands.readiness.type.readValue(t, workload4.readiness.type);
   // await page.stage.formNew.workload.commands.readiness.tcp.port.readValue(t, workload4.readiness.port);

   // await page.stage.tabs.status.click(t);

   // await actions.waitForStageValidation(t);
   // await actions.waitForReadyStage(t, '1');

   // // await actions.deleteTestStage(t);
});
