import { Selector } from 'testcafe';
import { getDataE2eSelector, SelectDataE2e, SelectDataQa } from '../helpers/extractors';
import { SECRET } from '../helpers/constants';

import { BaseComponent } from './components/BaseComponent';
import { Button } from './components/Button';
import { Row } from './components/Row';

import { YCSelect } from './components/YcSelect';
import { TextInput } from './components/TextInput';
import { TextArea } from './components/TextArea';
import { ModalLayout } from './components/ModalLayout';

const modal = new ModalLayout();

export class FormNewEnvironment {
   constructor(variables) {
      this.buttons = {
         addVariable: new Button(variables.find('span').withText('Add variable').parent('button')),
      };

      this.variable = name => {
         const variable = variables.find('tr').find('td').nt(0).withExactText(name).parent('tr');

         return {
            row: new Row(variable),
            name: new BaseComponent(variable.find('td').nt(0)),
            value: value => new BaseComponent(variable.find('td').nt(1).withExactText(value)),

            buttons: {
               deleteVariable: new Button(variable.find('td').nt(3)),
            },
         };
      };

      this.dialog = {
         buttons: {
            close: new Button(Selector('.yc-dialog-btn-close')),
            cancel: modal.cancelButton,
            apply: modal.okButton,
            add: modal.okButton,
            // addByList: new Button(modal.find(getDataE2eSelector('Modal:footer').find('span.button2__text').withText('add by list').parent('button'))),
         },

         type: modal.tabs,

         literal: {
            name: new TextInput(modal.body.find('label[for=name]').nextSibling('div')),
            value: new TextArea(modal.body.find('label[for=value]').nextSibling('div')),
         },

         secret: {
            add: new Button(modal.body.find(getDataE2eSelector('SecretSubForm:AddVersion'))),

            modal: {
               addButton: new Button(SelectDataQa('AddSecretModal:AddButton')),
               cancelButton: new Button(SelectDataQa('AddSecretModal:CancelButton')),

               alias: new YCSelect(SelectDataE2e('AddSecretModal').find('label[for="secretUuid"]').nextSibling('div')),
               // version: new YCSelect(modal.find('label[for="versionUuid"]').nextSibling('div')),
            },

            name: new TextInput(modal.body.find('label[for=name]').nextSibling('div')),
            alias: new YCSelect(modal.body.find('.test-SecretSubForm-version')),
            key: new YCSelect(modal.body.find('.test-SecretSubForm-key')),
         },
      };

      this.actions = {
         addLiteralVariable: async (t, name, value) => {
            await this.buttons.addVariable.click(t);
            await modal.closeButton.click(t);
            await this.buttons.addVariable.click(t);
            await modal.cancelButton.click(t);
            await this.buttons.addVariable.click(t);

            await this.dialog.literal.name.typeText(t, name);
            await this.dialog.literal.value.typeText(t, value);
            await modal.okButton.click(t);
         },

         addSecretVariable: async (t, name, key) => {
            await this.buttons.addVariable.click(t);

            await this.dialog.type.select(t, 'Secret');

            await this.dialog.secret.name.typeText(t, name);

            await this.dialog.secret.add.click(t);
            await this.dialog.secret.modal.cancelButton.click(t);

            await this.dialog.secret.add.click(t);
            await this.dialog.secret.modal.alias.select(t, SECRET.ALIAS);
            await this.dialog.secret.modal.addButton.click(t);
            await this.dialog.secret.key.select(t, key);
         },

         deleteVariable: async (t, name) => {
            // TODO
         },
      };
   }
}
