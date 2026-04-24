package com.pidev.utils.hackthon;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.pidev.models.Hackathon;

public class StripeService {
    private static final String SECRET_KEY = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";

    static {
        Stripe.apiKey = SECRET_KEY;
    }

    public static String getPaymentUrl(Hackathon hackathon) throws Exception {
        return createSession("Participation: " + hackathon.getTitle(), "Hackathon Participation Fee", hackathon.getFee());
    }

    public static String getSponsorPaymentUrl(com.pidev.models.Sponsor sponsor, Hackathon hackathon, double amount) throws Exception {
        return createSession("Sponsorship: " + sponsor.getName(), "Contribution for " + hackathon.getTitle(), amount);
    }

    private static String createSession(String name, String description, Double amount) throws Exception {
        if (amount == null || amount <= 0) return null;

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl("https://success.example.com")
                .setCancelUrl("https://cancel.example.com")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount((long) (amount * 100))
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(name)
                                        .setDescription(description)
                                        .build())
                                .build())
                        .build())
                .build();

        Session session = Session.create(params);
        return session.getUrl();
    }
}
