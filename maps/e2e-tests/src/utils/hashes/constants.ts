import { TaskTypeEnum } from '../constants';
import * as importKeyset from '../../../../src/translations/import';

export const EXAMPLE_TASKS_URLS_BY_TYPE: Record<TaskTypeEnum, string> = {
  [TaskTypeEnum.DEMO]: importKeyset.ru.taskExamples_demo_link,
  [TaskTypeEnum.SIMPLE_RU]: importKeyset.ru.taskExamples_0_link,
  [TaskTypeEnum.EXTENDED_RU]: importKeyset.ru.taskExamples_1_link,
  [TaskTypeEnum.SIMPLE_EN]: importKeyset.en.taskExamples_0_link,
  [TaskTypeEnum.EXTENDED_EN]: importKeyset.en.taskExamples_1_link,
  [TaskTypeEnum.SIMPLE_ES]: importKeyset.esLa.taskExamples_0_link,
  [TaskTypeEnum.EXTENDED_ES]: importKeyset.esLa.taskExamples_1_link,
};
