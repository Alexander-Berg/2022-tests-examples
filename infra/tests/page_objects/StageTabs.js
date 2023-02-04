import { getDataTestSelector, SelectDataE2e } from '../helpers/extractors';
import { Link } from './components/Link';

export class StageTabs {
   constructor() {
      const wrapper = SelectDataE2e('StageIndexPage:Tabs');

      this.status = new Link(wrapper.find(getDataTestSelector('navigation-tabs--status')));

      this.config = new Link(wrapper.find(getDataTestSelector('navigation-tabs--config')));

      this.formNew = new Link(wrapper.find(getDataTestSelector('navigation-tabs--config'))); // TODO remove

      this.logs = new Link(wrapper.find(getDataTestSelector('navigation-tabs--logs')));

      this.monitoring = new Link(wrapper.find(getDataTestSelector('navigation-tabs--monitoring')));

      this.history = new Link(wrapper.find(getDataTestSelector('navigation-tabs--history')));

      this.tickets = new Link(wrapper.find(getDataTestSelector('navigation-tabs--deploy-tickets')));

      this.balancers = new Link(wrapper.find(getDataTestSelector('navigation-tabs--balancers')));
   }
}
