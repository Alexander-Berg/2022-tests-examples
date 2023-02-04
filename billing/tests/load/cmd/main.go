package main

import (
	"github.com/mitchellh/mapstructure"

	diodGenerator "a.yandex-team.ru/billing/hot/diod/tests/load/generator"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/testing/load"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/testing/load/generator"
)

func main() {
	load.RegisterRuntimeProvider(
		"default_provider",
		func(generatorConfig generator.GeneratorConfig) generator.Generator {
			var diodConfig diodGenerator.DefaultDiodGeneratorConfig
			if mapstructure.Decode(generatorConfig, &diodConfig) != nil {
				// this will stop Pandora from running, but if you really want, you can panic.
				return nil
			}
			return diodGenerator.NewDiodGenerator(diodConfig)
		},
	)

	load.RegisterRuntimeProvider(
		"one_key_provider",
		func(generatorConfig generator.GeneratorConfig) generator.Generator {
			var diodConfig diodGenerator.OneKeyDiodGeneratorConfig
			if mapstructure.Decode(generatorConfig, &diodConfig) != nil {
				return nil
			}
			return diodGenerator.NewOneKeyGenerator(diodConfig)
		},
	)

	load.StartGun()
}
