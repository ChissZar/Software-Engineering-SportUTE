package vn.edu.ute.sportute.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Khoan thu va phieu thu. */
@Controller
@RequestMapping("/admin/payments")
public class PaymentController {

    @GetMapping
    public String index() {
        return "payments/index";
    }
}
