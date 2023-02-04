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
	"a.yandex-team.ru/infra/goxcart/internal/config"
	"a.yandex-team.ru/library/go/test/yatest"
	"github.com/stretchr/testify/require"
)

func TestServerBind(t *testing.T) {
	cfg, err := config.ReadYAML(yatest.SourcePath(filepath.Join(examplesDir, "extended.yaml")))
	require.NoError(t, err)

	sockFD, err := syscall.Socket(syscall.AF_INET6, syscall.SOCK_STREAM|syscall.SOCK_CLOEXEC, 0)
	require.NoError(t, err)

	sa := &syscall.SockaddrInet6{
		Port: cfg.UniStat.Addr.Port,
	}
	copy(sa.Addr[:], cfg.UniStat.Addr.IP)
	require.NoError(t, syscall.Bind(sockFD, sa))

	ctx, cancelCtx := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancelCtx()

	a := app.App{
		Config:             cfg,
		BalancerConfigPath: "config.lua",
		BalancerBinPath:    filepath.Join(sandboxDir, "balancer"),
		Logger:             log.New(ioutil.Discard, "", 0),
	}
	// error should not be caused by context timeout
	require.NotEqual(t, context.DeadlineExceeded, a.Start(ctx))
}
