package com.imooc.service.impl;

import com.imooc.bo.UpdatedUserBO;
import com.imooc.enums.Sex;
import com.imooc.enums.UserInfoModifyType;
import com.imooc.enums.YesOrNo;
import com.imooc.exceptions.GraceException;
import com.imooc.grace.result.ResponseStatusEnum;
import com.imooc.mapper.UsersMapper;
import com.imooc.pojo.Users;
import com.imooc.service.UserService;
import com.imooc.utils.DateUtil;
import com.imooc.utils.DesensitizationUtil;
import org.n3r.idworker.Sid;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.Date;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UsersMapper usersMapper;

    @Autowired
    private Sid sid;
    private static final String USER_FACE1 = "https://static-1316882214.cos.ap-guangzhou.myqcloud.com/img/202303081606414.png";

    @Override
    public Users queryMobileIsExist(String mobile) {// 通过手机号查询用户是否存在
        Example userExample = new Example(Users.class);// 通过Example类来构造查询条件
        Example.Criteria criteria = userExample.createCriteria();// 通过createCriteria()方法来构造查询条件
        criteria.andEqualTo("mobile", mobile);// andEqualTo()方法表示等于
        Users user = usersMapper.selectOneByExample(userExample);// 通过selectOneByExample()方法来查询
        return user;
    }

    @Transactional
    @Override
    public Users createUser(String mobile) {
        // 获得全局唯一主键
        String userId = sid.nextShort();// 通过Sid类来生成主键
        Users user = new Users();// 创建用户对象
        user.setId(userId);// 设置主键
        user.setMobile(mobile);// 设置手机号
        user.setNickname("用户：" + DesensitizationUtil.commonDisplay(mobile));// 设置昵称
        user.setImoocNum(DesensitizationUtil.commonDisplay(mobile));// 设置imooc号
        user.setFace(USER_FACE1);// 设置默认头像
        user.setBirthday(DateUtil.stringToDate("2001-01-01"));// 设置默认生日
        user.setSex(Sex.secret.type);
        user.setCountry("中国");
        user.setProvince("");
        user.setCity("");
        user.setDistrict("");
        user.setDescription("这家伙很懒，什么都没留下~");
        user.setCanImoocNumBeUpdated(YesOrNo.YES.type);// 设置可以修改imooc号
        user.setCreatedTime(new Date());// 设置创建时间
        user.setUpdatedTime(new Date());// 设置更新时间
        usersMapper.insert(user);// 通过insert()方法来插入数据
        return user;
    }

    // 通过用户id查询用户信息
    @Override
    public Users getUser(String userId) {
        return usersMapper.selectByPrimaryKey(userId);
    }

    // 更新用户信息
    @Transactional
    @Override
    public Users updateUserInfo(UpdatedUserBO updatedUserBO) {

        Users pendingUser = new Users();
        BeanUtils.copyProperties(updatedUserBO, pendingUser);

        int result = usersMapper.updateByPrimaryKeySelective(pendingUser);// 通过updateByPrimaryKeySelective()方法来更新数据，只更新不为null的字段
        if (result != 1) {
            GraceException.display(ResponseStatusEnum.USER_UPDATE_ERROR);
        }

        return getUser(updatedUserBO.getId());
    }

    // 更新用户信息
    @Transactional
    @Override
    public Users updateUserInfo(UpdatedUserBO updatedUserBO, Integer type) {
        Example example = new Example(Users.class);// 通过Example类来构造查询条件
        Example.Criteria criteria = example.createCriteria();// 通过createCriteria()方法来构造查询条件
        // 如果用户输入的昵称
        if (type == UserInfoModifyType.NICKNAME.type) {
            criteria.andEqualTo("nickname", updatedUserBO.getNickname());
            Users user = usersMapper.selectOneByExample(example);
            if (user != null) {
                GraceException.display(ResponseStatusEnum.USER_INFO_UPDATED_NICKNAME_EXIST_ERROR);
            }
        }

        // 如果用户输入的imooc号
        if (type == UserInfoModifyType.IMOOCNUM.type) {
            criteria.andEqualTo("imoocNum", updatedUserBO.getImoocNum());
            Users user = usersMapper.selectOneByExample(example);
            // 如果用户输入的imooc号已经存在，且不是自己的imooc号，则抛出异常
            if (user != null) {
                GraceException.display(ResponseStatusEnum.USER_INFO_UPDATED_NICKNAME_EXIST_ERROR);
            }

            Users tempUser =  getUser(updatedUserBO.getId());
            // 如果用户不可以修改imooc号，则抛出异常
            if (tempUser.getCanImoocNumBeUpdated() == YesOrNo.NO.type) {
                GraceException.display(ResponseStatusEnum.USER_INFO_CANT_UPDATED_IMOOCNUM_ERROR);
            }

            updatedUserBO.setCanImoocNumBeUpdated(YesOrNo.NO.type);
        }

        return updateUserInfo(updatedUserBO);
    }
}
