package com.gljr.jifen.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.gljr.jifen.common.CommonResult;
import com.gljr.jifen.common.JsonResult;
import com.gljr.jifen.common.ValidCheck;
import com.gljr.jifen.common.dtchain.vo.CommonOrderResponse;
import com.gljr.jifen.common.dtchain.GatewayResponse;
import com.gljr.jifen.constants.DBConstants;
import com.gljr.jifen.constants.GlobalConstants;
import com.gljr.jifen.dao.*;
import com.gljr.jifen.pojo.*;
import com.gljr.jifen.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;


import java.sql.Timestamp;
import java.util.*;

@Service
public class OnlineOrderServiceImpl implements OnlineOrderService {

    @Autowired
    private OnlineOrderMapper onlineOrderMapper;

    @Autowired
    private TransactionMapper transactionMapper;

    @Autowired
    private UserCreditsMapper userCreditsMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private SerialNumberService serialNumberService;

    @Autowired
    private UserExtInfoMapper userExtInfoMapper;

    @Autowired
    private StoreInfoMapper storeInfoMapper;

    @Autowired
    private DTChainService chainService;

    @Autowired
    private VirtualProductMapper virtualProductMapper;

    @Autowired
    private SystemVirtualProductMapper systemVirtualProductMapper;

    @Autowired
    private StoreOfflineOrderMapper storeOfflineOrderMapper;

    @Autowired
    private IntegralTransferOrderMapper integralTransferOrderMapper;


    private final static int PRODUCT_NOT_FOUND = 403;

    private final static int INVALID_PRODUCT_INTEGRAL = 404;

    private final static int INVALID_STORE_INFO = 405;

    private final static int STORE_NOT_FOUND = 406;

    private final static int INVALID_STORE_TYPE = 407;

    private final static int STORE_NOT_ACCEPT_INTEGRAL = 408;

