import { env } from './Environment';
import { secret } from './secret';

export const HOST = env.get('HOST', 'https://deploy.yandex-team.ru');

export const ROBOT_LOGIN = env.get('ROBOT_LOGIN', 'robot-infracloudui');

export const ROBOT_PASSWORD = env.get('ROBOT_PASSWORD');

export const STAGE_NAME = env.get('STAGE_NAME');

export const PROJECT_NAME = 'deploy-e2e';

export const SERVICE = env.get('SERVICE', 'backend');

export const ABC_SERVICE = 'Я.Деплой';

export const NETWORK = '_YA_DEPLOY_NETS_';

export const DISABLED_LOCATIONS = ['vla', 'man']; // кластеры с плохой кармой ("ученьки", "нет квоты" и т.д.)

console.log('Used constants:');
console.table({
   HOST,
   ROBOT_LOGIN,
   ROBOT_PASSWORD: secret(ROBOT_PASSWORD),
   STAGE_NAME,
   SERVICE,
});

export const ROUTES = {
   HOME: '/',
   NEW_STAGE: project => `/new-stage${project ? `?projectId=${project}` : ''}`,
   STAGE_STATUS: `/stages/${STAGE_NAME}`,
   STAGE_CONFIG: `/stages/${STAGE_NAME}/config`,
   STAGE_EDIT: `/stages/${STAGE_NAME}/edit`,
   STAGE_LOGS: `/stages/${STAGE_NAME}/logs`,
   STAGE_MONITORING: `/stages/${STAGE_NAME}/monitoring`,
   STAGE_HISTORY: `/stages/${STAGE_NAME}/history`,
   STAGE_TICKETS: `/stages/${STAGE_NAME}/deploy-tickets`,
   STAGE_BALANCERS: `/stages/${STAGE_NAME}/balancers`,
   PROJECT: name => `/projects/${name}`,
};

export const STAGE_STATUSES = {
   VALIDATION_SPEC: 'Validating spec',
   VALIDATION_FAILED: 'validation failed',
   DEPLOYING: 'in progress',
};

export const LINKS = {
   SUPPORT: {
      ST: 'https://st.yandex-team.ru/rtcsupport/',
      WIKI: 'https://wiki.yandex-team.ru/deploy/docs',
      TELEGRAM: 'https://t.me/joinchat/Be0kOD50fVxMoi_8hPvG6Q',
   },
};

export const SECRET = {
   ALIAS: 'aaa_testcafe_deploy',
   TVM: {
      SOURCES: {
         1: 'TVM_SOURCE_1',
         2: 'TVM_SOURCE_2',
      },
   },
};

/*
   Keys with **Name are deprecated, use **Id instead.
*/
export const DEFAULT_STAGE_PARAMS = {
   deployUnitName: 'deployUnit',
   deployUnitId: 'deployUnit',
   boxName: 'box',
   boxId: 'box',
   workloadName: 'workload',
   workloadId: 'workload',
};

export const DEFAULT_MCRS_STAGE_PARAMS = {
   ...DEFAULT_STAGE_PARAMS,
   deployUnitName: 'DeployUnitMCRS',
   deployUnitId: 'DeployUnitMCRS',
};

export const DEFAULT_RS_STAGE_PARAMS = {
   ...DEFAULT_STAGE_PARAMS,
   deployUnitName: 'DeployUnitRS',
   deployUnitId: 'DeployUnitRS',
};

export const TIMES_IN_S = {
   Second: 1,
   Minute: 60,
   Hour: 60 * 60,
   Day: 60 * 60 * 24,
   Week: 60 * 60 * 24 * 7,
};

export const TIMES_IN_MS = {
   Millisecond: 1,
   Second: 1000 * TIMES_IN_S.Second,
   Minute: 1000 * TIMES_IN_S.Minute,
   Hour: 1000 * TIMES_IN_S.Hour,
   Day: 1000 * TIMES_IN_S.Day,
   Week: 1000 * TIMES_IN_S.Week,
};

export const TIMEOUTS = {
   // presets

   /* 1 c - элемент уже должен быть на странице */
   immediately: TIMES_IN_MS.Second,

   /* 5 c - элемент либо уже есть, либо вот-вот будет (легкий API-запрос) */
   fast: 5 * TIMES_IN_MS.Second,

   /* 30 c - элемент скоро будет (тяжелый API-запрос) */
   slow: 30 * TIMES_IN_MS.Second,

   // forced. Значения, больше 30 секунд должны определятся явно для каждого случая.
   /**
    * Андрей Староверов:
    *
    * Поставьте 20 сек, если можете. Главное не создавать новый стейдж сразу после удаления.
    *
    * Это недостаток наследования ACL через stagectl, которое уйдет при переходе на наследование в YP.
    *
    * Если ты удалишь сразу после создания, то тоже ошибка будет,
    * потому что контроллеры не успеют твой стейдж подхватить, но это ретраями лечится хотя бы.
    */
   beforeRecreateStage: 60 * TIMES_IN_MS.Second,
   beforeRecreateProject: 60 * TIMES_IN_MS.Second, // могут приехать старые ACL #DEPLOY-5035
   beforeDeleteProject: 30 * TIMES_IN_MS.Second,

   projectAcl: 6 * TIMES_IN_MS.Minute,

   stageValidationAfterSave: 2 * TIMES_IN_MS.Minute,
   stageReadyAfterSave: 20 * TIMES_IN_MS.Minute,
   afterPassport: 2 * TIMES_IN_MS.Second,
   waitStageAcl: 30 * TIMES_IN_MS.Second,
};
