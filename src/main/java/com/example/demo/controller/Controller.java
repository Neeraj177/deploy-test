package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
public class Controller {
    @GetMapping("/login")
    public String login( )
    {
        return "hello";
    }

    @RequestMapping("/getid")
    String getData(@RequestParam(value = "un" , required = false) String uname )
    {

        return uname;
    }

    @RequestMapping("/getuser/{id}")
    String getDataById(@PathVariable int  id )
    {
        int x = id;
        switch(x) {
            case 1 -> System.out.println("user 1");
            case 2 ->System.out.println("user 2 ");
            case 3 ->System.out.println("user 3 ");
            case 4 ->System.out.println("user 4 ");
            case 5 ->System.out.println("user 5 ");
            case 6 ->System.out.println("user 6 ");
            case 7 ->System.out.println("user 7 ");
            case 8 ->System.out.println("user 8 ");
        }
        return "user - >"+id;
    }
}
