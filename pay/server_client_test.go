package server

import (
	"context"
	"fmt"

	"github.com/go-resty/resty/v2"

	"a.yandex-team.ru/pay/duckgo/mastercard"
	"a.yandex-team.ru/pay/duckgo/server/api"
	"a.yandex-team.ru/pay/duckgo/visa"
)

type Client struct {
	internalAPI *resty.Client
	externalAPI *resty.Client
}

func NewClient(server *Server, config *Config) *Client {
	internalAPI := resty.New().
		SetBaseURL("http://" + server.GetInternalAPIEndpoint()).
		SetAuthScheme("SharedKey").
		SetAuthToken(config.InternalAPI.SharedKey.SharedKey)

	externalAPI := resty.New().
		SetBaseURL("http://" + server.GetExternalAPIEndpoint()).
		SetAuthScheme("SharedKey").
		SetAuthToken(config.ExternalAPI.SharedKey.SharedKey)

	return &Client{
		internalAPI: internalAPI,
		externalAPI: externalAPI,
	}
}

func (c *Client) MastercardCheckout(ctx context.Context, request *MastercardCheckoutRequest) (*MastercardCheckoutResponse, error) {
	checkoutResponse := &MastercardCheckoutResponse{}
	apiResponse := &api.Response{
		Data: checkoutResponse,
	}
	resp, err := c.externalAPI.R().
		SetBody(request).
		SetResult(apiResponse).
		SetContext(ctx).
		Post("/v1/mastercard/checkout")
	if err != nil {
		return nil, err
	}

	if resp.IsError() {
		return nil, fmt.Errorf("mastercard/checkout: Unexpected status: %s", resp.Status())
	}

	if apiResponse.Status != api.StatusSuccess {
		return nil, fmt.Errorf("mastercard/checkout: status fail: %v", apiResponse.Data)
	}

	return checkoutResponse, nil
}

func (c *Client) VisaCheckout(ctx context.Context, request *VisaCheckoutRequest) (*VisaCheckoutResponse, error) {
	checkoutResponse := &VisaCheckoutResponse{}
	apiResponse := &api.Response{
		Data: checkoutResponse,
	}
	resp, err := c.externalAPI.R().
		SetBody(request).
		SetResult(apiResponse).
		SetContext(ctx).
		Post("/v1/visa/checkout")
	if err != nil {
		return nil, err
	}

	if resp.IsError() {
		return nil, fmt.Errorf("visa/checkout: Unexpected status: %s", resp.Status())
	}

	if apiResponse.Status != api.StatusSuccess {
		return nil, fmt.Errorf("visa/checkout: status fail: %v", apiResponse.Data)
	}

	return checkoutResponse, nil
}

func (c *Client) MastercardEnroll(ctx context.Context, request *MastercardEnrollRequest) (*mastercard.EnrollCardResponse, error) {
	resp, err := c.internalAPI.R().
		SetBody(request).
		SetResult(&mastercard.EnrollCardResponse{}).
		SetContext(ctx).
		Post("/v1/mastercard/enroll_card")
	if err != nil {
		return nil, err
	}

	if resp.IsError() {
		return nil, fmt.Errorf("mastercard/enroll: Unexpected status: %s", resp.Status())
	}

	return resp.Result().(*mastercard.EnrollCardResponse), nil
}

func (c *Client) VisaEnroll(ctx context.Context, request *VisaEnrollRequest) (*visa.EnrollCardResponse, error) {
	resp, err := c.internalAPI.R().
		SetBody(request).
		SetResult(&visa.EnrollCardResponse{}).
		SetContext(ctx).
		Post("/v1/visa/enroll_card")
	if err != nil {
		return nil, err
	}

	if resp.IsError() {
		return nil, fmt.Errorf("visa/enroll: Unexpected status: %s", resp.Status())
	}

	return resp.Result().(*visa.EnrollCardResponse), nil
}

