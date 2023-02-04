const getVehicleCategoryName = require('./getVehicleCategoryName');

it('должен вернуть название категории для lcv', () => {
    expect(getVehicleCategoryName({ vehicle_info: { truck_category: 'LCV' } }).vehicle_nominative).toBe('Лёгкий коммерческий транспорт');
});

it('должен вернуть название категории для cars', () => {
    expect(getVehicleCategoryName({ category: 'CARS' }).vehicle_nominative).toBe('Автомобиль');
});

it('должен вернуть дефолтное (cars) название категории для несуществующей категории', () => {
    expect(getVehicleCategoryName({ vehicle_info: { truck_category: 'HUI' } }).vehicle_nominative).toBe('Автомобиль');
});
