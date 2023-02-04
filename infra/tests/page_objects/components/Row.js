// import {Selector} from 'testcafe';
import { BaseComponent } from './BaseComponent';

export class Row extends BaseComponent {
   constructor(element) {
      super(element);
      this.element = element;
   }

   click = async t => {
      await this.findElement(t, this.element);
      await t.click(this.element);
   };
}