    @Override
    public JsonResult selectOrdersByUid(String uid, Integer page, Integer per_page, Integer sort, String start_time, String end_time, JsonResult jsonResult) {
        try {

            //在线订单
            OnlineOrderExample onlineOrderExample = new OnlineOrderExample();
            OnlineOrderExample.Criteria criteria = onlineOrderExample.or();
            criteria.andUidEqualTo(Integer.parseInt(uid));
            onlineOrderExample.setOrderByClause("id desc");


            List<OnlineOrder> onlineOrders = onlineOrderMapper.selectByExample(onlineOrderExample);



            List<UserOrder> userOrders = new ArrayList<>();


            if(!ValidCheck.validList(onlineOrders)) {
                for (OnlineOrder onlineOrder : onlineOrders) {

                    UserOrder userOrder = new UserOrder();


                    UserExtInfoExample userExtInfoExample = new UserExtInfoExample();
                    UserExtInfoExample.Criteria criteria1 = userExtInfoExample.or();
                    criteria1.andUidEqualTo(onlineOrder.getUid());
                    List<UserExtInfo> userExtInfos = userExtInfoMapper.selectByExample(userExtInfoExample);
                    if (ValidCheck.validList(userExtInfos)) {
                        CommonResult.userNotExit(jsonResult);
                        return jsonResult;
                    }

                    userOrder.setCreateTime(onlineOrder.getCreateTime());
                    userOrder.setId(onlineOrder.getId());
                    userOrder.setIntegral(onlineOrder.getIntegral());
                    userOrder.setQuantity(onlineOrder.getQuantity());
                    userOrder.setStatus(onlineOrder.getStatus());
                    userOrder.setTrxCode(onlineOrder.getTrxCode());
                    userOrder.setUpdateTime(onlineOrder.getUpdateTime());
                    userOrder.setUserName(userExtInfos.get(0).getCellphone());
                    userOrder.setTrxType(DBConstants.TrxType.OFFLINE.getCode());

                    if(onlineOrder.getSiId() != 0) {
                        Product product = productMapper.selectByPrimaryKey(onlineOrder.getPid());
                        userOrder.setName(product.getName());
                        userOrder.setDescription("数量：" + onlineOrder.getQuantity() + product.getUnit());


                        StoreInfo storeInfo = storeInfoMapper.selectByPrimaryKey(product.getSiId());
                        if (ValidCheck.validPojo(storeInfo)) {
                            CommonResult.noObject(jsonResult);
                            return jsonResult;
                        }
                        userOrder.setStoreName(storeInfo.getName());
                        userOrder.setTrxType(DBConstants.TrxType.ONLINE.getCode());
                    }else{
                        VirtualProduct virtualProduct = virtualProductMapper.selectByPrimaryKey(onlineOrder.getPid());
                        userOrder.setName(virtualProduct.getTitle());
                        userOrder.setDescription("数量：" + onlineOrder.getQuantity() + "张");
                        userOrder.setStoreName("够力金融");
                    }

                    userOrders.add(userOrder);
                }
            }


            //线下订单
            StoreOfflineOrderExample storeOfflineOrderExample = new StoreOfflineOrderExample();
            StoreOfflineOrderExample.Criteria criteria1 = storeOfflineOrderExample.or();
            criteria1.andUidEqualTo(Integer.parseInt(uid));
            storeOfflineOrderExample.setOrderByClause("id desc");

            List<StoreOfflineOrder> storeOfflineOrders = storeOfflineOrderMapper.selectByExample(storeOfflineOrderExample);

            if (!ValidCheck.validList(storeOfflineOrders)) {


                for (StoreOfflineOrder storeOfflineOrder : storeOfflineOrders) {

                    UserOrder userOrder = new UserOrder();

                    userOrder.setUpdateTime(storeOfflineOrder.getUpdateTime());
                    userOrder.setTrxType(DBConstants.TrxType.OFFLINE.getCode());
                    userOrder.setTrxCode(storeOfflineOrder.getTrxCode());
                    userOrder.setStatus(storeOfflineOrder.getStatus());
                    userOrder.setQuantity(0);
                    userOrder.setIntegral(storeOfflineOrder.getIntegral());
                    userOrder.setId(storeOfflineOrder.getId());
                    userOrder.setCreateTime(storeOfflineOrder.getCreateTime());

                    StoreInfo storeInfo = storeInfoMapper.selectByPrimaryKey(storeOfflineOrder.getSiId());
                    userOrder.setName(storeInfo.getName());
                    if (storeOfflineOrder.getExtCash() == 0) {
                        userOrder.setDescription("抵扣积分" + storeOfflineOrder.getIntegral() + "分");
                    } else {
                        userOrder.setDescription("抵扣积分" + storeOfflineOrder.getIntegral() + "分，现金支付" + storeOfflineOrder.getExtCash() + "元");
                    }

                    userOrders.add(userOrder);

                }
            }


            //转增订单
            IntegralTransferOrderExample integralTransferOrderExample = new IntegralTransferOrderExample();
            IntegralTransferOrderExample.Criteria criteria2 = integralTransferOrderExample.or();
            criteria2.andUidEqualTo(Integer.parseInt(uid));
            integralTransferOrderExample.setOrderByClause("id desc");

            List<IntegralTransferOrder> integralTransferOrders = integralTransferOrderMapper.selectByExample(integralTransferOrderExample);

            if(!ValidCheck.validList(integralTransferOrders)) {
                for (IntegralTransferOrder integralTransferOrder : integralTransferOrders) {

                    UserOrder userOrder = new UserOrder();

                    userOrder.setTrxType(DBConstants.TrxType.TRANSFER.getCode());
                    userOrder.setTrxCode(integralTransferOrder.getTrxCode());
                    userOrder.setStatus(integralTransferOrder.getStatus());
                    userOrder.setQuantity(0);
                    userOrder.setIntegral(integralTransferOrder.getIntegral());
                    userOrder.setId(integralTransferOrder.getId());
                    userOrder.setCreateTime(integralTransferOrder.getCreateTime());

                    UserExtInfoExample userExtInfoExample = new UserExtInfoExample();
                    UserExtInfoExample.Criteria criteria3 = userExtInfoExample.or();
                    criteria3.andUidEqualTo(integralTransferOrder.getgUid());

                    List<UserExtInfo> userExtInfos = userExtInfoMapper.selectByExample(userExtInfoExample);
                    if(ValidCheck.validList(userExtInfos)){
                        userOrder.setName("用户不存在");
                    }else {
                        userOrder.setName(userExtInfos.get(0).getCellphone());
                        userOrder.setDescription("转账" + integralTransferOrder.getIntegral() + "分");
                    }
                    userOrders.add(userOrder);
                }
            }


            //排序
            //积分低到高
            Collections.sort(userOrders, new Comparator<UserOrder>() {

                @Override
                public int compare(UserOrder o1, UserOrder o2) {
                    //按照学生的年龄进行升序排列
                    if (o1.getCreateTime().before(o2.getCreateTime())) {
                        return 1;
                    }
                    if (o1.getIntegral() == o2.getIntegral()) {
                        return 0;
                    }
                    return -1;
                }

            });




            PageInfo pageInfo = new PageInfo(userOrders);

            Map  map = new HashMap();
            map.put("data", userOrders);
            map.put("pages", pageInfo.getPages());

            map.put("total", pageInfo.getTotal());
            //当前页
            map.put("pageNum", pageInfo.getPageNum());

            jsonResult.setItem(map);
            CommonResult.success(jsonResult);

        } catch (Exception e) {
            System.out.println(e);
            CommonResult.sqlFailed(jsonResult);
        }

        return jsonResult;
    }

