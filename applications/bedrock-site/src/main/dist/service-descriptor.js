Bedrock.ServiceDescriptor = function () {
    let $ = Object.create (null);

    const EVENT_HELP = "help";


    $.tryExample = function (exampleName) {
        // show the result
        let hoverBoxRoot = document.getElementById ("bedrock-service-descriptor-hover-box");
        let hoverBoxBufferElement = document.getElementById ("bedrock-service-descriptor-hover-box-buffer");
        let Html = Bedrock.Html;
        Html.removeAllChildren(hoverBoxBufferElement);
        hoverBoxRoot.style.visibility = "visible";
        event.stopPropagation();

        Bedrock.ServiceBase.post (EVENT_HELP, {}, function (response) {
            // get the example, we know it's there because the link to it wouldn't have been created
            // in the display call if it wasn't
            let example = response.events[exampleName].example;
            Bedrock.ServiceBase.postWithFullResponse(exampleName, example, function (exampleResponse) {
                let innerHTML = Html.Builder
                    .begin ("div")
                    .add("h2", { style: { margin: 0 }, innerHTML: "Example for '" + exampleName + "'" })
                    .begin ("div", { style: { margin: "16px 0" }})
                    .add ("div", { style:  { margin: "16px 0 8px 0", fontWeight: "bold" }, innerHTML: "Response JSON: " })
                    .add ("pre", { class: "code-pre", innerHTML: JSON.stringify (exampleResponse, null, 4) })
                    .end ()
                    .end ();
                hoverBoxBufferElement.appendChild(innerHTML);
            }, function (exampleResponse) {
                hoverBoxBufferElement.appendChild (Html.Builder.begin ("div", { innerHTML: "ERROR: " + exampleResponse.error}).end ());
            });
        });
    };

    $.displaySpecification = function (specification) {
        // start with an empty build
        let innerHTML = "";

        if ("description" in specification) {
            innerHTML += block ("h2", {}, "Description") + div ("description-div", specification.description);
        }

        if ("events" in specification) {
            innerHTML += block ("h2", {}, "Events");
            let events = specification.events;
            let eventNames = Object.keys (events);
            let eventsHTML = "";
            for (let eventName of eventNames) {
                let event = events[eventName];

                // events are assumed to be published, but if they have a p"published: false"
                // attribute, we will skip it
                if (! (("published" in event) && ((event.published === false) || (event.published === "false")))) {

                    let eventHTML = "";

                    // if there is an example
                    if ("example" in event) {
                        eventHTML = block ("div", { class: "try-it", onclick: 'Bedrock.ServiceDescriptor.tryExample (&quot;' + eventName + '&quot;);' }, "[example]");
                    }
                    eventHTML = div ("event-name", eventName + eventHTML);

                    // if there is a description
                    if ("description" in event) {
                        eventHTML += div ("event-description", event.description);
                    }

                    let odd;
                    let evenOddTitle = function (title) {
                        odd = true;
                        eventHTML += div ("even-odd-title", title);
                        return odd;
                    };

                    let evenOdd = function (title, object) {
                        odd = true;
                        let names = Object.keys (object);
                        if (names.length > 0) {
                            eventHTML += div ("even-odd-title", title);
                            for (let name of names) {
                                let element = object[name];
                                let required = ("required" in element) ? ((element.required === true) || (element.required === "true")) : false;
                                eventHTML += div ("even-odd-div" + (odd ? " odd" : ""),
                                    div ("even-odd-name", name) +
                                    div ("even-odd-required", required ? "REQUIRED" : "OPTIONAL") +
                                    div ("even-odd-description", element.description));
                                odd = !odd;
                            }
                        }
                        return odd;
                    };

                    let listParameters = function (title, object) {
                        // parameters
                        if ("parameters" in object) {
                            evenOdd (title, object.parameters);
                        }

                        if (("strict" in object) && ((object.strict === false) || (object.strict === "false"))) {
                            if (!("parameters" in object)) {
                                evenOddTitle (title);
                            }
                            eventHTML += div ("even-odd-div" + (odd ? " odd" : ""),
                                div ("even-odd-name", "(any)") +
                                div ("even-odd-required", "OPTIONAL") +
                                div ("even-odd-description", "Event allows unspecified parameters."));
                        }
                    };

                    // parameters
                    listParameters ("Parameters:", event);

                    // post-data
                    if (("parameters" in event) && ("post-data" in event.parameters)) {
                        listParameters ("Post Data:", event.parameters["post-data"]);
                    }

                    // response
                    if ("response" in event) {
                        let response = event.response;
                        // return specification might be an array, indicating this event returns an
                        // array of something
                        if (Array.isArray (response)) {
                            // return specification might be an empty array, or an array with a single
                            // proto object
                            if (response.length > 0) {
                                evenOdd ("Response (Array):", response[0]);
                            } else {
                                evenOddTitle ("Response (Array):");
                                eventHTML += div ("even-odd-div" + (odd ? " odd" : ""),
                                    div ("even-odd-name", "(any)") +
                                    div ("even-odd-required", "OPTIONAL") +
                                    div ("even-odd-description", "Unspecified."));
                            }
                        } else {
                            evenOdd ("Response:", response);
                        }
                    }

                    eventsHTML += div ("event-div", eventHTML);
                }
            }
            innerHTML += div("events-div", eventsHTML);
        }

        if ("name" in specification) {
            document.title = specification.name;
            innerHTML = block ("h1", {}, specification.name) + div("container-div", innerHTML);
        }
        innerHTML += div ("content-center footer", "Built with " + a ("footer-link", "http://bedrock.brettonw.com", "Bedrock"));

        // now add a floating pane that is invisible and install the click handler to hide it
        innerHTML +=
            block ("div", { id: "bedrock-service-descriptor-hover-box" },
                block ("div", {id: "bedrock-service-descriptor-hover-box-buffer"},"")
            );
        document.addEventListener('click', function(event) {
            let hoverBoxElement = document.getElementById ("bedrock-service-descriptor-hover-box");
            if (!hoverBoxElement.contains(event.target)) {
                hoverBoxElement.style.visibility = "hidden";
            }
        });

        return innerHTML;
    };

    // a little black raincloud, of course
    $.display = function (displayInDivId) {
        Bedrock.ServiceBase.post(EVENT_HELP, {}, function (response) {
            document.getElementById(displayInDivId).innerHTML = Bedrock.ServiceDescriptor.displaySpecification (response);
        });
    };

    // convert the names into camel-case names (dashes are removed and the following character is uppercased)
    let makeName = function (input) {
        return input.replace (/-([^-])/g, (match, p1, offset, string) => p1.toUpperCase());
    };

    $.translateResponse = function (response) {
        // internal function to copy the object as a response
        let copyAsResponse = function (object) {
            let copy = {};
            let keys = Object.keys (object);
            for (let key of keys) {
                copy[makeName(key)] = object[key];
            }
            return copy;
        };

        // make a decision based on the type
        if (Array.isArray(response)) {
            let copy = [];
            for (let entry of response) {
                copy.push (copyAsResponse(entry));
            }
            return copy;
        } else {
            return copyAsResponse(response);
        }
    };

    // create a client side object with all of the api's for the host server
    $.api = function (onSuccess, onFailure, queryUrl) {
        Bedrock.ServiceBase.post(EVENT_HELP, {}, function (response) {
            // XXX this is on hold for the moment, it's a distraction
            /*
            // start with an empty build
            let api = Object.create (null);
            api.specification = response;

            // check that we got a response with events
            if ("events" in response) {
                for (let eventName of Object.keys (response.events)) {
                    let event = response.events[eventName];

                    // set up the function name and an empty parameter list
                    let functionName = makeName (eventName);
                    let functionParameters = "(";
                    let functionBody = '    let url = "' + baseUrl + '/api?event=' + eventName + '";\n';

                    // if there are parameters, add them
                    let first = true;
                    if ("parameters" in event) {
                        let names = Object.keys (event.parameters);
                        if (names.length > 0) {
                            for (let name of names) {
                                let parameterName = makeName (name);
                                functionParameters += ((first !== true) ? ", " : "") + parameterName;
                                functionBody += '    url += "' + name + '=" + ' + parameterName + ';\n';
                                first = false;
                            }
                        }
                    }
                    functionParameters += ((first !== true) ? ", " : "") + "onSuccess";
                    functionBody += '    Bedrock.ServiceBase.post (\"" + eventName + "\", function (response) {\n';
                    functionBody += '        if (response.status === "ok") {\n';
                    functionBody += '            response = Bedrock.ServiceDescriptor.translateResponse (response.response);\n';
                    functionBody += '            onSuccess (response);\n';
                    functionBody += '        }\n';
                    functionBody += '    });\n';
                    functionParameters += ")";

                    LOG (INFO, functionName + " " + functionParameters + ";\n");

                    let functionString = "return function " + functionParameters + " {\n" +functionBody + "};\n";
                    api[functionName] = new Function (functionString) ();
                }
            }

            // call the completion routine
            onSuccess (api);
            */
        }, onFailure, queryUrl);
    };

    return $;
} ();
