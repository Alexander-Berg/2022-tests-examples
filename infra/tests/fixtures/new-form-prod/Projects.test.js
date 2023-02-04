import { t } from 'testcafe';
import { ABC_SERVICE, HOST, ROUTES, STAGE_NAME, TIMEOUTS } from '../../helpers/constants';
import { robotRole } from '../../helpers/roles';
import { app } from '../../page_objects/AppNewForm';

const { page, actions } = app;
const { main, project } = page;

const projectName = STAGE_NAME + '-test-project';

async function deleteProject(projectName) {
   await t.navigateTo(`${HOST}${ROUTES.PROJECT(projectName)}`);

   await t.expect(project.name(projectName).exists).eql(true, { timeout: TIMEOUTS.slow });
   await t.expect(project.emptyContainer.projectWasNotFound.exists).eql(false);

   // Андрей Староверов
   // можешь удалять проект не сразу после создания, а подождать секунд 20-30?
   // 30с точно должно хватить
   // там суть в том, что Auth проиндексировал создание,
   // а когда захотел роль накинуть, то проект уже удалили
   // await t.wait(30000);

   // TODO: сломали noAvailableAcl, пока удаляем с просто с ретраями #DEPLOY-5303
   // console.log('   ...waiting for ACL');
   // await t.expect(project.access.noAvailableAcl.exists).eql(true, '', { timeout: TIMEOUTS.projectAcl });
   // await t.expect(project.access.noAvailableAcl.exists).eql(false, '', { timeout: TIMEOUTS.projectAcl });

   console.log('   ...waiting for deletion: ' + TIMEOUTS.beforeDeleteProject + 'ms');
   await t.wait(TIMEOUTS.beforeDeleteProject);

   const deleteProjectAction = async () => {
      await project.buttons.deleteProject.click(t);
      await project.deallocateConfirmation.modal.exists(t);
      await project.deallocateConfirmation.confirm.check(t);
      await project.deallocateConfirmation.delete.click(t);
   };

   await actions.ypRetry(t, 'Delete Project', deleteProjectAction);

   // TODO: не обновляется список проектов после удаления DEPLOY-5118
   await t.navigateTo(ROUTES.HOME);

   await t.expect(main.body.exists).eql(true);

   await main.filters.type.select(t, 'Project');
   await main.filters.name.typeText(t, projectName);
   await main.filters.buttons.search.click(t);

   await t.expect(main.noProjectsMatchingYourCriteria.exists).eql(true, { timeout: TIMEOUTS.slow });

   await t.navigateTo(`${HOST}${ROUTES.PROJECT(projectName)}`);

   await t.expect(project.emptyContainer.projectWasNotFound.exists).eql(true, { timeout: TIMEOUTS.slow });
   await t.expect(project.name(projectName).exists).eql(false);
   await t.expect(project.buttons.deleteProject.element.exists).eql(false);
}

async function createProject(projectName, withAbcService) {
   await t.navigateTo(`${HOST}${ROUTES.PROJECT(projectName)}`);

   if (await project.name(projectName).exists) {
      await deleteProject(projectName);

      await t.navigateTo(`${HOST}${ROUTES.PROJECT(projectName)}`);
   }

   console.log('   ...waiting for creation: ' + TIMEOUTS.beforeRecreateProject + 'ms');
   await t.wait(TIMEOUTS.beforeRecreateProject);

   await t.expect(project.emptyContainer.projectWasNotFound.exists).eql(true, { timeout: TIMEOUTS.slow });
   await t.expect(project.name(projectName).exists).eql(false);
   await t.expect(project.buttons.deleteProject.element.exists).eql(false);
   await project.emptyContainer.buttons.goToHome.click(t);

   await main.createProject.click(t);

   const projectForm = main.createProjectForm;
   await projectForm.projectName.typeText(t, projectName);

   if (withAbcService === true) {
      await projectForm.abcService.select(t, ABC_SERVICE);
   } else {
      await projectForm.temporaryAccount.check(t);
   }

   await projectForm.buttons.createProject.click(t);

   await t.expect(project.name(projectName).exists).eql(true, { timeout: TIMEOUTS.slow });
   await t.expect(project.emptyContainer.noStagesCreatedYet.exists).eql(true);
   await t.expect(project.buttons.deleteProject.element.exists).eql(true);
   await t.expect(project.emptyContainer.projectWasNotFound.exists).eql(false);
}

fixture`Project tests (new)`
   // .only
   // .skip
   .beforeEach(async t => {
      await t.useRole(robotRole);
   })
   .page(HOST);

test.skip('Create/delete abc project => Cypress tests', async t => {
   // const abcProjectName = projectName + '-abc';

   // await createProject(abcProjectName, true);

   // // TODO: add mainpage filter test

   // await deleteProject(abcProjectName, true);
});

// оторвать права роботу robot-infracloudui, если избавимся от TMP квоты
// https://a.yandex-team.ru/arc/commit/7708683#file-/trunk/arcadia/yp/scripts/initialize_users/__main__.py:L1153
// https://st.yandex-team.ru/YP-2549#5fe35263e254114edf132949
test('Create/delete tmp project', async t => {
   const tmpProjectName = projectName + '-tmp';

   await createProject(tmpProjectName);

   // TODO: add mainpage filter test

   await deleteProject(tmpProjectName);
});
