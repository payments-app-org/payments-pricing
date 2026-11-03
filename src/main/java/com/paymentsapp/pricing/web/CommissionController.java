package com.paymentsapp.pricing.web;

import com.paymentsapp.pricing.commission.Commission;
import com.paymentsapp.pricing.commission.CommissionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
public class CommissionController {

    private final CommissionService commissionService;

    public CommissionController(CommissionService commissionService) {
        this.commissionService = commissionService;
    }

    @PostMapping("/commission")
    public Commission calculate(@Valid @RequestBody CommissionRequest request) {
        return commissionService.calculate(request.userId(), request.operationType(), request.amount());
    }
}
