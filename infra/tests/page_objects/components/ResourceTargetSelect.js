import { Selector, t } from 'testcafe';
import { BaseComponent } from './BaseComponent';

export class ResourceTargetSelect extends BaseComponent {
   constructor(element) {
      super(element);
      this.button = element.find('span').parent('div');
      this.popup = Selector('.popup2').filterVisible();
      this.items = this.popup.find('ul');
   }

   async open() {
      await t.click(this.button);
   }

   async close() {
      await t.click(this.button);
   }

   select = async textValue => {
      await this.open();
      await t.click(this.items.find('div').withText(textValue).parent('li'));
   };

   unselect = async textValue => {
      await this.open();
      await t.click(this.items.find('span').withText(textValue).parent('li'));
   };
}
