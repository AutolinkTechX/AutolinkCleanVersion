package org.example.pidev.services;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import javafx.application.Platform;
import org.example.pidev.controllers.Payement;
import org.example.pidev.entities.Commande;
import org.example.pidev.entities.Facture;
import org.example.pidev.utils.MyDatabase;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class StripeService {


    // Use a thread pool.  Make it static so it's only created once.  Make
    // it a fixed size thread pool.
    //private static final Payement executorService = Executors.newFixedThreadPool(10);

    private static final String STRIPE_SECRET_KEY = "sk_test_51QslvaCFuk7NYR1jhBdNcg2vX9jCRZIPayvJEzbafmfBlg7Sx0xtzrYZlvCttZQ9lFJ8O9DwY2mYTdAJ1eSL92ed00JV2J9vvp";

    public Payement payement= new Payement();


    public StripeService() {
        Stripe.apiKey = STRIPE_SECRET_KEY;
    }

    public String createPaymentIntent(double amount, String currency, String description) throws StripeException {
        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount((long) (amount * 100)) // Stripe uses cents
                .setCurrency(currency)
                .setDescription(description)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                )
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(params);
        return paymentIntent.getClientSecret();
    }

    public boolean confirmPayment(String paymentIntentId) throws StripeException {
        PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
        return "succeeded".equals(paymentIntent.getStatus());
    }

/*
    public String createCheckoutSession(double amount) {
        Stripe.apiKey = "sk_test_51QslvaCFuk7NYR1jhBdNcg2vX9jCRZIPayvJEzbafmfBlg7Sx0xtzrYZlvCttZQ9lFJ8O9DwY2mYTdAJ1eSL92ed00JV2J9vvp";

        try {
            // Validate amount
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be greater than 0");
            }

            long unitAmount = (long) (amount * 100);
            if (unitAmount < 50) { // Stripe minimum is typically 0.50 EUR
                throw new IllegalArgumentException("Amount is too small");
            }

            SessionCreateParams params =
                    SessionCreateParams.builder()
                            .setMode(SessionCreateParams.Mode.PAYMENT)
                            .setSuccessUrl("https://example.com/success")
                            .setCancelUrl("https://example.com/cancel")
                            .addLineItem(
                                    SessionCreateParams.LineItem.builder()
                                            .setQuantity(1L)
                                            .setPriceData(
                                                    SessionCreateParams.LineItem.PriceData.builder()
                                                            .setCurrency("eur")
                                                            .setUnitAmount(unitAmount)
                                                            .setProductData(
                                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                            .setName("Commande")
                                                                            .build())
                                                            .build())
                                            .build())
                            .build();

            Session session = Session.create(params);
            return session.getUrl();
        } catch (StripeException e) {
            // Log the full error for debugging
            System.err.println("Stripe error: " + e.getMessage());
            System.err.println("Stripe error type: " + e.getStripeError().getType());
            throw new RuntimeException("Payment processing error", e);
        } catch (Exception e) {
            throw new RuntimeException("Error creating checkout session", e);
        }
    }
*/

/*
    public String createCheckoutSession(double amount) {
        Stripe.apiKey = "sk_test_51QslvaCFuk7NYR1jhBdNcg2vX9jCRZIPayvJEzbafmfBlg7Sx0xtzrYZlvCttZQ9lFJ8O9DwY2mYTdAJ1eSL92ed00JV2J9vvp";

        try {
            // Validate amount
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be greater than 0");
            }

            long unitAmount = (long) (amount * 100);
            if (unitAmount < 50) { // Stripe minimum is typically 0.50 EUR
                throw new IllegalArgumentException("Amount is too small");
            }

            SessionCreateParams params =
                    SessionCreateParams.builder()
                            .setMode(SessionCreateParams.Mode.PAYMENT)
                            .setSuccessUrl("https://example.com/success")  //  These should be dynamic in a real application.
                            .setCancelUrl("https://example.com/cancel")
                            .addLineItem(
                                    SessionCreateParams.LineItem.builder()
                                            .setQuantity(1L)
                                            .setPriceData(
                                                    SessionCreateParams.LineItem.PriceData.builder()
                                                            .setCurrency("eur")
                                                            .setUnitAmount(unitAmount)
                                                            .setProductData(
                                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                            .setName("Commande")
                                                                            .build())
                                                            .build())
                                            .build();

            Session session = Session.create(params);

            //  IMPORTANT:  Move the background processing after successful session creation.
            // Submit the task to the executor service.
            payement.completeOnlinePayment(); // Pass any needed data to the runnable
            return session.getUrl(); // Return the URL before the background task starts.

        } catch (StripeException e) {
            // Log the full error for debugging
            System.err.println("Stripe error: " + e.getMessage());
            System.err.println("Stripe error type: " + e.getStripeError().getType());
            throw new RuntimeException("Payment processing error", e);
        } catch (Exception e) {
            throw new RuntimeException("Error creating checkout session", e);
        }
    }
*/

    public String createCheckoutSession(double amount) {
        Stripe.apiKey = "sk_test_51QslvaCFuk7NYR1jhBdNcg2vX9jCRZIPayvJEzbafmfBlg7Sx0xtzrYZlvCttZQ9lFJ8O9DwY2mYTdAJ1eSL92ed00JV2J9vvp";

        try {
            // Validate amount
            if (amount <= 0) {
                throw new IllegalArgumentException("Amount must be greater than 0");
            }

            long unitAmount = (long) (amount * 100);
            if (unitAmount < 50) { // Stripe minimum is typically 0.50 EUR
                throw new IllegalArgumentException("Amount is too small");
            }

            SessionCreateParams params =
                    SessionCreateParams.builder()
                            .setMode(SessionCreateParams.Mode.PAYMENT)
                            .setSuccessUrl("https://example.com/success")  // These should be dynamic in a real application.
                            .setCancelUrl("https://example.com/cancel")
                            .addLineItem(
                                    SessionCreateParams.LineItem.builder()
                                            .setQuantity(1L)
                                            .setPriceData(
                                                    SessionCreateParams.LineItem.PriceData.builder()
                                                            .setCurrency("eur")
                                                            .setUnitAmount(unitAmount)
                                                            .setProductData(
                                                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                            .setName("Commande")
                                                                            .build())
                                                            .build())
                                            .build())
                            .build();

            Session session = Session.create(params);

            // IMPORTANT: Move the background processing after successful session creation.
            // Submit the task to the executor service.
            payement.completeOnlinePayment(); // Pass any needed data to the runnable
            return session.getUrl(); // Return the URL before the background task starts.

        } catch (StripeException e) {
            // Log the full error for debugging
            System.err.println("Stripe error: " + e.getMessage());
            System.err.println("Stripe error type: " + e.getStripeError().getType());
            throw new RuntimeException("Payment processing error", e);
        } catch (Exception e) {
            throw new RuntimeException("Error creating checkout session", e);
        }
    }



}
