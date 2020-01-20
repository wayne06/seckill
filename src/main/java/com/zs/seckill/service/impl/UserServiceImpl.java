package com.zs.seckill.service.impl;

import com.alibaba.druid.util.StringUtils;
import com.zs.seckill.dao.UserDOMapper;
import com.zs.seckill.dao.UserPasswordDOMapper;
import com.zs.seckill.dataObject.UserDO;
import com.zs.seckill.dataObject.UserPasswordDO;
import com.zs.seckill.error.BusinessErrEnum;
import com.zs.seckill.error.BusinessException;
import com.zs.seckill.service.UserService;
import com.zs.seckill.service.model.UserModel;
import com.zs.seckill.validator.ValidationResult;
import com.zs.seckill.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDOMapper userDOMapper;

    @Autowired
    private UserPasswordDOMapper userPasswordDOMapper;

    @Autowired
    private ValidatorImpl validator;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public UserModel getUserById(Integer id) {
        UserDO userDO = userDOMapper.selectByPrimaryKey(id);
        if (userDO == null) {
            return null;
        }
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());
        return convertFromDataObject(userDO, userPasswordDO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(UserModel userModel) throws BusinessException {
        if (userModel == null) {
            throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR);
        }
        //if (StringUtils.isEmpty(userModel.getName())
        //        || userModel.getGender() == null
        //        || userModel.getAge() == null
        //        || StringUtils.isEmpty(userModel.getTelphone())
        //        || StringUtils.isEmpty(userModel.getEncrptPassword())) {
        //    throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR);
        //}
        ValidationResult validationResult = validator.validate(userModel);
        if (validationResult.isHasErrors()) {
            throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, validationResult.getErrMsg());
        }
        UserDO userDO = convertFromModel(userModel);
        try {
            userDOMapper.insertSelective(userDO);
        } catch (DuplicateKeyException e) {
            throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, "手机号已注册");
        }

        userModel.setId(userDO.getId());

        UserPasswordDO userPasswordDO = convertFromPasswordModel(userModel);
        userPasswordDOMapper.insertSelective(userPasswordDO);
    }

    @Override
    public UserModel loginValidation(String telphone, String encrptPassword) throws BusinessException {
        UserDO userDO = userDOMapper.selectByTelphone(telphone);
        if (userDO == null) {
            throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, "手机号未注册");
        }
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());
        UserModel userModel = convertFromDataObject(userDO, userPasswordDO);
        if (!StringUtils.equals(encrptPassword, userModel.getEncrptPassword())) {
            throw new BusinessException(BusinessErrEnum.PARAMETER_VALIDATION_ERROR, "用户名或密码不正确");
        }
        return userModel;
    }

    @Override
    public UserModel getUserByIdInCache(Integer userId) {
        UserModel userModel = (UserModel) redisTemplate.opsForValue().get("user_validate_" + userId);
        if (userModel == null) {
            userModel = this.getUserById(userId);
            redisTemplate.opsForValue().set("user_validate_" + userId, userModel, 10, TimeUnit.MINUTES);
        }
        return userModel;
    }

    private UserPasswordDO convertFromPasswordModel(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserPasswordDO userPasswordDO = new UserPasswordDO();
        userPasswordDO.setUserId(userModel.getId());
        userPasswordDO.setEncrptPassword(userModel.getEncrptPassword());
        return userPasswordDO;
    }

    private UserDO convertFromModel(UserModel userModel) {
        if (userModel == null) {
            return null;
        }
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(userModel, userDO);
        return userDO;
    }


    private UserModel convertFromDataObject(UserDO userDO, UserPasswordDO userPasswordDO) {
        if (userDO == null) {
            return null;
        }
        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userDO, userModel);
        if (userPasswordDO != null) {
            userModel.setEncrptPassword(userPasswordDO.getEncrptPassword());
        }
        return userModel;
    }
}
