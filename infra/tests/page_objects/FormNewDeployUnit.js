import {
   getDataE2eSelector,
   getDataQaSelector,
   getDataTestSelector,
   SelectDataE2e,
   SelectDataTest,
} from '../helpers/extractors';

import { Button } from './components/Button';
import { YCCheckbox } from './components/YCCheckbox';
import { YCRadioButton } from './components/YCRadioButton';
import { TextInput } from './components/TextInput';
import { TextInputSelectField } from './components/TextInputSelectField';
import { YCSelect } from './components/YcSelect';
import { YCSuggest } from './components/YcSuggest';
import { YCTabs } from './components/YcTabs';
import { FormNewTvm } from './FormNewTvm';
import { FormNewYasmTags } from './FormNewYasmTags';

const form = SelectDataE2e('SubForm:deployUnit');

const formTabs = new YCTabs(form.find('.yc-tabs'));
// tabs = new YCTabs(form.find(getDataE2eSelector('FormTabs')));

/**
 * Предоставляет набор экшенов и селекторов для редактирования деплой юнита
 *
 * @export
 * @class FormNewDeployUnit
 */
export class FormNewDeployUnit {
   buttons = {
      clone: new Button(form.find(getDataE2eSelector('SubForm:CloneButton'))),
      remove: new Button(form.find(getDataE2eSelector('SubForm:RemoveButton'))),
      restore: new Button(form.find(getDataE2eSelector('SubForm:RestoreButton'))),
   };

   formTabs = {
      selectForm: async t => {
         await formTabs.select(t, 'Form');
      },
      selectDisks: async t => {
         await formTabs.select(t, 'Disks, volumes and resources');
      },
      selectSecrets: async t => {
         await formTabs.select(t, 'Secrets');
      },
   };

   id = new TextInput(form.find('label[for=id]').nextSibling('div'));

   patchersRevision = new YCSelect(form.find('label[for=patchersRevision]').nextSibling('div'));

   primitive = new YCRadioButton(form.find('label[for=type]').nextSibling('div'));

   location = name => {
      const cluster = form.find(getDataQaSelector(`LocationCard:${name.toUpperCase()}`));
      return {
         enabled: new YCCheckbox(cluster.find(`input[name="locations[${name}].enabled"]`).parent('label')),
         podCount: new TextInput(cluster.find(`label[for="locations[${name}].podCount"]`).nextSibling('div')),
         disruptionBudget: new TextInput(
            form.find(`label[for="locations[${name}].disruptionBudget"]`).nextSibling('div'),
         ),
         maintenanceBudget: new TextInput(
            form.find(`label[for="locations[${name}].maxTolerableDowntimePods"]`).nextSibling('div'),
         ),
         maxMaintenanceDuration: new TextInput(
            form.find(`label[for="locations[${name}].maxTolerableDowntimeSeconds"]`).nextSibling('div'),
         ),
         antiaffinity: {
            perNode: {
               checkbox: new YCCheckbox(cluster.find(getDataTestSelector('Antiaffinity:PerNode')).find('.yc-checkbox')),
               maxPods: new TextInput(cluster.find(getDataTestSelector('Antiaffinity:PerNode')).find('.yc-text-input')),
            },
            perRack: {
               checkbox: new YCCheckbox(cluster.find(getDataTestSelector('Antiaffinity:PerRack')).find('.yc-checkbox')),
               maxPods: new TextInput(cluster.find(getDataTestSelector('Antiaffinity:PerRack')).find('.yc-text-input')),
            },
         },
      };
   };

   antiaffinity = {
      perNode: {
         checkbox: new YCCheckbox(form.find(getDataTestSelector('Antiaffinity:PerNode')).find('.yc-checkbox')),
         maxPods: new TextInput(form.find(getDataTestSelector('Antiaffinity:PerNode')).find('.yc-text-input')),
      },
      perRack: {
         checkbox: new YCCheckbox(form.find(getDataTestSelector('Antiaffinity:PerRack')).find('.yc-checkbox')),
         maxPods: new TextInput(form.find(getDataTestSelector('Antiaffinity:PerRack')).find('.yc-text-input')),
      },
   };

   cpu = new TextInputSelectField(form.find('label[for=cpu]').nextSibling('div'));

   ram = new TextInputSelectField(form.find('label[for=ram]').nextSibling('div'));

