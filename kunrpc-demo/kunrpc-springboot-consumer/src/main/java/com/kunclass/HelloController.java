package com.kunclass;

import com.kunapi.HelloKunrpc;
import com.kunclass.annotation.KunrpcService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    //需要注入代理对象
    @KunrpcService
    private HelloKunrpc helloKunrpc;

    @GetMapping("/sayHi")
    public String sayHi() {
        return helloKunrpc.sayHi("项目结束咯！");
    }

}
