import { SelectDataQa, getDataQaSelector, getDataTestSelector } from '../helpers/extractors';

import { BaseComponent } from './components/BaseComponent';
import { Button } from './components/Button';
import { Checkbox } from './components/Checkbox';
import { YCRadioButton } from './components/YCRadioButton';
import { Selector } from 'testcafe';
import { TextArea } from './components/TextArea';
import { TextInput } from './components/TextInput';

const form = Selector('form').find('header').find('h2').withText('Workload settings').parent('form');

export class FormNewWorkloadCommands {
   cards = form.find('label[for=commands]');

   tab = name => {
      return new BaseComponent(form.find(getDataTestSelector(`commands.${name}.tab`)));
   };

   type = element => {
      return new YCRadioButton(element);
   };

   command = (name, id) => {
      if (id !== undefined) {
         return form.find(getDataTestSelector(`commands.${name}[${id}]`));
      } else {
         return form.find(getDataTestSelector(`commands.${name}`));
      }
   };

   exec = (element, name) => {
      return {
         buttons: {
            showAdvancedSettings: new Button(element.find(getDataQaSelector('FormAdvancedSection:ShowToggleButton'))),
            hideAdvancedSettings: new Button(element.find(getDataQaSelector('FormAdvancedSection:HideToggleButton'))),
         },
         value: new TextArea(element),
         // advanced
         user: new TextInput(element.find(`label[for="${name}.access.user"]`).nextSibling('div')),
         group: new TextInput(element.find(`label[for="${name}.access.group"]`).nextSibling('div')),
      };
   };

   tcp = (element, name) => {
      return {
         port: new TextInput(element.find(`label[for="${name}.tcp.port"]`).nextSibling('div')),
      };
   };

   http = (element, name) => {
      return {
         path: new TextInput(element.find(`label[for="${name}.http.path"]`).nextSibling('div')),
         port: new TextInput(element.find(`label[for="${name}.http.port"]`).nextSibling('div')),
         answer: {
            expected: new TextInput(element.find(`label[for="${name}.http.expectedAnswer"]`).nextSibling('div')),
            any: new Checkbox(element.find(`label[for="${name}.http.any"]`).nextSibling('div')),
         },
      };
   };

   // Start command
   start = {
      tab: this.tab('start'),
      exec: this.exec(this.command('start'), 'commands.start'),
   };

   buttons = {
      addInitCommand: new Button(SelectDataQa('commands.init.buttons.add')),
   };

   // Init commands
   init = {
      tab: id => this.tab(`init_${id}`),
      exec: id => this.exec(this.command(`init[${id}]`), `commands.init[${id}]`),
      buttons: {
         deleteCommand: id => new Button(form.find(`label[for="commands.init[${id}]"]`)),
      },
   };

   // Liveness probe
   liveness = {
      tab: this.tab('liveness'),
      type: this.type(this.command('liveness')),
      exec: this.exec(this.command('liveness'), 'commands.liveness.exec'),
      tcp: this.tcp(this.command('liveness'), 'commands.liveness'),
      http: this.http(this.command('liveness'), 'commands.liveness'),
   };

   readiness = {
      tab: this.tab('readiness'),
      type: this.type(this.command('readiness')),
      exec: this.exec(this.command('readiness'), 'commands.readiness.exec'),
      tcp: this.tcp(this.command('readiness'), 'commands.readiness'),
      http: this.http(this.command('readiness'), 'commands.readiness'),
   };

   stop = {
      tab: this.tab('stop'),
      type: this.type(this.command('stop')),
      exec: this.exec(this.command('stop'), 'commands.stop.exec'),
      http: this.http(this.command('stop'), 'commands.stop'),
   };

   destroy = {
      tab: this.tab('destroy'),
      type: this.type(this.command('destroy')),
      exec: this.exec(this.command('destroy'), 'commands.destroy.exec'),
      http: this.http(this.command('destroy'), 'commands.destroy'),
   };
}
