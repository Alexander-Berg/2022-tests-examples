import { PageProps } from 'common/__tests__/common.page';

import { ReconciliationsPage } from './page';
import { registry } from '../registry';
import * as components from '../components/components.desktop';

export class ReconciliationsDesktopPage extends ReconciliationsPage {
    constructor(props: PageProps) {
        registry.fill(components);
        super(props);
    }

    getRequests() {
        return this.wrapper.find('.yb-reconciliations-requests__table-row');
    }

    openRequestForm() {}
}
