package test

import (
	"context"
	"io/ioutil"
	"log"
	"path/filepath"
	"syscall"
	"testing"
	"time"

	"a.yandex-team.ru/infra/goxcart/internal/app"
	"a.yandex-team.ru/library/go/test/yatest"
	"github.com/stretchr/testify/require"
)

func TestWatchBalancer(t *testing.T) {
	a := app.App{
		ConfigPath:         yatest.SourcePath(filepath.Join(examplesDir, "extended.yaml")),
		BalancerConfigPath: "config.lua",
		BalancerBinPath:    filepath.Join(sandboxDir, "balancer"),
		Logger:             log.New(ioutil.Discard, "", 0),
	}
	ctx, cancelCtx := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancelCtx()
	go func() {
		<-time.After(3 * time.Second)
		require.NoError(t, syscall.Kill(a.BalancerPID(), syscall.SIGKILL))
	}()
	err := a.Start(ctx)
	require.NotEqual(t, context.DeadlineExceeded, err)
}