    @Override
    public JsonResult selectOnlineOrdersByUid(String uid, Integer page, Integer per_page, Integer sort, String start_time, String end_time, JsonResult jsonResult) {
        try {

            OnlineOrderExample onlineOrderExample = new OnlineOrderExample();
            OnlineOrderExample.Criteria criteria = onlineOrderExample.or();
            criteria.andUidEqualTo(Integer.parseInt(uid));
            onlineOrderExample.setOrderByClause("id desc");

            PageHelper.startPage(page,per_page);
            List<OnlineOrder> onlineOrders = onlineOrderMapper.selectByExample(onlineOrderExample);
            PageInfo pageInfo = new PageInfo(onlineOrders);

            if(!ValidCheck.validList(onlineOrders)) {
                for (OnlineOrder onlineOrder : onlineOrders) {

                    UserExtInfoExample userExtInfoExample = new UserExtInfoExample();
                    UserExtInfoExample.Criteria criteria1 = userExtInfoExample.or();
                    criteria1.andUidEqualTo(onlineOrder.getUid());
                    List<UserExtInfo> userExtInfos = userExtInfoMapper.selectByExample(userExtInfoExample);
                    if(ValidCheck.validList(userExtInfos)){
                        CommonResult.userNotExit(jsonResult);
                        return jsonResult;
                    }
                    onlineOrder.setUserName(userExtInfos.get(0).getCellphone());

                    if(onlineOrder.getSiId() != 0) {
                        Product product = productMapper.selectByPrimaryKey(onlineOrder.getPid());
                        onlineOrder.setName(product.getName());
                        onlineOrder.setDescription("数量：" + onlineOrder.getQuantity() + product.getUnit());


                        StoreInfo storeInfo = storeInfoMapper.selectByPrimaryKey(product.getSiId());
                        if (ValidCheck.validPojo(storeInfo)) {
                            CommonResult.noObject(jsonResult);
                            return jsonResult;
                        }
                        onlineOrder.setStoreName(storeInfo.getName());
                        onlineOrder.setTrxType(DBConstants.TrxType.ONLINE.getCode());
                    }else {
                        VirtualProduct virtualProduct = virtualProductMapper.selectByPrimaryKey(onlineOrder.getPid());
                        onlineOrder.setName(virtualProduct.getTitle());
                        onlineOrder.setDescription("数量：" + onlineOrder.getQuantity() + "张");
                        onlineOrder.setStoreName("够力金融");
                        onlineOrder.setTrxType(DBConstants.TrxType.OFFLINE.getCode());
                    }
                }
            }

            Map  map = new HashMap();
            map.put("data", onlineOrders);
            map.put("pages", pageInfo.getPages());

            map.put("total", pageInfo.getTotal());
            //当前页
            map.put("pageNum", pageInfo.getPageNum());

            jsonResult.setItem(map);
            CommonResult.success(jsonResult);

        } catch (Exception e) {
            CommonResult.sqlFailed(jsonResult);
        }

        return jsonResult;
    }


    @Override
    public JsonResult selectOnlineOrdersByUidNotPay(String uid, Integer page, Integer per_page, Integer sort, String start_time, String end_time, JsonResult jsonResult) {

        try {
            OnlineOrderExample onlineOrderExample = new OnlineOrderExample();
            OnlineOrderExample.Criteria criteria = onlineOrderExample.or();
            criteria.andStatusEqualTo(DBConstants.OrderStatus.UNPAID.getCode());
            criteria.andUidEqualTo(Integer.parseInt(uid));
            onlineOrderExample.setOrderByClause("id desc");

            PageHelper.startPage(page,per_page);
            List<OnlineOrder> onlineOrders = onlineOrderMapper.selectByExample(onlineOrderExample);
            PageInfo pageInfo = new PageInfo(onlineOrders);

            if(!ValidCheck.validList(onlineOrders)) {
                for (OnlineOrder onlineOrder : onlineOrders) {

                    UserExtInfoExample userExtInfoExample = new UserExtInfoExample();
                    UserExtInfoExample.Criteria criteria1 = userExtInfoExample.or();
                    criteria1.andUidEqualTo(onlineOrder.getUid());
                    List<UserExtInfo> userExtInfos = userExtInfoMapper.selectByExample(userExtInfoExample);
                    if(ValidCheck.validList(userExtInfos)){
                        CommonResult.userNotExit(jsonResult);
                        return jsonResult;
                    }
                    onlineOrder.setUserName(userExtInfos.get(0).getCellphone());

                    if(onlineOrder.getSiId() != 0) {
                        Product product = productMapper.selectByPrimaryKey(onlineOrder.getPid());
                        onlineOrder.setName(product.getName());
                        onlineOrder.setDescription("数量：" + onlineOrder.getQuantity() + product.getUnit());


                        StoreInfo storeInfo = storeInfoMapper.selectByPrimaryKey(product.getSiId());
                        if (ValidCheck.validPojo(storeInfo)) {
                            CommonResult.noObject(jsonResult);
                            return jsonResult;
                        }
                        onlineOrder.setStoreName(storeInfo.getName());
                        onlineOrder.setTrxType(DBConstants.TrxType.ONLINE.getCode());
                    }else {
                        VirtualProduct virtualProduct = virtualProductMapper.selectByPrimaryKey(onlineOrder.getPid());
                        onlineOrder.setName(virtualProduct.getTitle());
                        onlineOrder.setDescription("数量：" + onlineOrder.getQuantity() + "张");
                        onlineOrder.setStoreName("够力金融");
                        onlineOrder.setTrxType(DBConstants.TrxType.OFFLINE.getCode());
                    }
                }
            }

            Map  map = new HashMap();
            map.put("data", onlineOrders);
            map.put("pages", pageInfo.getPages());

            map.put("total", pageInfo.getTotal());
            //当前页
            map.put("pageNum", pageInfo.getPageNum());

            jsonResult.setItem(map);
            CommonResult.success(jsonResult);

        } catch (Exception e) {
            CommonResult.sqlFailed(jsonResult);
        }

        return jsonResult;
    }


