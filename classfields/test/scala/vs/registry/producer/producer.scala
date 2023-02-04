package vs.registry

import bootstrap.config.Source
import bootstrap.ydb.YDB

package object producer {
  type TEST = Source.Const[YDB.Config]

}
