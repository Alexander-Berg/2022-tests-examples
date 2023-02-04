import { PersonsSubpage, PersonsSubpageProps } from './subpage';
import { registry } from '../registry';
import * as components from '../components/components.desktop';

export class PersonsDesktopSubpage extends PersonsSubpage {
    constructor(props: PersonsSubpageProps) {
        registry.fill(components);
        super(props);
    }

    chooseArchivedPersons() {
        (this.wrapper.find('RadioButton[data-testid="archive"]').prop('onChange') as Function)({
            target: { value: 'archived' }
        });
    }

    getPersons() {
        return this.wrapper.find('.yb-persons__table-row');
    }
}
