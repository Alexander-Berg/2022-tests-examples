// import {Selector} from 'testcafe';
import { BaseComponent } from './BaseComponent';

export class CopyToClipboard extends BaseComponent {
   constructor(element) {
      super(element);
      this.element = element.find('.copy-to-clipboard');
      this.icon = this.element.find('i.far');
      this.textarea = this.element.find('textarea');
   }

   copy = async t => {
      await this.findElement(t, this.icon);
      await t.click(this.icon);
   };

   getValue = async () => {
      const val = await this.textarea.value;

      return val;
   };
}
