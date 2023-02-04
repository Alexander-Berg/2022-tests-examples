import { Selector } from 'testcafe';
import { getDataE2eSelector, getDataQaSelector, SelectDataE2e, SelectDataQa } from '../helpers/extractors';

import { TextInput } from './components/TextInput';
import { Select } from './components/Select';
import { YCSelect } from './components/YcSelect';
import { YCRadioButton } from './components/YCRadioButton';
import { Button } from './components/Button';

const modal = Selector('.modal.modal_visible_yes');

export class FormNewTvm {
   constructor(tvm) {
      this.mode = new YCRadioButton(tvm.find(getDataE2eSelector('Tvm:EnabledSwitcher')));

      this.buttons = {
         show: new Button(tvm.find(getDataQaSelector('FormAdvancedSection:ShowToggleButton'))),
         hide: new Button(tvm.find(getDataQaSelector('FormAdvancedSection:HideToggleButton'))),
      };

      this.settings = tvm.find(getDataE2eSelector('FormAdvancedSection:Body'));

      this.clientPort = new TextInput(tvm.find('label[for="tvm.clientPort"]').nextSibling('div'));

      this.blackbox = new Select(tvm.find('label[for="tvm.blackbox"]').nextSibling('div'));

      this.clients = {
         'buttons': {
            'addClient': new Button(tvm.find(getDataQaSelector('Tvm:AddClientButton'))),
         },

         'client': c => {
            const client = tvm.find(getDataE2eSelector(`TvmClient:${c}`));

            return {
               'buttons': {
                  'remove': new Button(client.find(getDataE2eSelector('TvmClient:RemoveClientButton'))),
                  'addDestination': new Button(client.find(getDataQaSelector('TvmClient:AddDestinationButton'))),
               },

               'source': {
                  'app': new TextInput(client.find(`[name="tvm.clients[${c - 1}].source.app"]`)),
                  'alias': new TextInput(client.find(`[name="tvm.clients[${c - 1}].source.alias"]`)),
               },

               'destination': d => {
                  const destination = client.find(getDataE2eSelector(`TvmDestination:${d}`));

                  return {
                     'buttons': {
                        'deleteDestination': new Button(
                           destination.find(getDataE2eSelector('TvmDestination:RemoveButton')),
                        ),
                     },

                     'app': new TextInput(
                        destination.find(`[name="tvm.clients[${c - 1}].destinations[${d - 1}].app"]`),
                     ),
                     'alias': new TextInput(
                        destination.find(`[name="tvm.clients[${c - 1}].destinations[${d - 1}].alias"]`),
                     ),
                  };
               },

               'secret': {
                  add: new Button(client.find(getDataQaSelector('SecretSubForm:AddVersion'))),

                  modal: {
                     addButton: new Button(SelectDataQa('AddSecretModal:AddButton')),
                     cancelButton: new Button(SelectDataQa('AddSecretModal:CancelButton')),

                     alias: new YCSelect(
                        SelectDataE2e('AddSecretModal').find('label[for="secretUuid"]').nextSibling('div'),
                     ),
                     // version: new YCSelect(modal.find('label[for="versionUuid"]').nextSibling('div')),
                  },

                  'alias': new YCSelect(client.find('.test-SecretSubForm-version')),
                  'key': new YCSelect(client.find('.test-SecretSubForm-key')),
               },
            };
         },
      };
   }
}
