import { Selector } from 'testcafe';

import { ROUTES, STAGE_NAME, TIMEOUTS } from '../helpers/constants';
import { getDataE2eSelector, getDataTestSelector, SelectDataE2e, SelectDataTest } from '../helpers/extractors';

import { BaseComponent } from './components/BaseComponent';
import { Button } from './components/Button';
import { Checkbox } from './components/Checkbox';
import { Link } from './components/Link';
import { RadioButtonCustom } from './components/RadioButtonCustom';
import { Row } from './components/Row';
import { YCDropDown } from './components/YCDropDown';
import { YCSelect } from './components/YcSelect';
import { TextInput } from './components/TextInput';
import { PageBase } from './PageBase';

export class PageStageStatus extends PageBase {
   body = SelectDataTest('view-stage--status');

   // TODO перенести на уровень выше
   buttons = {
      editStage: new Button(SelectDataE2e('EditStageButton')),
      deallocateStage: new Button(SelectDataE2e('RemoveStageButton')),
      stageActionsPopup: new Button(SelectDataE2e('StageActionsPopupButton')),
   };

   deallocateConfirmation = {
      modal: new BaseComponent(Selector('.modal.modal_visible_yes').find('.confirm-dialog')),

      confirm: new Checkbox(
         Selector('.modal.modal_visible_yes').find('.confirm-dialog').find('.confirm-dialog__confirmation'),
      ),

      // modal buttons
      close: new Button(
         Selector('.modal.modal_visible_yes').find('.confirm-dialog').find(getDataTestSelector('button-close')),
      ),
      cancel: new Button(
         Selector('.modal.modal_visible_yes').find('.confirm-dialog').find(getDataTestSelector('action-cancel')),
      ),
      delete: new Button(
         Selector('.modal.modal_visible_yes').find('.confirm-dialog').find(getDataTestSelector('action-delete')),
      ),
   };

   stage = {
      status: status => SelectDataTest('stage--badge').withText(status),
      // name: SelectDataTest('stage--name').withExactText(STAGE_NAME),
      // ready: Selector('.stage-status__stage-section').find('.stage-status__badge_ready').withExactText('Ready'),
      // badge: (status) => Selector('.stage-status__stage-section').find(`.stage-status__badge_${status}`), // status = ready | in-progress
      // status: (status) => Selector('.stage-status__stage-section').find(getDataTestSelector('stage--badge')).withExactText(status), // status = Ready | Deploying
      // revision: (revision) => Selector('.stage-status__stage-section').find(getDataTestSelector('stage--revision')).withExactText(revision)
      revision: revision => {
         // const stage = SelectDataTest('stage--revision').withExactText(revision).parent(getDataTestSelector('status--stage'));
         const stage = SelectDataTest('stage--name')
            .withExactText(STAGE_NAME)
            .parent(getDataTestSelector('status--stage'))
            .find(getDataTestSelector('stage--revision'))
            .withExactText(revision)
            .parent(getDataTestSelector('status--stage'));

         return {
            'name': stage.find(getDataTestSelector('stage--name')).withExactText(STAGE_NAME),
            'ready': stage.find(getDataTestSelector('stage--badge')).withText('ready'),
            'xrayLink': new Link(stage.find('.stage-status__xray-link')),
         };
      },
   };

   deployUnits = {
      count: count =>
         Selector('.stage-status__stage-inner__deploy-units')
            .find('.stage-status__stage-inner__caption__elements-count')
            .withExactText(count),
   };

   deployUnit = deployUnitName => {
      return {
         tab: SelectDataTest('status--deploy-unit'),
         // ready: deployUnit.find('.stage-status__small-badge_ready').parent('.stage-status__deploy-unit').find('.stage-status__deploy-unit__status').withExactText('ready'),
         // badge: (status) => deployUnit.find(`.stage-status__small-badge_${status}`),
         // status: (status) => deployUnit.find('.stage-status__deploy-unit__status').withExactText(status),
         'revision': revision => {
            const deployUnit = SelectDataTest('deploy-unit--name')
               .withExactText(deployUnitName)
               .parent(getDataTestSelector('status--deploy-unit'))
               .find('.stage-status__deploy-unit__revision__number')
               .withExactText(revision)
               .parent(getDataTestSelector('status--deploy-unit'));

            return {
               'tab': deployUnit,
               'ready': deployUnit
                  .find('.stage-status__small-badge_ready')
                  .parent('.stage-status__deploy-unit')
                  .find('.stage-status__deploy-unit__status')
                  .withExactText('ready'),
            };
         },
      };
   };

   replicaSet = cluster => {
      return {
         // ready: replicaSet.find('.stage-status__small-badge_ready'),
         // badge: (status) => replicaSet.find(`.stage-status__small-badge_${status}`), // status = ready | in-progress | unknown
         // status: (status) => ... ,
         // revision: (revision) => replicaSet.find('.stage-status__replica-set__revision-number').withExactText(revision),
         'revision': revision => {
            const replicaSet = SelectDataTest('replica-set--cluster')
               .withExactText(cluster.toUpperCase())
               .parent(getDataTestSelector('status--replica-set'))
               .find('.stage-status__replica-set__revision-number')
               .withExactText(revision)
               .parent(getDataTestSelector('status--replica-set'));

            return {
               'tab': replicaSet,
               'ready': replicaSet.find('.stage-status__small-badge_ready'),
               'podsTotal': total =>
                  replicaSet.find(getDataTestSelector('replica-set--pods-total')).withExactText(total),
            };
         },
      };
   };

