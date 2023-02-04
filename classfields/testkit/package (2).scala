package billing.howmuch.model

package object testkit {

  def mkRequestContext(criteriaList: (String, String)*): RequestContext =
    RequestContext(criteriaList.map { case (key, value) => RequestCriteria(key, value) })

  def mkRuleContext(criteriaList: (String, String)*): RuleContext =
    RuleContext(
      criteriaList.map {
        case (key, "*") =>
          RuleCriteria(key, RuleCriteria.Value.Fallback(true))
        case (key, value) =>
          RuleCriteria(key, RuleCriteria.Value.DefinedValue(value))
      }
    )
}
