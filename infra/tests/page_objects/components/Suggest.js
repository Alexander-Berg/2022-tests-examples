// import {Selector} from 'testcafe';
import { getDataTestSelector } from '../../helpers/extractors';
import { BaseComponent } from './BaseComponent';

export class Suggest extends BaseComponent {
   constructor(element) {
      super(element);
      this.suggest = element.find('.suggest');
      this.input = this.suggest.find('input[type=text]');
      this.itemList = this.suggest.find('.suggest__omnibar_visible').find('.scrollable-list').filterVisible();
   }

   change = async (t, actual, expected) => {
      await this.findElement(t, this.suggest);
      await t
         .expect(this.input.exists)
         .eql(true)
         .expect(this.input.value)
         .eql(actual)
         .typeText(this.input, expected, { replace: true })
         .expect(this.itemList.exists)
         .eql(true, '', { timeout: 120000 })
         .expect(this.itemList.find('.scrollable-list__item').withExactText(expected).exists)
         .eql(true)
         .click(this.itemList.find('.scrollable-list__item').withExactText(expected))
         .expect(this.input.value)
         .eql(expected);
   };

   select = async (t, expected) => {
      await this.findElement(t, this.suggest);
      await t
         .expect(this.input.exists)
         .eql(true)
         .typeText(this.input, expected, { replace: true })
         .expect(this.itemList.exists)
         .eql(true, '', { timeout: 120000 })
         .expect(this.itemList.find('.scrollable-list__item').withExactText(expected).exists)
         .eql(true)
         .click(this.itemList.find('.scrollable-list__item').withExactText(expected))
         .expect(this.input.value)
         .eql(expected);
   };

   typeText = async (t, expected) => {
      await this.findElement(t, this.suggest);
      await t
         .expect(this.input.exists)
         .eql(true)
         .typeText(this.input, expected, { replace: true })
         .expect(this.input.value)
         .eql(expected);
   };

   readValue = async (t, value) => {
      await t.expect(this.input.exists).eql(true).expect(this.input.value).eql(value);
   };

   hasItems = async (t, timeout) => {
      await t.expect(this.itemList.exists).eql(true, '', { timeout });
   };

   getItems = async () => {
      const itemSelectors = this.element.find(getDataTestSelector('list-item'));
      const itemCount = await itemSelectors.count;
      const items = [];
      for (let i = 0; i < itemCount; i++) {
         const itemText = await itemSelectors.nth(i).textContent;
         items.push(itemText);
      }

      return items;
   };

   focus = async t => {
      await t.click(this.input);
   };

   // TODO: clearValue
   clear = async t => {
      await this.focus(t);
      await t.pressKey('ctrl+a delete');
   };
}
