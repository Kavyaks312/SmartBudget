package com.fullStack.expenseTracker.services.impls;

import com.fullStack.expenseTracker.dto.reponses.ApiResponseDto;
import com.fullStack.expenseTracker.enums.ApiResponseStatus;
import com.fullStack.expenseTracker.dto.requests.BudgetRequest;
import com.fullStack.expenseTracker.exceptions.UserNotFoundException;
import com.fullStack.expenseTracker.exceptions.UserServiceLogicException;
import com.fullStack.expenseTracker.models.Budget;
import com.fullStack.expenseTracker.models.User;
import com.fullStack.expenseTracker.repository.BudgetRepository;
import com.fullStack.expenseTracker.repository.TransactionRepository;
import com.fullStack.expenseTracker.repository.UserRepository;
import com.fullStack.expenseTracker.services.BudgetService;
import com.fullStack.expenseTracker.services.EmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class BudgetServiceImpl implements BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private EmailService emailService;

    @Override
    public ResponseEntity<ApiResponseDto<?>> createBudget(BudgetRequest budgetRequest)
            throws UserNotFoundException, UserServiceLogicException {

        if (!userRepository.existsById(budgetRequest.getUserId())) {
            throw new UserNotFoundException("User not found with id " + budgetRequest.getUserId());
        }

        try {
            Budget budget = budgetRepository.findByUserIdAndMonthAndYear(
                    budgetRequest.getUserId(),
                    LocalDate.now().getMonthValue(),
                    LocalDate.now().getYear()
            );

            if (budget == null) {
                budget = new Budget(
                        budgetRequest.getUserId(),
                        budgetRequest.getAmount(),
                        LocalDate.now().getMonthValue(),
                        LocalDate.now().getYear()
                );
            } else {
                budget.setAmount(budgetRequest.getAmount());
            }

            budgetRepository.save(budget);

            // ✅ Check for 80% spent after setting/updating budget
            String alertMessage = checkBudgetThresholdAndNotify(budgetRequest.getUserId(), budget.getAmount());

            String responseMessage = "Budget created successfully!";
            if (!alertMessage.isEmpty()) {
                responseMessage += " " + alertMessage;
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponseDto<>(
                    ApiResponseStatus.SUCCESS,
                    HttpStatus.CREATED,
                    responseMessage
            ));

        } catch (Exception e) {
            log.error("Failed to create or update budget: {}", e.getMessage());
            throw new UserServiceLogicException("Failed to create budget: Try again later!");
        }
    }

    @Override
    public ResponseEntity<ApiResponseDto<?>> getBudgetByMonth(long userId, int month, long year)
            throws UserServiceLogicException {

        try {
            Budget budget = budgetRepository.findByUserIdAndMonthAndYear(userId, month, year);
            double amount = budget == null ? 0 : budget.getAmount();

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDto<>(
                    ApiResponseStatus.SUCCESS,
                    HttpStatus.OK,
                    amount
            ));
        } catch (Exception e) {
            log.error("Failed to fetch budget amount: {}", e.getMessage());
            throw new UserServiceLogicException("Failed to fetch budget: Try again later!");
        }
    }

    /**
     * ✅ Sends Gmail notification if user spends ≥ 80% of their budget.
     * Returns message for API, silent for below 80%.
     */
    private String checkBudgetThresholdAndNotify(long userId, double budgetAmount) {
        try {
            double totalExpense = transactionRepository.getTotalExpenseByUserId(userId);
            double percentSpent = (totalExpense / budgetAmount) * 100;

            if (percentSpent >= 80) {
                User user = userRepository.findById(userId).orElseThrow();

                // Send Gmail alert
                emailService.sendBudgetAlert(user.getEmail(), totalExpense, budgetAmount);

                String alertMsg = "📩 Gmail notification sent: Budget usage exceeded 80% for user " + user.getEmail();
                log.info(alertMsg);

                // Return message for API response
                return "📩 Gmail notification sent: Budget usage exceeded 80%.";
            } else {
                // No alert if under 80%
                log.info("✅ No Gmail alert triggered — Budget usage is below 80%.");
                return "";
            }
        } catch (Exception e) {
            log.error("❌ Error while checking budget threshold: {}", e.getMessage());
            return "";
        }
    }
}
