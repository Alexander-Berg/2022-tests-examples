import { PersonsSubpage, PersonsSubpageProps } from './subpage';
import { registry } from '../registry';
import * as components from '../components/components.mobile';

export class PersonsMobileSubpage extends PersonsSubpage {
    constructor(props: PersonsSubpageProps) {
        registry.fill(components);
        super(props);
    }

    chooseArchivedPersons() {
        (this.wrapper.find('Select[data-testid="archive"]').prop('onChange') as Function)(
            'archived'
        );
    }

    getPersons() {
        return this.wrapper.find('.yb-persons__list-item');
    }
}
