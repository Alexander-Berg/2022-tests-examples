import { runSaga } from 'redux-saga';
import { handleOrderClick } from 'modules/mvrp/selectedOrders/sagas';
import { toggleSelectedOrders } from 'modules/mvrp/selectedOrders/actions';
import { sagaMiddleware } from 'lib/store';
import mvrp from 'modules/mvrp/reducer';
import { applyMiddleware, combineReducers, compose, createStore } from 'redux';

describe('handleClick', () => {
  it('change map bounds after click on 1 point', async () => {
    const store = createStore(
      combineReducers({ mvrp }),
      {
        mvrp: {
          map: {
            bounds: null,
            solutionHash: null,
            zoom: null,
            solutionGeoJson: {
              points: {
                point_hello: {
                  type: 'Feature',
                  id: 'point_13526240',
                  payload: {},
                  options: {},
                  properties: {},
                  geometry: {
                    type: 'Point',
                    coordinates: [1, 2] as [number, number],
                  },
                },
              },
              lines: {},
            },
            isVisibleFreezeDialog: false,
          },
          task: {
            id: 'temp',
            depotsById: {},
            locationById: {},
            vehicleById: {},
            options: null,
            loading: false,
            isPreplanned: false,
            rawData: null,
          },
          selectedOrders: {
            ids: [],
            vehicleId: null,
          },
          solutionsById: {
            '123': {
              id: null,
              parentId: null,
              loading: false,
              isOptimisticSolution: false,
              estimate: null,
              error: false,
              hasCrossdock: false,
            },
          },
          solutionsList: ['123'],
        },
      },
      compose(applyMiddleware(sagaMiddleware)),
    );

    await runSaga(
      store,
      handleOrderClick,
      toggleSelectedOrders({
        ids: ['13526240'],
        shouldConcatIds: false,
        shouldCenterMap: true,
        vehicleId: '1584',
      }),
    ).toPromise();

    expect(store.getState().mvrp.map.bounds).toEqual([
      [0.995, 1.995],
      [1.005, 2.005],
    ]);
  });
});
