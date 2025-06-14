
// excerpt from jqury 3.71., which uses throws as property name
function ajaxConvert(conv, s, response) {
    if (conv !== true) {
        // Unless errors are allowed to bubble, catch and return them
        if (conv && s.throws) {
            response = conv(response);
        } else {
            try {
                response = conv(response);
            } catch (e) {
                return {
                    state: "parsererror",
                    error: conv ? e : "No conversion from " + prev + " to " + current
                };
            }
        }
    }
}