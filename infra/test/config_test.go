package test

import (
	"context"
	"os"
	"path"
	"path/filepath"
	"testing"

	"a.yandex-team.ru/infra/goxcart/internal/balancer"
	"a.yandex-team.ru/infra/goxcart/internal/config"
	"a.yandex-team.ru/library/go/test/yatest"
	"github.com/stretchr/testify/assert"
)

const (
	// TODO: where DATA() from ya.make actually is?
	examplesDir = "infra/goxcart/internal/config/testdata"

	// TODO: why here? always?
	sandboxDir = "pack"
)

func TestExamples(t *testing.T) {
	matches, err := filepath.Glob(yatest.SourcePath(filepath.Join(examplesDir, "*.yaml")))
	assert.NoError(t, err)
	for _, m := range matches {
		t.Run(path.Base(m), func(t *testing.T) {
			cfg, err := config.ReadYAML(m)
			assert.NoError(t, err)
			assert.NoError(t, cfg.GenLuaConfig("config.lua"))
			b := balancer.New(filepath.Join(sandboxDir, "balancer"), "config.lua", "-K")
			b.Stdout = os.Stdout
			b.Stderr = os.Stderr
			assert.NoError(t, b.Run(context.Background()))
		})
	}
}
