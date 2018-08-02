package com.example

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.request.*
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.auth.*
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.jackson.*
import io.ktor.features.*
import io.ktor.locations.*

fun main(args: Array<String>): Unit = io.ktor.server.netty.DevelopmentEngine.main(args)

/*
Example project : How to implement an OAuth login with Google, taken from https://ktor.io/quickstart/guides/oauth.html

edit setup local dns entry for me.mydomain.com
edit file C:\Windows\System32\drivers\etc\hosts and add the following line:
127.0.0.1	me.mydomain.com

Kto project generator: http://ktor.io/quickstart/generator.html
Note: from the generated files, replace the contents of the folder ../gradle/wrapper with newer versions of gradle-wrapper.* files.

login to https://console.developers.google.com with anton.decraen@unifiedpost.com account
a new project OAuth2Tester and a OAuth2.0 client (Web client 1) is created with the following settings (in json format):

{
	"web":
	{
		"client_id":"844523080616-lns0d6cq1a1jsq6qo7btb7tkupffojmr.apps.googleusercontent.com",
		"project_id":"oauth2tester-212111",
		"auth_uri":"https://accounts.google.com/o/oauth2/auth",
		"token_uri":"https://www.googleapis.com/oauth2/v3/token",
		"auth_provider_x509_cert_url":"https://www.googleapis.com/oauth2/v1/certs",
		"client_secret":"vswO51g3y_XNcMEfQY5DGuKI",
		"redirect_uris":["http://me.mydomain.com:8080/googleOauth2login"],
		"javascript_origins":["http://me.mydomain.com:8080"]
	}
}


Ktor Http Client:
from the url https://ktor.io/clients/http-client.html , the supported Ktor HttpClient engines are Apache, CIO, and Jetty.
Apache is the most configurable HTTP client about right now.
Artifact io.ktor:ktor-client-apache:$ktor_version
Transitive dependency: org.apache.httpcomponents:httpasyncclient:4.1.3

test-me: http://me.mydomain.com:8080/googleOauth2login
NOTE: in Microsoft Edge, the user is NOT redirected to the Google signin page https://accounts.google.com/signin/oauth

see also:
https://www.scottbrady91.com/Kotlin/Experimenting-with-Kotlin-and-OAuth
*/

val googleOAuthProvider = OAuthServerSettings.OAuth2ServerSettings(
		name = "google",
		authorizeUrl = "https://accounts.google.com/o/oauth2/auth",
		accessTokenUrl = "https://www.googleapis.com/oauth2/v3/token",
		requestMethod = HttpMethod.Post,
		clientId = "844523080616-lns0d6cq1a1jsq6qo7btb7tkupffojmr.apps.googleusercontent.com",
		clientSecret = "vswO51g3y_XNcMEfQY5DGuKI",
		defaultScopes = listOf("profile")
)

private fun ApplicationCall.redirectUrl(path:String):String {
    val defaultPort = when (request.origin.scheme) {
        "http" -> 80
        else -> 443
    }
    val hostPort = request.host()!! + request.port().let {
        when(it) {
            defaultPort -> ""
            else -> ":$it"
        }
    }
    val protocol = request.origin.scheme
    return "$protocol://$hostPort$path"
}

const val googleAuthorizedRedirectURI = "/googleOauth2login"

fun Application.module() {
    install(StatusPages){ exception<Throwable> { failWithBadRequest(it) } }
    install(ContentNegotiation) { jackson { enable(SerializationFeature.INDENT_OUTPUT) } }
    install(Locations)
    install(Authentication) {
        oauth("google-oauth") {
            client = HttpClient(Apache)
            providerLookup = { googleOAuthProvider }
            urlProvider = { redirectUrl(googleAuthorizedRedirectURI) }
        }
    }

	routing {
		get("/") {
			call.respondText("HELLO WORLD!", contentType = ContentType.Text.Plain)
		}

		get("/json/jackson") {
			call.respond(mapOf("hello" to "world"))
		}
        authenticate("google-oauth"){
            get(googleAuthorizedRedirectURI) {
                val principal = call.authentication.principal<OAuthAccessTokenResponse.OAuth2>()?:error("No principal")
                val json = HttpClient().get<String>("https://www.googleapis.com/userinfo/v2/me") { header("Authorization", "Bearer ${principal.accessToken}") }
                val data = ObjectMapper().readValue<Map<String,Any?>>(json)
                val id = data["id"] as String?
                if(!id.isNullOrEmpty()){
                    // todo something
                }
                call.respond(json)
                //call.respondRedirect("/hello/${json}")
            }
        }
	}
}


