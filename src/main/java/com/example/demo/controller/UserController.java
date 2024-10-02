package com.example.demo.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping("/user")
@Controller
public class UserController {
    @RequestMapping("/index1")
    public String getDashBoard(HttpServletRequest request) {

            return "redirect:/swt/index";

    }

}
