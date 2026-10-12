package vn.edu.ute.sportute.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/promotions")
public class PromotionController {

    @GetMapping
    public String index(Model model) {
        return "promotions/index";
    }
}
