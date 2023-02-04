return {
    exchange_oauth_for_user_ticket = function()
        ngx.req.set_header('X-Ya-User-Ticket', 'dummy-ticket-' .. ngx.var.http_authorization)
    end,
    remove_request_oauth = function()
        ngx.req.set_header('Authorization', nil)
    end
}
