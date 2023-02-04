package tvm_test

import (
	"log"

	vlogrus "github.com/YandexClassifieds/go-common/log/logrus"
	"github.com/YandexClassifieds/go-common/tvm"
	"github.com/YandexClassifieds/go-common/tvm/tvmauth"
	"github.com/YandexClassifieds/go-common/tvm/tvmtool"
)

func Example_tvmauth() {
	// example creating tvmauth CGO client
	log := vlogrus.NewLogger()
	tvmauth.NewClient(
		tvmauth.WithIssueTicket(42, []int{43, 44}, "some-secret"), // optional, can be skipped if issuing tickets not required
		tvmauth.WithLogger(log),
	)
}

func Example_tvmtool() {
	// example tvmtool client
	tvmtool.NewClient(tvmtool.NewConf("http://tvmtool:1234", "some-token"))
}

func Example_getTicket() {
	// initialised elsewhere
	var tvmCli tvm.Client

	// example acquiring service ticket
	ticketStr, err := tvmCli.ServiceTicket(42, 43)
	if err != nil {
		// react to error
	}
	_ = ticketStr
}

func Example_checkTicket() {
	// initialised elsewhere
	var tvmCli tvm.Client

	// example checking service ticket
	srvTicket, err := tvmCli.CheckServiceTicket("3:serv:GFFG___")
	if err != nil {
		// react to error
	}
	log.Printf("ticket metadata: %+v", srvTicket)
}
