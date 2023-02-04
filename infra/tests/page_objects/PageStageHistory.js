// файл оставлен до синхронизации старых и новых тестов по истории стейджа
import { Selector } from 'testcafe';
import { SelectDataTest, getDataTestSelector, getDataE2eSelector, SelectDataE2e } from '../helpers/extractors';
import { Link } from './components/Link';
import { PageBase } from './PageBase';

import { Row } from './components/Row';
import { YCSelect } from './components/YCSelect';
import { Button } from './components/Button';
import { Viewer } from './components/Viewer';
import { Checkbox } from './components/Checkbox';
import { TextArea } from './components/TextArea';
import { YCRadioButton } from './components/YCRadioButton';

const diff = Selector('.stage-history__diff');
const diffSpec = diff.find(getDataTestSelector('diff-spec'));
const diffApply = diff.find(getDataTestSelector('diff-apply'));

export class PageStageHistory extends PageBase {
   body = SelectDataE2e('StageIndexPage:content').find('.stage-history');

   buttons = {
      // ...
   };

   table = {
      'revision': id => {
         const revision = SelectDataTest('revision--id').withExactText(id).parent('tr');

         return {
            'row': new Row(revision),
            'id': revision.find(getDataTestSelector('revision--id')),
            'time': revision.find(getDataTestSelector('revision--time')),
            'owner': revision.find(getDataTestSelector('revision--owner')),
            'message': value => revision.find(getDataTestSelector('revision--message')).withExactText(value),
            'actions': {
               'buttons': {
                  // 'apply':
               },
               'currentRevision': revision
                  .find(getDataTestSelector('revision--actions'))
                  .find(getDataTestSelector('current-revision'))
                  .withExactText('Current revision'),
            },
            // ''
         };
      },
   };

   diff = {
      buttons: {
         backToHistory: new Link(SelectDataTest('StageHistory:BackHistoryButton'), 'StageHistory:BackHistoryButton'),
         backToRevision: new Link(SelectDataTest('StageHistory:BackHistoryButton'), 'StageHistory:BackHistoryButton'),
         backToEdit: new Link(SelectDataTest('StageHistory:BackHistoryButton'), 'StageHistory:BackHistoryButton'),
         compareRevisions: new Button(diffSpec.find(getDataTestSelector('compare-revisions'))),
         rearrangeRevisions: new Row(diffSpec.find(getDataTestSelector('rearrange-revisions'))),
         apply: new Button(diffApply),
         applyAsIs: new Link(SelectDataTest('StageHistory:ApplyAsIsLink'), 'StageHistory:ApplyAsIsLink'),
         applyWithChanges: new Link(
            SelectDataTest('StageHistory:ApplyWithChangesLink'),
            'StageHistory:ApplyWithChangesLink',
         ),
         deploy: new Button(diff.find('.form__bottom').find('span').withExactText('Deploy').parent('button')),
         cancel: new Button(diff.find('.form__bottom').find('span').withExactText('Cancel').parent('button')),
      },

      currentRevision: diffApply.find(getDataTestSelector('current-revision')).withExactText('Current revision'),

      selector: id => new YCSelect(diffSpec.find(getDataTestSelector('select-revision')).nth(id - 1)),
      info: id => new Button(diffSpec.find(getDataTestSelector('revision-info')).nth(id - 1)),

      /*'selectors': {
            'base': new Select(diffSpec.find('.stage-history__select-revision').nth(0)),
            'compared': new Select(diffSpec.find('.stage-history__select-revision').nth(1))
        },*/

      options: {
         viewType: new YCRadioButton(diff.find(getDataE2eSelector('DiffView:Toolbar'))),
         allExpanded: new Checkbox(diff.find(getDataE2eSelector('DiffView:Toolbar'))),
      },

      message: {
         text: value => diff.find(getDataTestSelector('meta-message')).withExactText(value),
         empty: diff
            .find(getDataTestSelector('meta-message'))
            .find(getDataTestSelector('message_empty'))
            .withText('No message'),
      },

      title: value => diff.find('.stage-history__diff__title').withExactText(value),
      description: new TextArea(diff.find('.stage-history__diff__message')),

      viewer: new Viewer(diff.find(getDataTestSelector('diff-viewer'))),

      popup: {
         buttons: {
            close: new Button(
               Selector('.yc-popup.yc-popup_open.yc-tooltip__tooltip')
                  .find('.stage-history__diff__popup')
                  .filterVisible()
                  .parent('.yc-popup')
                  .find('.yc-tooltip__tooltip-close'),
            ),

            showYaml: new Button(
               Selector('.yc-popup.yc-popup_open.yc-tooltip__tooltip')
                  .find('.stage-history__diff__popup')
                  .filterVisible()
                  .find('.stage-history__diff__show-yaml'),
            ),
         },

         title: value =>
            Selector('.yc-popup.yc-popup_open.yc-tooltip__tooltip')
               .find('.stage-history__diff__popup')
               .filterVisible()
               .parent('.yc-popup')
               .find('h3')
               .withExactText(value),

         message: {
            text: value =>
               Selector('.yc-popup.yc-popup_open.yc-tooltip__tooltip')
                  .find('.stage-history__diff__popup')
                  .filterVisible()
                  .find('.stage-history__diff__meta-message')
                  .withExactText(value),

            empty: Selector('.yc-popup.yc-popup_open.yc-tooltip__tooltip')
               .find('.stage-history__diff__popup')
               .filterVisible()
               .find('.stage-history__diff__meta-message')
               .find('.stage-history__diff__message_empty')
               .withText('No message'),
         },
      },
   };
}
