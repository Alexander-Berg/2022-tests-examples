package test

import (
	"context"
	"io/ioutil"
	"log"
	"path/filepath"
	"testing"
	"time"

	"a.yandex-team.ru/infra/goxcart/internal/app"
	"a.yandex-team.ru/infra/goxcart/internal/config"
	"a.yandex-team.ru/library/go/test/yatest"
	"github.com/stretchr/testify/require"
	"github.com/vishvananda/netlink"
)

func TestCleanUpInterface(t *testing.T) {
	cfg, err := config.ReadYAML(yatest.SourcePath(filepath.Join(examplesDir, "extended.yaml")))
	require.NoError(t, err)
	a := app.App{
		Config:             cfg,
		BalancerConfigPath: "config.lua",
		BalancerBinPath:    filepath.Join(sandboxDir, "balancer"),
		Logger:             log.New(ioutil.Discard, "", 0),
	}
	ctx, cancelCtx := context.WithTimeout(context.Background(), 5*time.Second)
	defer cancelCtx()
	require.Equal(t, context.DeadlineExceeded, a.Start(ctx))
	_, err = netlink.LinkByName(app.Iface.Name) // try to find interface
	require.IsType(t, netlink.LinkNotFoundError{}, err)
}