    @Transactional
    @Override
    public JsonResult insertOnlineOrder(OnlineOrder onlineOrder, String uid, JsonResult jsonResult) {

        try {
            Product product = productMapper.selectByPrimaryKey(onlineOrder.getPid());
            if(ValidCheck.validPojo(product)){
                CommonResult.noObject(jsonResult);
                return jsonResult;
            }

            if(product.getStatus() != DBConstants.ProductStatus.ON_SALE.getCode()){
                jsonResult.setMessage("商品不可销售！");
                jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                return jsonResult;
            }

            if(onlineOrder.getQuantity() <= 0){
                jsonResult.setMessage("购买商品数量必须大于0！");
                jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                return jsonResult;
            }

            //0不限制购买
            if(product.getMaxPurchases() != 0) {
                if (onlineOrder.getQuantity() > product.getMaxPurchases()) {
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    jsonResult.setMessage("超过购买限制数量！");
                    return jsonResult;
                }

                OnlineOrderExample onlineOrderExample = new OnlineOrderExample();
                OnlineOrderExample.Criteria criteria = onlineOrderExample.or();
                criteria.andUidEqualTo(Integer.parseInt(uid));
                criteria.andPidEqualTo(onlineOrder.getPid());
                criteria.andStatusNotEqualTo(DBConstants.OrderStatus.CANCELED.getCode());
                //查询已经购买该商品的数量
                int alreadyNum = 0;
                List<OnlineOrder> exitOnlineOrders = onlineOrderMapper.selectByExample(onlineOrderExample);
                if (!ValidCheck.validPojo(exitOnlineOrders)) {
                    for (OnlineOrder exitOnlineOrder : exitOnlineOrders) {
                        alreadyNum += exitOnlineOrder.getQuantity();
                    }
                }
                alreadyNum += onlineOrder.getQuantity();

                if (alreadyNum > product.getMaxPurchases()) {
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    jsonResult.setMessage("当前购买数量和已购买数量超过购买限制数量！");
                    return jsonResult;
                }
            }

            if(onlineOrder.getQuantity() >= product.getInvetory()){
                jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                jsonResult.setMessage("购买数量大于库存量！");
                return jsonResult;
            }

            //确保购买积分正确
            onlineOrder.setIntegral(product.getIntegral()*onlineOrder.getQuantity());

            //验证用户积分
            UserCredits userCredits = this.userCreditsMapper.getUserCredits(Integer.parseInt(uid),
                    DBConstants.OwnerType.CUSTOMER.getCode());

            if (null == userCredits) {
                jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                jsonResult.setMessage("用户积分信息不存在！");
                return jsonResult;
            }

            if (userCredits.getIntegral() < onlineOrder.getIntegral()) {
                jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                jsonResult.setMessage(GlobalConstants.INTEGRAL_NOT_ENOUGH);
                return jsonResult;
            }

            //更新商品数量
            product.setSales(product.getSales() + onlineOrder.getQuantity());
            product.setInvetory(product.getInvetory() - onlineOrder.getQuantity());

            onlineOrder.setCreateTime(new Timestamp(System.currentTimeMillis()));
            onlineOrder.setUid(Integer.parseInt(uid));
            onlineOrder.setStatus(DBConstants.OrderStatus.UNPAID.getCode());
            onlineOrder.setSiId(product.getSiId());


            //添加一条通用交易信息
            Transaction transaction = new Transaction();
            transaction.setType(DBConstants.TrxType.ONLINE.getCode());
            transaction.setOwnerType(DBConstants.OwnerType.CUSTOMER.getCode());
            transaction.setOwnerId(Integer.parseInt(uid));
            transaction.setIntegral(-1 * onlineOrder.getIntegral());
            transaction.setCreateTime(new Timestamp(System.currentTimeMillis()));
            transaction.setStatus(DBConstants.TrxStatus.UNPAID.getCode());
            transaction.setCode(serialNumberService.getNextTrxCode(DBConstants.TrxType.ONLINE.getCode()));

            transactionMapper.insert(transaction);

            onlineOrder.setTrxCode(transaction.getCode());
            onlineOrder.setTrxId(transaction.getId());

            onlineOrderMapper.insert(onlineOrder);

            productMapper.updateByPrimaryKey(product);

            Map map = new HashMap();
            map.put("data", onlineOrder);

            jsonResult.setErrorCode(GlobalConstants.OPERATION_SUCCEED);
            jsonResult.setMessage(onlineOrder.getTrxCode());
            jsonResult.setItem(map);

        }catch (Exception e){
            System.out.println(e);
            CommonResult.sqlFailed(jsonResult);
        }

        return jsonResult;
    }

