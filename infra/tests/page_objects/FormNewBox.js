import { Selector } from 'testcafe';
import { getDataE2eSelector, getDataQaSelector, getDataTestSelector } from '../helpers/extractors';

import { Button } from './components/Button';
import { YCRadioButton } from './components/YCRadioButton';
import { TextInput } from './components/TextInput';
import { TextInputSelectField } from './components/TextInputSelectField';
import { YCTabs } from './components/YcTabs';

import { FormNewEnvironment } from './FormNewEnvironment';
import { YCSelect } from './components/YcSelect';

const form = Selector('form').find('header').find('h2').withText('Box settings').parent('form');

const formTabs = new YCTabs(form.find('.yc-tabs'));
// tabs = new YCTabs(form.find(getDataE2eSelector('FormTabs')));

const dockerImage = form.find('label[for=dockerImage]').nextSibling('div');
const layers = form.find(getDataTestSelector('Box:Layers'));
const staticResources = form.find(getDataTestSelector('Box:StaticResources'));

export class FormNewBox {
   buttons = {
      clone: new Button(form.find(getDataE2eSelector('SubForm:CloneButton'))),
      remove: new Button(form.find(getDataE2eSelector('SubForm:RemoveButton'))),
      restore: new Button(form.find(getDataE2eSelector('SubForm:RestoreButton'))),
   };

   formTabs = {
      selectForm: async t => {
         await formTabs.select(t, 'Form');
      },
      selectResources: async t => {
         await formTabs.select(t, 'Resources');
      },
   };

   id = new TextInput(form.find('label[for=id]').nextSibling('div'));

   resolvConf = new YCRadioButton(form.find('label[for=resolvConf]').nextSibling('div'));

   bindSkynet = new YCRadioButton(form.find('label[for=bindSkynet]').nextSibling('div'));

   cpuPerBox = new TextInputSelectField(form.find('label[for=cpuPerBox]').nextSibling('div'));

   ramPerBox = new TextInputSelectField(form.find('label[for=ramPerBox]').nextSibling('div'));

   threadLimit = new TextInput(form.find('label[for=threadLimit]').nextSibling('div'));

   // TODO:
   variables = new FormNewEnvironment(form.find('label[for=environment]').nextSibling('div'));

   dockerImage = {
      enabled: new YCRadioButton(dockerImage),
      name: new TextInput(dockerImage.find('input[name="dockerImage.name"]')),
      tag: new TextInput(dockerImage.find('input[name="dockerImage.tag"]')),
   };

   juggler = {
      mode: new YCRadioButton(form.find('label[for=juggler]').nextSibling('div')),
      settings: {
         // error: '', TODO
         buttons: {
            addJugglerBundle: new Button(form.find(getDataQaSelector('JugglerSettings:AddJugglerBundle'))),
         },
         port: new TextInput(form.find('label[for="juggler.port"]').nextSibling('div')),
         bundle: id => {
            const bundle = form(getDataTestSelector(`BundleRow:${id}`));
            return {
               url: new TextInput(bundle.find(getDataTestSelector('BundleRow:Url'))),
               buttons: {
                  deleteBundle: new Button(bundle.find(getDataE2eSelector(`Bundle${id}:Clear`))),
               },
            };
         },
      },
   };

   staticResources = {
      buttons: {
         addResource: new Button(form.find(getDataQaSelector('StaticResources:AddStaticResource'))),
      },

      resource: id => ({
         id: new YCSelect(staticResources.find(getDataTestSelector(`StaticResource:${id}:Id`))),
         mountPoint: new TextInput(staticResources.find(getDataTestSelector(`StaticResource:${id}:MountPoint`))),
         buttons: {
            deleteStaticResource: new Button(staticResources.find(getDataTestSelector(`StaticResource:${id}:Buttons`))),
         },
      }),
   };

   layers = {
      buttons: {
         'addLayer': new Button(form.find(getDataQaSelector('Layers:AddLayer'))),
      },

      layer: id => ({
         id: new YCSelect(layers.find(getDataTestSelector(`Layer:${id}:Id`))),
         buttons: {
            deleteLayer: new Button(layers.find(getDataTestSelector(`Layer:${id}:Buttons`))),
         },
      }),
   };

