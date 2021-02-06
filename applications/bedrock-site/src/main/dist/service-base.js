Bedrock.ServiceBase = function () {
    let $ = Object.create (null);

    const API = "api";

    /**
     * build a query url. note bedrock no longer accepts any parameters on the query line, so this
     * is either a valid url, or it's going to be replaced with the local server url
     * @param parameters
     * @returns {string|{}}
     */
    $.getQueryUrl = function (queryUrl) {
        return ((typeof queryUrl === "string") && (queryUrl.length > 0)) ? queryUrl : (Bedrock.Cookie.get("full-context-path") + API);
    };

    /**
     * post an event to a bedrock service, and get back the full response context
     * @param event
     * @param parameters - optional
     * @param onSuccess - optional
     * @param onFailure - optional
     * @param queryUrl - optional
     */
    $.postWithFullResponse = function (event, parameters, onSuccess, onFailure, queryUrl) {
        queryUrl = $.getQueryUrl(queryUrl);

        // build the event description, we force the event to be specified to avoid the super common
        // case of the caller building an object with just an event, and we copy other parameters in
        // to the object that we will subsequently turn into a JSON string
        let postData = {};
        if ((typeof parameters === "object") && (parameters !== null)) {
            Object.assign(postData, parameters);
        }
        postData.event = event;
        postData = JSON.stringify(postData);

        // post the query to the server...
        Bedrock.Http.post (queryUrl, postData, function (response) {
            LOG (INFO, query + " (status: " + response.status + ")");
            if (response.status === "ok") {
                if (onSuccess instanceof Function) {
                    onSuccess(response);
                }
            } else if (onFailure instanceof Function) {
                onFailure (response);
            } else {
                // default on failure, alert...
                alert (response.error);
            }
        });
    };

    /**
     * post an event to a bedrock service, and get just the response
     * @param event
     * @param parameters - optional
     * @param onSuccess - optional
     * @param onFailure - optional
     * @param queryUrl - optional
     */
    $.post = function (event, parameters, onSuccess, onFailure, queryUrl) {
        $.postWithFullResponse(event, parameters,
            function (response) {
                if (onSuccess instanceof Function) {
                    onSuccess(("response" in response) ? response.response : response.status);
                }
            },
            function (response) {
                if (onFailure instanceof Function) {
                    onFailure(response.error);
                }
            },
            queryUrl);
    };

    return $;
} ();
