/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License;
 * you may not use this file except in compliance with the Elastic License.
 */
package org.elasticsearch.xpack.search;

import org.elasticsearch.client.node.NodeClient;
import org.elasticsearch.rest.BaseRestHandler;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.action.RestCancellableNodeClient;
import org.elasticsearch.rest.action.RestStatusToXContentListener;
import org.elasticsearch.xpack.core.search.action.AsyncSearchResponse;
import org.elasticsearch.xpack.core.search.action.SubmitAsyncSearchAction;
import org.elasticsearch.xpack.core.search.action.SubmitAsyncSearchRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.IntConsumer;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static org.elasticsearch.rest.RestRequest.Method.POST;
import static org.elasticsearch.rest.action.search.RestSearchAction.parseSearchRequest;

public final class RestSubmitAsyncSearchAction extends BaseRestHandler {
    static final String TYPED_KEYS_PARAM = "typed_keys";
    static final Set<String> RESPONSE_PARAMS = Collections.singleton(TYPED_KEYS_PARAM);

    @Override
    public List<Route> routes() {
        return unmodifiableList(asList(
            new Route(POST, "/_async_search"),
            new Route(POST, "/{index}/_async_search")));
    }

    @Override
    public String getName() {
        return "async_search_submit_action";
    }

    @Override
    protected RestChannelConsumer prepareRequest(RestRequest request, NodeClient client) throws IOException {
        SubmitAsyncSearchRequest submit = new SubmitAsyncSearchRequest();
        IntConsumer setSize = size -> submit.getSearchRequest().source().size(size);
        request.withContentOrSourceParamParserOrNull(parser ->
            parseSearchRequest(submit.getSearchRequest(), request, parser, setSize));

        if (request.hasParam("wait_for_completion")) {
            submit.setWaitForCompletion(request.paramAsTime("wait_for_completion", submit.getWaitForCompletion()));
        }
        if (request.hasParam("keep_alive")) {
            submit.setKeepAlive(request.paramAsTime("keep_alive", submit.getKeepAlive()));
        }
        if (request.hasParam("clean_on_completion")) {
            submit.setCleanOnCompletion(request.paramAsBoolean("clean_on_completion", submit.isCleanOnCompletion()));
        }
        return channel -> {
            RestStatusToXContentListener<AsyncSearchResponse> listener = new RestStatusToXContentListener<>(channel);
            RestCancellableNodeClient cancelClient = new RestCancellableNodeClient(client, request.getHttpChannel());
            cancelClient.execute(SubmitAsyncSearchAction.INSTANCE, submit, listener);
        };
    }

    @Override
    protected Set<String> responseParams() {
        return RESPONSE_PARAMS;
    }
}