func (c *Client) PANCheckout(ctx context.Context, request *PANCheckoutRequest) (*PANCheckoutResponse, error) {
	checkoutResponse := &PANCheckoutResponse{}
	apiResponse := &api.Response{
		Data: checkoutResponse,
	}
	resp, err := c.internalAPI.R().
		SetBody(request).
		SetResult(apiResponse).
		SetContext(ctx).
		Post("/v1/pan/checkout")
	if err != nil {
		return nil, err
	}

	if resp.IsError() {
		return nil, fmt.Errorf("pan/checkout: Unexpected status: %s", resp.Status())
	}

	if apiResponse.Status != api.StatusSuccess {
		return nil, fmt.Errorf("pan/checkout: status fail: %v", apiResponse.Data)
	}

	return checkoutResponse, nil
}

func (c *Client) ThalesEncryptedCard(ctx context.Context, request *ThalesEncryptedCardRequest) (*ThalesEncryptedCardResponse, error) {
	thalesResponse := &ThalesEncryptedCardResponse{}
	apiResponse := &api.Response{
		Data: thalesResponse,
	}
	resp, err := c.internalAPI.R().
		SetBody(request).
		SetResult(apiResponse).
		SetContext(ctx).
		Post("/v1/wallet/thales/encrypted_card")
	if err != nil {
		return nil, err
	}

	if resp.IsError() {
		return nil, fmt.Errorf("thales/encrypted_card: Unexpected status: %s", resp.Status())
	}

	if apiResponse.Status != api.StatusSuccess {
		return nil, fmt.Errorf("thales/encrypted_card: status fail: %v", apiResponse.Data)
	}

	return thalesResponse, nil
}

func (c *Client) VisaSignRequest(ctx context.Context, request *SignRequest) (*VisaSignResponse, error) {
	visaSignResponse := &VisaSignResponse{}
	apiResponse := &api.Response{
		Data: visaSignResponse,
	}

	resp, err := c.externalAPI.R().
		SetBody(request).
		SetResult(&apiResponse).
		SetContext(ctx).
		Post("/v1/visa/sign_request")
	if err != nil {
		return nil, err
	}

	if resp.IsError() {
		return nil, fmt.Errorf("visa/sign_request: Unexpected status: %s", resp.Status())
	}

	if apiResponse.Status != api.StatusSuccess {
		return nil, fmt.Errorf("visa/sign_request: status fail: %v", apiResponse.Data)
	}

	return visaSignResponse, nil
}

func (c *Client) VisaVerifyRequest(ctx context.Context, request *VisaVerifyRequest) error {
	visaVerifyResponse := &VisaVerifyResponse{}
	apiResponse := &api.Response{
		Data: visaVerifyResponse,
	}

	resp, err := c.externalAPI.R().
		SetBody(request).
		SetResult(&apiResponse).
		SetContext(ctx).
		Post("/v1/visa/verify_request")
	if err != nil {
		return err
	}

	if resp.IsError() {
		return fmt.Errorf("visa/verify_request: Unexpected status: %s", resp.Status())
	}

	if apiResponse.Status != api.StatusSuccess {
		return fmt.Errorf("visa/verify_request: status fail: %v", apiResponse.Data)
	}

	return nil
}

func (c *Client) PaymentTokenVerifyRecipientKey(ctx context.Context, request *PaymentTokenVerifyRecipientKeyRequest) error {
	verifyResp := PaymentTokenVerifyRecipientKeyResponse{}
	apiResponse := &api.Response{
		Data: verifyResp,
	}

	resp, err := c.externalAPI.R().
		SetBody(request).
		SetResult(&apiResponse).
		SetContext(ctx).
		Post("/v1/payment_token/verify_recipient_key")
	if err != nil {
		return err
	}

	if resp.IsError() {
		return fmt.Errorf("payment_token/verify_recipient_key: Unexpected status: %s", resp.String())
	}

	if apiResponse.Status != api.StatusSuccess {
		return fmt.Errorf("payment_token/verify_recipient_key: status fail: %v", apiResponse.Data)
	}

	return nil
}
