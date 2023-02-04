import { Selector } from 'testcafe';
import { STAGE_NAME } from '../helpers/constants';
import { getDataE2eSelector, SelectDataE2e, SelectDataTest } from '../helpers/extractors';

import { Button } from './components/Button';
import { Link } from './components/Link';
import { ModalLayout } from './components/ModalLayout';
import { TextArea } from './components/TextArea';
import { FormNewBox } from './FormNewBox';
import { FormNewDeployUnit } from './FormNewDeployUnit';

import { FormNewStage } from './FormNewStage';
import { FormNewWorkload } from './FormNewWorkload';

import { PageBase } from './PageBase';

const sideTree = Selector('aside[data-e2e="side-tree"]');

const stageTab = sideTree.find('li[data-e2e="SideTreeItem:stage"]').find('span').withText(STAGE_NAME).parent('li');

/**
 * Класс представляющий форму создания/редактирования стейджа
 *
 * @export
 * @class PageStageFormNew
 * @extends {PageBase}
 */
export class PageStageFormNew extends PageBase {
   //  // селектор для страницы нового стейджа
   pageCreateStage = SelectDataTest('page-create-stage');
   //  // селектор для страницы изменения существующего стейджа
   //  pageStage = SelectDataTest('page-stage');

   // обертки над кнопками (включают в себя селектор + метод click, см. компоненты в ./components)
   buttons = {
      revertChanges: new Button(SelectDataE2e('StageHugeForm:RevertUnsavedChangesButton')),
      cancel: new Button(SelectDataE2e('StageHugeForm:CancelButton')),
      update: new Button(SelectDataE2e('StageHugeForm:UpdateButton')),
   };

   sideTree = {
      stage: {
         buttons: {
            addDeployUnit: new Button(stageTab.find(getDataE2eSelector('SideTreeItem:Actions'))),
         },

         link: new Link(stageTab),
      },
      deployUnit: deployUnitId => {
         const deployUnitTab = sideTree
            .find(getDataE2eSelector('SideTreeItem:deployUnit'))
            .find(getDataE2eSelector('SideTreeItem:Title'))
            .withText(deployUnitId)
            .parent('li');

         return {
            buttons: {
               addBox: new Button(deployUnitTab.find(getDataE2eSelector('SideTreeItem:Actions'))),
            },

            link: new Link(deployUnitTab),
            clone: n => new Link(deployUnitTab.nth(n)),
            isRemoved: deployUnitTab.find(getDataE2eSelector('SideTreeItem:isRemoved')), // .withText('(removed)'),
            isAdded: deployUnitTab.find(getDataE2eSelector('SideTreeItem:isAdded')), // .withText('(added)'),

            box: boxId => {
               const boxTab = deployUnitTab
                  .nextSibling('li[data-e2e="SideTreeItem:box"]')
                  .find('span[data-e2e="SideTreeItem:Title"]')
                  .withText(boxId)
                  .parent('li');

               return {
                  buttons: {
                     addWorkload: new Button(boxTab.find(getDataE2eSelector('SideTreeItem:Actions'))),
                  },

                  link: new Link(boxTab),
                  clone: n => new Link(boxTab.nth(n)),
                  isRemoved: boxTab.find(getDataE2eSelector('SideTreeItem:isRemoved')), // .withText('(removed)'),
                  isAdded: boxTab.find(getDataE2eSelector('SideTreeItem:isAdded')), // .withText('(added)'),

                  workload: workloadId => {
                     const workloadTab = boxTab
                        .nextSibling('li[data-e2e="SideTreeItem:workload"]')
                        .find('span[data-e2e="SideTreeItem:Title"]')
                        .withText(workloadId)
                        .parent('li');

                     return {
                        link: new Link(workloadTab),
                        clone: n => new Link(workloadTab.nth(n)),
                        isRemoved: workloadTab.find(getDataE2eSelector('SideTreeItem:isRemoved')), // .withText('(removed)'),
                        isAdded: workloadTab.find(getDataE2eSelector('SideTreeItem:isAdded')), // .withText('(added)'),
                     };
                  },
               };
            },
         };
      },
   };

   stage = new FormNewStage();

   deployUnit = new FormNewDeployUnit();

   box = new FormNewBox();

   workload = new FormNewWorkload();

   diff = {
      //   body: Selector('.page-stage__diff'),

      buttons: {
         closeDiff: new Button(SelectDataE2e('StageHugeForm:CancelButton'), 'StageHugeForm:CancelButton'),

         cancel: new Button(SelectDataE2e('StageDiffView:CancelButton'), 'StageDiffView:CancelButton'), // history diff
         saveDraft: new Button(SelectDataE2e('StageDiffView:SaveDraftButton')),
         deployStage: new Button(SelectDataE2e('StageDiffView:DeployButton')),
         // cancel: new Button(SelectDataE2e('BottomFooter').find('span').withText('Cancel').parent('button')), // history diff
         continueEdit: new Button(SelectDataE2e('StageDiffView:CancelButton')),
      },

      description: new TextArea(SelectDataE2e('BottomFooter')),
      //   viewer: new Viewer(Selector('.page-stage__diff'))
   };

   validationModal = new ModalLayout();

   // обертки над полями ввода формы

   //  abcService = new Suggest(SelectDataTest('form-field--abc-service'));
   //  tmpAccount = new Checkbox(SelectDataTest('form-field--use-temporary-account'));
   //  tmpAccountWarning = Selector('.project-form__field_account-tmp').find('.project-form__warning.project-form__warning_tmp');

   //  deleteConfirmation = {
   //      modal: new BaseComponent(Selector('.modal.modal_visible_yes').find('.confirm-dialog')),

   //      confirm: new Checkbox(Selector('.modal.modal_visible_yes').find('.confirm-dialog').find('.confirm-dialog__confirmation')),

   //      // modal buttons
   //      close: new Button(Selector('.modal.modal_visible_yes').find('.confirm-dialog').find(getDataTestSelector('button-close'))),
   //      cancel: new Button(Selector('.modal.modal_visible_yes').find('.confirm-dialog').find(getDataTestSelector('action-cancel'))),
   //      delete: new Button(Selector('.modal.modal_visible_yes').find('.confirm-dialog').find(getDataTestSelector('action-delete')))
   //  };
}
