package metrics

import (
	"a.yandex-team.ru/library/go/yandex/unistat"
	"a.yandex-team.ru/library/go/yandex/unistat/aggr"
)

type API struct {
	Latency  *unistat.Histogram
	Requests *unistat.Numeric
	Errors   *unistat.Numeric
}

func NewAPI() *API {
	serv := &API{
		Latency: unistat.NewHistogram("latency", 0,
			aggr.Histogram(),
			[]float64{0, 25, 50, 75, 100, 150, 200, 250, 500, 1000}),
		Requests: unistat.NewNumeric("requests", 1, aggr.Counter(), unistat.Sum),
		Errors:   unistat.NewNumeric("errors", 1, aggr.Counter(), unistat.Sum),
	}

	unistat.Register(serv.Latency)
	unistat.Register(serv.Requests)
	unistat.Register(serv.Errors)

	return serv
}
