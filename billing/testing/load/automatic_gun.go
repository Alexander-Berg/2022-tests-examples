package load

import (
	"bytes"
	"context"
	"crypto/tls"
	"fmt"
	"io"
	"io/ioutil"
	"math/rand"
	"net/http"
	"strings"
	"sync"
	"time"

	"github.com/opentracing/opentracing-go"
	"github.com/uber/jaeger-client-go"
	"github.com/uber/jaeger-client-go/config"
	"github.com/yandex/pandora/cli"
	"github.com/yandex/pandora/core"
	"github.com/yandex/pandora/core/aggregator/netsample"
	coreimport "github.com/yandex/pandora/core/import"
	"github.com/yandex/pandora/core/register"

	"a.yandex-team.ru/billing/library/go/billingo/pkg/testing/load/generator"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xlog"
	"a.yandex-team.ru/billing/library/go/billingo/pkg/xtrace"
	"a.yandex-team.ru/library/go/core/metrics/collect"
	"a.yandex-team.ru/library/go/core/metrics/collect/policy/inflight"
	"a.yandex-team.ru/library/go/core/metrics/solomon"
	"a.yandex-team.ru/library/go/maxprocs"
	"a.yandex-team.ru/library/go/yandex/tvm/cachedtvm"
	"a.yandex-team.ru/library/go/yandex/tvm/tvmtool"
)

const DefaultGunName = "billingun"

var cache AutomaticGunCache

type AutomaticGunCache struct {
	// tvmTicket for shooting with enabled tmv.
	tvmTicket string
	// setupOnce is used to setup tvm and tracing correctly.
	setupOnce sync.Once
	// ctx is internal context
	ctx context.Context
	// gunName is a name of the gun. Technically allows to use different pools, but Pandora supports only one pool.
	gunName string
}

type TvmConfig struct {
	// Enabled activates tvm configuration.
	Enabled bool
	// TvmAliasSrc is alias of service available in target service (see service tvmtool configuration).
	TvmAliasSrc string `validate:"required"`
	// TvmAliasSrc is alias of target service (see service tvmtool configuration).
	TvmAliasDst string `validate:"required"`
}

type TracingConfig struct {
	Enabled            bool
	ReporterQueueSize  int    `yaml:"reporterQueueSize"`
	ServiceName        string `yaml:"serviceName"`
	LocalAgentHostPort string `yaml:"localAgentHostPort"`
	CollectorEndpoint  string `yaml:"collectorEndpoint"`
}

type AutomaticGunConfig struct {
	// Target is host(FQDN of target pod) and port.
	Target string `validate:"required"`
	// Host is host(FQDN of target pod).
	Host string `validate:"required"`
	// TvmConfig setup settings for tvm, if service needs to authenticate via tvm.
	TvmConfig TvmConfig
	// TracingConfig allows tracing with opentelemetry package and send traces to Jaeger.
	TracingConfig TracingConfig
}

func defaultAutomaticGunConfig() AutomaticGunConfig {
	return AutomaticGunConfig{
		Target: "default target",
		Host:   "default host",
		TvmConfig: TvmConfig{
			Enabled:     false,
			TvmAliasSrc: "self",
			TvmAliasDst: "self",
		},
		TracingConfig: TracingConfig{
			Enabled:            false,
			ReporterQueueSize:  5000,
			ServiceName:        "active-gun",
			LocalAgentHostPort: "",
			CollectorEndpoint:  "",
		},
	}
}

type AutomaticGun struct {
	client http.Client
	conf   AutomaticGunConfig
	aggr   core.Aggregator

	gen generator.Generator
	core.GunDeps
}

func NewAutomaticGun(conf AutomaticGunConfig) *AutomaticGun {
	return &AutomaticGun{conf: conf}
}

func prepareTvmTicket(srcAlias string, dstAlias string) {
	if cache.tvmTicket != "" {
		return
	}

	client, err := tvmtool.NewDeployClient(tvmtool.WithSrc(srcAlias))
	if err != nil {
		panic(err)
	}

	tvmClient, err := cachedtvm.NewClient(client, cachedtvm.WithCheckServiceTicket(1*time.Minute, 1_000))

	if err != nil {
		panic(err)
	}

	ticket, err := tvmClient.GetServiceTicketForAlias(context.Background(), dstAlias)

	if err != nil {
		panic(err)
	}

	cache.tvmTicket = ticket
}

