package runserver

import (
	"context"
	"log"
	"testing"
	"time"

	fesmock "a.yandex-team.ru/payplatform/fes/collector/tests/fesmock/lib"
)

func TestRunServer(t *testing.T) {
	ch := make(chan error, 1)
	fesServer := fesmock.FESmock{}

	go func() {
		ch <- fesServer.Start()
	}()

	select {
	case err := <-ch:
		t.Fatalf(err.Error())
	case <-time.After(20 * time.Second):
		err := fesServer.Stop(context.Background())
		if err != nil {
			log.Fatal("FESmock:", err)
		}
	}
}
