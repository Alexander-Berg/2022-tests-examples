import { ClientFunction } from 'testcafe';
import { TIMEOUTS } from '../../helpers/constants';

export class BaseComponent {
   scrollToElement = ClientFunction(({ top }) => {
      window.scrollTo(0, window.scrollY + top - 300);
      window.dispatchEvent(new Event('scroll'));
   });

   /* eslint-enable no-undef */
   constructor(element, name = '', timeout = TIMEOUTS.slow) {
      this.element = element;
      this.name = name;

      this.checkOptions = { timeout };
   }

   async findElement(t, element) {
      await t.expect(element.exists).ok(this.name, this.checkOptions);
      await this.scrollToElement(await element.boundingClientRect);

      return element;
   }

   async click(t) {
      await this.findElement(t, this.element);
      await t.click(this.element);
   }

   async exists(t) {
      await t.expect(this.element.exists).ok(this.name, this.checkOptions);
   }

   async notExists(t) {
      await t.expect(this.element.exists).notOk(this.name, this.checkOptions);
   }
}
