import { BaseComponent } from './BaseComponent';

export class YCCheckbox extends BaseComponent {
   constructor(element, name = '') {
      super(element, name);

      this.input = element.find('input[type=checkbox]');
      this.checkbox = this.input.parent('label.yc-checkbox');
   }

   async checked(t) {
      await this.findElement(t, this.checkbox);
      await t.expect(this.checkbox.classNames).contains('yc-checkbox_checked');
   }

   async unchecked(t) {
      await this.findElement(t, this.checkbox);
      await t.expect(this.checkbox.classNames).notContains('yc-checkbox_checked');
   }

   async check(t) {
      await this.findElement(t, this.input);
      await this.unchecked(t, this.input);
      await t.click(this.input);
      await this.checked(t, this.input);
   }

   async uncheck(t) {
      await this.findElement(t, this.input);
      await this.checked(t, this.input);
      await t.click(this.input);
      await this.unchecked(t, this.input);
   }
}
