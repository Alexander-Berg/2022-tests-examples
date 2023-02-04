import { BaseComponent } from './BaseComponent';

export class Tumbler extends BaseComponent {
   constructor(element) {
      super(element);
      this.element = element;
      this.box = element.find('.tumbler__box');
      this.input = element.find('input');
   }

   readValue = async (t, value) => {
      await this.findElement(t, this.element);

      await t.expect(this.input.value).eql(value);
   };

   activate = async t => {
      await this.readValue(t, false);
      await t.click(this.box);
   };

   deactivate = async t => {
      await this.readValue(t, true);
      await t.click(this.box);
   };
}
