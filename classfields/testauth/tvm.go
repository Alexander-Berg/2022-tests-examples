package testauth

import (
	"encoding/json"
	"fmt"
	"github.com/YandexClassifieds/goLB"
	"io/ioutil"
	"log"
	"net"
	"net/http"
	"time"
)

var (
	url = "http://127.0.0.1:8085/tvm/tickets?dsts=log_broker"
)

type Provider interface {
	goLB.TokenProvider
	Init() error
}

// Change from OAuth provider to TVM provider
type TVMProvider struct {
	authorization string
	token         string
	client        *http.Client
}

func NewTVMProvider(authorization string) *TVMProvider {
	localTCPAddr := net.TCPAddr{}
	client := &http.Client{
		Transport: &http.Transport{
			Proxy: http.ProxyFromEnvironment,
			DialContext: (&net.Dialer{
				LocalAddr: &localTCPAddr,
				Timeout:   30 * time.Second,
				KeepAlive: 30 * time.Second,
				DualStack: true,
			}).DialContext,
			MaxIdleConns:          100,
			IdleConnTimeout:       90 * time.Second,
			TLSHandshakeTimeout:   10 * time.Second,
			ExpectContinueTimeout: 1 * time.Second,
		},
	}

	provider := &TVMProvider{
		authorization: authorization,
		client:        client,
	}
	err := provider.Init()
	if err != nil {
		panic(err)
	}
	return provider
}

func (p *TVMProvider) Type() goLB.TokenType {
	return goLB.TokenTVM
}

func (p *TVMProvider) Token() []byte {
	return []byte(p.token)
}

func (p *TVMProvider) Init() error {

	err := p.updateToken()
	if err != nil {
		log.Printf("TVM update token fail")
		return err
	}
	go func() {
		ticker := time.NewTicker(60 * time.Second)
		defer ticker.Stop()
		for {
			<-ticker.C
			err := p.updateToken()
			if err != nil {
				panic(err)
			}
		}
	}()
	return nil
}

func (p *TVMProvider) updateToken() error {

	request, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return err
	}
	request.Header.Add("Authorization", p.authorization)
	resp, err := p.client.Do(request)
	if err != nil {
		return err
	}
	if resp.StatusCode != 200 {
		return fmt.Errorf("TVM http error %d: %s", resp.StatusCode, resp.Status)
	}
	body, err := ioutil.ReadAll(resp.Body)
	resp.Body.Close()
	if err != nil {
		return err
	}
	response := &Response{}
	err = json.Unmarshal(body, response)
	if err != nil {
		return err
	}
	if response.LogBroker.Error != "" {
		return fmt.Errorf("TVM error: %s", response.LogBroker.Error)
	}
	p.token = response.LogBroker.Ticket
	return nil
}

type Response struct {
	LogBroker ResponseItem `json:"log_broker"`
}

type ResponseItem struct {
	Ticket string `json:"ticket"`
	TVMId  int    `json:"tvm_id"`
	Error  string `json:"error"`
}
