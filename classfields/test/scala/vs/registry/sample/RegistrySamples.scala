package vs.registry.sample

import strict.Bytes
import vs.registry.domain.{DocumentData, RegistryRecord, Status}

import java.time.Instant

object RegistrySamples {

  val one_0_1: RegistryRecord = RegistryRecord(
    pk = "one",
    version = 1,
    epoch = 0,
    modifyTimestamp = Instant.ofEpochSecond(1),
    ttl = None,
    document = DocumentData(
      status = Status.Exists,
      data = Bytes("one".getBytes),
    ),
  )

  val two_0_1: RegistryRecord = RegistryRecord(
    pk = "two",
    version = 1,
    epoch = 0,
    modifyTimestamp = Instant.ofEpochSecond(1),
    ttl = None,
    document = DocumentData(
      status = Status.Exists,
      data = Bytes("two".getBytes),
    ),
  )

  val three_0_1: RegistryRecord = RegistryRecord(
    pk = "three",
    version = 1,
    epoch = 0,
    modifyTimestamp = Instant.ofEpochSecond(1),
    ttl = None,
    document = DocumentData(
      status = Status.Exists,
      data = Bytes("three".getBytes),
    ),
  )

  val four_0_1: RegistryRecord = RegistryRecord(
    pk = "four",
    version = 1,
    epoch = 0,
    modifyTimestamp = Instant.ofEpochSecond(1),
    ttl = None,
    document = DocumentData(
      status = Status.Exists,
      data = Bytes(RawDocumentSamples.upsert1.toByteArray),
    ),
  )

}
