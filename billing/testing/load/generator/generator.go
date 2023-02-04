package generator

import "github.com/yandex/pandora/core"

/*
	ammo examples

	{"uri": "/v1/test", "method": "GET", "tag": "test_ammo_get",
		"headers": {"X-Ya-Service-Ticket: ...", "X-Service-ID: ..." }}
	{"uri": "/v1/test", "method": "POST", "tag": "test_ammo_post",
		"headers": {"X-Ya-Service-Ticket: ...", "X-Service-ID: ..." }, "body": "body_data"}
*/

// GeneratorConfig is config that settings for generator.
// In order to cast GeneratorConfig to config for concrete generator - mapstructure.Decode can be used.
type GeneratorConfig any

// Generator is an object that generates ammo according to load pattern.
// Generator extends default Pandora scheme(https://wiki.yandex-team.ru/load/pandora/#custom)
//  by allowing to create different load patterns for one Provider in runtime.
// Generator is used with RuntimeProvider.
type Generator interface {
	// NextAmmo generates ammo for Provider in runtime.
	NextAmmo() core.Ammo
}
