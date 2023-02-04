import { HOST, ROUTES, TIMEOUTS, DEFAULT_MCRS_STAGE_PARAMS } from '../../helpers/constants';
import { robotRole } from '../../helpers/roles';
import { app } from '../../page_objects/AppNewForm';

const { page, actions } = app;

fixture`Deploy tickets tests (new)`
   // .only
   // .skip
   .beforeEach(async t => {
      await t.useRole(robotRole);
      await actions.launchTestStage(t);
   })
   .page(HOST);

test('Deploy tickets for draft', async t => {
   await actions.createTestStage(t);

   await t.wait(TIMEOUTS.waitStageAcl); // чтобы проросли права

   await page.stage.tabs.tickets.click(t);

   const tickets = page.stage.tickets;

   await t.expect(tickets.body.exists).ok('', { timeout: TIMEOUTS.slow });
   await t.expect(tickets.getLocation()).contains(ROUTES.STAGE_TICKETS);

   await tickets.buttons.approvalPolicy.click(t);

   const approvalPolicy = tickets.approvalPolicy;

   const actionCreateApprovalPolicy = async () => {
      await approvalPolicy.actions.create(t);
   };

   await t.expect(approvalPolicy.body.exists).ok('', { timeout: TIMEOUTS.slow });

   await approvalPolicy.actions.init(t);
   await t.expect(approvalPolicy.form.body.exists).ok('', { timeout: TIMEOUTS.slow });
   await approvalPolicy.form.count.typeText(t, '1');

   await actions.ypRetry(t, 'Create Approval policy', actionCreateApprovalPolicy);

   await t.expect(approvalPolicy.form.body.exists).ok('', { timeout: TIMEOUTS.slow });

   await tickets.approvalPolicy.actions.close(t);
   await t.expect(approvalPolicy.body.exists).notOk();

   await actions.editTestStage(t);

   await page.stage.formNew.buttons.update.click(t);

   await page.stage.formNew.diff.description.exists(t);
   await page.stage.formNew.diff.description.typeText(t, 'testcafe create draft');

   const actionSaveDraft = async () => {
      await page.stage.formNew.diff.buttons.saveDraft.click(t);
   };

   await actions.ypRetry(t, 'Deploy draft', actionSaveDraft);

   const ticket = tickets.ticket;

   const actionApproveTicket = async () => {
      await ticket.actions.approve(t);
   };

   const actionCommitTicket = async () => {
      await ticket.actions.commit(t);
   };

   await t.expect(ticket.body.exists).ok('', { timeout: TIMEOUTS.slow });
   await t.expect(ticket.status.withExactText('Waiting for approve').exists).ok();

   await actions.ypRetry(t, 'Approve ticket', actionApproveTicket);
   await t.expect(ticket.status.withExactText('Waiting for commit').exists).ok('', { timeout: TIMEOUTS.slow });

   await actions.ypRetry(t, 'Commit ticket', actionCommitTicket);
   await t.expect(ticket.status.withExactText('Committed').exists).ok('', { timeout: TIMEOUTS.slow });

   await page.stage.tabs.status.click(t);

   await actions.waitForStageValidation(t);
   await actions.waitForReadyStage(t, '2');

   await actions.deleteTestStage(t);
});

test('Release rules', async t => {
   const { deployUnitId, boxId } = DEFAULT_MCRS_STAGE_PARAMS;

   const staticResource = {
      id: 'my-static',
      url: 'sbr:755375039',
      mountPoint: '/tmp/static',
   };

   const releaseRule = {
      taskType: 'SAMPLE_RELEASE_TO_YA_DEPLOY_2',
      releaseTypes: ['stable', 'prestable'],
      static: {
         type: 'SAMPLE_RELEASE_INTEGRATION_STATIC',
         target: 'my-static',
         name: 'patch-my-static',
      },
      // layer: {
      //    type: 'SAMPLE_RELEASE_INTEGRATION_LAYER',
      //    target: 'my-layer',
      //    name: 'patch-my-layer',
      // },
   };

   await actions.createTestStage(t);

   await t.wait(TIMEOUTS.waitStageAcl); // чтобы проросли права

   await actions.editTestStage(t);

   await actions.editDeployUnitSettings(t, deployUnitId);

   await page.stage.formNew.deployUnit.formTabs.selectDisks(t);

   await page.stage.formNew.deployUnit.disk.staticResources.buttons.addResource.click(t);
   await page.stage.formNew.deployUnit.disk.staticResources.card(1).id.typeText(t, staticResource.id);
   await page.stage.formNew.deployUnit.disk.staticResources.card(1).url.typeText(t, staticResource.url);

   await actions.editBoxSettings(t, deployUnitId, boxId);
   await page.stage.formNew.box.formTabs.selectResources(t);

   await page.stage.formNew.box.staticResources.buttons.addResource.click(t);
   await page.stage.formNew.box.staticResources.resource(1).id.select(t, staticResource.id);
   await page.stage.formNew.box.staticResources.resource(1).mountPoint.typeText(t, staticResource.mountPoint);

   await actions.updateTestStage(t);

   await page.stage.tabs.tickets.click(t);

   const tickets = page.stage.tickets;

   await tickets.releaseRule.select.click(t);
   await tickets.releaseRule.actions.newSandboxRule.click(t);

   await tickets.releaseRule.body.releaseTypes.select(t, releaseRule.releaseTypes, 'multiple');
   await tickets.releaseRule.body.taskType.typeText(t, releaseRule.taskType);

   await tickets.releaseRule.body.buttons.addPatch.click(t);

   await tickets.releaseRule.body.resource(2).resourceType.typeText(t, releaseRule.static.type);
   await tickets.releaseRule.body.resource(2).resourceTarget.select(releaseRule.static.target);
   await tickets.releaseRule.body.resource(2).name.typeText(t, releaseRule.static.name);

   await tickets.releaseRule.body.resource(1).deleteButton.click(t);

   await tickets.releaseRule.body.resource(1).resourceType.readValue(t, releaseRule.static.type);

   // await tickets.releaseRule.body.resource(1).resourceType.typeText(t, releaseRule.layer.target);
   // await tickets.releaseRule.body.resource(1).resourceTarget.select(releaseRule.layer.type);
   // await tickets.releaseRule.body.resource(1).name.typeText(t, releaseRule.layer.name);

   await tickets.releaseRule.body.buttons.create.click(t);

   await page.stage.tabs.status.click(t);

   await actions.waitForStageValidation(t);
   await actions.waitForReadyStage(t, '2');

   await actions.deleteTestStage(t);
});
