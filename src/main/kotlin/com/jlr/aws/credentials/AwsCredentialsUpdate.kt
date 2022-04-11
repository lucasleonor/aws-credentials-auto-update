package com.jlr.aws.credentials

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.*

fun main(vararg args: String) {
    if (args.size < 3) {
        println("Not enough arguments provided, please provide the accoundId as the first argument, the role name as the second and the SSO Token third")
        return
    }
    AwsCredentialsUpdate(args[0].toLong(), args[1], args[2]).run()
}

class AwsCredentialsUpdate(private val accountId: Long, private val role: String, private val ssoToken: String) :
    TimerTask() {
    private val region = "eu-west-2"
    private val credentialsUrl =
        "https://portal.sso.$region.amazonaws.com/federation/credentials/?account_id=$accountId&role_name=$role"


    override fun run() {
        val response = makeHttpRequest(credentialsUrl, "GET", "x-amz-sso_bearer_token", ssoToken)
        val statusCode = response.statusCode()
        if (statusCode != 200) {
            println("Request to Amazon failed with code: $statusCode")
            return
        }
        val json = (Parser.default().parse(response.body()) as JsonObject)["roleCredentials"] as JsonObject
        writeToFile(json)

        val expiration = Date.from(Instant.ofEpochMilli(json["expiration"] as Long))
        print("Next update at: $expiration")
        Timer().schedule(AwsCredentialsUpdate(accountId, role, ssoToken), expiration)
    }

    private fun makeHttpRequest(uri: String, method: String, vararg headers: String): HttpResponse<InputStream> {
        val httpClient = HttpClient.newBuilder().build()
        val builder = HttpRequest.newBuilder(URI.create(uri)).method(method, BodyPublishers.noBody())
        if (headers.isNotEmpty()) {
            builder.headers(*headers)
        }
        val httpRequest = builder.build()

        return httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofInputStream())
    }

    private fun writeToFile(result: JsonObject) {
        val accessKeyId = result["accessKeyId"] as String
        val secretAccessKey = result["secretAccessKey"] as String
        val sessionToken = result["sessionToken"] as String
        val credentialsFileContent = "[default]\n" +
                "aws_access_key_id=$accessKeyId\n" +
                "aws_secret_access_key=$secretAccessKey\n" +
                "aws_session_token=$sessionToken"
        val credentialsFilePath = Path.of(System.getProperty("user.home")).resolve(".aws").resolve("credentials")
        Files.write(credentialsFilePath, credentialsFileContent.toByteArray())
    }
}
