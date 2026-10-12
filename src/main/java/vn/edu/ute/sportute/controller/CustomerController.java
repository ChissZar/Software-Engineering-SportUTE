package vn.edu.ute.sportute.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** Ho so khach hang. */
@Controller
@RequestMapping("/admin/customers")
public class CustomerController {

    @GetMapping
    public String index() {
        return "customers/index";
    }
}
