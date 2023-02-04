package ru.yandex.realty.giraffic.service

import ru.yandex.realty.canonical.base.params.{Parameter, ParameterType}
import ru.yandex.realty.canonical.base.request.Request
import ru.yandex.realty.giraffic.model.links._
import ru.yandex.realty.urls.landings.ListingFilterType
import ru.yandex.realty.urls.router.model.filter.FilterDeclaration
import zio.test.Assertion
import zio.test.Assertion._

object CheckUtils {

  def extractRequests(links: LinksPattern): Iterable[Request] =
    links.links.map(_.linkRequest)

  def hasSameGroupsPattern(expected: GroupPatterns): Assertion[GroupPatterns] =
    Assertion.assertion("isSameGroupsPattern")(Render.param(expected))(checkSameGroupsPattern(expected, _))

  def checkSameGroupsPattern(expected: GroupPatterns, actual: GroupPatterns): Boolean =
    dummyIterableEqCheck(
      expected.groups,
      actual.groups,
      checkSameGroupPattern
    )

  def containsPartWithSameRequest(
    expectedRequests: Iterable[Request],
    strategy: LinkSelectionStrategy
  ): Assertion[GroupPatterns] =
    Assertion.assertion("containsGroupPart")(Render.param((expectedRequests, strategy))) { actual =>
      actual.groups.exists { groupPart =>
        dummyIterableEqCheck[Request](
          expectedRequests,
          groupPart.linksPattern.links.map(_.linkRequest),
          (e, a) => e.key == a.key
        ) && strategy == groupPart.linksPattern.selectionStrategy
      }
    }

  private def dummyIterableEqCheck[A](expected: Iterable[A], actual: Iterable[A], eqCheck: (A, A) => Boolean): Boolean =
    if (expected.size != actual.size) false
    else expected.forall(e => actual.exists(eqCheck(e, _)))

  private def checkSameGroupPattern(expected: GroupPartPattern, actual: GroupPartPattern): Boolean =
    expected.groupTitle == actual.groupTitle && checkSameLinksPattern(expected.linksPattern, actual.linksPattern)

  private def checkSameLinksPattern(expected: LinksPattern, actual: LinksPattern): Boolean =
    expected.selectionStrategy == actual.selectionStrategy &&
      dummyIterableEqCheck(expected.links, actual.links, checkSameLinkPattern)

  private def checkSameLinkPattern(expected: LinkPattern, actual: LinkPattern): Boolean =
    expected.title == actual.title && expected.linkRequest.key == actual.linkRequest.key

  implicit class RequestOps(val req: Request) extends AnyVal {

    def addParams(params: Parameter*): Request =
      Request.Raw(
        req.`type`,
        req.params ++ params
      )

    def addFilterParams(filter: FilterDeclaration): Request =
      addParams(filter.parameters: _*)

    def addKnowType(lft: ListingFilterType): Request =
      addParams(lft.asParams.toSeq: _*)

    def dropParam(pt: ParameterType.Value): Request =
      dropParams(Set(pt))

    def dropParams(params: Set[ParameterType.Value]): Request =
      Request.Raw(
        req.`type`,
        req.params.filterNot(p => params.contains(p.`type`))
      )
  }
}
