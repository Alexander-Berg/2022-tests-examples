import { BaseComponent } from './BaseComponent';

export class Checkbox extends BaseComponent {
   constructor(element, name = '', direct = false) {
      super(element, name);

      if (direct) {
         this.checkbox = element.parent().parent();
         this.input = element;
         this.label = this.checkbox.find('label');
      } else {
         this.checkbox = element.find('span.checkbox'); // input
         this.input = element.find('input[type=checkbox]');
         this.label = element.find('label');
      }
   }

   async checked(t) {
      await this.findElement(t, this.checkbox);
      await t.expect(this.checkbox.classNames).contains('checkbox_checked_yes');
   }

   async unchecked(t) {
      await this.findElement(t, this.checkbox);
      await t.expect(this.checkbox.classNames).notContains('checkbox_checked_yes');
   }

   async check(t) {
      await this.findElement(t, this.checkbox);
      await this.unchecked(t, this.checkbox);
      await t.click(this.checkbox);
      await this.checked(t, this.checkbox);
   }

   async uncheck(t) {
      await this.findElement(t, this.checkbox);
      await this.checked(t, this.checkbox);
      await t.click(this.checkbox);
      await this.unchecked(t, this.checkbox);
   }
}