    @Override
    @Transactional
    public JsonResult updateOnlineOrderByTrxCode(String trxCode, String uid, JsonResult jsonResult) {

        System.out.println(trxCode);
        OnlineOrderExample onlineOrderExample = new OnlineOrderExample();
        OnlineOrderExample.Criteria criteria = onlineOrderExample.or();
        criteria.andTrxCodeEqualTo(trxCode);
        criteria.andUidEqualTo(Integer.parseInt(uid));

        try {
            //查询订单
            List<OnlineOrder> onlineOrders = onlineOrderMapper.selectByExample(onlineOrderExample);

            if(ValidCheck.validList(onlineOrders)){
                CommonResult.noObject(jsonResult);
                return jsonResult;
            }
            OnlineOrder onlineOrder = onlineOrders.get(0);
            //订单状态为未付款才能继续进行
            if(onlineOrder.getStatus() == DBConstants.OrderStatus.UNPAID.getCode()){

                GatewayResponse<CommonOrderResponse> response = this.chainService.postOrders(onlineOrder.getUid() + 0L,
                        onlineOrder.getPid() + 0L, DBConstants.OnlineProductType.NORMAL_PRODUCT.getCode(), onlineOrder.getIntegral(), onlineOrder.getQuantity());
                if (null == response) {
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    return jsonResult;
                }

                if (PRODUCT_NOT_FOUND == response.getCode()) {
                    jsonResult.setMessage("要付款的商品不存在或已下架！");
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    return jsonResult;
                }

                if (INVALID_PRODUCT_INTEGRAL == response.getCode()) {
                    jsonResult.setMessage("商品积分与支付积分不匹配！");
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    return jsonResult;
                }

                if (INVALID_STORE_INFO == response.getCode()) {
                    jsonResult.setMessage("商品对应的商户信息无效！");
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    return jsonResult;
                }

                if (STORE_NOT_FOUND == response.getCode()) {
                    jsonResult.setMessage("商户信息不存在！");
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    return jsonResult;
                }

                if (INVALID_STORE_TYPE == response.getCode()) {
                    jsonResult.setMessage("商户类型不匹配！");
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    return jsonResult;
                }

                if(STORE_NOT_ACCEPT_INTEGRAL == response.getCode()) {
                    jsonResult.setMessage("商户暂不接受积分支付！");
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    return jsonResult;
                }

                if(response.getCode() != 200) {
                    jsonResult.setMessage(response.getMessage());
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    return jsonResult;
                }

                onlineOrder.setDtchainBlockId(response.getContent().getBlockId());
                onlineOrder.setExtOrderId(response.getContent().getExtOrderId());
                onlineOrder.setStatus(DBConstants.OrderStatus.PAID.getCode());
                onlineOrder.setUpdateTime(new Timestamp(System.currentTimeMillis()));

                onlineOrderMapper.updateByPrimaryKey(onlineOrder);

                Transaction transaction = transactionMapper.selectByPrimaryKey(onlineOrder.getTrxId());
                transaction.setStatus(DBConstants.TrxStatus.COMPLETED.getCode());

                transactionMapper.updateByPrimaryKey(transaction);

                Map map = new HashMap();
                map.put("data", onlineOrder);

                jsonResult.setErrorCode(GlobalConstants.OPERATION_SUCCEED);
                jsonResult.setMessage(onlineOrder.getTrxCode());
                jsonResult.setItem(map);
            }else{
                jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                jsonResult.setMessage("该订单已付款，请勿重复操作！");
            }

        }catch (Exception e){
            CommonResult.sqlFailed(jsonResult);
        }

        return jsonResult;
    }

