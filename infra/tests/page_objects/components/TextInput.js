// import {Selector} from 'testcafe';
import { BaseComponent } from './BaseComponent';

export class TextInput extends BaseComponent {
   constructor(element) {
      super(element);
      this.element = element;
      this.input = this.element.find('input');
   }

   readValue = async (t, value) => {
      await this.findElement(t, this.element);

      if ((await this.element.tagName) === 'input') {
         await t.expect(this.element.value).eql(value);
      } else {
         await t.expect(this.input.value).eql(value);
      }
   };

   // to rename Deploy Unit, Box, Workload
   typeText = async (t, text) => {
      await this.findElement(t, this.element);

      if ((await this.element.tagName) === 'input') {
         await t.typeText(this.element, text, { replace: true });
         await this.readValue(t, text);
      } else {
         await t.typeText(this.input, text, { replace: true });
         await this.readValue(t, text);
      }
   };

   replaceText = async (t, actual, expected) => {
      await this.findElement(t, this.element);
      await this.readValue(t, actual);
      await this.typeText(t, expected);
      await this.readValue(t, expected);
   };

   clearValue = async t => {
      await this.findElement(t, this.element);

      if ((await this.element.tagName) === 'input') {
         await t.click(this.element);
      } else {
         await t.click(this.input);
      }

      await t.pressKey('ctrl+a delete');

      await this.readValue(t, '');
   };
}
