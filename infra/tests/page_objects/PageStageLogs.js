import { Selector } from 'testcafe';
import { SelectDataTest, getDataTestSelector } from '../helpers/extractors';
import { PageBase } from './PageBase';

import { Select } from './components/Select';
import { MultiSelect } from './components/MultiSelect';
import { MonacoEditor } from './components/MonacoEditor';
import { JsonCustom } from './components/JsonCustom';
import { CopyToClipboard } from './components/CopyToClipboard';
import { DateTimePicker } from './components/DateTimePicker';
import { Button } from './components/Button';

const logs = SelectDataTest('view-stage--logs');

export class PageStageLogs extends PageBase {
   body = logs;

   filters = {
      buttons: {
         'reset': new Button(Selector(getDataTestSelector('filter-reset'))),
         'search': new Button(Selector(getDataTestSelector('filter-logs-submit'))),
      },

      'deployUnit': new Select(Selector(getDataTestSelector('filter-logs-by-deploy-unit'))),
      'level': new MultiSelect(Selector(getDataTestSelector('filter-logs-by-level'))),

      'dateFrom': new DateTimePicker(Selector(getDataTestSelector('filter-logs-from-date'))),
      'dateTo': new DateTimePicker(Selector(getDataTestSelector('filter-logs-to-date'))),

      'noProject': new DateTimePicker(Selector(getDataTestSelector('filter-logs--no-project'))),

      // 'query': new TextArea(Selector('.stage-logs__filter-query')),
      'query': new MonacoEditor(Selector('.stage-logs__filter-query')),

      'order': {
         // desc -> asc
         'asc': Selector('.stage-logs__table')
            .find('th')
            .find('.stage-logs__th__date-sort')
            .find('i.far.fa-sort-amount-down'),
         // asc -> desc
         'desc': Selector('.stage-logs__table')
            .find('th')
            .find('.stage-logs__th__date-sort')
            .find('i.far.fa-sort-amount-down-alt'),
      },

      'help': {
         'host': Selector('.stage-logs__filter-row_help').find('span.stage-logs__link').withExactText('host'),
         'pod': Selector('.stage-logs__filter-row_help').find('span.stage-logs__link').withExactText('pod'),
         'box': Selector('.stage-logs__filter-row_help').find('span.stage-logs__link').withExactText('box'),
         'workload': Selector('.stage-logs__filter-row_help').find('span.stage-logs__link').withExactText('workload'),
         'container': Selector('.stage-logs__filter-row_help')
            .find('span.stage-logs__link')
            .withExactText('container_id'),
         'logger': Selector('.stage-logs__filter-row_help').find('span.stage-logs__link').withExactText('logger_name'),
         'message': Selector('.stage-logs__filter-row_help').find('span.stage-logs__link').withExactText('message'),
      },

      'error': error => Selector('.stage-logs__filter-row_help').find('.stage-logs__error').withText(error),
   };

   results = {
      'rows': {
         'buttons': {
            'changeOrder': Selector('.stage-logs__th__date-sort'),
         },

         'row': id => {
            const row = Selector('.stage-logs__table')
               .find(getDataTestSelector('logs--row'))
               .nth(id - 1);
            const expanded = row.nextSibling(getDataTestSelector('log--expand'));

            return {
               'buttons': {
                  'expand': row.find('td.stage-logs__td_log-message'),
               },
               'link': new CopyToClipboard(row.find('td.stage-logs__td_log-link')),
               'date': date =>
                  row.find('td.stage-logs__td_log-time').find('.stage-logs__log-timestamp_date').withText(date),
               'time': time =>
                  row.find('td.stage-logs__td_log-time').find('.stage-logs__log-timestamp_time').withText(time),
               'level': level => row.find('td.stage-logs__td_log-level').withText(level),
               'message': message =>
                  row.find('td.stage-logs__td_log-message').find('.stage-logs__log-message-short').withText(message),
               'logger': logger => row.find('td.stage-logs__td_log-logger').withText(logger),
               'expanded': {
                  'buttons': {
                     'copyLogJson': expanded
                        .find('.stage-logs__log-opened-copy')
                        .find('.copy-to-clipboard')
                        .find('.far.copy-to-clipboard__icon'),
                  },
                  'openedMessageJson': new JsonCustom(expanded.find('.stage-logs__log-opened-log-json')),
               },
            };
         },
      },

      'empty': SelectDataTest('EmptyContainer').find('h2').withText('There are no results'),
   };
}
