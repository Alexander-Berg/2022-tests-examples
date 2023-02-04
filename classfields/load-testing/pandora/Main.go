package main

import (
	"context"
	"fmt"
	"github.com/spf13/afero"
	"github.com/yandex/pandora/cli"
	"github.com/yandex/pandora/core"
	"github.com/yandex/pandora/core/aggregator/netsample"
	coreimport "github.com/yandex/pandora/core/import"
	"github.com/yandex/pandora/core/register"
	"google.golang.org/grpc"
	pb "pandora/proto"
	"time"
)

type Ammo struct {
	Tag         string
	Query       string
}

type GunConfig struct {
	Target string `validate:"required"`
	Balancer string `validate:"required"`
}

type Gun struct {
	client 		  grpc.ClientConn
	conf          GunConfig
	aggr          core.Aggregator
	core.GunDeps
}

func NewGun(conf GunConfig) *Gun {
	return &Gun{conf: conf}
}

func (g *Gun) Bind(aggr core.Aggregator, deps core.GunDeps) error {
	ctx, cancel := context.WithTimeout(context.Background(), time.Second)
	defer cancel()
	conn, err := grpc.DialContext(ctx,
		g.conf.Target,
		grpc.WithInsecure(),
		grpc.WithUserAgent("load test, pandora custom shooter"),
		grpc.WithAuthority(g.conf.Balancer))
	if err != nil {
		fmt.Printf("grpc.Dial(...) failed with error %s\n", err)
		return err
	}
	g.client = *conn
	g.aggr = aggr
	g.GunDeps = deps
	return nil
}

func (g *Gun) Shoot(ammo core.Ammo) {
	customAmmo := ammo.(*Ammo)
	g.shoot(customAmmo)
}

func (g *Gun) shoot(ammo *Ammo) {

	code := 0
	sample := netsample.Acquire(ammo.Tag)

	conn := g.client
	client := pb.NewSearchClient(&conn)

	switch ammo.Tag {
	case "Search":
		code = g.send(client, ammo)
	default:
		code = 404
	}

	defer func() {
		sample.SetProtoCode(code)
		g.aggr.Report(sample)
	}()
}

func (g *Gun) send(client pb.SearchClient, ammo *Ammo) int {
	code := 0
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	metadata := map[string]string{
		"page_num": "0",
		"page_size": "72",
		"sort":"relevance",
		"asc":"false",
	}

	// here data from ammo should be used
	out, err := client.Execute(ctx,
		&pb.ExecutionRequest{
			Domain: &pb.DomainId{
				Id: "general",
			},
			Query: &pb.Query{
				Filter: &pb.Filter{
					Op: &pb.Filter_Eq{
						Eq: &pb.Eq{
							Field: "offer.region",
							Value: &pb.RawValue{
								ValueType: &pb.RawValue_Integer{
									Integer: &pb.IntegerValue{
										Primitive: &pb.IntegerValue_Sint64{
											Sint64: 213,
										},
									},
								},
								FulltextAlterView: nil,
							},
						},
					},
				},
				Text: &pb.Text{
					Query: ammo.Query,
				},
				Plan: &pb.ExecutionPlan{
					PlanId: "search",
				},
				Metadata: metadata,
			},
		})

	if err != nil {
		fmt.Printf(" FATAL: %s", err)
		code = 500
	}

	if out != nil {
		code = 200
	}
	return code
}

func main() {
	fs := afero.NewOsFs()
	coreimport.Import(fs)
	coreimport.RegisterCustomJSONProvider("custom_provider", func() core.Ammo { return &Ammo{} })
	register.Gun("grpc_pandora_gun", NewGun, func() GunConfig {
		return GunConfig{
			Target: "",
		}
	})
	cli.Run()
}
