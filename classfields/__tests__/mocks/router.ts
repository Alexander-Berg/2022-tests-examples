/* eslint-disable @typescript-eslint/no-explicit-any */
/* eslint-disable @typescript-eslint/explicit-module-boundary-types */

export class Router {
    static link(routeName: string, routeParams: any): string {
        if (routeName === 'village') {
            return `/${routeParams.villageName}/`;
        }

        return '';
    }
}
