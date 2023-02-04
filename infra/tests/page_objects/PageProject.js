import { Selector } from 'testcafe';
import { SelectDataTest, getDataTestSelector, SelectDataE2e } from '../helpers/extractors';

import { PageBase } from './PageBase';

import { Button } from './components/Button';
import { Checkbox } from './components/Checkbox';

import { BaseComponent } from './components/BaseComponent';
import { Link } from './components/Link';
// import {Spin} from './components/Spin';

export class PageProject extends PageBase {
   body = SelectDataTest('page-project');

   name = value => Selector('.project__title').withExactText(value);

   buttons = {
      deleteProject: new Button(SelectDataTest('project-actions__delete')),
      createStage: new Link(SelectDataTest('project-actions__create-stage').find('a')),
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

   access = {
      noAvailableAcl: SelectDataE2e('no-available-acl').withText('No available acl.'),
   };

   emptyContainer = {
      buttons: {
         goToHome: new Link(
            SelectDataTest('EmptyContainer').find('span').withExactText('Back to Home page').parent('a'),
         ),
      },
      projectWasNotFound: SelectDataTest('EmptyContainer').find('h2').withText('Project was not found'),
      noStagesCreatedYet: SelectDataTest('EmptyContainer').find('h2').withText('No stages created yet'),
   };
}
