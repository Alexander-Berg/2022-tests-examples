package ru.auto.salesman.client.impl

import akka.http.scaladsl.server.Directives._
import com.google.protobuf.Message
import org.apache.http.entity.{ContentType, StringEntity}
import org.scalacheck.Gen
import ru.auto.api.ResponseModel.OfferListingResponse
import ru.auto.salesman.client.VosClient._
import ru.auto.salesman.client.impl.AutoRuVosClient.resolver
import ru.auto.salesman.client.proto.ProtoExecutorException.ConflictException
import ru.auto.salesman.client.proto.impl.ProtoExecutorImpl
import ru.auto.salesman.client.proto.{ProtoExecutor, Resolver}
import ru.auto.salesman.test.BaseSpec
import ru.auto.salesman.test.dao.gens.VosRequestGenerators
import ru.auto.salesman.test.model.gens.{OfferCategoryGen => _, _}
import ru.auto.salesman.util.{AutomatedContext, RequestContext}
import ru.yandex.vertis.protobuf.BasicProtoFormats.IntFormat
import ru.yandex.vertis.util.akka.http.protobuf.ProtobufSupport
import sourcecode.{File, Name}

class AutoRuVosClientSpec
    extends BaseSpec
    with VosRequestGenerators
    with ProtobufSupport {

  private val executor = mock[ProtoExecutor]
  private val client = new AutoRuVosClient(executor)
  implicit private val rc: RequestContext = AutomatedContext("test")

  private val addServicesMock = toMockFunction5 {
    executor.put(_: Query, _: Option[Message])(
      _: Resolver[Query],
      _: File,
      _: Name
    )
  }

  private val setCountersStartDateMock = toMockFunction6 {
    executor.post(_: Query, _: Option[Message])(
      _: Resolver[Query],
      _: RequestContext,
      _: File,
      _: Name
    )
  }

  "AutoRuVosClient.addServices()" should {

    "return ServicesAdded on OK from vos" in {
      forAll(
        OfferIdentityGen,
        Gen.option(OfferCategoryGen),
        readableString,
        addServicesRequestGen
      ) { (offerId, offerCategory, userRef, services) =>
        addServicesMock
          .expects(
            AddServicesQuery(offerId, offerCategory, userRef),
            Some(services),
            resolver,
            *,
            *
          )
          .returningZ(None)
        client
          .addServices(offerId, offerCategory, userRef, services)
          .success
          .value shouldBe ServicesAdded
      }
    }

    "return ServicesNotAdded on 409 from vos" in {
      forAll(
        OfferIdentityGen,
        Gen.option(OfferCategoryGen),
        readableString,
        addServicesRequestGen,
        Gen.option(readableString)
      ) { (offerId, offerCategory, userRef, services, conflictReason) =>
        addServicesMock
          .expects(
            AddServicesQuery(offerId, offerCategory, userRef),
            Some(services),
            resolver,
            *,
            *
          )
          .throwingZ(
            ConflictException(
              "",
              conflictReason.map(new StringEntity(_, ContentType.TEXT_PLAIN))
            )
          )
        val result = client
          .addServices(offerId, offerCategory, userRef, services)
          .success
          .value
        result shouldBe ServicesNotAdded(
          Some(
            s"failed request to []: ${conflictReason.getOrElse("(empty response entity)")}"
          )
        )
      }
    }

    "return exception on exception from vos" in {
      forAll(
        OfferIdentityGen,
        Gen.option(OfferCategoryGen),
        readableString,
        addServicesRequestGen
      ) { (offerId, offerCategory, userRef, services) =>
        val exception = new Exception
        addServicesMock
          .expects(
            AddServicesQuery(offerId, offerCategory, userRef),
            Some(services),
            resolver,
            *,
            *
          )
          .throwingZ(exception)
        client
          .addServices(offerId, offerCategory, userRef, services)
          .failure
          .exception shouldBe exception
      }
    }
  }

  "AutoRuVosClient.setCountersStartDate()" should {
    "work fine" in {
      forAll(OfferIdentityGen, Gen.option(OfferCategoryGen), dateTimeInPast) {
        (offerId, offerCategory, countersStartDate) =>
          setCountersStartDateMock
            .expects(
              SetCountersStartDateQuery(
                offerId,
                offerCategory,
                Some(countersStartDate)
              ),
              None,
              resolver,
              *,
              *,
              *
            )
            .returningT(None)
          client
            .setCountersStartDate(
              offerId,
              offerCategory,
              Some(countersStartDate)
            )
            .success
            .value shouldBe unit
      }
    }
  }

  "AutoruVosClient" should {

    def paramsUserOffersShouldBe(
        query: UserOffersQuery,
        params: Map[String, String],
        multiMapParams: Map[String, List[String]]
    ): Unit = {
      params.contains("owner") shouldBe true
      params("owner") shouldBe "0"

      query.geoId.foreach(_.toString shouldBe params("geobase_id"))
      val mark_model = (query.mark, query.model) match {
        case (Some(mark), None) => Some(mark.toString)
        case (Some(mark), Some(model)) => Some(s"$mark#$model")
        case _ => None
      }
      mark_model.foreach(_ shouldBe params("mark_model"))
      query.section.foreach(_.toString shouldBe params("section"))

      query.createDateFrom.foreach { createDate =>
        createDate.toString shouldBe params("create_date_from")
      }
      query.createDateTo.foreach { createDateTo =>
        createDateTo.toString shouldBe params("create_date_to")
      }

      if (query.statuses.nonEmpty)
        query.statuses.map(_.toString.toLowerCase) shouldBe multiMapParams(
          "status"
        )

      if (query.includeRemoved)
        params("include_removed") shouldBe "1"
      else
        params("include_removed") shouldBe "0"
    }

    def paramsGetUserOffersShouldBe(
        query: GetUserOffersQuery,
        params: Map[String, String],
        multiMapParams: Map[String, List[String]]
    ): Unit = {
      paramsUserOffersShouldBe(query, params, multiMapParams)

      query.page.toString shouldBe params("page")
      query.pageSize.toString shouldBe params("page_size")
    }

    def paramsCountUserOffersShouldBe(
        query: CountUserOffersQuery,
        params: Map[String, String],
        multiMapParams: Map[String, List[String]]
    ): Unit =
      paramsUserOffersShouldBe(query, params, multiMapParams)

    "getUserOffers right uri and right parameters in it without category" in {
      forAll(GetUserOffersQueryGen.map(_.copy(category = None)), offerGen()) {
        (query, offer) =>
          val serverAddress = runServer {
            (get & pathPrefix("api" / "v1" / "offers" / "all" / query.userRef) &
              parameterMap & parameterMultiMap &
              pathEnd) { (params, multiMapParams) =>
              paramsGetUserOffersShouldBe(query, params, multiMapParams)

              complete(OfferListingResponse.newBuilder.addOffers(offer).build)

            } ~ complete(throw new Exception("Didn't match route"))
          }

          val executor = new ProtoExecutorImpl(serverAddress.toString)

          val vosClient = new AutoRuVosClient(executor)

          vosClient.getUserOffers(query).success
      }
    }

    "getUserOffers right uri and right parameters in it with category" in {
      forAll(GetUserOffersQueryGen, offerGen()) { (query, offer) =>
        val serverAddress = runServer {
          (get & pathPrefix(
            "api" / "v1" / "offers" / query.category
              .map(_.toString.toLowerCase)
              .get / query.userRef
          ) &
            parameterMap & parameterMultiMap &
            pathEnd) { (params, multiMapParams) =>
            paramsGetUserOffersShouldBe(query, params, multiMapParams)

            complete(OfferListingResponse.newBuilder.addOffers(offer).build)

          } ~ complete(throw new Exception("Didn't match route"))
        }

        val executor = new ProtoExecutorImpl(serverAddress.toString)

        val vosClient = new AutoRuVosClient(executor)

        vosClient.getUserOffers(query).success
      }
    }

    "countUserOffers right uri and right parameters without category" in {
      forAll(
        CountUserOffersQueryGen.map(_.copy(category = None)),
        Gen.posNum[Int]
      ) { (query, count) =>
        val serverAddress = runServer {
          (get & pathPrefix(
            "api" / "v1" / "offers" / "all" / query.userRef / "count"
          ) &
            parameterMap & parameterMultiMap &
            pathEnd) { (params, multiMapParams) =>
            paramsCountUserOffersShouldBe(query, params, multiMapParams)

            complete(count)
          }
        }

        val executor = new ProtoExecutorImpl(serverAddress.toString)

        val vosClient = new AutoRuVosClient(executor)

        vosClient.countUserOffers(query).success
      }
    }

    "countUserOffers right uri and right parameters with category" in {
      forAll(CountUserOffersQueryGen, Gen.posNum[Int]) { (query, count) =>
        val serverAddress = runServer {
          (get & pathPrefix(
            "api" / "v1" / "offers" / "all" / query.userRef / "count"
          ) &
            parameterMap & parameterMultiMap &
            pathEnd) { (params, multiMapParams) =>
            paramsCountUserOffersShouldBe(query, params, multiMapParams)

            complete(count)
          }
        }

        val executor = new ProtoExecutorImpl(serverAddress.toString)

        val vosClient = new AutoRuVosClient(executor)

        vosClient.countUserOffers(query).success
      }
    }

  }

}
