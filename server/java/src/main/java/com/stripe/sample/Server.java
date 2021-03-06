package com.stripe.sample;

import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFiles;
import static spark.Spark.port;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.SetupIntent;
import com.stripe.exception.*;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;

import io.github.cdimascio.dotenv.Dotenv;

public class Server {
    private static Gson gson = new Gson();

    public static void main(String[] args) {
        port(4242);
        String ENV_PATH = "../../";
        Dotenv dotenv = Dotenv.configure().directory(ENV_PATH).load();

        Stripe.apiKey = dotenv.get("STRIPE_SECRET_KEY");

        staticFiles.externalLocation(
                Paths.get(Paths.get("").toAbsolutePath().toString(), dotenv.get("STATIC_DIR")).normalize().toString());

        get("/public-key", (request, response) -> {
            response.type("application/json");
            JsonObject publicKey = new JsonObject();
            publicKey.addProperty("publicKey", dotenv.get("STRIPE_PUBLIC_KEY"));
            return publicKey.toString();
        });

        post("/create-setup-intent", (request, response) -> {
            response.type("application/json");

            Map<String, Object> params = new HashMap<>();
            SetupIntent setupIntent = SetupIntent.create(params);

            return gson.toJson(setupIntent);
        });

        post("/create-customer", (request, response) -> {
            response.type("application/json");

            SetupIntent setupIntent = ApiResource.GSON.fromJson(request.body(), SetupIntent.class);
            // This creates a new Customer and attaches the PaymentMethod in one API call.
            Map<String, Object> customerParams = new HashMap<String, Object>();
            customerParams.put("payment_method", setupIntent.getPaymentMethod());
            Customer customer = Customer.create(customerParams);
            return customer.toJson();
        });

        post("/webhook", (request, response) -> {
            String payload = request.body();
            String sigHeader = request.headers("Stripe-Signature");
            String endpointSecret = dotenv.get("STRIPE_WEBHOOK_SECRET");

            Event event = null;

            try {
                event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
            } catch (SignatureVerificationException e) {
                // Invalid signature
                response.status(400);
                return "";
            }

            switch (event.getType()) {
            case "setup_intent.created":
                System.out.println("Occurs when a new SetupIntent is created.");
                break;
            case "setup_intent.succeeded":
                System.out.println("Occurs when an SetupIntent has successfully setup a payment method.");
                break;
            case "setup_intent.setup_failed":
                System.out.println("Occurs when a SetupIntent has failed the attempt to setup a payment method.");
                break;
            default:
                // Unexpected event type
                response.status(400);
                return "";
            }

            response.status(200);
            return "";
        });
    }
}