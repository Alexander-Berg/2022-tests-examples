#!/bin/bash

yt get --proxy arnold --format json //home/webmaster/prod/searchqueries/reports_v4/favorite_queries/@least_recent_source | yt set --proxy arnold --format json //home/webmaster/test/searchqueries/reports_v4/favorite_queries/@least_recent_source
yt get --proxy arnold --format json //home/webmaster/prod/searchqueries/reports_v4/favorite_queries/@most_recent_source | yt set --proxy arnold --format json //home/webmaster/test/searchqueries/reports_v4/favorite_queries/@most_recent_source
yt get --proxy arnold --format json //home/webmaster/prod/searchqueries/reports_v4/top_3month.top/@least_recent_source | yt set --proxy arnold --format json //home/webmaster/test/searchqueries/reports_v4/top_3month.top/@least_recent_source
yt get --proxy arnold --format json //home/webmaster/prod/searchqueries/reports_v4/top_3month.top/@most_recent_source | yt set --proxy arnold --format json //home/webmaster/test/searchqueries/reports_v4/top_3month.top/@most_recent_source
yt get --proxy arnold --format json //home/webmaster/prod/searchqueries/reports_v4/top_urls/@least_recent_source | yt set --proxy arnold --format json //home/webmaster/test/searchqueries/reports_v4/top_urls/@least_recent_source
yt get --proxy arnold --format json //home/webmaster/prod/searchqueries/reports_v4/top_urls/@most_recent_source | yt set --proxy arnold --format json //home/webmaster/test/searchqueries/reports_v4/top_urls/@most_recent_source
yt get --proxy arnold --format json //home/webmaster/prod/searchqueries/reports_v4/last_queries/@least_recent_source | yt set --proxy arnold --format json //home/webmaster/test/searchqueries/reports_v4/last_queries/@least_recent_source
yt get --proxy arnold --format json //home/webmaster/prod/searchqueries/reports_v4/last_queries/@most_recent_source | yt set --proxy arnold --format json //home/webmaster/test/searchqueries/reports_v4/last_queries/@most_recent_source
