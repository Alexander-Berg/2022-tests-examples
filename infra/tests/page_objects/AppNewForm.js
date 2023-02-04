import { ClientFunction, Selector } from 'testcafe';

import {
   DEFAULT_MCRS_STAGE_PARAMS,
   DISABLED_LOCATIONS,
   NETWORK,
   PROJECT_NAME,
   ROBOT_LOGIN,
   ROUTES,
   SERVICE,
   STAGE_NAME,
   STAGE_STATUSES,
   TIMEOUTS,
} from '../helpers/constants';
import { SelectDataE2e, SelectDataQa, SelectDataTest } from '../helpers/extractors';
import { getRawConfig } from '../helpers/variableGetters';
import { Button } from './components/Button';
import { Header } from './components/Header';
import { Link } from './components/Link';
import { RemoveStageModal } from './modals/RemoveStageModal';
import { PageMain } from './PageMain';
import { PageProject } from './PageProject';
import { PageStageBalancers } from './PageStageBalancers';
import { PageStageConfig } from './PageStageConfig';
import { PageStageFormNew } from './PageStageFormNew';
import { PageStageLogs } from './PageStageLogs';
import { PageStageMonitoring } from './PageStageMonitoring';
import { PageStageStatus } from './PageStageStatus';
import { PageStageTickets } from './PageStageTickets';

import { StageTabs } from './StageTabs';

const setLocalStorageItem = ClientFunction((prop, value) => {
   localStorage.setItem(prop, value);
});

const { deployUnitId, boxId, workloadId } = DEFAULT_MCRS_STAGE_PARAMS;

/**
 * Класс, описывающий и реализующий функционал нашего веб-приложения.
 * Предоставляет готовые селекторы отдельных страниц и элементов приложения (формы, навигация, табы и пр.)
 * Реализует часто используемые действия, которые совершает пользователь (создание, изменение, удаление стейджа)
 *
 * @class App
 */
class App {
   header = new Header();

   page = {
      main: new PageMain(),

      project: new PageProject(),

      stage: {
         // TODO
         breadcrumbs: {
            all: new Link(SelectDataTest('breadcrumbs').find('a').withText('All')),
            stage: new Link(SelectDataTest('breadcrumbs').find('a').withText(STAGE_NAME)),
         },
         buttons: {
            editStage: new Link(SelectDataQa('EditStageButton')),
            deallocateStage: new Button(SelectDataE2e('RemoveStageButton')),
            stageActionsPopup: new Button(SelectDataE2e('StageActionsPopupButton')),
         },
         removeModal: new RemoveStageModal(),

         // stage navigation
         tabs: new StageTabs(),

         // stage pages
         status: new PageStageStatus(),
         formNew: new PageStageFormNew(),
         config: new PageStageConfig(),
         logs: new PageStageLogs(),
         monitoring: new PageStageMonitoring(),
         // history: new PageStageHistory(), // новые тесты истории в cypress
         balancers: new PageStageBalancers(),
         tickets: new PageStageTickets(),
         // ...
      },
   };

   notification = {
      buttons: {
         close: new Button(
            Selector('.notifications-wrapper').find('.simple-notification').find('button.simple-notification__close'),
         ),
      },

      title: title =>
         Selector('.notifications-wrapper')
            .find('.simple-notification')
            .find('.simple-notification__title')
            .withText(title),
      text: text =>
         Selector('.notifications-wrapper')
            .find('.simple-notification')
            .find('.simple-notification__text')
            .withText(text),
      json: Selector('.notifications-wrapper').find('.simple-notification').find('[data-test="yt-error"]'),
      // json: Selector('.notifications-wrapper').find('.simple-notification').find('.json'),
   };

