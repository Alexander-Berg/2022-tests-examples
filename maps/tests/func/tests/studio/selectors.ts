const SELECTORS = {
    container: '.studio',
    mapStatusBar: {
        container: '.map-statusbar-view',
        layersMenuButton: '.map-statusbar-view .map-hover-menu__open-menu-button'
    },
    mapLayersMenu: {
        container: '.gui-menu',
        designControl: '.map-menu-item._day .checkbox__control',
        carparksControl: '.map-menu-item._carparks .checkbox__control',
        trfControl: '.map-menu-item._traffic .checkbox__control',
        trfeControl: '.map-menu-item._traffic-info .checkbox__control',
        streetViewControl: '.map-menu-item._street-view .checkbox__control',
        stvControl: '.map-menu-item._stv',
        mrceControl: '.map-menu-item._mrce',
        mrcpeControl: '.map-menu-item._mrcpe'
    }
} as const;

export {SELECTORS};