func (g *AutomaticGun) Bind(aggr core.Aggregator, deps core.GunDeps) error {
	g.aggr = aggr
	g.GunDeps = deps

	tr := &http.Transport{
		MaxIdleConns:       1,
		IdleConnTimeout:    0,
		DisableCompression: true,
		TLSClientConfig:    &tls.Config{InsecureSkipVerify: true},
	}

	g.client = http.Client{Transport: tr}

	// this section of setup is done only once.
	cache.setupOnce.Do(func() {
		if g.conf.TvmConfig.Enabled {
			prepareTvmTicket(g.conf.TvmConfig.TvmAliasSrc, g.conf.TvmConfig.TvmAliasDst)
		}

		if g.conf.TracingConfig.Enabled {
			setupTracing(g.conf.TracingConfig)
		}
	})

	return nil
}

func (g *AutomaticGun) Shoot(ammo core.Ammo) {
	customAmmo := ammo.(*Ammo)
	g.shoot(customAmmo)
}

func (g *AutomaticGun) shoot(ammo *Ammo) {
	code := 0

	var body io.Reader

	if ammo.Method == "GET" {
		body = nil
	} else {
		body = bytes.NewReader(ammo.Body)
	}

	req, _ := http.NewRequest(ammo.Method, strings.Join([]string{g.conf.Target, ammo.URI}, ""), body)

	for header, val := range ammo.Headers {
		req.Header.Add(header, val)
	}

	req.Header.Add("Host", g.conf.Host)

	if g.conf.TvmConfig.Enabled {
		req.Header.Add("X-Ya-Service-Ticket", cache.tvmTicket)
	}

	sample := netsample.Acquire(ammo.Tag)

	rs, err := g.client.Do(req)

	if err == nil {
		code = rs.StatusCode
		_, _ = ioutil.ReadAll(rs.Body)
		_ = rs.Body.Close()
	} else {
		fmt.Println(err)
	}

	defer func() {
		sample.SetProtoCode(code)
		g.aggr.Report(sample)
	}()
}

func setupTracing(conf TracingConfig) {
	registry := solomon.NewRegistry(
		solomon.
			NewRegistryOpts().
			AddCollectors(
				cache.ctx, inflight.NewCollectorPolicy(), collect.GoMetrics, collect.ProcessMetrics, xlog.LoggerMetrics,
			),
	)

	reporterConfig := &config.ReporterConfig{
		QueueSize:          conf.ReporterQueueSize,
		LocalAgentHostPort: conf.LocalAgentHostPort,
		CollectorEndpoint:  conf.CollectorEndpoint,
	}

	reporter, err := reporterConfig.NewReporter(conf.ServiceName, xtrace.JaegerReporterMetrics(registry), nil)

	if err != nil {
		panic(err)
	}

	tracer, closer, err := config.Configuration{
		Disabled:    false,
		ServiceName: conf.ServiceName,
		Sampler: &config.SamplerConfig{
			Type:  jaeger.SamplerTypeConst,
			Param: 1,
		},
	}.NewTracer(config.Reporter(reporter))

	if err != nil {
		panic(err)
	}

	opentracing.SetGlobalTracer(tracer)

	canceler(cache.ctx, closer)
}

func canceler(ctx context.Context, closer io.Closer) {
	<-ctx.Done()
	if closer != nil {
		_ = closer.Close()
	}
}

func SetGunName(newName string) {
	cache.gunName = newName
}

func StartGun() {
	maxprocs.AdjustAuto()
	rand.Seed(time.Now().UnixNano())

	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	cache.ctx = ctx

	if cache.gunName == "" {
		SetGunName(DefaultGunName)
	}

	fs := coreimport.GetFs()
	coreimport.Import(fs)

	register.Gun(cache.gunName, NewAutomaticGun, defaultAutomaticGunConfig)

	cli.Run()
}
