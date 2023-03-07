package helpers;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Path;
import java.time.Duration;

import static constants.Parameters.AUTH;

/**
 * Util methods related to HTTP requests
 */
public class Utils_HTTP {

    public static JSONObject getJSON(String url) {
        String result = getData(url);
        return new JSONObject(result);
    }

    public static JSONArray getJSONArray(String url) {
        String result = getData(url);
        return new JSONArray(result);
    }

    /**
     * Get data from url into a String via HttpRequest.
     *
     * @param url the request url string
     * @return json data in a String
     */
    public static String getData(String url) {
        return httpRequest("GET", url, "");
    }

    /**
     * Get file from url via HttpRequest.
     *
     * @param url      the request url string
     * @param filename output filename (WITH extension!)
     */
    public static void getFile(String url, String filename) {
        String result = httpRequest("GET", url, filename);
        printError(result);
    }

    /**
     * Put string data to url via HttpRequest.
     *
     * @param url  the request url string
     * @param data the input data in String
     */
    public static void putData(String url, String data) {
        String result = httpRequest("PUT", url, data);
        printError(result);
    }

    /**
     * Put json data to url via HttpRequest.
     *
     * @param url      the request url string
     * @param filename the input file (without extension!)
     */
    public static void putFile(String url, String filename) {
        putData(url, filename + ".json");
    }

    /**
     * Handle a http request. For GET, PUT, use the corresponding method instead.
     *
     * @param method request method
     * @param url    the request url string
     * @param data   data needed
     *               <br>For GET, when data is nonempty, it's the output filename (WITH extension!)
     *               <br>For PUT, it's the data or the json filename (without extension!)
     *               <br>For others, empty string if no data
     * @return resulting data get from the request as a String
     */
    public static String httpRequest(String method, String url, String data) {
        String error = "Something wrong with " + url + " for data: \n" + data;
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofMinutes(2))
                    .build();
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + AUTH);
            if (!method.startsWith("P"))
                builder.method(method, BodyPublishers.noBody());
            else if (data.endsWith(".json"))
                builder.method(method, BodyPublishers.ofFile(Path.of(data)));
            else
                builder.method(method, BodyPublishers.ofString(data));

            HttpRequest request = builder.build();

            HttpResponse<?> response;
            if (method.equals("GET") && !data.isEmpty())
                response = client.send(request, BodyHandlers.ofFile(Path.of(data)));
            else
                response = client.send(request, BodyHandlers.ofString());
            if (response.statusCode() != 200)
                return error + response.body();
            else return response.body().toString();

        } catch (Exception e) {
            return "Exception: " + error + e;
        }
    }

    private static void printError(String result) {
        if (result.contains("Something wrong with "))
            System.out.println(result);
    }

}
