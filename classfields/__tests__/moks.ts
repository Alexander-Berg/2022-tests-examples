export const heatmaps = ['infrastructure', 'carsharing', 'price-rent', 'price-sell', 'profitability', 'transport'];
export const initialState = {
    map: {
        heatmaps: {
            availableHeatmaps: heatmaps,
        },
    },
};

export const heatmapsAtPoint = heatmaps.map((item) => ({
    item,
    point: {
        level: 1,
    },
}));
