import com.overzealous.remark.Remark;
import org.apache.commons.lang3.StringEscapeUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import spark.Request;
import spark.Response;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.*;

import static spark.Spark.port;
import static spark.Spark.post;

public class Main {

    public static void main(String[] args) {
        port(Integer.valueOf(System.getenv("PORT")));
        Main main = new Main();
        post("/incoming/*", main::handleRequest);
    }

    private String handleRequest(Request request, Response response) throws ParseException {
        String[] segments = request.uri().split("/");
        String channelName = segments[segments.length - 1];
        String webHookUrl = System.getenv("#" + channelName);
        if(webHookUrl == null) {
            response.status(404);
            return "Channel not found";
        }
        JSONObject incomingEmail = (JSONObject) new JSONParser().parse(request.body());

        try {
            URL endpoint = new URL(webHookUrl);
            HttpURLConnection connection = (HttpURLConnection)endpoint.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.getOutputStream().write(buildMessage(incomingEmail).toJSONString().getBytes());
            connection.getInputStream().close();
        } catch (Exception e) {
            e.printStackTrace();
            response.status(500);
            return "Failed";
        }
        return "OK";
    }

    private JSONObject buildMessage(JSONObject email) {
        String messageBody = convertToMarkdown(email);
        String messageFrom = StringEscapeUtils.escapeHtml4(getFrom(email));
        String fromEmail = getFromEmail(email);
        String subject = getSubject(email);

        String messageText = String.format("*Subject:* %s\n*From: *<mailto:%s|%s>\n\n%s",
                subject,
                fromEmail,
                messageFrom,
                messageBody);

        JSONObject build = new JSONObject();
        build.put("text", messageText);

        return build;
    }

    private String getFromEmail(JSONObject email) {
        JSONObject envelope = (JSONObject) email.get("envelope");
        return envelope == null ? "" : (String) envelope.get("from");
    }

    private String getSubject(JSONObject email) {
        JSONObject headers = (JSONObject) email.get("headers");
        return headers == null ? "No Subject" : (String) headers.get("subject");
    }

    private static String getFrom(JSONObject email) {
        JSONObject headers = (JSONObject) email.get("headers");
        return headers == null ? "Unknown" : (String) headers.get("from");
    }

    private String convertToMarkdown(JSONObject email) {
        String bodyHtml = (String)email.get("html");
        Remark remark = new Remark();
        return remark.convert(bodyHtml);
    }
}