    @Override
    public JsonResult selectOnlineOrders(Integer page, Integer per_page, String trxCode, Integer status, Date begin, Date end, JsonResult jsonResult) {

        try{
            OnlineOrderExample onlineOrderExample = new OnlineOrderExample();
            OnlineOrderExample.Criteria criteria = onlineOrderExample.or();
            criteria.andStatusNotEqualTo(DBConstants.OrderStatus.DELETED.getCode());
            if(!StringUtils.isEmpty(trxCode)){
                criteria.andTrxCodeEqualTo(trxCode);
            }
            if(!StringUtils.isEmpty(status)){
                criteria.andStatusEqualTo(status);
            }
            criteria.andCreateTimeBetween(begin, end);
            onlineOrderExample.setOrderByClause("id desc");


            PageHelper.startPage(page,per_page);
            List<OnlineOrder> onlineOrders = onlineOrderMapper.selectByExample(onlineOrderExample);
            PageInfo pageInfo = new PageInfo(onlineOrders);

            //查询商品名称，商户名称，用户名称
            if(!ValidCheck.validList(onlineOrders)){
                for (OnlineOrder onlineOrder : onlineOrders){

                    UserExtInfoExample userExtInfoExample = new UserExtInfoExample();
                    UserExtInfoExample.Criteria criteria1 = userExtInfoExample.or();
                    criteria1.andUidEqualTo(onlineOrder.getUid());
                    List<UserExtInfo> userExtInfos = userExtInfoMapper.selectByExample(userExtInfoExample);

                    if (ValidCheck.validList(userExtInfos)) {
                        onlineOrder.setUserName("不存在");
                    } else {
                        onlineOrder.setUserName(userExtInfos.get(0).getCellphone());
                    }

                    if(onlineOrder.getSiId() != 0) {

                        //查找商品名称，商户名称
                        Product product = productMapper.selectByPrimaryKey(onlineOrder.getPid());
                        if (ValidCheck.validPojo(product)) {
                            onlineOrder.setName("已删除");
                            onlineOrder.setStoreName("不存在");
                        } else {
                            onlineOrder.setName(product.getName());
                            StoreInfo storeInfo = storeInfoMapper.selectByPrimaryKey(product.getSiId());
                            if (ValidCheck.validPojo(storeInfo)) {
                                onlineOrder.setStoreName("不存在");
                            } else {
                                onlineOrder.setStoreName(storeInfo.getName());
                            }
                        }
                    }else{
                        VirtualProduct virtualProduct = virtualProductMapper.selectByPrimaryKey(onlineOrder.getPid());
                        onlineOrder.setName(virtualProduct.getTitle());
                        onlineOrder.setDescription("数量：" + onlineOrder.getQuantity() + "张");
                        onlineOrder.setStoreName("够力金融");
                        onlineOrder.setTrxType(DBConstants.TrxType.OFFLINE.getCode());
                    }
                }
            }

            Map map = new HashMap();
            map.put("data", onlineOrders);
            map.put("pages", pageInfo.getPages());

            map.put("total", pageInfo.getTotal());
            //当前页
            map.put("pageNum", pageInfo.getPageNum());

            CommonResult.success(jsonResult);
            jsonResult.setItem(map);
        }catch (Exception e){
            System.out.println(e);
            CommonResult.sqlFailed(jsonResult);
        }

        return jsonResult;

    }



    @Override
    @Transactional
    public JsonResult cancelOnlineOrderByTrxCode(String trxCode, String uid, JsonResult jsonResult) {

        OnlineOrderExample onlineOrderExample = new OnlineOrderExample();
        OnlineOrderExample.Criteria criteria = onlineOrderExample.or();
        criteria.andUidEqualTo(Integer.parseInt(uid));
        criteria.andTrxCodeEqualTo(trxCode);

        try {
            List<OnlineOrder> onlineOrders = onlineOrderMapper.selectByExample(onlineOrderExample);

            if (ValidCheck.validList(onlineOrders)) {
                CommonResult.noObject(jsonResult);
                return jsonResult;
            }

            //更新订单状态
            OnlineOrder onlineOrder = onlineOrders.get(0);

            if(onlineOrder.getStatus() == DBConstants.OrderStatus.PAID.getCode()){
                jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                jsonResult.setMessage("已付款订单不能取消！");
                return jsonResult;
            }

            onlineOrder.setStatus(DBConstants.OrderStatus.CANCELED.getCode());
            onlineOrder.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            onlineOrderMapper.updateByPrimaryKey(onlineOrder);

            //更新通用交易表状态
            Transaction transaction = transactionMapper.selectByPrimaryKey(onlineOrder.getTrxId());
            if (ValidCheck.validPojo(transaction)) {
                CommonResult.noObject(jsonResult);
                return jsonResult;
            }
            transaction.setStatus(DBConstants.TrxStatus.CANCELED.getCode());
            transactionMapper.updateByPrimaryKey(transaction);

            //恢复商品的销量和库存
            Product product = productMapper.selectByPrimaryKey(onlineOrder.getPid());
            if (ValidCheck.validPojo(product)) {
                CommonResult.noObject(jsonResult);
                return jsonResult;
            }
            product.setInvetory(product.getInvetory() + onlineOrder.getQuantity());
            product.setSales(product.getSales() - onlineOrder.getQuantity());
            productMapper.updateByPrimaryKey(product);

            CommonResult.success(jsonResult);
        }catch (Exception e){
            CommonResult.sqlFailed(jsonResult);
        }

        return jsonResult;
    }

