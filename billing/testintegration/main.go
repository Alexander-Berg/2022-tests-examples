package main

import (
	"log"

	"github.com/spf13/cobra"

	"a.yandex-team.ru/billing/configshop/pkg/commands"
	"a.yandex-team.ru/billing/configshop/pkg/commands/testintegration"
	"a.yandex-team.ru/library/go/maxprocs"
)

func main() {
	maxprocs.AdjustAuto()

	cmd := &cobra.Command{
		Use:   "testintegration",
		Short: "configshop test integration binary",
		RunE:  commands.RunCommand(testintegration.CommandCreator),
	}
	commands.AddDefaultFlagSet(cmd)
	testintegration.AddLocalFlagSet(cmd)

	if err := cmd.Execute(); err != nil {
		log.Fatal(err)
	}
}
