import { SelectDataTest } from '../helpers/extractors';
import { PageBase } from './PageBase';

export class PageStageBalancers extends PageBase {
   body = SelectDataTest('stage-balancers');
}