   // TODO:

   // OLD FORM
   // constructor(box) {
   //    this.buttons = {
   //          'duplicateBox': new Button(box.find('.boxes-section__buttons').find(getDataTestSelector('duplicate-box'))),
   //          'deleteBox': new Button(box.find('.boxes-section__buttons').find(getDataTestSelector('delete-box'))),
   //          'addWorkload': new Button(box.nextSibling('.workloads-section__button_add').filterVisible())
   //    };

   //    this.name = new TextInput(box.find('div.form-layout__row-title').withExactText('Box Name').parent('.form-layout__row').find('.form-layout__row-right'));

   //    this.resolvConf = new RadioButton(box.find(getDataTestSelector('form-field--resolv-conf')));

   //    this.juggler = {
   //          'mode': new RadioButton(box.find(getDataTestSelector('form-field--juggler-enabled'))), // 'enabled' | 'disabled'
   //          'settings': {
   //             'error': box.find('.boxes-section__juggler-settings').find('.boxes-section__error'),
   //             'buttons': {
   //                'addJugglerBundle': new Button(box.find('.boxes-section__juggler-settings').find('.boxes-section__add-juggler-check'))
   //             },
   //             'port': new TextInput(box.find('.boxes-section__juggler-settings').find(getDataTestSelector('form-field--juggler-port'))),
   //             'bundle': (id) => {
   //                const bundle = box.find('.boxes-section__juggler-settings').find(getDataTestSelector(`form-field--juggler-check-${id}`));

   //                return {
   //                      'url': new TextInput(bundle.find('.boxes-section__juggler-check-url')),
   //                      'buttons': {
   //                         'deleteBundle': new Button(bundle.find(getDataTestSelector('juggler-checks--delete-juggler-check')))
   //                      }
   //                };
   //             }
   //          }
   //    };

   //    this.staticResources = {
   //          'buttons': {
   //             'addResource': new Button(box.find('.static-resources__add-resource'))
   //          },

   //          // error:

   //          'resource': (id) => {
   //             const resource = box.find('.static-resources__resource').nth(id - 1);

   //             return {
   //                'index': resource.find('.static-resources__resource--index').find('div').withExactText(`#${id}`),
   //                'id': new TextInput(resource.find('.static-resources__resource--id')),
   //                'url': new TextInput(resource.find('.static-resources__resource--url')),
   //                'mountPoint': new TextInput(resource.find('.static-resources__resource--mount-point')),

   //                'buttons': {
   //                      'deleteResource': new Button(box.find('.static-resources__delete-resource'))
   //                }
   //             };
   //          }
   //    };

   //    this.layers = {
   //          'buttons': {
   //             'addLayer': new Button(box.find('.box-layers__add-layer'))
   //          },

   //          'base': {
   //             'type': new RadioButton(box.find('.base-layer__type')),
   //             'default': new Select(box.find('.base-layer__default')),
   //             'custom': new TextInput(box.find('.base-layer__custom')),
   //             'docker': {
   //                'name': new TextInput(box.find('.base-layer__docker-name')),
   //                'tag': new TextInput(box.find('.base-layer__docker-tag'))
   //             }
   //          },

   //          'layer': (id) => {
   //             const layer = box.find('.box-layers__layer').nth(id - 1);

   //             return {
   //                'index': layer.find('.box-layers__layer--index').find('div').withExactText(`#${id}`),
   //                'id': new TextInput(layer.find('.box-layers__layer--id')),
   //                'url': new TextInput(layer.find('.box-layers__layer--url')),

   //                'buttons': {
   //                      'deleteLayer': new Button(layer.find('.box-layers__delete-layer'))
   //                }
   //             };
   //          }
   //    };

   //    this.workload = (name) => new FormWorkload(box.parent(getDataTestSelector('section')).find(`[data-test="section--workload"]#${name}`));
   // }
}
