import { getDataE2eSelector, getDataQaSelector, SelectDataE2e } from '../helpers/extractors';

import { Button } from './components/Button';
import { YCRadioButton } from './components/YCRadioButton';
import { TextInput } from './components/TextInput';

import { FormNewEnvironment } from './FormNewEnvironment';
import { FormNewWorkloadCommands } from './FormNewWorkloadCommands';
import { FormNewYasmTags } from './FormNewYasmTags';

const form = SelectDataE2e('SubForm:workload');

export class FormNewWorkload {
   buttons = {
      clone: new Button(form.find(getDataE2eSelector('SubForm:CloneButton'))),
      remove: new Button(form.find(getDataE2eSelector('SubForm:RemoveButton'))),
      restore: new Button(form.find(getDataE2eSelector('SubForm:RestoreButton'))),
      addUnistat: new Button(form.find(getDataQaSelector('Unistat:AddUnistatButton'))),
   };

   id = new TextInput(form.find('label[for=id]').nextSibling('div'));

   //  TODO:
   commands = new FormNewWorkloadCommands();

   // Environment variables
   variables = new FormNewEnvironment(form.find('label[for=environment]').nextSibling('div'));

   logs = new YCRadioButton(form.find('label[for=logs]').nextSibling('div'));

   yasm = {
      url: new TextInput(form.find('label[for="yasm.unistats[0].url"]').nextSibling('div')),
      port: new TextInput(form.find('label[for="yasm.unistats[0].port"]').nextSibling('div')),
      yasmTags: new FormNewYasmTags(form.find(getDataE2eSelector('YasmTags:Itype')).parent()),
   };

   //  TODO:
   // yasm = {};

   // OLD FORM
   // constructor(workload) {
   //    this.buttons = {
   //       'duplicateWorkload': new Button(
   //          workload.find('.workloads-section__buttons').find(getDataTestSelector('duplicate-workload')),
   //       ),
   //       'deleteWorkload': new Button(
   //          workload.find('.workloads-section__buttons').find(getDataTestSelector('delete-workload')),
   //       ),
   //    };

   //    this.selector = workload;

   //    this.name = new TextInput(
   //       workload
   //          .find('div.form-layout__row-title')
   //          .withExactText('Workload Name')
   //          .parent('.form-layout__row')
   //          .find('.form-layout__row-right'),
   //    );

   //    this.commands = new FormNewWorkloadCommands(workload.find('.workload-commands'));

   //    this.variables = new FormNewEnvironment(workload.find('.workload-variables'));

   //    this.logs = new RadioButton(workload.find(getDataTestSelector('form-field--workload-logs')));

   //    this.unistat = {
   //       path: new TextInput(workload.find('.form-field-path-port').find('.form-field-path-port__path')),
   //       port: new TextInput(workload.find('.form-field-path-port').find('.form-field-path-port__port')),
   //    };

   //    this.yasmTags = {
   //       buttons: {
   //          addTag: new Button(workload.find(getDataTestSelector('workload-tags-add-btn'))),
   //       },
   //       itype: new Suggest(workload.find(getDataTestSelector('workload-itype'))),
   //       tags: workload.find(getDataTestSelector('workload-tag-item')),
   //       tag: id => {
   //          const tag = workload.find(getDataTestSelector('workload-tag-item')).nth(id - 1);

   //          return {
   //             buttons: {
   //                // TODO: переделать в кнопку!!!
   //                // delete: new Button(tag.find(getDataTestSelector('workload-tag-delete')))
   //                delete: tag.find(getDataTestSelector('workload-tag-delete')),
   //             },

   //             name: new Suggest(tag.find(getDataTestSelector('workload-tag-name'))),
   //             value: new Suggest(tag.find(getDataTestSelector('workload-tag-value'))),

   //             errorMessage: tag
   //                .parent(getDataTestSelector('form-field'))
   //                .find(getDataTestSelector('form-field-message')),
   //          };
   //       },
   //    };
   // }
}
