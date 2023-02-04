import { Selector } from 'testcafe';
import { TextInput } from './components/TextInput';
import { YCSelect } from './components/YcSelect';

const form = Selector('form').find('header').find('h2').withText('Stage settings').parent('form');

export class FormNewStage {
   id = new TextInput(form.find('label[for=id]').nextSibling('div'));
   project = new YCSelect(form.find('label[for=project]').nextSibling('div'));
}
