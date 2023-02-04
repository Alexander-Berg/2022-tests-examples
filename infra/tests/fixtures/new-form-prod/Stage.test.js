import { HOST, PROJECT_NAME, ROUTES } from '../../helpers/constants';
import { robotRole } from '../../helpers/roles';
import { app } from '../../page_objects/AppNewForm';

const { page, actions } = app;
const statusPage = app.page.stage.status;

fixture`Stage smoke tests`
   // .only
   .beforeEach(async t => {
      await t.useRole(robotRole);

      await actions.sureTestStageExists(t);
   })
   .page(HOST);

test.skip('Stage tabs => Cypress', async t => {
   // Config
   await page.stage.tabs.config.click(t);
   await t.expect(page.stage.config.body.exists).ok();
   await t.expect(page.stage.config.getLocation()).eql(ROUTES.STAGE_CONFIG);

   // Logs
   await page.stage.tabs.logs.click(t);
   await t.expect(page.stage.logs.body.exists).ok();
   await t.expect(page.stage.logs.getLocation()).eql(ROUTES.STAGE_LOGS);

   // Monitoring
   await page.stage.tabs.monitoring.click(t);
   await t.expect(page.stage.monitoring.body.exists).ok();
   await t.expect(page.stage.monitoring.getLocation()).eql(ROUTES.STAGE_MONITORING);

   // History
   await page.stage.tabs.history.click(t);
   await t.expect(page.stage.history.body.exists).ok();
   await t.expect(page.stage.history.getLocation()).eql(ROUTES.STAGE_HISTORY);

   // Deploy tickets
   await page.stage.tabs.tickets.click(t);
   await t.expect(page.stage.tickets.body.exists).ok();
   await t.expect(page.stage.tickets.getLocation()).eql(ROUTES.STAGE_TICKETS);

   // Balancers
   await page.stage.tabs.balancers.click(t);
   await t.expect(page.stage.balancers.body.exists).ok();
   await t.expect(page.stage.balancers.getLocation()).eql(ROUTES.STAGE_BALANCERS);

   // Status (last, because it's need time to validate spec)
   await page.stage.tabs.status.click(t);
   await page.stage.status.checkLocation(t);
});

// test.skip('Stage breadcrumbs', async t => {
//    await t.click(page.main.project);
//
//    await page.stage.breadcrumbs.all.click(t);
//    // await t.expect(page.main.body.exists).ok();
//    // await t.expect(page.main.stage.status('Ready').exists).ok('', { timeout: TIMEOUTS.stageReadyAfterSave });
// });

test.skip('Stage edit => Cypress', async t => {
   await page.stage.buttons.editStage.click(t);

   await t.expect(page.stage.config.getLocation()).eql(ROUTES.STAGE_EDIT);
});

test('Stage delete', async t => {
   const stageActionsPopupButton = page.stage.buttons.stageActionsPopup;
   const deleteButton = page.stage.buttons.deallocateStage;
   const removeModal = page.stage.removeModal;

   // close button (in header)
   await stageActionsPopupButton.click(t);
   await deleteButton.click(t);
   await removeModal.exists(t);
   await removeModal.closeButton.click(t);
   await removeModal.notExists(t);

   // cancel button (in footer)
   // сломан stageActionsPopup #DEPLOY-5045
   // await stageActionsPopupButton.click(t);
   await deleteButton.click(t);
   await removeModal.exists(t);
   await removeModal.cancelButton.click(t);
   await removeModal.notExists(t);

   // ok button
   // сломан stageActionsPopup #DEPLOY-5045
   // await stageActionsPopupButton.click(t);
   await deleteButton.click(t);
   await removeModal.confirm(t);
   await removeModal.notExists(t);

   await t.expect(page.project.getLocation()).eql(ROUTES.PROJECT(PROJECT_NAME));
});
