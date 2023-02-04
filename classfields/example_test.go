package blackbox_test

import (
	"context"

	"github.com/YandexClassifieds/go-common/blackbox"
	"github.com/YandexClassifieds/go-common/tvm"
)

func Example() {
	var tvmClient tvm.Client // need init
	blackbox.NewClient(blackbox.Config{SelfTvmId: 42}, tvmClient)

	// custom blackbox url
	blackbox.NewClient(blackbox.Config{SelfTvmId: 42}, tvmClient,
		blackbox.WithEndpoint("http://other-blackbox.yandex.net", 1337),
	)
}

func Example_checkSession() {
	var cli blackbox.Client

	// session only
	cli.CheckSession(context.Background(), "Session_id", "sessionid2", "::1")
}

func Example_checkOAuth() {
	var cli blackbox.Client

	// token only
	res, err := cli.CheckOAuth(context.Background(), "token", "1.2.3.4")
	// check result and err
	_ = res
	_ = err
}

func Example_custom() {
	var tc tvm.Client // init tvm client yourself
	cfg := blackbox.Config{SelfTvmId: 42}
	cli := blackbox.NewClient(cfg, tc,
		// use custom blackbox endpoint
		blackbox.WithEndpoint("https://other-blackbox.yandex.net", 1337))
	_ = cli
}
