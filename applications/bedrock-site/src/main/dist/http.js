Bedrock.Http = function () {
    let $ = Object.create (null);

    $.get = function (queryString, onSuccess) {
        let request = new XMLHttpRequest ();
        request.onload = function (event) {
            if (request.status === 200) {
                let response = JSON.parse (this.responseText);
                onSuccess (response);
            }
        };
        request.overrideMimeType ("application/json");
        request.open ("GET", queryString, true);
        request.send ();
    };

    $.post = function (queryString, postData, onSuccess) {
        let request = new XMLHttpRequest ();
        request.onload = function (event) {
            if (request.status === 200) {
                let response = JSON.parse (this.responseText);
                onSuccess (response);
            }
        };
        request.open ("POST", queryString, true);
        request.overrideMimeType ("application/json");
        request.send (postData);
    };

    return $;
} ();
