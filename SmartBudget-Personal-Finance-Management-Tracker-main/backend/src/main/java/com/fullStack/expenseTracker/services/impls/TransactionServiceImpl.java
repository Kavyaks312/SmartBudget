package com.fullStack.expenseTracker.services.impls;

import com.fullStack.expenseTracker.dto.reponses.*;
import com.fullStack.expenseTracker.enums.ApiResponseStatus;
import com.fullStack.expenseTracker.exceptions.*;
import com.fullStack.expenseTracker.services.CategoryService;
import com.fullStack.expenseTracker.services.TransactionService;
import com.fullStack.expenseTracker.services.UserService;
import com.fullStack.expenseTracker.services.EmailService;  // ✅ Added
import com.fullStack.expenseTracker.dto.requests.TransactionRequestDto;
import com.fullStack.expenseTracker.models.Transaction;
import com.fullStack.expenseTracker.models.Budget;  // ✅ Added
import com.fullStack.expenseTracker.models.User;    // ✅ Added
import com.fullStack.expenseTracker.repository.TransactionRepository;
import com.fullStack.expenseTracker.repository.BudgetRepository;  // ✅ Added
import com.fullStack.expenseTracker.repository.UserRepository;    // ✅ Added
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private BudgetRepository budgetRepository;   // ✅ Added

    @Autowired
    private UserRepository userRepository;       // ✅ Added

    @Autowired
    private EmailService emailService;           // ✅ Added

    @Override
    public ResponseEntity<ApiResponseDto<?>> addTransaction(TransactionRequestDto transactionRequestDto)
            throws UserNotFoundException, CategoryNotFoundException, TransactionServiceLogicException {

        Transaction transaction = TransactionRequestDtoToTransaction(transactionRequestDto);

        try {
            transactionRepository.save(transaction);

            // ✅ Check if budget usage exceeded 80%
            checkBudgetThresholdAndNotify(transaction.getUser().getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new ApiResponseDto<>(
                            ApiResponseStatus.SUCCESS,
                            HttpStatus.CREATED,
                            "Transaction has been successfully recorded!"
                    )
            );

        } catch (Exception e) {
            log.error("Error happen when adding new transaction: {}", e.getMessage());
            throw new TransactionServiceLogicException("Failed to record your new transaction, Try again later!");
        }
    }

    @Override
    public ResponseEntity<ApiResponseDto<?>> getTransactionsByUser(String email,
                                                                   int pageNumber, int pageSize,
                                                                   String searchKey, String sortField,
                                                                   String sortDirec, String transactionType)
            throws TransactionServiceLogicException {

        Sort.Direction direction = sortDirec.equalsIgnoreCase("DESC") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(pageNumber, pageSize).withSort(direction, sortField);

        Page<Transaction> transactions = transactionRepository.findByUser(email, pageable, searchKey, transactionType);

        try {
            if (transactions.getTotalElements() == 0) {
                return ResponseEntity.status(HttpStatus.OK).body(
                        new ApiResponseDto<>(
                                ApiResponseStatus.SUCCESS,
                                HttpStatus.OK,
                                new PageResponseDto<>(new ArrayList<>(), 0, 0L)
                        )
                );
            }

            List<TransactionResponseDto> transactionResponseDtoList = new ArrayList<>();
            for (Transaction transaction : transactions) {
                transactionResponseDtoList.add(transactionToTransactionResponseDto(transaction));
            }

            return ResponseEntity.status(HttpStatus.OK).body(
                    new ApiResponseDto<>(
                            ApiResponseStatus.SUCCESS,
                            HttpStatus.OK,
                            new PageResponseDto<>(
                                    groupTransactionsByDate(transactionResponseDtoList),
                                    transactions.getTotalPages(),
                                    transactions.getTotalElements()
                            )
                    )
            );

        } catch (Exception e) {
            log.error("Error retrieving user transactions: {}", e.getMessage());
            throw new TransactionServiceLogicException("Failed to fetch transactions! Try again later");
        }
    }

    @Override
    public ResponseEntity<ApiResponseDto<?>> getTransactionById(Long transactionId)
            throws TransactionNotFoundException {

        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(
                () -> new TransactionNotFoundException("Transaction not found with id : " + transactionId)
        );

        return ResponseEntity.ok(
                new ApiResponseDto<>(
                        ApiResponseStatus.SUCCESS,
                        HttpStatus.OK,
                        transactionToTransactionResponseDto(transaction)
                )
        );
    }

    @Override
    public ResponseEntity<ApiResponseDto<?>> updateTransaction(Long transactionId, TransactionRequestDto transactionRequestDto)
            throws TransactionNotFoundException, UserNotFoundException, CategoryNotFoundException, TransactionServiceLogicException {

        Transaction transaction = transactionRepository.findById(transactionId).orElseThrow(
                () -> new TransactionNotFoundException("Transaction not found with id : " + transactionId)
        );

        transaction.setAmount(transactionRequestDto.getAmount());
        transaction.setDate(transactionRequestDto.getDate());
        transaction.setUser(userService.findByEmail(transactionRequestDto.getUserEmail()));
        transaction.setCategory(categoryService.getCategoryById(transactionRequestDto.getCategoryId()));
        transaction.setDescription(transactionRequestDto.getDescription());

        try {
            transactionRepository.save(transaction);

            // ✅ Re-check 80% budget usage after update
            checkBudgetThresholdAndNotify(transaction.getUser().getId());

            return ResponseEntity.status(HttpStatus.OK).body(
                    new ApiResponseDto<>(
                            ApiResponseStatus.SUCCESS,
                            HttpStatus.OK,
                            "Transaction has been successfully updated!"
                    )
            );

        } catch (Exception e) {
            log.error("Error updating transaction: {}", e.getMessage());
            throw new TransactionServiceLogicException("Failed to update your transaction! Try again later");
        }
    }

    @Override
    public ResponseEntity<ApiResponseDto<?>> deleteTransaction(Long transactionId)
            throws TransactionNotFoundException, TransactionServiceLogicException {

        if (transactionRepository.existsById(transactionId)) {
            try {
                transactionRepository.deleteById(transactionId);
                return ResponseEntity.status(HttpStatus.OK).body(
                        new ApiResponseDto<>(
                                ApiResponseStatus.SUCCESS,
                                HttpStatus.OK,
                                "Transaction has been successfully deleted!"
                        )
                );
            } catch (Exception e) {
                log.error("Error deleting transaction: {}", e.getMessage());
                throw new TransactionServiceLogicException("Failed to delete your transaction! Try again later");
            }
        } else {
            throw new TransactionNotFoundException("Transaction not found with id : " + transactionId);
        }
    }

    @Override
    public ResponseEntity<ApiResponseDto<?>> getAllTransactions(int pageNumber, int pageSize, String searchKey)
            throws TransactionServiceLogicException {

        Pageable pageable = PageRequest.of(pageNumber, pageSize).withSort(Sort.Direction.DESC, "transaction_id");
        Page<Transaction> transactions = transactionRepository.findAll(pageable, searchKey);

        try {
            if (transactions.getTotalElements() == 0) {
                return ResponseEntity.status(HttpStatus.OK).body(
                        new ApiResponseDto<>(
                                ApiResponseStatus.SUCCESS,
                                HttpStatus.OK,
                                new PageResponseDto<>(new ArrayList<>(), 0, 0L)
                        )
                );
            }

            List<TransactionResponseDto> transactionResponseDtoList = new ArrayList<>();
            for (Transaction transaction : transactions) {
                transactionResponseDtoList.add(transactionToTransactionResponseDto(transaction));
            }

            return ResponseEntity.status(HttpStatus.OK).body(
                    new ApiResponseDto<>(
                            ApiResponseStatus.SUCCESS,
                            HttpStatus.OK,
                            new PageResponseDto<>(
                                    transactionResponseDtoList,
                                    transactions.getTotalPages(),
                                    transactions.getTotalElements()
                            )
                    )
            );

        } catch (Exception e) {
            log.error("Failed to fetch all transactions: {}", e.getMessage());
            throw new TransactionServiceLogicException("Failed to fetch all transactions: Try again later!");
        }
    }

    private Transaction TransactionRequestDtoToTransaction(TransactionRequestDto transactionRequestDto)
            throws UserNotFoundException, CategoryNotFoundException {
        return new Transaction(
                userService.findByEmail(transactionRequestDto.getUserEmail()),
                categoryService.getCategoryById(transactionRequestDto.getCategoryId()),
                transactionRequestDto.getDescription(),
                transactionRequestDto.getAmount(),
                transactionRequestDto.getDate()
        );
    }

    private TransactionResponseDto transactionToTransactionResponseDto(Transaction transaction) {
        return new TransactionResponseDto(
                transaction.getTransactionId(),
                transaction.getCategory().getCategoryId(),
                transaction.getCategory().getCategoryName(),
                transaction.getCategory().getTransactionType().getTransactionTypeId(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getDate(),
                transaction.getUser().getEmail()
        );
    }

    private Map<String, List<TransactionResponseDto>> groupTransactionsByDate(List<TransactionResponseDto> transactionResponseDtoList) {
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);

        return transactionResponseDtoList.stream()
                .collect(Collectors.groupingBy(t -> {
                    if (t.getDate().equals(today)) return "Today";
                    else if (t.getDate().equals(yesterday)) return "Yesterday";
                    else return t.getDate().toString();
                }))
                .entrySet()
                .stream()
                .sorted((entry1, entry2) -> {
                    if (entry1.getKey().equals("Today")) return -1;
                    else if (entry2.getKey().equals("Today")) return 1;
                    else if (entry1.getKey().equals("Yesterday")) return -1;
                    else if (entry2.getKey().equals("Yesterday")) return 1;
                    else return entry2.getKey().compareTo(entry1.getKey());
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (oldValue, newValue) -> oldValue, LinkedHashMap::new));
    }

    // ✅ Budget usage email alert checker
    private void checkBudgetThresholdAndNotify(long userId) {
        try {
            Budget budget = budgetRepository.findByUserIdAndMonthAndYear(
                    userId, LocalDate.now().getMonthValue(), LocalDate.now().getYear()
            );
            if (budget == null) return;

            double totalExpense = transactionRepository.getTotalExpenseByUserId(userId);
            double percentSpent = (totalExpense / budget.getAmount()) * 100;

            if (percentSpent >= 80 && percentSpent < 100) {
                User user = userRepository.findById(userId).orElseThrow();
                emailService.sendBudgetAlert(user.getEmail(), totalExpense, budget.getAmount());
                log.info("📩 Budget alert email sent to {}", user.getEmail());
            }
        } catch (Exception e) {
            log.error("Error while sending budget alert from TransactionService: {}", e.getMessage());
        }
    }
}
