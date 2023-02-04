import { SelectDataTest } from '../helpers/extractors';
import { PageBase } from './PageBase';

export class PageStageMonitoring extends PageBase {
   body = SelectDataTest('view-stage--monitoring');
}
