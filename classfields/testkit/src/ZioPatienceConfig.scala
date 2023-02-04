package vertis.zio.test

import vertis.zio.test.ZioSpecBase.TestEnv
import zio.Schedule

/**
  */
case class ZioPatienceConfig(schedule: Schedule[TestEnv, Any, Any])
