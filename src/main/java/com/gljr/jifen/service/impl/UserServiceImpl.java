package com.gljr.jifen.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.gljr.jifen.common.*;
import com.gljr.jifen.constants.DBConstants;
import com.gljr.jifen.constants.GlobalConstants;
import com.gljr.jifen.dao.*;
import com.gljr.jifen.pojo.*;
import com.gljr.jifen.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserServiceImpl implements UserService {


    @Autowired
    private UserAddressMapper userAddressMapper;

    @Autowired
    private UserOnlineMapper userOnlineMapper;

    @Autowired
    private UserCreditsMapper userCreditsMapper;

    @Autowired
    private UserExtInfoMapper userExtInfoMapper;

    @Autowired
    private MessageMapper messageMapper;


    @Override
    public JsonResult selecteUserInfoByUid(Integer uid, String gltoken, JsonResult jsonResult) {
        String realName;
        Integer totalValue;
        Integer validValue;
        String phone;
        String walletAddress;
        Integer viewType;
        String token = "";
        Integer readMessage;
        String token_key = StrUtil.randomKey(32);

        try {

            //带够力token，新用户生成在线状态，老用户更新商城token，不带够力token，返回商城用户信息
            if(!StringUtils.isEmpty(gltoken)){
                //去沟里验证token




                UserOnlineExample userOnlineExample = new UserOnlineExample();
                UserOnlineExample.Criteria criteria = userOnlineExample.or();
                criteria.andUidEqualTo(uid);

                List<UserOnline> userOnlines = userOnlineMapper.selectByExample(userOnlineExample);
                if(ValidCheck.validList(userOnlines)){
                    //第一次进入商城
                    UserOnline userOnline = new UserOnline();
                    userOnline.setLoginTime(new Timestamp(System.currentTimeMillis()));
                    userOnline.setUid(uid);
                    userOnline.setToken(token_key);

                    userOnlineMapper.insert(userOnline);
                }else{
                    //不是第一次，更新token，修改登陆时间
                    UserOnline userOnline = userOnlines.get(0);
                    userOnline.setToken(token_key);
                    userOnline.setLoginTime(new Timestamp(System.currentTimeMillis()));

                    userOnlineMapper.updateByPrimaryKey(userOnline);
                }

                JSONObject jsonObject = new JSONObject();
                jsonObject.put("uid", uid);
                jsonObject.put("role", "USER");

                //生成token
                token = JwtUtil.createJWT(GlobalConstants.GLJR_PREFIX, jsonObject.toString(), token_key, 60*60*24*365);
            }


            //获取用户信息
            UserCreditsExample userCreditsExample = new UserCreditsExample();
            UserCreditsExample.Criteria criteria = userCreditsExample.or();
            criteria.andOwnerIdEqualTo(uid);
            criteria.andOwnerTypeEqualTo(DBConstants.OwnerType.CUSTOMER.getCode());

            List<UserCredits> userCreditsList = userCreditsMapper.selectByExample(userCreditsExample);
            UserCredits userCredits = userCreditsList.get(0);

            if(ValidCheck.validList(userCreditsList)){
                CommonResult.userNotExit(jsonResult);
                return jsonResult;
            }

            UserExtInfoExample userExtInfoExample = new UserExtInfoExample();
            UserExtInfoExample.Criteria criteria1 = userExtInfoExample.or();
            criteria1.andUidEqualTo(uid);
            List<UserExtInfo> userExtInfos = userExtInfoMapper.selectByExample(userExtInfoExample);
            UserExtInfo userExtInfo = userExtInfos.get(0);

            if(ValidCheck.validList(userExtInfos)){
                CommonResult.userNotExit(jsonResult);
                return jsonResult;
            }

            //判断是否有未读消息
            MessageExample messageExample = new MessageExample();
            MessageExample.Criteria criteria2 = messageExample.or();
            criteria2.andUidEqualTo(uid);
            List<Message> messages = messageMapper.selectByExample(messageExample);
            if(ValidCheck.validList(messages)){
                readMessage = 0;
            }else {
                readMessage = 1;
            }

            totalValue = userCredits.getIntegral();
            validValue = userCredits.getFrozenIntegral();
            walletAddress = userCredits.getWalletAddress();
            phone = userExtInfo.getCellphone();
            viewType = userExtInfo.getViewType();

            Map map = new HashMap();
            map.put("totalValue", totalValue);
            map.put("validValue", validValue);
            map.put("walletAddress", walletAddress);
            map.put("phone", phone);
            map.put("viewType", viewType);
            map.put("token", token);
            map.put("readMessage", readMessage);
            map.put("uid", uid);

            jsonResult.setItem(map);
            CommonResult.success(jsonResult);


        }catch (Exception e){
            System.out.println(e);
            CommonResult.sqlFailed(jsonResult);
        }
        return jsonResult;
    }





    @Override
    public int insertUserAddress(UserAddress userAddress) {
        return userAddressMapper.insert(userAddress);
    }

    @Override
    public List<UserAddress> selectUserAddressByUid(Integer uid) {
        UserAddressExample userAddressExample = new UserAddressExample();
        UserAddressExample.Criteria criteria = userAddressExample.or();
        criteria.andUidEqualTo(uid);
        userAddressExample.setOrderByClause("is_default desc");
        return userAddressMapper.selectByExample(userAddressExample);
    }

    @Override
    public UserAddress selectUserAddressByIsDefault(Integer uid) {
        UserAddressExample userAddressExample = new UserAddressExample();
        UserAddressExample.Criteria criteria = userAddressExample.or();
        criteria.andIsDefaultEqualTo(1);
        criteria.andUidEqualTo(uid);
        List<UserAddress> userAddresses = userAddressMapper.selectByExample(userAddressExample);
        //如果不存在返回最新地址
        if(userAddresses.size() == 0){
            UserAddressExample.Criteria criteria1 = userAddressExample.or();
            criteria1.andUidEqualTo(uid);
            userAddressExample.setOrderByClause("id desc");
            List<UserAddress> userAddresses1 = userAddressMapper.selectByExample(userAddressExample);
            if(userAddresses1.size() != 0){
                return userAddresses1.get(0);
            }
        }else{
            return userAddresses.get(0);
        }
        return null;
    }

    @Override
    public int updateUserAddressById(UserAddress userAddress) {
        return userAddressMapper.updateByPrimaryKey(userAddress);
    }

    @Override
    public int deleteUserAddressById(Integer id) {
        return userAddressMapper.deleteByPrimaryKey(id);
    }

    @Override
    public UserAddress selectUserAddressById(Integer id) {
        return userAddressMapper.selectByPrimaryKey(id);
    }

    @Override
    @Transactional
    public int insertUserInfo(UserCredits userCredits, UserExtInfo userExtInfo, UserOnline userOnline) {
        userCreditsMapper.insert(userCredits);

        userExtInfoMapper.insert(userExtInfo);

        userOnlineMapper.insert(userOnline);
        return 0;
    }

    @Override
    public List<UserOnline> selectUserOnlineByUid(Integer uid) {
        UserOnlineExample userOnlineExample = new UserOnlineExample();
        UserOnlineExample.Criteria criteria = userOnlineExample.or();
        criteria.andUidEqualTo(uid);
        return userOnlineMapper.selectByExample(userOnlineExample);
    }

    @Override
    @Transactional
    public int updateUserInfo(UserCredits userCredits, UserOnline userOnline) {
        userCreditsMapper.updateByPrimaryKey(userCredits);

        userOnlineMapper.updateByPrimaryKey(userOnline);
        return 0;
    }

    @Override
    public int insertUserOnline(UserOnline userOnline) {
        return userOnlineMapper.insert(userOnline);
    }

    @Override
    public List<UserExtInfo> selectUserExtInfoByUid(Integer uid) {
        UserExtInfoExample userExtInfoExample = new UserExtInfoExample();
        UserExtInfoExample.Criteria criteria = userExtInfoExample.or();
        criteria.andUidEqualTo(uid);
        return userExtInfoMapper.selectByExample(userExtInfoExample);
    }

    @Override
    public int insertUserExtInfo(UserExtInfo userExtInfo) {
        return userExtInfoMapper.insert(userExtInfo);
    }
}
