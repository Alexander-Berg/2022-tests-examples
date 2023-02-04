import { t, Selector } from 'testcafe';
import { BaseComponent } from './BaseComponent';

export class CheckboxList extends BaseComponent {
   constructor(element) {
      super(element);
      this.popup = Selector('.popup2').filterVisible();
      this.button = element.find('button');
      this.list = this.popup.find('ul');
      this.items = this.list.child();
   }

   async open() {
      await t.click(this.button);
   }

   async close() {
      await t.click(this.button);
   }

   async select(textValues) {
      await this.open();

      textValues.forEach(async textValue => {
         await t.click(this.list.find('span').withText(textValue));
      });

      await this.close();
   }
}
