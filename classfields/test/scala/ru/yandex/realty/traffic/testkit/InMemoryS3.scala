package ru.yandex.realty.traffic.testkit

import ru.yandex.realty.traffic.model.s3.S3Path
import ru.yandex.realty.traffic.model.s3.S3Path.{S3DirPath, S3FilePath}
import ru.yandex.realty.traffic.service.s3.S3
import ru.yandex.realty.traffic.service.s3.S3.S3
import ru.yandex.realty.traffic.testkit.InMemoryS3.InMemoryS3Exception.{IncompatibleTypesException, NoFileFound}
import ru.yandex.realty.traffic.testkit.InMemoryS3._
import ru.yandex.realty.traffic.testkit.InMemoryS3.Node.{DirectoryNode, FileNode}
import zio._
import zio.blocking.Blocking
import zio.stream.ZStream

import java.nio.file.{Files, Path}

class InMemoryS3(storage: ERefM[InMemoryS3Exception, Buckets], blocking: Blocking.Service) extends S3.Service {

  override def writeFile(filePath: S3Path.S3FilePath, file: Path): Task[Unit] = {
    for {
      _ <- ensureBucket(filePath.bucket)
      fileNode <- blocking
        .effectBlocking(Files.readAllBytes(file))
        .map(FileNode(filePath, _))
      _ <- storage.update(getUpdatedBucket(fileNode))
    } yield ()
  }

  override def writeString(file: S3FilePath, content: String): Task[Unit] =
    storage.update(getUpdatedBucket(FileNode(file, content.getBytes("UTF-8"))))

  private def nodesToRoot(fileNode: FileNode): UIO[Seq[Node]] =
    UIO.effectTotal {
      fileNode.path.dir.path
        .foldLeft(Seq[S3DirPath](S3DirPath(fileNode.path.bucket, Seq.empty))) {
          case (acc, currentPart) =>
            val dir = acc.last.child(currentPart)

            acc ++ Seq(dir)
        }
        .map(DirectoryNode) ++ Seq(fileNode)
    }

  private def overwrite(bucket: Bucket, node: Node): IO[InMemoryS3Exception, Bucket] = {
    ZIO.debug(s"overwrite $node") *>
      bucket
        .get(node.innerKey)
        .filter(_.objType != node.objType)
        .map { existing =>
          IO.fail(IncompatibleTypesException(existing.path.key, existing.objType, node.objType))
        }
        .getOrElse {
          UIO.effectTotal(bucket.updated(node.innerKey, node))
        }
  }

  private def getUpdatedBucket(fileNode: FileNode)(actual: Buckets): IO[InMemoryS3Exception, Buckets] =
    nodesToRoot(fileNode)
      .flatMap { toAdd =>
        ZIO.foldLeft(toAdd)(actual(fileNode.path.bucket))(overwrite)
      }
      .map(actual.updated(fileNode.path.bucket, _))

  override def list(dirPath: S3Path.S3DirPath): Task[Seq[S3Path]] =
    ensureBucket(dirPath.bucket) *>
      storage.get
        .map(_(dirPath.bucket))
        .map { bucketNode =>
          val innerKey = getInnerKey(dirPath)

          bucketNode
            .filterKeys(_.startsWith(innerKey))
            .values
            .collect {
              case Node.DirectoryNode(path) if isValidDirForList(dirPath, path) =>
                path
              case FileNode(path, _) if path.dir == dirPath =>
                path
            }
            .toSeq
        }

  private def isValidDirForList(search: S3DirPath, found: S3DirPath) = {
    println(s"is valid for list $search, $found")
    val equals = search == found

    !equals && (found.path.nonEmpty && search.child(found.path.last) == found)
  }

  override def deleteFile(path: S3Path.S3FilePath): Task[Unit] =
    ensureBucket(path.bucket) *>
      storage.update { st =>
        UIO
          .effectTotal {
            st(path.bucket).values.collect {
              case f: FileNode if f.innerKey != getInnerKey(path) =>
                f
            }
          }
          .flatMap(ZIO.foreach(_)(nodesToRoot))
          .map(_.flatten.map(_.innerKey).toSet)
          .map { requiredToStay =>
            st.updated(
              path.bucket,
              st(path.bucket).filterKeys(requiredToStay.contains)
            )
          }
      }

  private def ensureBucket(bucket: String): IO[InMemoryS3Exception, Unit] =
    storage.get
      .map { map =>
        map
          .get(bucket)
      }
      .someOrFail(InMemoryS3Exception.NoBucketFound(bucket))
      .unit

  override def createBucket(name: String): Task[Unit] =
    storage.update { mp =>
      UIO.effectTotal {
        if (mp.contains(name)) {
          mp
        } else {
          mp.updated(name, Map.empty)
        }
      }
    }

  override def read(file: S3FilePath): ZStream[Any, Throwable, Byte] =
    ZStream.fromIterableM(
      ensureBucket(file.bucket) *>
        storage.get
          .map(_(file.bucket))
          .flatMap { bucket =>
            bucket
              .get(getInnerKey(file))
              .filter(_.isFile)
              .map(_.asInstanceOf[FileNode].content)
              .map(UIO.effectTotal(_))
              .getOrElse(IO.fail(NoFileFound(file.key)))
          }
          .map(_.toIterable)
    )

  override def deleteAll(files: Seq[S3FilePath]): Task[Unit] =
    ZIO.foreach_(files)(deleteFile)

  override def copy(from: S3FilePath, to: S3FilePath): Task[Unit] =
    for {
      _ <- ensureBucket(from.bucket)
      _ <- ensureBucket(to.bucket)
      bytes <- read(from).runCollect
      fileNode <- UIO.effectTotal(FileNode(to, bytes.toArray))
      _ <- storage.update(getUpdatedBucket(fileNode))
    } yield ()
}

object InMemoryS3 {

  sealed class InMemoryS3Exception(msg: String) extends Throwable(msg)

  object InMemoryS3Exception {
    final case class NoBucketFound(name: String) extends InMemoryS3Exception(s"Bucket `$name` not found!")
    final case class NoFileFound(key: String) extends InMemoryS3Exception(s"File `$key` not found!")
    final case class IncompatibleTypesException(key: String, actualType: String, newType: String)
      extends InMemoryS3Exception(s"Couldn't change $key type from $actualType to $newType")
  }

  type BucketName = String
  type Bucket = Map[String, Node]
  type Buckets = Map[BucketName, Bucket]

  private def getInnerKey(path: S3Path): String =
    path match {
      case S3DirPath(_, path) => path.map(_.value).mkString("#")
      case S3FilePath(dir, name) => s"${getInnerKey(dir)}#${name.value}"
    }

  sealed trait Node {
    def path: S3Path

    def isFile: Boolean
    final def isDir: Boolean = !isFile
    final private[testkit] def innerKey: String = getInnerKey(path)
    final def objType: String =
      if (isDir) "directory" else "file"
  }

  object Node {
    final case class DirectoryNode(
      path: S3DirPath
    ) extends Node {
      override def isFile: Boolean = false
    }

    final case class FileNode(
      path: S3FilePath,
      content: Array[Byte]
    ) extends Node {
      override def isFile: Boolean = true
    }
  }

  private def bucketsLayer: UIO[ERefM[InMemoryS3Exception, Buckets]] =
    ZRefM.make(Map.empty[BucketName, Bucket])

  val live: URLayer[Blocking, S3] =
    Blocking.any ++ bucketsLayer.toLayer >>> (new InMemoryS3(_, _)).toLayer
}
