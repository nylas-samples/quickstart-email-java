// Import Java Utilities
import java.util.*;
import static spark.Spark.*;

import com.google.gson.Gson;
import com.nylas.NylasClient;
import com.nylas.models.*;

//Import DotEnv to handle .env files
import io.github.cdimascio.dotenv.Dotenv;

public class quickstart_email_java {

    public static void main(String[] args) {
        // Load the .env file
        Dotenv dotenv = Dotenv.load();
        // Connect it to Nylas using the Access Token from the .env file
        NylasClient nylas = new NylasClient.Builder(dotenv.get("NYLAS_API_KEY")).apiUri(dotenv.get("NYLAS_API_URI")).build();

        get("/nylas/auth", (request, response) -> {

            List<String> scope = new ArrayList<>();
            scope.add("https://www.googleapis.com/auth/gmail.modify");

            UrlForAuthenticationConfig config = new UrlForAuthenticationConfig("a7ac3c13-a324-49ad-91ba-3faf3caf1dc3",
                    "http://localhost:4567/oauth/exchange",
                    AccessType.ONLINE,
                    AuthProvider.GOOGLE,
                    Prompt.DETECT,
                    scope,
                    true,
                    "sQ6vFQN",
                    "swag@nylas.com");

            String url = nylas.auth().urlForOAuth2(config);
            response.redirect(url);
            return null;
        });

        get("/oauth/exchange", (request, response) -> {
            String code = request.queryParams("code");
            if(code == null) { response.status(401);}
            assert code != null;
            CodeExchangeRequest codeRequest = new CodeExchangeRequest(
                    "http://localhost:4567/oauth/exchange",
                    code,
                    dotenv.get("NYLAS_CLIENT_ID"),
                    null,
                    null);
            try{
                CodeExchangeResponse codeResponse = nylas.auth().exchangeCodeForToken(codeRequest);
                request.session().attribute("grant_id", codeResponse.getGrantId());
                return "%s".formatted(codeResponse.getGrantId());
            }catch(Exception e){
                return  "%s".formatted(e);
            }
        });

        get("/nylas/recent-emails", (request, response) -> {
            try {
                ListMessagesQueryParams queryParams = new ListMessagesQueryParams.Builder().limit(5).build();
                ListResponse<Message> emails = nylas.messages().list(request.session().attribute("grant_id"), queryParams);
                Gson gson = new Gson();
                return (gson.toJson(emails.getData()));
            } catch (Exception e){
                return "%s".formatted(e);
            }
        });

        get("/nylas/send-email", (request, response) -> {
            try {

                List<EmailName> emailNames = new ArrayList<>();
                emailNames.add(new EmailName(dotenv.get("EMAIL"), "Name"));

                SendMessageRequest requestBody = new SendMessageRequest.Builder(emailNames).
                        replyTo(emailNames).
                        subject("Your Subject Here").
                        body("Your email body here").
                        build();

                Response<Message> email = nylas.messages().send(dotenv.get("GRANT_ID"), requestBody);
                Gson gson = new Gson();

                return gson.toJson(email.getData());
            }catch (Exception e) {
                return "%s".formatted(e);
            }
        });
    }
}
