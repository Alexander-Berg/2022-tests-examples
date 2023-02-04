package ru.yandex.vertis.passport.service.user

import org.scalatest.FreeSpec
import ru.yandex.vertis.passport.dao.impl.couchbase.CouchbaseSessionDao
import ru.yandex.vertis.passport.dao.impl.memory._
import ru.yandex.vertis.passport.dao.impl.mysql.{LegacyAutoruUserDao, MysqlUserAuthTokenDao, MysqlUserPreviousPasswordsDao}
import ru.yandex.vertis.passport.dao._
import ru.yandex.vertis.passport.dao.impl.redis.{RedisIdentityTokenCache, RedisSessionDao, RedisUserEssentialsCache}
import ru.yandex.vertis.passport.test.{CouchbaseSupport, MySqlSupport, RedisSupport}
import ru.yandex.vertis.passport.util.mysql.DualDatabase

/**
  *
  * @author zvez
  */
trait DaoProvider {
  def sessionDao: SessionDao
  def userCache: UserEssentialsCache
  def userDao: FullUserDao
  def previousPasswordsDao: UserPreviousPasswordsDao
  def userAuthTokenDao: UserAuthTokenDao
  def userTicket: UserTicketDao
}

trait InMemoryDaoProvider extends DaoProvider {
  override lazy val sessionDao = new InMemorySessionDao
  override lazy val userCache = new InMemoryUserEssentialsCache
  override lazy val userDao = new InMemoryFullUserDao
  override lazy val previousPasswordsDao = new InMemoryUserPreviousPasswordsDao
  override lazy val userAuthTokenDao: UserAuthTokenDao = new InMemoryUserAuthTokenDao
  override lazy val userTicket: UserTicketDao = new InMemoryUserTicketCache
}

trait DbDaoProvider extends FreeSpec with DaoProvider with MySqlSupport with RedisSupport {

  lazy val sessionDao = new RedisSessionDao(createCache("test-session"))
  lazy val userCache = new RedisUserEssentialsCache(createCache("test-user-essentials"))
  lazy val userTicket = new InMemoryUserTicketCache

  lazy val userDao = new LegacyAutoruUserDao(
    DualDatabase(dbs.legacyUsers),
    DualDatabase(dbs.legacyOffice)
  )

  lazy val previousPasswordsDao = new MysqlUserPreviousPasswordsDao(DualDatabase(dbs.passport))

  lazy val userAuthTokenDao = new MysqlUserAuthTokenDao(DualDatabase(dbs.passport))

  lazy val identityTokenCache = new RedisIdentityTokenCache(createCache("test-identity-token"))
}
