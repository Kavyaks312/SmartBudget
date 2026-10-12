package com.fullStack.expenseTracker.controllers;

import com.fullStack.expenseTracker.exceptions.*;
import com.fullStack.expenseTracker.services.TransactionService;
import com.fullStack.expenseTracker.dto.reponses.ApiResponseDto;
import com.fullStack.expenseTracker.dto.requests.TransactionRequestDto;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController
@RequestMapping("/mywallet/transaction")
public class TransactionController {

    @Autowired
    TransactionService transactionService;

    @GetMapping("/getAll")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<ApiResponseDto<?>> getAllTransactions(
            @RequestParam("pageNumber") int pageNumber,
            @RequestParam("pageSize") int pageSize,
            @RequestParam(value = "searchKey", required = false) String searchKey
    ) throws TransactionServiceLogicException {
        return transactionService.getAllTransactions(pageNumber, pageSize, searchKey);
    }

    @PostMapping("/new")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ApiResponseDto<?>> addTransaction(
            @RequestBody @Valid TransactionRequestDto transactionRequestDto
    ) throws UserNotFoundException, CategoryNotFoundException, TransactionServiceLogicException {
        return transactionService.addTransaction(transactionRequestDto);
    }

    @GetMapping("/getByUser")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ApiResponseDto<?>> getTransactionsByUser(
            @RequestParam("email") String email,
            @RequestParam("pageNumber") int pageNumber,
            @RequestParam("pageSize") int pageSize,
            @RequestParam(value = "searchKey", required = false) String searchKey,
            @RequestParam(value = "sortField", required = false) String sortField,
            @RequestParam(value = "sortDirec", required = false) String sortDirec,
            @RequestParam(value = "transactionType", required = false) String transactionType
    ) throws UserNotFoundException, TransactionServiceLogicException {
        return transactionService.getTransactionsByUser(email, pageNumber, pageSize, searchKey, sortField, sortDirec, transactionType);
    }

    @GetMapping("/getById")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ApiResponseDto<?>> getTransactionById(
            @RequestParam("id") Long id
    ) throws TransactionNotFoundException {
        return transactionService.getTransactionById(id);
    }

    @PutMapping("/update")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ApiResponseDto<?>> updateTransaction(
            @RequestParam("transactionId") Long transactionId,
            @RequestBody @Valid TransactionRequestDto transactionRequestDto
    ) throws UserNotFoundException, CategoryNotFoundException, TransactionNotFoundException, TransactionServiceLogicException {
        return transactionService.updateTransaction(transactionId, transactionRequestDto);
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<ApiResponseDto<?>> deleteTransaction(
            @RequestParam("transactionId") Long transactionId
    ) throws TransactionNotFoundException, TransactionServiceLogicException {
        return transactionService.deleteTransaction(transactionId);
    }

}
