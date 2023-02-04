package testintegration

import (
	"context"
	"os"

	"github.com/heetch/confita"
	"github.com/spf13/cobra"
	"golang.org/x/sync/errgroup"

	"a.yandex-team.ru/billing/configshop/pkg/commands"
	"a.yandex-team.ru/billing/configshop/pkg/core"
	"a.yandex-team.ru/billing/configshop/pkg/core/test/integration"
	"a.yandex-team.ru/billing/configshop/pkg/server"
	bcommands "a.yandex-team.ru/billing/library/go/billingo/pkg/commands"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/taskapp"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/library/go/core/log"
)

func CommandCreator(ctx context.Context, cmd *cobra.Command, loader *confita.Loader) (*core.Config, bcommands.Command, error) {
	config, err := core.ParseConfig(ctx, loader)
	if err != nil {
		return nil, nil, err
	}

	processorConfigPath, err := cmd.Flags().GetString("processor-config")
	if err != nil {
		return nil, nil, err
	}
	if processorConfigPath != "" {
		config.InfraTasks.ProcessorConfigPath = processorConfigPath
	}

	command := newCommand(config)
	return config, &command, nil
}

type Command struct {
	commands.StorageCommand

	processor *taskapp.TaskProcessor
}

func AddLocalFlagSet(cmd *cobra.Command) {
	cmd.Flags().String("processor-config", os.Getenv("PROCESSOR_CONFIG"), "processor config path")
}

func newCommand(cfg *core.Config) Command {
	cmd := Command{}
	cmd.Config = cfg
	return cmd
}

func (d *Command) Init(ctx context.Context) error {
	if err := d.StorageCommand.Init(ctx); err != nil {
		return err
	}

	sqsProcessor, err := integration.NewIntegrationTaskApp(ctx, d.Config.InfraTasks.ProcessorConfigPath, d.SQS)
	if err != nil {
		return err
	}
	d.processor = sqsProcessor

	return nil
}

func (d *Command) Run() error {
	app := server.New(d.NewRepository(), d.TVM, d.Config, d.Registry)

	g, ctx := errgroup.WithContext(d.Context())

	go func() {
		<-d.Context().Done()
		if err := app.Stop(context.TODO()); err != nil {
			xlog.Error(d.Context(), "app stop error", log.Error(err))
		}
	}()

	g.Go(func() error {
		return app.Start(ctx)
	})

	g.Go(func() error {
		return d.processor.Start(ctx)
	})

	return g.Wait()
}
