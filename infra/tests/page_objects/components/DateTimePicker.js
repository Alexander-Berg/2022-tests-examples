import { Selector } from 'testcafe';
import { BaseComponent } from './BaseComponent';

export class DateTimePicker extends BaseComponent {
   constructor(element) {
      super(element);
      this.time = element.find('span.yc-text-input').nth(0).find('input.yc-text-input__control');
      this.date = element.find('span.yc-text-input').nth(1).find('input.yc-text-input__control');

      this.popup = Selector('.yc-popup.yc-popup_open').filterVisible();
      this.submit = this.popup.find('span.yc-button__text').withExactText('Select').parent('button');
   }

   setDateTime = async (t, date, time) => {
      await this.findElement(t, this.date);
      await t.typeText(this.date, date, { replace: true });
      await t.typeText(this.time, time, { replace: true });
      await t.click(this.submit);
   };

   readDateTime = async (t, date, time) => {
      await t.expect(this.date.value).eql(date);
      await t.expect(this.time.value).eql(time);
   };

   setDate = async (t, value) => {
      await this.findElement(t, this.date);
      await t.typeText(this.date, value, { replace: true });
      await t.click(this.submit);
   };

   setTime = async (t, value) => {
      await this.findElement(t, this.time);
      await t.typeText(this.time, value, { replace: true });
      await t.click(this.submit);
   };

   readDate = async (t, value) => {
      await t.expect(this.date.value).eql(value);
   };

   readTime = async (t, value) => {
      await t.expect(this.time.value).eql(value);
   };

   clearValue = async t => {
      await this.findElement(t, this.date);
      await t.click(this.date);
      await t.pressKey('shift+home delete shift+end delete');
      await t.click(this.submit);
   };
}
