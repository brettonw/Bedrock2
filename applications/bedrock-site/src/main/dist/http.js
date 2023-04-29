Bedrock.Http = function () {
    let $ = Object.create (null);

    let setupRequest = function (method, queryString, onSuccess, onError, timeout) {
        let errorHandler = function (event, onError) {
            let request = event.currentTarget;
            Bedrock.LogLevel.say (Bedrock.LogLevel.ERROR, "Fetch for " + request.__queryString + " FAILED with status (" + request.status + ") at [" + event.type + "]");
            if (typeof (onError) !== "undefined") {
                onError (event);
            }
        };

        // create the request and set the components
        let request = new XMLHttpRequest ();
        request.timeout = timeout;
        request.ontimeout = (event) => { errorHandler(event, onError); };
        request.onerror = (event) => { errorHandler(event, onError); };
        request.onload = (event) => {
            // check if the result was successful
            if (request.status === 200) {
                // it was a successfully completed load
                let response = JSON.parse (request.responseText);
                onSuccess (response);
            } else {
                // some failure occurred, report it
                errorHandler(event, onError);
            }
        };

        // save the query string (url) on the object for later reporting
        request.__queryString = queryString;
        request.open (method, queryString, true);

        // return the result
        return request;
    };

    $.get = function (queryString, onSuccess, onError, timeout = 3000) {
        let request = setupRequest("GET", queryString, onSuccess, onError, timeout);
        request.send();
    };

    $.post = function (queryString, postData, onSuccess, onError, timeout = 3000) {
        let request = setupRequest("POST", queryString, onSuccess, onError, timeout);
        // save the postData on the object for later reporting
        request.__postData = postData;
        request.send(postData);
    };

    return $;
} ();
