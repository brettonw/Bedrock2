Bedrock.Http = function () {
    let $ = Object.create (null);

    $.get = function (queryString, onSuccess, onError = (event) => { LOG(event); }) {
        let request = new XMLHttpRequest ();
        request.onload = function (event) {
            if (request.status === 200) {
                let response = JSON.parse (this.responseText);
                onSuccess (response);
            }
        };
        request.onerror = onError;
        request.open ("GET", queryString, true);
        request.send ();
    };

    $.post = function (queryString, postData, onSuccess, onError = (event) => { LOG(event); }) {
        let request = new XMLHttpRequest ();
        request.onload = function (event) {
            if (request.status === 200) {
                let response = JSON.parse (this.responseText);
                onSuccess (response);
            }
        };
        request.onerror = onError;
        request.open ("POST", queryString, true);
        request.send (postData);
    };

    return $;
} ();
