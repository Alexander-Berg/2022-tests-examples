import { BaseComponent } from './BaseComponent';
import { YCDropDown } from './YCDropDown';
import { TextInput } from './TextInput';

export class TextInputSelectField extends BaseComponent {
   constructor(element) {
      super(element);
      this.value = new TextInput(element);
      this.unit = new YCDropDown(element);
   }
}
