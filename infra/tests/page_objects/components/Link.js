import { BaseComponent } from './BaseComponent';

export class Link extends BaseComponent {
   constructor(element, name) {
      super(element, name);

      this.element = element;
      this.a = element.find('a');
   }

   click = async t => {
      await this.findElement(t, this.element);
      // if element has no parent selector
      if ((await this.element.tagName) === 'a') {
         await t.click(this.element);
      } else {
         await t.click(this.a);
      }
   };
}