   actions = {
      ypRetry: async (t, name, action, retry) => {
         await action();

         await t.wait(5000);

         let i = 0;
         while ((await this.notification.json.exists) && i < 20) {
            console.log(`   ...${name || 'YP request'} #${i} is failed`);
            await this.notification.buttons.close.click(t);

            if (retry) {
               await retry();
            } else {
               await action();
            }

            await t.wait(5000);
            i++;
         }
      },

      // TODO rename to fillNewStageForm
      launchTestStage: async t => {
         const DEPLOY_UNIT_NAME = 'deployUnit';
         await t.navigateTo(ROUTES.STAGE_STATUS);
         // await t.expect(this.page.stage.status.spinner.exists).eql(false, '', { timeout: 120000 });

         if (await this.page.stage.status.body.exists) {
            await this.actions.deleteTestStage(t);
            await this.page.project.buttons.createStage.click(t);
         } else {
            await this.page.stage.config.emptyContainer.buttons.launchNewStage.click(t);
         }

         // прячем промо тултипы, чтобы не мешали
         await setLocalStorageItem('promo.visible.du.tabs.disks', 'false');
         await setLocalStorageItem('promo.visible.du.tabs.disks.resources', 'false');
         await setLocalStorageItem('promo.visible.box.tabs.resources', 'false');

         await this.page.stage.formNew.stage.id.readValue(t, `${ROBOT_LOGIN}-stage`);

         // DEPLOY-5640
         await t.navigateTo(ROUTES.NEW_STAGE(PROJECT_NAME));

         await this.page.stage.formNew.stage.id.typeText(t, STAGE_NAME);
         await this.page.stage.formNew.stage.project.readValue(t, PROJECT_NAME); // должен прилетать из урла #DEPLOY-5640
         await this.page.stage.formNew.sideTree.deployUnit(DEPLOY_UNIT_NAME).link.click(t);
         await this.page.stage.formNew.deployUnit.id.typeText(t, deployUnitId);
         await this.page.stage.formNew.deployUnit.network.select(t, NETWORK);

         await this.page.stage.formNew.deployUnit.primitive.change(
            t,
            'Per-cluster replica set',
            'Multi-cluster replica set',
         );

         const config = await getRawConfig();
         const clusters = config.clusters.filter(v => !DISABLED_LOCATIONS.includes(v.value));

         await this.page.stage.formNew.deployUnit.disruptionBudget.typeText(t, String(clusters.length - 1));
         for (const { value } of clusters) {
            const location = await this.page.stage.formNew.deployUnit.location(value);

            try {
               await location.enabled.unchecked(t);
               await location.enabled.check(t);
            } catch (e) {
               // console.log(value + ' checked already');
            }

            await location.podCount.typeText(t, '1');
         }

         await this.page.stage.formNew.deployUnit.formTabs.selectDisks(t);
         await this.page.stage.formNew.deployUnit.disk.bandwidth.guarantee.typeText(t, '1');

         // await this.page.stage.formNew.deployUnit.tabs.select(t, 'Disks and volumes')
         // await this.page.stage.formNew.deployUnit.disk.bandwidth.guarantee.readValue(t, '15'); // default value

         await this.page.stage.formNew.sideTree
            .deployUnit(DEFAULT_MCRS_STAGE_PARAMS.deployUnitId)
            .box(boxId)
            .workload(workloadId)
            .link.click(t);
         await this.page.stage.formNew.workload.logs.change(t, 'Enabled', 'Disabled');
      },

      createTestStage: async t => {
         await this.page.stage.formNew.buttons.update.click(t);

         // @eugene_belyakov:
         // https://st.yandex-team.ru/DEPLOY-4683
         // прямо сейчас делаю
         // Workaround: удалить стейдж. Подождать минуту, создать снова - должно отпустить.
         // (проблема в быстром Удалении + Создании)
         console.log('   ...waiting for recreation #DEPLOY-4683');
         await t.wait(TIMEOUTS.beforeRecreateStage);

         try {
            await this.page.stage.formNew.diff.buttons.deployStage.click(t);
         } catch (e) {
            await this.page.stage.formNew.validationModal.okButton.click(t);
            await this.page.stage.formNew.diff.buttons.deployStage.click(t);
         }

         // падает после создания стейджа "стейдж не найден"
         // DEPLOY-4881
         await t.navigateTo(ROUTES.STAGE_STATUS);
         await this.page.stage.status.checkLocation(t);

         await t
            .expect(this.page.stage.status.stage.status(STAGE_STATUSES.VALIDATION_SPEC).exists)
            .ok('', { timeout: TIMEOUTS.stageReadyAfterSave });
      },

      sureTestStageExists: async t => {
         await t.navigateTo(ROUTES.STAGE_STATUS);
         await t.expect(this.page.stage.status.getLocation()).contains(ROUTES.STAGE_STATUS);

         if (await this.page.stage.status.body.exists) {
            return;
         }

         await this.actions.launchTestStage(t); // fill form
         await this.actions.createTestStage(t); // create stage on backend
      },

      editTestStage: async t => {
         await this.page.stage.tabs.formNew.click(t);
         await this.page.stage.buttons.editStage.click(t);
      },

      editStageSettings: async (t, du) => {
         await this.page.stage.formNew.sideTree.stage.link.click(t);
         await this.page.stage.formNew.stage.id.readValue(t, du);
      },

      editDeployUnitSettings: async (t, du) => {
         await this.page.stage.formNew.sideTree.deployUnit(du).link.click(t);
         // у removed форм readonly формат
         // await this.page.stage.formNew.deployUnit.id.readValue(t, du);
      },

      editBoxSettings: async (t, du, box) => {
         await this.page.stage.formNew.sideTree.deployUnit(du).box(box).link.click(t);
         // у removed форм readonly формат
         // await this.page.stage.formNew.box.id.readValue(t, box);
      },

      editWorkloadSettings: async (t, du, box, workload) => {
         await this.page.stage.formNew.sideTree.deployUnit(du).box(box).workload(workload).link.click(t);
         // у removed форм readonly формат
         // await this.page.stage.formNew.workload.id.readValue(t, workload);
      },

      updateTestStage: async t => {
         const action = async () => {
            await this.page.stage.formNew.buttons.update.click(t);
            // timeout для больших спек
            // await t.expect(this.page.stage.formNew.diff.body.exists).eql(true, '', { timeout: 120000 });
            // await this.page.stage.formNew.diff.description.exists(t); // TODO custom timeout?
            await this.page.stage.formNew.diff.description.typeText(t, 'testcafe update stage');
            await this.page.stage.formNew.diff.buttons.deployStage.click(t);
         };

         await this.actions.ypRetry(t, 'Update Stage', action);

         await this.page.stage.status.checkLocation(t);
         await this.actions.waitForStageValidation(t);

         // await t
         //    .expect(this.page.stage.status.stage.status(STAGE_STATUSES.VALIDATION_SPEC).exists)
         //    .ok('', { timeout: TIMEOUTS.stageReadyAfterSave });
      },

      deployTestStage: async (t, revision) => {
         const action = async () => {
            await this.page.stage.formNew.diff.buttons.deployStage.click(t);
         };

         await this.actions.ypRetry(t, 'Deploy Stage', action);

         await this.page.stage.status.checkLocation(t);
         await this.actions.waitForStageValidation(t);
         // await this.actions.waitForReadyStage(t, revision ? revision : '2');

         // await t
         //    .expect(this.page.stage.status.stage.revision(revision ? revision : '2').name.exists)
         //    .eql(true, '', { timeout: TIMEOUTS.stageReadyAfterSave });
      },

      deployHistoryTestStage: async (t, revision) => {
         await this.actions.deployTestStage(t, revision);
      },

      waitForStageValidation: async t => {
         await t
            .expect(this.page.stage.status.stage.status(STAGE_STATUSES.VALIDATION_SPEC).exists)
            .notOk('', { timeout: TIMEOUTS.stageValidationAfterSave });

         await t
            .expect(this.page.stage.status.stage.status(STAGE_STATUSES.DEPLOYING).exists)
            .ok('', { timeout: TIMEOUTS.stageValidationAfterSave });
      },

      waitForReadyStage: async (t, revision) => {
         // await this.stageValidation(t);

         if (SERVICE === 'backend') {
            await t
               .expect(this.page.stage.status.stage.revision(revision).ready.exists)
               .ok('', { timeout: TIMEOUTS.stageReadyAfterSave });
         }
      },

      deleteTestStage: async t => {
         await t.navigateTo(ROUTES.STAGE_STATUS);
         // await t.expect(this.page.stage.status.spinner.exists).eql(false, '', { timeout: 120000 });

         if (await this.page.stage.status.body.exists) {
            const action = async () => {
               await this.page.stage.status.buttons.stageActionsPopup.click(t);
               await this.page.stage.status.buttons.deallocateStage.click(t);
               await this.page.stage.removeModal.confirm(t);
            };

            await this.actions.ypRetry(t, 'Deallocate Stage', action);

            await this.page.stage.removeModal.notExists(t);
            await t.expect(this.page.project.body.exists).ok('', { timeout: TIMEOUTS.slow });
         }

         // @Efgen нужен таймаут после удаления стейджа, пару минут
         // https://st.yandex-team.ru/DEPLOY-4337
         await t.wait(TIMEOUTS.beforeRecreateStage);
      },
   };
}

export const app = new App();
