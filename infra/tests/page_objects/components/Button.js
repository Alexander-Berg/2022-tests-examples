// import {Selector} from 'testcafe';
import { BaseComponent } from './BaseComponent';

export class Button extends BaseComponent {
   constructor(element, name) {
      super(element, name);
      this.element = element;

      // TODO удалить, использовать напрямую внутри click
      this.button = element.find('button');
   }

   async click(t) {
      await this.findElement(t, this.element);
      // if element has no parent selector (duplicateDeployUnit, deleteDeployUnit, tvm--add-client, tvm__button_delete-destination)
      if ((await this.element.tagName) === 'button') {
         await t.click(this.element);
      } else {
         await t.click(this.button);
      }
   }
}
