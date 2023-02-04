import { BaseComponent } from './BaseComponent';

export class RadioButton extends BaseComponent {
   constructor(element) {
      super(element);
      this.radioButton = element.find('span.radio-button');
      this.items = this.radioButton.find('label');
      this.checked = this.radioButton.find('label.radio-button__radio_checked_yes');
   }

   getLabel(value) {
      return this.items.find('span.radio-button__text').withExactText(value).parent('label');
   }

   getCheckedLabel(value) {
      return this.checked.find('span.radio-button__text').withExactText(value).parent('label');
   }

   async hasLabels(t, items) {
      await t.expect(this.items.count).eql(items.length, `Should contains ${items.length} items`);

      for (let i = 0, l = items.length; i < l; i++) {
         await t.expect(this.getLabel(items[i]).exists).ok();
      }
   }

   async change(t, actual, expected) {
      await this.findElement(t, this.radioButton);
      await t
         // .expect(this.getCheckedLabel(actual).exists)
         // .ok()
         // .expect(this.getLabel(expected).exists)
         // .ok()
         .click(this.getLabel(expected));
      // .expect(this.getCheckedLabel(expected).exists)
      // .ok();
   }

   async select(t, expected) {
      await this.findElement(t, this.radioButton);
      await t
         .expect(this.getLabel(expected).exists)
         .ok()
         .click(this.getLabel(expected))
         .expect(this.getCheckedLabel(expected).exists)
         .ok();
   }

   async readValue(t, value) {
      await t.expect(this.getCheckedLabel(value).exists).ok();
   }
}
