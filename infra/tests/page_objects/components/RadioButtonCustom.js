import { RadioButton } from './RadioButton';

export class RadioButtonCustom extends RadioButton {
   constructor(element) {
      super(element);
      this.radioButton = element.find('span.radio-button');
      this.items = this.radioButton.find('label');
      this.checked = this.radioButton.find('label.radio-button__radio_checked_yes');
   }

   getLabel = value => {
      return this.items.find(`input[value="${value.toLowerCase()}"]`).parent('label');
   };

   getCheckedLabel = value => {
      return this.checked.find(`input[value="${value.toLowerCase()}"]`).parent('label');
   };
}
