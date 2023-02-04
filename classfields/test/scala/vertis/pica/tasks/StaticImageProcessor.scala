package vertis.pica.tasks

import vertis.pica.model.ImageRecord
import vertis.pica.tasks.ImageTaskProcessor.ProcessingException
import vertis.zio.ServerEnv
import zio.{IO, ZIO}

/** @author ruslansd
  */
class StaticImageProcessor(effect: ImageRecord => IO[ProcessingException, ImageRecord]) extends ImageTaskProcessor {

  override def process(record: ImageRecord): ZIO[ServerEnv, ProcessingException, ImageRecord] =
    effect(record)
}
