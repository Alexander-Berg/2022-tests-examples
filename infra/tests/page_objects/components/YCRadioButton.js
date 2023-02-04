import { BaseComponent } from './BaseComponent';

export class YCRadioButton extends BaseComponent {
   constructor(element) {
      super(element);
      this.items = element.find('label.yc-radio-button__option');
      this.checked = element.find('label.yc-radio-button__option.yc-radio-button__option_checked');

      this.radioButton = this.items.parent('.yc-radio-button');
   }

   getLabel(value) {
      return this.items.find('span.yc-radio-button__option-text').withExactText(value).parent('label');
   }

   getCheckedLabel(value) {
      return this.checked.find('span.yc-radio-button__option-text').withExactText(value).parent('label');
   }

   async hasLabels(t, items) {
      await t.expect(this.items.count).eql(items.length, `Should contains ${items.length} items`);

      for (let i = 0, l = items.length; i < l; i++) {
         await t.expect(this.getLabel(items[i]).exists).ok();
      }
   }

   async change(t, actual, expected) {
      await this.findElement(t, this.radioButton);
      await t.click(this.getLabel(expected));
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
