package com.zs.seckill.service;

import com.zs.seckill.error.BusinessException;
import com.zs.seckill.service.model.UserModel;

public interface UserService {

    UserModel getUserById(Integer id);

    void register(UserModel userModel) throws BusinessException;

    UserModel loginValidation(String telphone, String encrptPassword) throws BusinessException;

    UserModel getUserByIdInCache(Integer userId);
}