    @Override
    @Transactional
    public JsonResult insetVirtualProductOnlineOrder(OnlineOrder onlineOrder, String uid, JsonResult jsonResult) {
        try {
            VirtualProduct product = virtualProductMapper.selectByPrimaryKey(onlineOrder.getPid());
            if(ValidCheck.validPojo(product)){
                CommonResult.noObject(jsonResult);
                return jsonResult;
            }

            SystemVirtualProduct systemVirtualProduct = systemVirtualProductMapper.selectByPrimaryKey(product.getVpId());
            product.setIntegral(systemVirtualProduct.getIntegral());


            if(product.getStatus() != DBConstants.ProductStatus.ON_SALE.getCode()){
                jsonResult.setMessage("商品不可销售！");
                jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                return jsonResult;
            }

            if(onlineOrder.getQuantity() <= 0){
                jsonResult.setMessage("购买商品数量必须大于0！");
                jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                return jsonResult;
            }


            if(onlineOrder.getQuantity() >= product.getRemainingAmount()){
                jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                jsonResult.setMessage("购买数量大于库存量！");
                return jsonResult;
            }

            //确保购买积分正确
            onlineOrder.setIntegral(product.getIntegral()*onlineOrder.getQuantity());

            //验证用户积分
            UserCredits userCredits = this.userCreditsMapper.getUserCredits(Integer.parseInt(uid),
                    DBConstants.OwnerType.CUSTOMER.getCode());

            if (null == userCredits) {
                jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                jsonResult.setMessage("用户积分信息不存在！");
                return jsonResult;
            }

            if (userCredits.getIntegral() < onlineOrder.getIntegral()) {
                jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                jsonResult.setMessage(GlobalConstants.INTEGRAL_NOT_ENOUGH);
                return jsonResult;
            }

            //更新商品数量
            product.setRemainingAmount(product.getRemainingAmount() - onlineOrder.getQuantity());

            onlineOrder.setCreateTime(new Timestamp(System.currentTimeMillis()));
            onlineOrder.setUid(Integer.parseInt(uid));
            onlineOrder.setStatus(DBConstants.OrderStatus.UNPAID.getCode());
            onlineOrder.setSiId(0);


            //添加一条通用交易信息
            Transaction transaction = new Transaction();
            transaction.setType(DBConstants.TrxType.ONLINE.getCode());
            transaction.setOwnerType(DBConstants.OwnerType.CUSTOMER.getCode());
            transaction.setOwnerId(Integer.parseInt(uid));
            transaction.setIntegral(-1 * onlineOrder.getIntegral());
            transaction.setCreateTime(new Timestamp(System.currentTimeMillis()));
            transaction.setStatus(DBConstants.TrxStatus.UNPAID.getCode());
            transaction.setCode(serialNumberService.getNextTrxCode(DBConstants.TrxType.ONLINE.getCode()));

            transactionMapper.insert(transaction);

            onlineOrder.setTrxCode(transaction.getCode());
            onlineOrder.setTrxId(transaction.getId());

            onlineOrderMapper.insert(onlineOrder);

            virtualProductMapper.updateByPrimaryKey(product);


            Map map = new HashMap();
            map.put("data", onlineOrder);

            jsonResult.setErrorCode(GlobalConstants.OPERATION_SUCCEED);
            jsonResult.setMessage(onlineOrder.getTrxCode());
            jsonResult.setItem(map);

        }catch (Exception e){
            System.out.println(e);
            CommonResult.sqlFailed(jsonResult);
        }

        return jsonResult;
    }

