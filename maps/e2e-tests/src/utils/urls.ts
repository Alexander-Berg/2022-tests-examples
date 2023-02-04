import qs from 'qs';

const urls = {
  couriersList: {
    createLink: (companyId: string | number, params: { date?: string }): string => {
      const { date } = params;

      return `companies/${companyId}/depots/all/couriers?${qs.stringify({ date })}`;
    },
  },
  dashboard: {
    createLink: (companyId: string | number, params: { date?: string; sort?: string }): string => {
      const { date, sort } = params;

      return `companies/${companyId}/depots/all/dashboard?${qs.stringify({ date, sort })}`;
    },
    dateFormat: 'YYYY-MM-DD',
    sorts: {
      nameAsc: 'name-asc',
      nameDesc: 'name-desc',
      lateAsc: 'late-asc',
      lateDesc: 'late-desc',
      finishedAsc: 'finished-asc',
      finishedDesc: 'finished-desc',
      courierPositionStateAsc: 'courier_position_state-asc',
      courierPositionStateDesc: 'courier_position_state-desc',
    },
  },
  main: {
    createLink: (companyId: string | number): string => {
      return `/companies/${companyId}`;
    },
  },
  ordersList: {
    createLink: (
      companyId: string | number,
      params: { date?: string; filter?: string },
    ): string => {
      const { date, filter } = params;

      return `companies/${companyId}/depots/all/orders?date=${date || ''}&filter=${filter || ''}`;
    },
    dateFormat: 'YYYY-MM-DD',
    filters: {
      isCancelled: 'is:cancelled',
    },
  },
  usersCourier: {
    createLink: (companyId: string | number): string => {
      return `companies/${companyId}/depots/all/users/couriers`;
    },
  },
  mvrp: {
    openTask: (companyId: string | number, taskId: string): string => {
      return `companies/${companyId}/depots/all/mvrp/${taskId}`;
    },
  },
};

export default urls;
