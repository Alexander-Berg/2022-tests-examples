import { AnyAction } from 'redux';

interface ActionLookup {
    count: number;
    promise?: Promise<any>;
    callback?: Function;
    reject?: Function;
}

export class ModuleSagaTester {
    private actionLookups: Record<string, ActionLookup> = {};

    private addAction(actionType: string, futureOnly = false) {
        let action = this.actionLookups[actionType];

        if (!action || futureOnly) {
            action = { count: 0 };
            action.promise = new Promise(function (resolve, reject) {
                action.callback = resolve;
                action.reject = reject;
            });
            this.actionLookups[actionType] = action;
        }

        return action;
    }

    getMiddleware() {
        const self = this;
        return () => (next: any) => (action: any) => {
            if (!action.type.startsWith('@@redux/')) {
                const actionObj = self.addAction(action.type);
                actionObj.count++;
                actionObj.callback?.(action);
            }
            return next(action);
        };
    }

    waitFor(actionType: string, futureOnly = false) {
        return this.addAction(actionType, futureOnly).promise;
    }

    dispatch(action: AnyAction) {
        throw new Error('У ModuleSagaTester нет доступа к store');
    }
}
