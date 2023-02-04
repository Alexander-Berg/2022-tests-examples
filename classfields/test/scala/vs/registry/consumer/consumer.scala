package vs.registry

import bootstrap.config.Source
import bootstrap.ydb.YDB

package object consumer {
  type TEST = Source.Const[YDB.Config]

}