   // const filter = Selector('.stage-status__pods-filter');

   pods = {
      location: new RadioButtonCustom(SelectDataTest('pods-filter--location')),
      filter: {
         selector: SelectDataTest('pods-filter'),
         status: new YCSelect(SelectDataTest('pods-filter--status')),
         revision: new YCSelect(SelectDataTest('pods-filter--revision')),
         custom: {
            checkbox: new Checkbox(SelectDataTest('pods-filter--custom-checkbox')),
            input: new TextInput(SelectDataTest('pods-filter--custom-input')),
            button: new Button(SelectDataTest('pods-filter--custom-button')),
         },
      },
      pagination: {
         // 'page': (value) => new Button(Selector('.stage-status__pod-pagination').find('span.button2__text').withExactText(value).parent('button.button2')),
         page: page =>
            SelectDataTest('pods-filter--pagination')
               .find('.yc-radio-button')
               .find(getDataE2eSelector(`Pagination:${page}`)),
         limit: new YCDropDown(SelectDataTest('pods-filter--pagination')),
      },
      pod: () => {
         return {
            // tab: new BaseComponent(pod),
            // ready: pod.find('.stage-status__small-badge_ready').nextSibling(getDataTestSelector('pod--status')).withExactText('ready'),
            // badge: (status) => SelectDataTest(''),
            // status: (status) => pod.find('.stage-status__deploy-unit__status').withExactText(status),
            revision: revision => {
               const pod = SelectDataTest('status--pod')
                  .find(getDataTestSelector('pod--revision'))
                  .withExactText(revision) // N/A
                  // .withText(revision) // N/A
                  .parent(getDataTestSelector('status--pod'));

               return {
                  row: new Row(pod),
                  N: pod.find('td').nth(0),
                  fqdn: pod.find(getDataTestSelector('pod--fqdn')),
                  host: pod.find(getDataTestSelector('pod--host')),
                  // 'cluster': pod.find(getDataTestSelector('pod--cluster')).withExactText(cluster.toUpperCase()),
                  ready: pod,
                  // 'status': (status) => pod
                  //    .find(getDataTestSelector('pod--status'))
                  //    .withExactText(status),
                  openedRow: pod.nextSibling(getDataTestSelector('pod--additional')),
                  // spinner: pod.find(getDataTestSelector('pod--revision')).find('.stage-status__spinner'),
               };
            },
         };
      },
   };

   /*
      pod = (cluster) => {
          return {
              // tab: new BaseComponent(pod),
              // ready: pod.find('.stage-status__small-badge_ready').nextSibling(getDataTestSelector('pod--status')).withExactText('ready'),
              // badge: (status) => SelectDataTest(''),
              // status: (status) => pod.find('.stage-status__deploy-unit__status').withExactText(status),
              'revision': (revision) => {
                  const pod = SelectDataTest('pod--cluster').withExactText(cluster.toUpperCase()).parent(getDataTestSelector('status--pod')).find(getDataTestSelector('pod--revision')).withExactText(revision).parent(getDataTestSelector('status--pod'));

                  return {
                      'row': new Row(pod),
                      'ready': pod.find('.stage-status__small-badge_ready').nextSibling(getDataTestSelector('pod--status')).withExactText('ready')
                  };
              }
          };
      };
      */

   /*
          .expect(Selector('[data-test=pod--status]').withText('ready').exists).eql(true, '', {timeout: 660000})
          .expect(Selector('[data-test=replica-set--badge]').withAttribute('class', 'stage-status__small-badge').exists).eql(true, '', {timeout: 660000})
          .expect(Selector('[data-test=replica-set--pods-total]').withText('1').exists).eql(true, '', {timeout: 660000})
          .expect(Selector('[data-test=replica-set--pods-in-progress]').withText('').exists).eql(true, '', {timeout: 660000})
          .expect(Selector('[data-test=stage--badge]').withText('Ready').exists).eql(true, '', {timeout: 660000})

          .expect(Selector('[data-test=pod--status]').withText('ready').exists).eql(true, '', {timeout: 660000})
          .expect(Selector('[data-test=replica-set--badge]').withAttribute('class', 'stage-status__small-badge').exists).eql(true, '', {timeout: 660000})
          .expect(Selector('[data-test=deploy-unit--badge]').withAttribute('class', 'stage-status__small-badge').exists).eql(true, '', {timeout: 660000})
          .expect(Selector('[data-test=stage--badge]').withText('Ready').exists).eql(true, '', {timeout: 660000})
  */

   async checkLocation(t) {
      await t.expect(this.getLocation()).contains(ROUTES.STAGE_STATUS);
      await t.expect(this.body.exists).eql(true, { timeout: TIMEOUTS.slow });
   }
}
