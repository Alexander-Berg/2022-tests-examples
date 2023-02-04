package generator

import (
	"encoding/json"
	"fmt"
	"strings"

	"github.com/yandex/pandora/core"

	"a.yandex-team.ru/billing/hot/diod/pkg/server/schemas"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/testing/load"
)

type OneKeyDiodGeneratorConfig struct {
	PercentageOfUpdateOperations float32 // [0.0; 1.0]
	MaxNumberOfKeys              int

	ServiceID string
}

type OneKeyDiodGenerator struct {
	shell  Shell
	Config OneKeyDiodGeneratorConfig
}

func NewOneKeyGenerator(config OneKeyDiodGeneratorConfig) *OneKeyDiodGenerator {

	shell := createShell(config.MaxNumberOfKeys)

	fmt.Println("============== Generated key: ================")
	fmt.Printf("NAMESPACE: %s\n", shell[0].Namespace)

	for _, el := range shell {
		fmt.Printf("KEY: %s, VALUE: %s\n", el.Key, el.Value)
	}

	return &OneKeyDiodGenerator{shell: shell, Config: config}
}

func (o *OneKeyDiodGenerator) updateAmmo() {
	updateShell(o.shell)
}

func (o *OneKeyDiodGenerator) convertShellToPostAmmo() *load.Ammo {
	body, _ := json.Marshal(map[string][]schemas.BatchItem{"items": o.shell})

	return load.NewAmmo(
		V1URIPattern,
		"POST",
		map[string]string{
			"X-Service-ID": o.Config.ServiceID,
		},
		"v1_update",
		body,
	)
}

func (o *OneKeyDiodGenerator) convertShellToGetAmmo() *load.Ammo {
	return load.NewAmmo(
		strings.Join([]string{V1URIPattern, "?", "namespace=", o.shell[0].Namespace, "&", keyQueryFromShell(o.shell)}, ""),
		"GET",
		map[string]string{
			"X-Service-ID": o.Config.ServiceID,
		},
		"v1_get",
		nil,
	)
}

func (o *OneKeyDiodGenerator) NextAmmo() core.Ammo {
	if decide(o.Config.PercentageOfUpdateOperations) {
		o.updateAmmo()
		return o.convertShellToPostAmmo()
	}

	return o.convertShellToGetAmmo()
}
