package br.com.microservices.orchestrated.paymentservice.core.service;

import br.com.microservices.orchestrated.paymentservice.config.exception.ValidationException;
import br.com.microservices.orchestrated.paymentservice.core.dto.Event;
import br.com.microservices.orchestrated.paymentservice.core.dto.History;
import br.com.microservices.orchestrated.paymentservice.core.dto.OrderProducts;
import br.com.microservices.orchestrated.paymentservice.core.enums.PaymentStatus;
import br.com.microservices.orchestrated.paymentservice.core.model.Payment;
import br.com.microservices.orchestrated.paymentservice.core.producer.KafkaProducer;
import br.com.microservices.orchestrated.paymentservice.core.repository.PaymentRepository;
import br.com.microservices.orchestrated.paymentservice.core.utils.JsonUtil;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static br.com.microservices.orchestrated.paymentservice.core.enums.SagaStatus.SUCCESS;

@Slf4j
@Service
@AllArgsConstructor
public class PaymentService {

    private static final String CURRENT_SOURCE = "PAYMENT_SERVICE";
    private static final Integer REDUCE_SUM_VALUE = 0;
    private static final Integer MIM_AMOUNT_VALUE = 0;

    private final JsonUtil jsonUtil;
    private final KafkaProducer kafkaProducer;
    private final PaymentRepository paymentRepository;

    public void realizePayment(Event event) {
        try {
            checkCurrentValidation(event);
            createPendingPayment(event);

            var payment = findByOrderIdAndTransactionId(event);

            validateAmount(payment.getTotalAmount());
            changePaymentToSuccess(payment);
            handleSuccess(event);
        } catch (Exception e) {
            log.error("Error trying to make payment: ", e);
        }
        kafkaProducer.sendEvent(jsonUtil.toJson(event));
    }

    private void checkCurrentValidation(Event event) {
        if (paymentRepository.existsByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())) {
            throw new ValidationException("There's another validation process running for this order.");
        }
    }

    public void createPendingPayment(Event event) {
        var totalAmount = calculateTotalAmount(event);
        var totalItems = calculateTotalItems(event);
        var payment = Payment.builder()
                .orderId(event.getPayload().getId())
                .transactionId(event.getTransactionId())
                .totalAmount(totalAmount)
                .totalItems(totalItems)
                .build();
        save(payment);
        setEventAmountItems(event, payment);
    }

    private BigDecimal calculateTotalAmount(Event event) {
        return event.getPayload()
                .getProducts()
                .stream()
                .map(product -> product.getProduct()
                                .getUnitValue()
                                .multiply(BigDecimal.valueOf(product.getQuantity()))
                )
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int calculateTotalItems(Event event) {
        return event.getPayload()
                .getProducts()
                .stream()
                .map(OrderProducts::getQuantity)
                .reduce(REDUCE_SUM_VALUE, Integer::sum);
    }

    private void setEventAmountItems(Event event, Payment payment) {
        event.getPayload().setTotalAmount(payment.getTotalAmount());
        event.getPayload().setTotalItems(payment.getTotalItems());
    }

    private void changePaymentToSuccess(Payment payment) {
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        save(payment);
    }

    private void validateAmount(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Total amount must be greater than " + MIM_AMOUNT_VALUE);
        }
    }

    private void handleSuccess(Event event) {
        event.setStatus(SUCCESS);
        event.setSource(CURRENT_SOURCE);
        addHistory(event, "Payment realized successfully.");
    }

    private void addHistory(Event event, String message) {
        var history = History.builder()
                .source(event.getSource())
                .status(event.getStatus())
                .message(message)
                .createdAt(LocalDateTime.now())
                .build();

        event.addToHistory(history);
    }

    private Payment findByOrderIdAndTransactionId(Event event) {
        return paymentRepository
                .findByOrderIdAndTransactionId(event.getOrderId(), event.getTransactionId())
                .orElseThrow(() -> new ValidationException("Payment not found for the given orderId and transactionId."));
    }

    private void save(Payment payment) {
        paymentRepository.save(payment);
    }

}
