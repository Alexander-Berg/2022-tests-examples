// import {Selector} from 'testcafe';
import { BaseComponent } from './BaseComponent';

export class TextArea extends BaseComponent {
   constructor(element) {
      super(element);
      this.textarea = element.find('textarea');
      this.clear = element.find('span.icon.textarea__clear');
   }

   typeText = async (t, text) => {
      await this.findElement(t, this.textarea);
      await t.typeText(this.textarea, text, { replace: true });
   };

   replaceText = async (t, actual, expected) => {
      await this.findElement(t, this.textarea);
      await t
         .expect(this.textarea.value)
         .eql(actual)
         .typeText(this.textarea, expected, { replace: true })
         .expect(this.textarea.value)
         .eql(expected);
   };

   readValue = async (t, value) => {
      await this.findElement(t, this.textarea.withText(value));
   };

   clearValue = async t => {
      await this.findElement(t, this.clear);
      await t.click(this.clear);
   };
}
