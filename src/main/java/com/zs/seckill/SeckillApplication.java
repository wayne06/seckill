package com.zs.seckill;

import com.zs.seckill.dao.UserDOMapper;
import com.zs.seckill.dataObject.UserDO;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication(scanBasePackages = {"com.zs.seckill"})
@MapperScan("com.zs.seckill.dao")
@RestController
public class SeckillApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }

    @Autowired
    private UserDOMapper userDOMapper;

    @RequestMapping("/")
    public String home() {
        UserDO userDO = userDOMapper.selectByPrimaryKey(1);
        if (userDO == null) {
            return "user does not exist.";
        } else {
            return userDO.getName();
        }
    }

}
