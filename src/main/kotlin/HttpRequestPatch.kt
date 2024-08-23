import burp.api.montoya.http.message.params.HttpParameterType
import burp.api.montoya.http.message.requests.HttpRequest


// This method patches the https://portswigger.github.io/burp-extensions-montoya-api/javadoc/burp/api/montoya/http/message/requests/HttpRequest.html interface to include a new "addOrUpdateHeader(header,value)" method.
public fun HttpRequest.addOrUpdateHeader(headerName : String, headerValue : String) : HttpRequest {

    return if(this.hasHeader(headerName))
        this.withUpdatedHeader(headerName, headerValue)
    else
        this.withAddedHeader(headerName, headerValue)
}
