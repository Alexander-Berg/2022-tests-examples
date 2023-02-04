package load

import (
	"context"
	"errors"
	"fmt"

	"github.com/opentracing/opentracing-go"
	"github.com/yandex/pandora/core"
	"github.com/yandex/pandora/core/provider"
	"github.com/yandex/pandora/core/register"
	"go.uber.org/zap"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/testing/load/generator"
)

type Ammo struct {
	// api uri (ex: /v1/get)
	URI string `json:"uri"`
	// If Method is GET - body is omitted
	// If Method is POST - body is used.
	Method  string            `json:"method"`
	Headers map[string]string `json:"headers"`
	// https://yandextank.readthedocs.io/en/latest/tutorial.html#tags
	Tag  string `json:"tag"`
	Body []byte `json:"body,omitempty"`
}

func NewAmmo(URI string, method string, headers map[string]string, tag string, body []byte) *Ammo {
	ammo := Ammo{
		URI:     URI,
		Method:  method,
		Headers: headers,
		Tag:     tag,
		Body:    body,
	}
	return &ammo
}

type RuntimeProviderConfig struct {
	GeneratorConfig generator.GeneratorConfig `config:"generatorConfig"`
	QueueConfig     provider.AmmoQueueConfig  `config:",squash"`

	Limit int `config:"limit" validate:"min=0"`
}

// RuntimeProvider allows to generate ammo in runtime.
// RuntimeProvider includes generator.Generator that generates ammo
type RuntimeProvider struct {
	provider.AmmoQueue

	gen  generator.Generator
	conf RuntimeProviderConfig
	core.ProviderDeps
}

func NewRuntimeProvider(genCtor func(generatorConfig generator.GeneratorConfig) generator.Generator, conf RuntimeProviderConfig) *RuntimeProvider {
	return &RuntimeProvider{
		AmmoQueue: *provider.NewAmmoQueue(func() core.Ammo {
			return &Ammo{}
		}, conf.QueueConfig),
		gen:  genCtor(conf.GeneratorConfig),
		conf: conf,
	}
}

func (r *RuntimeProvider) Run(ctx context.Context, deps core.ProviderDeps) error {
	r.ProviderDeps = deps

	defer close(r.OutQueue)

	if r.gen == nil {
		return errors.New("could not construct generator")
	}

	var ammoNum int

	for ; r.conf.Limit <= 0 || ammoNum < r.conf.Limit; ammoNum++ {
		span := opentracing.StartSpan("ProviderGenerateAmmo")

		span.SetTag("queue_size", len(r.OutQueue))

		ammo := r.gen.NextAmmo()

		if ammo == nil {
			return fmt.Errorf("prepared ammo is nil: %v", ammo)
		}

		insertSpan := opentracing.StartSpan("ProviderGenerateAmmo::AppendToQueue", opentracing.ChildOf(span.Context()))

		select {
		case r.OutQueue <- ammo:
		case <-ctx.Done():
			r.Log.Info("Provider context is Done", zap.Int("generated_ammo", ammoNum+1))
			insertSpan.Finish()
			insertSpan.SetTag("exiting", true)
			span.Finish()
			return nil
		}

		insertSpan.Finish()
		span.Finish()
	}

	r.Log.Info("Ammo limit is reached", zap.Int("generated_ammo", ammoNum+1))
	return nil
}

func (r *RuntimeProvider) Release(_ core.Ammo) {
	// do not overflow pool
}

func DefaultRegisterRuntimeProviderConfig() RuntimeProviderConfig {
	return RuntimeProviderConfig{
		QueueConfig: provider.DefaultAmmoQueueConfig(),
	}
}

// RegisterRuntimeProvider registers new provider of type RuntimeProvider.
// genCtor should return func, that constructs concrete implementation generator.Generator with config.
// generator.GeneratorConfig can be casted to concrete instance with mapstructure.Decode.
func RegisterRuntimeProvider(name string, genCtor func(generatorConfig generator.GeneratorConfig) generator.Generator) {
	register.Provider(
		name,
		func(conf RuntimeProviderConfig) core.Provider {
			return NewRuntimeProvider(genCtor, conf)
		},
		DefaultRegisterRuntimeProviderConfig,
	)
}
