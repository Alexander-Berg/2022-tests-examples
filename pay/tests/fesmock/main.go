package main

import (
	"context"
	"log"

	fesmock "a.yandex-team.ru/payplatform/fes/collector/tests/fesmock/lib"
)

func main() {
	server := fesmock.FESmock{}

	defer func() {
		err := server.Stop(context.Background())
		if err != nil {
			log.Fatal("FESmock stop:", err)
		}
	}()

	err := server.Start()
	if err != nil {
		log.Fatal("FESmock:", err)
	}
}
