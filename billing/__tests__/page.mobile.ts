import { PageProps } from 'common/__tests__/common.page';

import { ReconciliationsPage } from './page';
import { registry } from '../registry';
import * as components from '../components/components.mobile';

export class ReconciliationsMobilePage extends ReconciliationsPage {
    constructor(props: PageProps) {
        registry.fill(components);
        super(props);
    }

    getRequests() {
        return this.wrapper.find('.yb-reconciliations-requests__list-item');
    }

    openRequestForm() {
        this.wrapper.find('button.yb-reconciliations-header__button').simulate('click');
    }
}
