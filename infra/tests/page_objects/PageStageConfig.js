import { SelectDataTest, SelectDataE2e } from '../helpers/extractors';
import { PageBase } from './PageBase';

import { Link } from './components/Link';

export class PageStageConfig extends PageBase {
   body = SelectDataE2e('HugeForm');

   spinner = SelectDataTest('page-content').find('.page-stage__content-spinner');

   emptyContainer = {
      buttons: {
         launchNewStage: new Link(
            SelectDataTest('page-content').find('span.yc-button__text').withExactText('Launch new Stage').parent('a'),
         ),
      },
   };
}
