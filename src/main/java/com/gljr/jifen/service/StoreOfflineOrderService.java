package com.gljr.jifen.service;

import com.gljr.jifen.common.JsonResult;
import com.gljr.jifen.pojo.*;
import com.qiniu.util.Json;

import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public interface StoreOfflineOrderService {

    /**
     * 添加一个线下订单
     * @param storeOfflineOrder
     * @return
     */
    JsonResult insertOfflineOrder(StoreOfflineOrder storeOfflineOrder, String uid, JsonResult jsonResult);



    /**
     * 查询某个用户的线下订单
     * @return
     */
    JsonResult selectOfflineOrderByUid(String uid, Integer page, Integer per_page, Integer sort, String start_time, String end_time, JsonResult jsonResult);


    /**
     * 查询所有线下订单
     * @return
     */
    JsonResult selectOfflineOrders(Integer page, Integer per_page, String trxCode, Integer status, Date begin, Date end, String phone, String storeName, JsonResult jsonResult);

    JsonResult selectOfflineRefundOrders(Integer page, Integer per_page, String trxCode, Integer status, Date begin, Date end, JsonResult jsonResult);

    /**
     * 根据条件查询线下订单
     * @param reqParam 查询参数
     * @param storeId 商户ID
     * @return
     */
    List<StoreOfflineOrder> selectAllOfflineOrderByExample(OfflineOrderReqParam reqParam, int storeId);

    /**
     * 根据uid获取商户已经获得的积分和现金总额
     */
    HashMap getStoreTotalInfo(int uid);

    /**
     * 根据订单号获取订单信息
     *
     * @param trxCode 订单号
     * @return
     */
    List<StoreOfflineOrder> getStoreTotalInfo(String trxCode);

    /**
     * 根据商户ID和订单号获取订单信息
     * @param uid 商户ID
     * @param trxCode 订单号
     * @return
     */
    List<StoreOfflineOrder> getStoreTotalInfo(int uid, String trxCode);

    /**
     * 商户退款操作
     * @param storeOfflineOrder 退款订单信息
     */
    void refund(StoreOfflineOrder storeOfflineOrder);


    /**
     * 积分交易
     *
     * @param userCredits 用户积分
     * @param userId 用户id
     * @param storeUid 商户id
     * @param integral 交易积分
     */
    JsonResult integralTrad(UserCredits userCredits, int userId, int storeUid, int integral, JsonResult jsonResult);

    /**
     * 修改一条线下订单
     * @param storeOfflineOrder
     * @return
     */
    int updateOfflineOrder(StoreOfflineOrder storeOfflineOrder);

    /**
     * 查询某个用户的线下订单
     * @return
     */
    List<StoreOfflineOrder> selectAllOfflineOrderByUid(int uid, int sort, String start_time, String end_time);

    /**
     * 代金券校验
     *
     * @param uid 用户id
     * @param storeInfo 商户信息
     * @param userCoupon 用户购买代金券信息
     * @param storeCoupon 商户发放代金券信息
     */
    String verifyCoupon(int uid, StoreInfo storeInfo, UserCoupon userCoupon, StoreCoupon storeCoupon);

    /**
     * 根据代码获取用户购买的代金券
     *
     * @param couponCode 代金券代码
     * @param storeId 商店ID
     * @return UserCoupon
     */
    UserCoupon findUserCouponByCode(String couponCode, int storeId);

    /**
     * 根据商铺代金券id获取代金券具体信息
     *
     * @param storeCouponId 代金券Id
     * @return UserCoupon
     */
    StoreCoupon findStoreCouponById(int storeCouponId);
}
