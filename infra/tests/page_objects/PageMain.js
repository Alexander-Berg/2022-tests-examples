import { Selector } from 'testcafe';
import { PROJECT_NAME, ROBOT_LOGIN, STAGE_NAME } from '../helpers/constants';
import { SelectDataTest } from '../helpers/extractors';

import { BaseComponent } from './components/BaseComponent';
import { Button } from './components/Button';
import { Checkbox } from './components/Checkbox';

import { Link } from './components/Link';
import { RadioButton } from './components/RadioButton';
import { Row } from './components/Row';
import { Select } from './components/Select';
import { Suggest } from './components/Suggest';
import { TextInput } from './components/TextInput';

import { PageBase } from './PageBase';

// import {Spin} from './components/Spin';

export class PageMain extends PageBase {
   body = SelectDataTest('page-main');
   header = this.body.find('.page__header');
   content = this.body.find('.page__content');

   buttons = {
      launchNewStage: new Link(SelectDataTest(`project--${PROJECT_NAME}`).find('.project-card__create-stage-button')),
      launchNewProject: new Button(SelectDataTest()),
   };

   filters = {
      buttons: {
         reset: new Button(SelectDataTest('filter-projects-reset')),
         search: new Button(SelectDataTest('filter-projects-search')),
      },

      owner: new RadioButton(SelectDataTest('filter-projects-by-owner')),
      type: new Select(SelectDataTest('filter-projects-by-type')),
      name: new TextInput(SelectDataTest('filter-projects-by-name')),
   };

   // spinner = new Spin(this.content.find('.page-main__table-spinner'));
   spinner = this.content.find('.page-main__table-spinner');

   project = SelectDataTest(`project--${PROJECT_NAME}`);

   createProject = new Button(SelectDataTest('create-project-button'));

   createProjectForm = new (class extends PageBase {
      projectName = new TextInput(SelectDataTest('form-field--project-name'));
      abcService = new Suggest(SelectDataTest('form-field--abc-service'));
      temporaryAccount = new Checkbox(SelectDataTest('form-field--use-temporary-account'));

      buttons = {
         createProject: new Button(SelectDataTest('project-create-form__button_submit')),
      };
   })();

   stage = {
      link: {
         // TODO does not work
         click: t => t.click(SelectDataTest(`stage--${STAGE_NAME}`).find('a').child('div')),
      },
      row: new Row(SelectDataTest(STAGE_NAME).parent('tr.table__body-row_is-clickable')),
      card: {
         row: SelectDataTest(STAGE_NAME)
            .parent('tr.table__body-row_is-clickable')
            .nextSibling('tr.table__body-row_with-card'),
         abcService: SelectDataTest(`project-card--${STAGE_NAME}`)
            .find('.definition-list__definition')
            .find('a[href="https://abc.yandex-team.ru/services/drug"]')
            .withExactText('Я.Деплой'),
         tmpAccount: SelectDataTest(`project-card--${STAGE_NAME}`)
            .find('.definition-list__definition')
            .withExactText('tmp'),
         tmpAccountWarning: SelectDataTest(`project-card--${STAGE_NAME}`)
            .find('.definition-list__definition')
            .withExactText('tmp'),
      },
      status: status =>
         new BaseComponent(
            SelectDataTest(STAGE_NAME)
               .parent('tr.table__body-row_is-clickable')
               .find('.page-main__warning.page-main__warning_tmp'),
         ),
   };

   listOfMyStages = SelectDataTest('list-of-my-projects');
   listOfAllStages = SelectDataTest('list-of-all-projects');

   listOfMyStagesByLogin = Selector(`[data-test=list-of-my-projects-by-${ROBOT_LOGIN}]`);
   listOfAllStagesByLogin = Selector(`[data-test=list-of-all-projects-by-${ROBOT_LOGIN}]`);

   i = 0;
   anyProject = SelectDataTest.startsWith('project--')
      .nth(this.i++)
      .child('.project-card__header');
   anyMyStage = Selector('.stage-card__my-label');
   anyStage = SelectDataTest.startsWith('stage--');
   noProjectsMatchingYourCriteria = SelectDataTest('EmptyContainer')
      .find('h2')
      .withText('No projects matching your criteria');
   noStagesMatchingYourCriteria = SelectDataTest('EmptyContainer')
      .find('h2')
      .withText('No stages matching your criteria');

   projectWithName(name) {
      return SelectDataTest('project--' + name).child('.project-card__header');
   }

   stagesIntoProject(projectName) {
      return SelectDataTest('project--' + projectName)
         .find('project-card__stage-list')
         .child();
   }

   myStagesIntoProject(projectName) {
      return this.stagesIntoProject(projectName).filter(element => element.find('.stage-card__my-label').exists);
   }

   stageWithName(name) {
      return SelectDataTest('stage--' + name);
   }

   // stage status
   // .expect(testStageLink.parent('tr').find('[data-test=project-status-caption]').withText('Ready').exists).eql(true, '', {timeout: 60000});
}
