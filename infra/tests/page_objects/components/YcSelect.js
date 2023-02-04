import { Selector } from 'testcafe';
import { BaseComponent } from './BaseComponent';

export class YCSelect extends BaseComponent {
   constructor(element) {
      super(element);
      this.element = element;

      this.arrow = this.element.find('.yc-select-control__arrow');
      this.tokens = this.element.find('.yc-select-control__tokens');
      this.tokensText = this.tokens.find('.yc-select-control__tokens-text');

      this.control = this.arrow.parent('.yc-select-control');

      this.popup = Selector('.yc-select-popup').filterVisible();
      this.popupItems = this.popup.find('.yc-select-items');
      this.popupItem = this.popupItems.find('.yc-select-item__title');
      this.popupGroup = this.popup.find('.yc-select-items__group-title');
      this.search = this.popup.find('.yc-select-search').find('input');
   }

   readValue = async (t, value) => {
      await t.expect(this.tokensText.withExactText(value).exists).eql(true);
   };

   select = async (t, value, type = 'single', skipTextCheck = false) => {
      await this.findElement(t, this.control);

      await t
         .expect(this.control.classNames)
         .notContains('yc-select-control_disabled', '', { timeout: 1000 })
         .expect(this.popup.exists)
         .eql(false)
         .click(this.arrow)
         .expect(this.popup.exists)
         .eql(true);

      if (type === 'multiple') {
         for (const v of value) {
            await this.findElement(t, this.popupItem.withExactText(v));
            await t.click(this.popupItem.withExactText(v));
         }
         if (!skipTextCheck) {
            await this.readValue(t, value.sort().join(', '));
         }
      } else {
         if (await this.search.exists) {
            await t.typeText(this.search, value, { replace: true });
         }

         await this.findElement(t, this.popupItem.withText(value));
         await t.click(this.popupItem.withText(value).parent('.yc-select-item'));
         if (!skipTextCheck) {
            await this.readValue(t, value);
         }
      }
   };

   selectWithoutTextCheck = async (t, value, type = 'single') => {
      await this.select(t, value, type, true);
   };

   selectByGroup = async (t, group, value) => {
      await this.findElement(t, this.control);

      await t
         .expect(this.control.classNames)
         .notContains('yc-select-control_disabled', '', { timeout: 1000 })
         .expect(this.popup.exists)
         .eql(false)
         .click(this.arrow)
         .expect(this.popup.exists)
         .eql(true);

      await this.findElement(
         t,
         this.popupGroup
            .withExactText(group)
            .nextSibling('.yc-select-item-wrap')
            .find('.yc-select-item__title')
            .withText(value),
      );

      await t.click(this.popupItem.withText(value).parent('.yc-select-item'));
   };
}
