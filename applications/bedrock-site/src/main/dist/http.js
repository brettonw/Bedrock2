Bedrock.Http = function () {
    let $ = Object.create (null);

    let errorHandler = function (message) {
        LOG(ERROR, message);
    };

    let setupRequest = function (method, queryString, onSuccess, onError) {
        let request = new XMLHttpRequest ();
        if (typeof (onError) === "undefined") {
            onError = errorHandler;
        }
        request.onload = function (event) {
            // check if the result was successful
            if (request.status === 200) {
                // it was a successfully completed load
                let response = JSON.parse (request.responseText);
                onSuccess (response);
            } else {
                // some failure occurred, report it
                onError(request.responseText);
            }
        };
        request.onerror = function (event) {
            onError(request.responseText);
        };
        request.open (method, queryString, true);
        return request;
    };

    $.get = function (queryString, onSuccess, onError) {
        let request = setupRequest("GET", queryString, onSuccess, onError);
        request.send();
    };

    $.post = function (queryString, postData, onSuccess, onError) {
        let request = setupRequest("POST", queryString, onSuccess, onError);
        request.send(postData);
    };

    return $;
} ();