   disk = {
      type: new YCRadioButton(form.find('label[for="disks.[0].type"]').nextSibling('div')),
      size: new TextInput(form.find('label[for="disks.[0].size"]').nextSibling('div')),
      bandwidth: {
         guarantee: new TextInput(form.find('label[for="disks.[0].bandwidth.guarantee"]').nextSibling('div')),
      },

      layers: {
         buttons: {
            addLayer: new Button(form.find(getDataQaSelector('Layers:AddLayer'))),
         },

         card: i => {
            const layerCard = SelectDataTest(`Layer:${i}`);

            return {
               id: new TextInput(layerCard.find(getDataTestSelector('Layer:Id'))),
               url: new TextInput(layerCard.find(getDataTestSelector('Layer:URL'))),
               buttons: {
                  deleteLayer: new Button(layerCard.find(getDataE2eSelector('Layer:Remove'))),
               },
            };
         },
      },

      staticResources: {
         buttons: {
            addResource: new Button(form.find(getDataQaSelector('StaticResources:AddStaticResource'))),
         },

         card: i => {
            const resourceCard = SelectDataTest(`StaticResource:${i}`);

            return {
               id: new TextInput(
                  resourceCard.find(getDataQaSelector(`disks.[0].staticResources[${i - 1}].id:FieldLayout`)),
               ),
               url: new TextInput(
                  resourceCard.find(getDataQaSelector(`disks.[0].staticResources[${i - 1}].url:FieldLayout`)),
               ),
               buttons: {
                  deleteResource: new Button(resourceCard.find(getDataE2eSelector('StaticResource:Remove'))),
               },
            };
         },
      },
   };

   network = new YCSuggest(form.find('label[for="networkDefaults.networkId"]').nextSibling('div'));

   endpointSet = i => ({
      id: new TextInput(form.find(`label[for="endpointSets[${i}].id"]`).nextSibling('div')),
      port: new TextInput(form.find(`label[for="endpointSets[${i}].port"]`).nextSibling('div')),
   });

   disruptionBudget = new TextInput(form.find('label[for=disruptionBudget]').nextSibling('div'));
   maintenanceBudget = new TextInput(form.find('label[for=maxTolerableDowntimePods]').nextSibling('div'));
   maxMaintenanceDuration = new TextInput(form.find('label[for=maxTolerableDowntimeSeconds]').nextSibling('div'));

   yasmTags = new FormNewYasmTags(form.find('label[for=yasm]').nextSibling('div'));

   tvm = new FormNewTvm(form.find('label[for=tvm]').nextSibling('div'));

   // OLD FORM
   // constructor() {
   //    this.buttons = {
   //          'duplicateDeployUnit': new Button(deployUnit.find('.deploy-unit-section__button_duplicate').filterVisible()),
   //          'deleteDeployUnit': new Button(deployUnit.find('.deploy-unit-section__button_delete').filterVisible()),
   //          'addBox': new Button(deployUnit.nextSibling('.boxes-section__button_add').filterVisible())
   //    };

   //    this.name = new TextInput(deployUnit.find('div.form-layout__row-title').withExactText('Deploy Unit Name').parent('.form-layout__row').find('.form-layout__row-right'));

   //    this.primitive = new Select(deployUnit.find('div.deploy-unit-section__field_deploy-primitive'));

   //    this.location = (cluster) => {
   //          return {
   //             name: new Checkbox(deployUnit.find(getDataTestSelector(`form-field--location-${cluster}`))),
   //             podCount: new TextInput(deployUnit.find(getDataTestSelector(`form-field--location-${cluster}`)).find('.textinput').nth(0)),
   //             disruptionBudget: new TextInput(deployUnit.find(getDataTestSelector(`form-field--location-${cluster}`)).find('.textinput').nth(1)),
   //          };
   //    };

   //    this.antiaffinity = {
   //          maxPods: new TextInput(deployUnit.find(getDataTestSelector('antiaffinity--max-pods'))),
   //          key: new Select(deployUnit.find(getDataTestSelector('antiaffinity--key')))
   //    };

   //    this.disruptionBudget = new TextInput(deployUnit.find(getDataTestSelector('form-field--disruption-budget')));
   //    this.cpu = new TextInput(deployUnit.find(getDataTestSelector('form-field--cpu')));
   //    this.ram = new TextInput(deployUnit.find(getDataTestSelector('form-field--ram')));

   //    this.disk = {
   //          type: new RadioButton(deployUnit.find(getDataTestSelector('form-field--disk-type'))),
   //          size: new TextInput(deployUnit.find(getDataTestSelector('form-field--disk-size'))),
   //          bandwidth: new TextInput(deployUnit.find(getDataTestSelector('form-field--disk-bandwidth'))),
   //    };

   //    this.network = new Suggest(deployUnit.find('.form-field-suggest-network'));
   //    this.endpointSetPort = new TextInput(deployUnit.find(getDataTestSelector('form-field--endpoint-set-port')));

   //    this.tvm = new FormTvm(deployUnit);

   //    this.box = (name) => new FormBox(deployUnit.parent(getDataTestSelector('section')).find(`[data-test="section--box"]#${name}`));

   //    /*
   //    this.rename = async (t, newName) => {
   //          await this.name.typeText(t, newName);
   //    };

   //    this.changePrimitive = async (t, actual, expected) => {
   //          await this.primitive.change(t, actual, expected);
   //    };
   //    */

   //    // ??? (disruptionBudget)
   //    this.setDeployUnitLocation = async (t, cluster, pods) => {
   //          await this.location(cluster).name.check(t);
   //          await this.location(cluster).podCount.typeText(t, pods);
   //    };

   //    // ??? (disruptionBudget)
   //    this.unsetDeployUnitLocation = async (t, cluster) => {
   //          await this.location(cluster).name.uncheck(t);
   //    };
   // }
}
