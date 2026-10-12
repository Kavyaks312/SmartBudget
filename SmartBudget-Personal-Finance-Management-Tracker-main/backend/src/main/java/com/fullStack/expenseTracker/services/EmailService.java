package com.fullStack.expenseTracker.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendBudgetAlert(String toEmail, double spent, double budget) {
        double percent = (spent / budget) * 100;
        String subject = "⚠️ SmartBudget Alert: 80% of your budget reached!";
        String body = String.format(
            "Hello,\n\nYou have spent %.2f%% of your budget.\n" +
            "Budget: Rs. %.2f\nSpent: Rs. %.2f\nRemaining: Rs. %.2f\n\n" +
            "Please review your spending to stay within your budget.\n\n- SmartBudget Team",
            percent, budget, spent, (budget - spent)
        );

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(subject);
        message.setText(body);

        mailSender.send(message);
    }
}
