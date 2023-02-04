package generator

import (
	"encoding/json"
	"fmt"
	"math/rand"
	"strings"

	"github.com/yandex/pandora/core"

	"a.yandex-team.ru/billing/hot/diod/pkg/server/schemas"
	btesting "a.yandex-team.ru/billing/library/go/billingo/pkg/testing"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/testing/load"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/testing/load/generator"
)

const (
	V1URIPattern = "/v1/batch"
	keyLen       = 5
	valueLen     = 5
	namespaceLen = 5
	maxCacheSize = 2_000
)

type Shell []schemas.BatchItem

type DefaultDiodGeneratorConfig struct {
	PercentageOfCreateOperations        float32 // [0.0; 1.0]
	PercentageOfNotExistingGetOperation float32 // [0.0; 1.0]
	PercentageOfOverwrittenRequests     float32 // [0.0; 1.0]
	PercentageOfGetRequests             float32 // [0.0; 1.0]
	MaxNumberOfKeys                     int
	ServiceIDs                          []string
}

func (d DefaultDiodGeneratorConfig) Print() string {
	return fmt.Sprintf("[DiodGeneratorConfiguration]\n"+
		"============================\n"+
		"PercentageOfCreateOperations\t[%f]\n"+
		"PercentageOfNotExistingGetOperation\t[%f]\n"+
		"PercentageOfOverwrittenRequests\t[%f]\n"+
		"PercentageOfGetRequests\t[%f]\n"+
		"MaxNumberOfKeys\t[%d]\n"+
		"ServiceIDs\t[%s]\n"+
		"============================\n",
		d.PercentageOfCreateOperations,
		d.PercentageOfNotExistingGetOperation,
		d.PercentageOfOverwrittenRequests,
		d.PercentageOfGetRequests,
		d.MaxNumberOfKeys,
		d.ServiceIDs)
}

type DefaultDiodGenerator struct {
	Config DefaultDiodGeneratorConfig

	ammoCache []Shell

	iterator int
}

var _ generator.Generator = (*DefaultDiodGenerator)(nil)

func NewDiodGenerator(config DefaultDiodGeneratorConfig) *DefaultDiodGenerator {
	return &DefaultDiodGenerator{Config: config}
}

func decide(probability float32) bool {
	return rand.Float32() < probability
}

func createShell(maxNumberOfKeys int) Shell {
	var shell Shell

	numberOfKeys := rand.Intn(maxNumberOfKeys) + 1

	namespace := btesting.RandS(namespaceLen)

	for i := 0; i < numberOfKeys; i++ {
		shell = append(shell, schemas.BatchItem{
			Namespace: namespace,
			Key:       btesting.RandS(keyLen),
			Value:     btesting.RandS(valueLen),
			Immutable: false,
		})
	}
	return shell
}

func (d *DefaultDiodGenerator) iterate() {
	d.iterator = (d.iterator + 1) % len(d.ammoCache)
}

func (d *DefaultDiodGenerator) getShellFromCache() Shell {
	if len(d.ammoCache) == 0 {
		return nil
	}

	shell := d.ammoCache[d.iterator]
	d.iterate()
	return shell
}

func updateShell(shell Shell) {
	for i := range shell {
		shell[i].Value = btesting.RandS(keyLen)
	}
}

func keyQueryFromShell(shell Shell) string {
	var keyQuery string
	for _, item := range shell {
		keyQuery += fmt.Sprintf("key=%s&", item.Key)
	}

	keyQuery = strings.TrimSuffix(keyQuery, "&")

	return keyQuery
}

func (d *DefaultDiodGenerator) convertShellToPostAmmo(shell Shell, tag string) *load.Ammo {
	body, _ := json.Marshal(map[string][]schemas.BatchItem{"items": shell})

	return load.NewAmmo(
		V1URIPattern,
		"POST",
		// X-Service-Ticket is added in the gun
		// HOST header is added in gun as well from target field (gun section)
		map[string]string{
			"X-Service-ID": d.Config.ServiceIDs[rand.Intn(len(d.Config.ServiceIDs))],
		},
		tag,
		body,
	)
}

func (d *DefaultDiodGenerator) convertShellToGetAmmo(shell Shell, tag string) *load.Ammo {
	return load.NewAmmo(
		strings.Join([]string{V1URIPattern, "?", "namespace=", shell[0].Namespace, "&", keyQueryFromShell(shell)}, ""),
		"GET",
		map[string]string{
			"X-Service-ID": d.Config.ServiceIDs[rand.Intn(len(d.Config.ServiceIDs))],
		},
		tag,
		nil,
	)
}

func (d *DefaultDiodGenerator) preparePostAmmo() *load.Ammo {
	var shell Shell
	var tag string

	if decide(d.Config.PercentageOfCreateOperations) || len(d.ammoCache) == 0 {
		shell = createShell(d.Config.MaxNumberOfKeys)
		tag = "v1_create_new"

		if len(d.ammoCache) < maxCacheSize {
			d.ammoCache = append(d.ammoCache, shell)
		} else if decide(d.Config.PercentageOfOverwrittenRequests) {
			d.ammoCache[d.iterator] = shell
			d.iterate()
		}

	} else {
		// update existing shell
		shell = d.getShellFromCache()
		updateShell(shell)
		tag = "v1_create_update"
	}

	return d.convertShellToPostAmmo(shell, tag)
}

func (d *DefaultDiodGenerator) prepareGetAmmo() *load.Ammo {

	var shell Shell
	var tag string
	if decide(d.Config.PercentageOfNotExistingGetOperation) || len(d.ammoCache) == 0 {
		shell = createShell(d.Config.MaxNumberOfKeys)
		tag = "v1_get_not_exists"
	} else {
		shell = d.getShellFromCache()
		tag = "v1_get_exists"
	}

	return d.convertShellToGetAmmo(shell, tag)
}

func (d *DefaultDiodGenerator) NextAmmo() core.Ammo {
	var ammo *load.Ammo
	if decide(d.Config.PercentageOfGetRequests) {
		ammo = d.prepareGetAmmo()
	} else {
		ammo = d.preparePostAmmo()
	}

	return ammo
}