    @Override
    @Transactional
    public JsonResult updateVirtualProductOnlineOrderByTrxCode(String trxCode, String uid, JsonResult jsonResult) {
        OnlineOrderExample onlineOrderExample = new OnlineOrderExample();
        OnlineOrderExample.Criteria criteria = onlineOrderExample.or();
        criteria.andTrxCodeEqualTo(trxCode);
        criteria.andUidEqualTo(Integer.parseInt(uid));

        try {
            //查询订单
            List<OnlineOrder> onlineOrders = onlineOrderMapper.selectByExample(onlineOrderExample);

            if(ValidCheck.validList(onlineOrders)){
                CommonResult.noObject(jsonResult);
                return jsonResult;
            }
            OnlineOrder onlineOrder = onlineOrders.get(0);
            //订单状态为未付款才能继续进行
            if(onlineOrder.getStatus() == DBConstants.OrderStatus.UNPAID.getCode()){

                GatewayResponse<CommonOrderResponse> response = this.chainService.postOrders(onlineOrder.getUid() + 0L,
                        onlineOrder.getPid() + 0L, DBConstants.OnlineProductType.SYSTEM_VIRTUAL_PRODUCT.getCode(), onlineOrder.getIntegral(), onlineOrder.getQuantity());
                if (null == response) {
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    return jsonResult;
                }

                if (PRODUCT_NOT_FOUND == response.getCode()) {
                    jsonResult.setMessage("要付款的商品不存在或已下架！");
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    return jsonResult;
                }

                if (INVALID_PRODUCT_INTEGRAL == response.getCode()) {
                    jsonResult.setMessage("商品积分与支付积分不匹配！");
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    return jsonResult;
                }

                if (INVALID_STORE_INFO == response.getCode()) {
                    jsonResult.setMessage("商品对应的商户信息无效！");
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    return jsonResult;
                }

                if (STORE_NOT_FOUND == response.getCode()) {
                    jsonResult.setMessage("商户信息不存在！");
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    return jsonResult;
                }

                if (INVALID_STORE_TYPE == response.getCode()) {
                    jsonResult.setMessage("商户类型不匹配！");
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    return jsonResult;
                }

                if(STORE_NOT_ACCEPT_INTEGRAL == response.getCode()) {
                    jsonResult.setMessage("商户暂不接受积分支付！");
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    return jsonResult;
                }

                if(response.getCode() != 200) {
                    jsonResult.setMessage(response.getMessage());
                    jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                    return jsonResult;
                }

                onlineOrder.setDtchainBlockId(response.getContent().getBlockId());
                onlineOrder.setExtOrderId(response.getContent().getExtOrderId());
                onlineOrder.setStatus(DBConstants.OrderStatus.PAID.getCode());
                onlineOrder.setUpdateTime(new Timestamp(System.currentTimeMillis()));

                onlineOrderMapper.updateByPrimaryKey(onlineOrder);

                Transaction transaction = transactionMapper.selectByPrimaryKey(onlineOrder.getTrxId());
                transaction.setStatus(DBConstants.TrxStatus.COMPLETED.getCode());

                transactionMapper.updateByPrimaryKey(transaction);

                Map map = new HashMap();
                map.put("data", onlineOrder);

                jsonResult.setErrorCode(GlobalConstants.OPERATION_SUCCEED);
                jsonResult.setMessage(onlineOrder.getTrxCode());
                jsonResult.setItem(map);
            }else{
                jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                jsonResult.setMessage("该订单已付款，请勿重复操作！");
            }

        }catch (Exception e){
            CommonResult.sqlFailed(jsonResult);
        }

        return jsonResult;
    }


    @Override
    @Transactional
    public JsonResult cancelVirtualProductOnlineOrderByTrxCode(String trxCode, String uid, JsonResult jsonResult) {

        OnlineOrderExample onlineOrderExample = new OnlineOrderExample();
        OnlineOrderExample.Criteria criteria = onlineOrderExample.or();
        criteria.andUidEqualTo(Integer.parseInt(uid));
        criteria.andTrxCodeEqualTo(trxCode);

        try {
            List<OnlineOrder> onlineOrders = onlineOrderMapper.selectByExample(onlineOrderExample);

            if (ValidCheck.validList(onlineOrders)) {
                CommonResult.noObject(jsonResult);
                return jsonResult;
            }

            //更新订单状态
            OnlineOrder onlineOrder = onlineOrders.get(0);

            if(onlineOrder.getStatus() == DBConstants.OrderStatus.PAID.getCode()){
                jsonResult.setErrorCode(GlobalConstants.OPERATION_FAILED);
                jsonResult.setMessage("已付款订单不能取消！");
                return jsonResult;
            }

            onlineOrder.setStatus(DBConstants.OrderStatus.CANCELED.getCode());
            onlineOrder.setUpdateTime(new Timestamp(System.currentTimeMillis()));
            onlineOrderMapper.updateByPrimaryKey(onlineOrder);

            //更新通用交易表状态
            Transaction transaction = transactionMapper.selectByPrimaryKey(onlineOrder.getTrxId());
            if (ValidCheck.validPojo(transaction)) {
                CommonResult.noObject(jsonResult);
                return jsonResult;
            }
            transaction.setStatus(DBConstants.TrxStatus.CANCELED.getCode());
            transactionMapper.updateByPrimaryKey(transaction);

            //恢复商品的销量和库存
            VirtualProduct product = virtualProductMapper.selectByPrimaryKey(onlineOrder.getPid());
            if (ValidCheck.validPojo(product)) {
                CommonResult.noObject(jsonResult);
                return jsonResult;
            }
            product.setRemainingAmount(product.getRemainingAmount() + onlineOrder.getQuantity());
            virtualProductMapper.updateByPrimaryKey(product);

            CommonResult.success(jsonResult);
        }catch (Exception e){
            CommonResult.sqlFailed(jsonResult);
        }

        return jsonResult;
    }

}
