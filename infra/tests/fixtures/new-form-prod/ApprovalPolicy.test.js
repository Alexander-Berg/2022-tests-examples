import { HOST, ROUTES, TIMEOUTS } from '../../helpers/constants';
import { robotRole } from '../../helpers/roles';
import { app } from '../../page_objects/AppNewForm';

const { page, actions } = app;

fixture`Approval Policy tests (new)`
   // .only
   // .skip
   .beforeEach(async t => {
      await t.useRole(robotRole);
      await actions.launchTestStage(t);
   })
   .page(HOST);

test('Approval Policy: Create, Update, Delete', async t => {
   const tickets = page.stage.tickets;
   const approvalPolicy = tickets.approvalPolicy;

   await actions.createTestStage(t);

   await t.wait(TIMEOUTS.waitStageAcl); // чтобы проросли права

   await page.stage.tabs.tickets.click(t);

   await t.expect(tickets.getLocation()).contains(ROUTES.STAGE_TICKETS);
   await t.expect(tickets.body.exists).eql(true);

   await tickets.buttons.approvalPolicy.click(t);

   await t.expect(approvalPolicy.body.exists).eql(true);

   const actionCreateApprovalPolicy = async () => {
      await approvalPolicy.actions.init(t);
      await t.expect(approvalPolicy.form.body.exists).eql(true);
      await approvalPolicy.form.count.typeText(t, '1');
      await approvalPolicy.actions.create(t);
   };

   const actionUpdateApprovalPolicy = async () => {
      // await approvalPolicy.actions.update(t);
      await approvalPolicy.actions.confirmUpdating(t);
   };

   const actionDeleteApprovalPolicy = async () => {
      // await approvalPolicy.actions.delete(t);
      await approvalPolicy.actions.confirmDeleting(t);
   };

   await actions.ypRetry(t, 'Create Approval policy', actionCreateApprovalPolicy);

   await t.expect(approvalPolicy.form.body.exists).eql(true);

   await tickets.approvalPolicy.actions.close(t);
   await t.expect(approvalPolicy.body.exists).eql(false);

   await tickets.buttons.approvalPolicy.click(t);
   await t.expect(approvalPolicy.body.exists).eql(true);
   await approvalPolicy.form.count.replaceText(t, '1', '2');
   await approvalPolicy.actions.update(t);
   // await approvalPolicy.actions.confirmUpdating(t);
   await actions.ypRetry(t, 'Update Approval policy', actionUpdateApprovalPolicy);
   await t.expect(approvalPolicy.form.body.exists).eql(true);

   await approvalPolicy.actions.delete(t);
   // await approvalPolicy.actions.confirmDeleting(t);
   await actions.ypRetry(t, 'Delete Approval policy', actionDeleteApprovalPolicy);
   await t.expect(approvalPolicy.form.body.exists).eql(false);

   await actions.deleteTestStage(t);
});
