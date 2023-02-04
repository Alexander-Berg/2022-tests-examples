package ru.yandex.vertis.telepony.client.records

import akka.actor.ActorSystem
import akka.testkit.TestKit
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import org.apache.commons.io.IOUtils
import ru.yandex.vertis.telepony.SpecBase

class RecordsBaseSpec extends TestKit(ActorSystem("RecognizeBaseSpec", ConfigFactory.empty)) with SpecBase {

  protected def readBytes(resource: String): Array[Byte] = IOUtils.toByteArray(getClass.getResourceAsStream(resource))
  // ffprobe shows:
  // 42 sec, Audio: mp3, 8000 Hz, mono, fltp, 8 kb/s
  // Как распознает speechkit cloud, с предварительной конвертацией в ogg
  // да здравствуйте (1) | подскажите 90 прикольно (1) | перепутали улица коляна (1) | дети квартиру (1) | ура (1) | поняла спасибо (1)
  protected val mts_old_record: Array[Byte] = readBytes("/records/mts_old_---MTrvomuw.mp3")

  // 38 sec, Audio: pcm_alaw ([6][0][0][0] / 0x0006), 8000 Hz, 1 channels, s16, 64 kb/s
  // Как распознает speechkit cloud, с предварительной конвертацией в ogg
  // але (1) | здорово (1) | это юр завяжи пожалуйста мужику этому спроси деньги все пришли (1) | блядь сейчас я моему вотсапе напишу хули она звонить уже время поздно (1) | да блядь напиши мы сейчас в этом вотсапе (1) | ну я тебя скачал в этом в вайбере на пришел ответ (1) | ну давай (1) | ну давай (1)
  protected val mts_new_record: Array[Byte] = readBytes("/records/mts_new_--JICtHKasQ.wav")

  // 24 sec, Audio: adpcm_ms ([2][0][0][0] / 0x0002), 8000 Hz, 2 channels, s16, 64 kb/s
  // да (1) | здравствуйте назани походу машину вот подъехали сейчас (2) | я понял сейчас я вас найду зонтик сейчас буду выходить там дождь идет еще (1) | ну да да (2) | захочу тогда еще не ладно сейчас я вас найду (1)
  protected val mtt_record: Array[Byte] = readBytes("/records/mtt_CTl8bKvm3R8.wav")
  // 1 - target, 2 - source

  // 40 sec, Audio: mp3, 8000 Hz, stereo, fltp, 64 kb/s
  // да слышала (2) | здравствуйте вы молодая да (1) | да (2) | закачать какая (1) | 80 (2) | а там сейчас все в порядке в кузов (1) | битая или ржавчина (1) | машина меловая (2) | приезжайте посмотрите можете меня не видеть (2) | ржавчина есть нет (1) | хорошо (1)
  protected val vox_record: Array[Byte] = readBytes("/records/vox_FJUd-3bNI9c.mp3")
  // 1 - source, 2 - target

  // 27 sec, Audio: mp3, 8000 Hz, mono, fltp, 16 kb/s
  // раз раз (1) | www.avto.ru (1) | авито (1) | карпрайс (1) | раз раз слышно (1) | проверка связи (1) | проверка (1) | алло алло (1) | вы где стоите (1) | все понял уезжаю (1)
  protected val beeline_record: Array[Byte] = readBytes("/records/beeline_119_186770948722331.mp3")

  protected val beeline_new_record: Array[Byte] = readBytes("/records/beeline_new_126_188712306623402.wav")
  // 1 - target, 2 - source

}
