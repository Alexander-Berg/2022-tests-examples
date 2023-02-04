import { Selector } from 'testcafe';
import { BaseComponent } from './BaseComponent';

export class Select extends BaseComponent {
   constructor(element) {
      super(element);
      this.button = element.find('.select2').find('button');

      this.popup = Selector('.select2__popup').find('.menu').filterVisible();
      this.popupItems = this.popup.find('.menu__item');
      this.popupSelectedItem = this.popup.find('.menu__item_checked_yes');
   }

   getPopupItemN = n => {
      return this.popupItems.find('span.menu__text').nth(n).parent('.menu__item');
   };

   getPopupItemByText = text => {
      return this.popupItems.find('span.menu__text').withText(text).parent('.menu__item');
   };

   getSelected = text => {
      return this.button.find('span.button2__text').withText(text);
   };

   hasItems = async (t, items) => {
      await this.findElement(t, this.button);
      await t
         .expect(this.popup.exists)
         .eql(false)
         .click(this.button)
         .expect(this.popup.exists)
         .eql(true)
         .expect(this.popupItems.count)
         .eql(items.length, `Should contains ${items.length} items`);

      for (let i = 0, l = items.length; i < l; i++) {
         await t.expect(this.popupItems.find('span.menu__text').withText(items[i]).exists).eql(true);
      }
   };

   change = async (t, actual, expected) => {
      await this.findElement(t, this.button);
      await t
         .expect(this.getSelected(actual).exists)
         .eql(true)
         .expect(this.button.classNames)
         .notContains('select2_disabled', '', { timeout: 120000 })
         // .expect(this.popup.exists).eql(false)
         .click(this.button)
         .expect(this.popup.exists)
         .eql(true, '', { timeout: 120000 })
         .expect(this.popupSelectedItem.find('span.menu__text').withText(actual).exists)
         .eql(true)
         .expect(this.popupSelectedItem.find('span.menu__text').withText(expected).exists)
         .eql(false)
         .expect(this.popupItems.find('span.menu__text').withText(expected).exists)
         .eql(true)
         .click(this.getPopupItemByText(expected))
         // .expect(this.popupSelectedItem.find('span.menu__text').withText(expected).exists).eql(true)
         .expect(this.getSelected(expected).exists)
         .eql(true);
   };

   select = async (t, expected) => {
      await this.findElement(t, this.button);
      await t
         .expect(this.button.classNames)
         .notContains('select2_disabled', '', { timeout: 120000 })
         // .expect(this.popup.exists).eql(false)
         .click(this.button)
         .expect(this.popup.exists)
         .eql(true, '', { timeout: 120000 })
         .expect(this.popupItems.find('span.menu__text').withText(expected).exists)
         .eql(true)
         .click(this.getPopupItemByText(expected))
         // .expect(this.popupSelectedItem.find('span.menu__text').withText(expected).exists).eql(true)
         // .expect(this.getSelected(expected).exists) // history rearrange
         // .eql(true);
   };

   selectFirst = async t => {
      await this.findElement(t, this.button);
      await t
         .expect(this.button.classNames)
         .notContains('select2_disabled', '', { timeout: 120000 })
         .expect(this.popup.exists)
         .eql(false)
         .click(this.button)
         .expect(this.popup.exists)
         .eql(true, '', { timeout: 120000 })
         .expect(this.popupItems.find('span.menu__text').nth(0).exists)
         .eql(true)
         .click(this.getPopupItemN(0));
   };

   /*
    selectN = async (t, n) => {
        await this.findElement(t, this.button);
        await t
            .expect(this.button.classNames).notContains('select2_disabled', '', {timeout: 120000})
            .expect(this.popup.exists).eql(false)
            .click(this.button)
            .expect(this.popup.exists).eql(true)
            .expect(this.popupItems.find('span.menu__text').nth(n).exists).eql(true)
            .click(this.getPopupItemN(n));
        // .expect(this.popupSelectedItem.find('span.menu__text').withText(expected).exists).eql(true)
        // .expect(this.getSelected(expected).exists).eql(true)
    } */

   readValue = async (t, value) => {
      await t.expect(this.getSelected(value).exists).eql(true);
   };
}
