import { getDataE2eSelector, SelectDataE2e } from '../helpers/extractors';
import { Button } from './components/Button';
import { TextInput } from './components/TextInput';
import { Tumbler } from './components/Tumbler';
import { YCSelect } from './components/YcSelect';
import { ResourceTargetSelect } from './components/ResourceTargetSelect';

const form = SelectDataE2e('FormPageLayout:Header').parent('form');

export class ReleaseSandboxRule {
   buttons = {
      create: new Button(SelectDataE2e('FormPageLayout:Buttons').find('button').withText('Create')),
      cancel: new Button(SelectDataE2e('FormPageLayout:Buttons').find('button').withText('Cancel')),
      addPatch: new Button(SelectDataE2e('DeployPatches:AddPatch').find('button')),
   };

   id = new TextInput(form.find('label[for=id]').nextSibling('div'));
   description = new TextInput(form.find('label[for=description]').nextSibling('div'));
   releaseTypes = new YCSelect(form.find('label[for=releaseTypes]').nextSibling('div'));
   autocommit = new Tumbler(form.find('label[for=autocommit]').nextSibling('div'));

   taskType = new TextInput(form.find('label[for="sandbox.taskType"]').nextSibling('div'));

   resource = id => {
      return {
         resourceType: new TextInput(form.find(getDataE2eSelector(`DeployPatches:${id}:ResourceType`))),
         resourceTarget: new ResourceTargetSelect(form.find(getDataE2eSelector(`DeployPatches:${id}:ResourceTarget`))),
         name: new TextInput(form.find(getDataE2eSelector(`DeployPatches:${id}:Name`))),
         deleteButton: new Button(form.find(getDataE2eSelector(`DeployPatches:${id}:RemovePatch`))),
      };
   };
}
