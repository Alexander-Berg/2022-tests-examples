import { ClientFunction } from 'testcafe';

/**
 * Базовый класс, описывающий общие для всех страниц действия
 *
 * @export
 * @class PageBase
 */
export class PageBase {
   /* eslint-disable no-undef */
   getLocation = ClientFunction(() => window.location.pathname + window.location.search);

   getLocationPathName = ClientFunction(() => window.location.pathname);

   getPageUrl = ClientFunction(() => window.location.href);

   go = ClientFunction(url => {
      window.navigateTo(url);
      return true;
   });

   reload = ClientFunction(() => {
      window.location.reload();
      return true;
   });

   goBack = ClientFunction(() => {
      window.history.back();
      return true;
   });

   scroll = ClientFunction((x, y) => window.scrollBy(x, y));

   scrollToElement = ClientFunction(({ top }) => {
      window.scrollTo(0, window.scrollY + top - 300);
      window.dispatchEvent(new Event('scroll'));
   });
   /* eslint-enable no-undef */

   findElement = async (test, element) => {
      await test.expect(element.exists).eql(true);
      // await test.expect(element.exists).eql(true, `element exists`, {timeout: 60000})
      // console.log(await element.boundingClientRect)
      await this.scrollToElement(await element.boundingClientRect);

      return element;
   };

   /* clickElement = async (test, element) => {
        await this.findElement(test, element);
        await test.click(element);
    }*/
}
