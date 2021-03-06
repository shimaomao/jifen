package com.gljr.jifen.controller;


import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.gljr.jifen.common.CommonResult;

import com.gljr.jifen.common.JsonResult;
import com.gljr.jifen.common.ValidCheck;
import com.gljr.jifen.constants.DBConstants;
import com.gljr.jifen.dao.*;
import com.gljr.jifen.pojo.*;
import com.gljr.jifen.service.ModuleAggregationService;
import com.gljr.jifen.service.RedisService;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CrossOrigin(origins = "*", maxAge = 3600)
@Controller
@RequestMapping(value = "/v1/moduleaggregations")
public class ModuleAggregationController {

    @Autowired
    private ModuleAggregationService moduleAggregationService;

    @Autowired
    private ModuleAggregationMapper moduleAggregationMapper;

    @Autowired
    private ModuleAggregationConditionMapper moduleAggregationConditionMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private RedisService redisService;

    @Autowired
    private ModuleAggregationProductMapper moduleAggregationProductMapper;

    @Autowired
    private StoreInfoMapper storeInfoMapper;

    /**
     * 通过路径获取一个聚合页
     * @param link
     * @param page
     * @param per_page
     * @param sort 排序
     * @return
     */
    @GetMapping(value = "/{link}")
    @ResponseBody
    public JsonResult selectModuleAggregations(@PathVariable(value = "link") String link, @RequestParam(value = "page", required = false) Integer page,
                                               @RequestParam(value = "per_page", required = false) Integer per_page, @RequestParam(value = "sort", required = false) Integer sort){
        JsonResult jsonResult = new JsonResult();
        try {
            Map map = new HashMap();

            if(StringUtils.isEmpty(page)){
                page = 1;
            }

            if(StringUtils.isEmpty(per_page)){
                per_page = 10;
            }

            String _sort;
            //如果没有设置排序，按照默认读取数据
            if(sort == null || sort > 4 || sort < 0 || sort == 0){
                _sort = "";
                sort = 0;
            }else {
                _sort = sort +"";
            }

            ModuleAggregationExample moduleAggregationExample = new ModuleAggregationExample();
            ModuleAggregationExample.Criteria criteria = moduleAggregationExample.or();
            criteria.andLinkCodeEqualTo(link);
            List<ModuleAggregation> moduleAggregations = moduleAggregationMapper.selectByExample(moduleAggregationExample);

            if(ValidCheck.validList(moduleAggregations)){
                CommonResult.noObject(jsonResult);
                return jsonResult;
            }

            ModuleAggregation moduleAggregation = moduleAggregations.get(0);

            //条件聚合
            if(moduleAggregation.getType() == DBConstants.ModuleAggregationType.CONDITION.getCode()){

                ModuleAggregationConditionExample moduleAggregationConditionExample = new ModuleAggregationConditionExample();
                ModuleAggregationConditionExample.Criteria criteria1 = moduleAggregationConditionExample.or();
                criteria1.andAggregationIdEqualTo(moduleAggregation.getId());

                List<ModuleAggregationCondition> moduleAggregationConditions = moduleAggregationConditionMapper.selectByExampleWithBLOBs(moduleAggregationConditionExample);

                String str = moduleAggregationConditions.get(0).getExpStr();

                String[] conditions = str.split(",");
                String from = conditions[0];
                String to = conditions[1];

                ProductExample productExample = new ProductExample();
                ProductExample.Criteria criteria2 = productExample.or();
                criteria2.andIntegralBetween(Integer.parseInt(from), Integer.parseInt(to));
                criteria2.andStatusEqualTo(DBConstants.ProductStatus.ON_SALE.getCode());

                if(sort == 0){
                    productExample.setOrderByClause("id desc");
                }else if (sort == 1 || sort == 2){
                    productExample.setOrderByClause("sales desc");
                }else if (sort == 3){
                    productExample.setOrderByClause("integral desc");
                }else if (sort == 4){
                    productExample.setOrderByClause("integral asc");
                }

                PageHelper.startPage(page,per_page);
                List<Product> products = productMapper.selectByExample(productExample);
                for (Product product : products){
                    product.setLogoKey(product.getLogoKey() + "!popular");
                }
                PageInfo pageInfo = new PageInfo(products);

                map = new HashMap();
                map.put("data", products);
                map.put("pages", pageInfo.getPages());

                map.put("total", pageInfo.getTotal());
                //当前页
                map.put("pageNum", pageInfo.getPageNum());

                jsonResult.setItem(map);
                CommonResult.success(jsonResult);
                return jsonResult;
            }



            //商品聚合
            if(moduleAggregation.getType() == DBConstants.ModuleAggregationType.PRODUCT.getCode()){
                ModuleAggregationProductExample moduleAggregationProductExample = new ModuleAggregationProductExample();
                ModuleAggregationProductExample.Criteria criteria1 = moduleAggregationProductExample.or();
                criteria1.andAggregationIdEqualTo(moduleAggregation.getId());
                criteria1.andTypeEqualTo(DBConstants.ModuleAggregationType.PRODUCT.getCode());
                moduleAggregationProductExample.setOrderByClause("id desc");

                List<ModuleAggregationProduct> moduleAggregationProducts = moduleAggregationProductMapper.selectByExample(moduleAggregationProductExample);

                List<AggregationProduct> aggregationProducts = new ArrayList<>();
                for (ModuleAggregationProduct moduleAggregationProduct : moduleAggregationProducts){
                    Product product = productMapper.selectByPrimaryKey(moduleAggregationProduct.getProductId());

                    AggregationProduct aggregationProduct = new AggregationProduct();
                    aggregationProduct.setId(product.getId());
                    aggregationProduct.setIntegral(product.getIntegral());
                    aggregationProduct.setName(product.getName());
                    aggregationProduct.setPrice(product.getPrice());
                    aggregationProduct.setType(DBConstants.CategoryType.PRODUCT.getCode());
                    aggregationProduct.setLogoKey(product.getLogoKey() + "!popular");
                    aggregationProduct.setSales(product.getSales());

                    aggregationProducts.add(aggregationProduct);
                }

                map = new HashMap();
                map.put("data", aggregationProducts);
                map.put("pages", 1);

                map.put("total", aggregationProducts.size());
                //当前页
                map.put("pageNum", 1);
                jsonResult.setItem(map);
                CommonResult.success(jsonResult);
                return jsonResult;
            }

            //商户聚合
            if(moduleAggregation.getType() == DBConstants.ModuleAggregationType.STORE.getCode()){
                ModuleAggregationProductExample moduleAggregationProductExample = new ModuleAggregationProductExample();
                ModuleAggregationProductExample.Criteria criteria1 = moduleAggregationProductExample.or();
                criteria1.andAggregationIdEqualTo(moduleAggregation.getId());
                criteria1.andTypeEqualTo(DBConstants.ModuleAggregationType.STORE.getCode());
                moduleAggregationProductExample.setOrderByClause("id desc");

                List<ModuleAggregationProduct> moduleAggregationProducts = moduleAggregationProductMapper.selectByExample(moduleAggregationProductExample);

                List<AggregationProduct> aggregationProducts = new ArrayList<>();
                for (ModuleAggregationProduct moduleAggregationProduct : moduleAggregationProducts){

                    StoreInfo storeInfo = storeInfoMapper.selectByPrimaryKey(moduleAggregationProduct.getStoreId());
                        AggregationProduct aggregationProduct = new AggregationProduct();

                        aggregationProduct.setLogoKey(storeInfo.getLogoKey() + "!popular");
                        aggregationProduct.setType(DBConstants.CategoryType.STORE.getCode());
                        aggregationProduct.setName(storeInfo.getName());
                        aggregationProduct.setId(storeInfo.getId());
                        aggregationProduct.setAddress(storeInfo.getAddress());

                        aggregationProducts.add(aggregationProduct);
                }
                map = new HashMap();
                map.put("data", aggregationProducts);
                map.put("pages", 1);

                map.put("total", aggregationProducts.size());
                //当前页
                map.put("pageNum", 1);
                jsonResult.setItem(map);
                CommonResult.success(jsonResult);
                return jsonResult;
            }


//            //获取缓存中对应的排序结果
//            String json = this.redisService.get(link+_sort);
//
//            //为空，提示没有找到数据
//            if(StringUtils.isEmpty(json)){
//                jsonResult = moduleAggregationService.restartmoduleAggregationByLink(link, jsonResult);
//            }
//
//            json = this.redisService.get(link+_sort);
//
//            //还为空
//            if(StringUtils.isEmpty(json)){
//                CommonResult.noObject(jsonResult);
//                return jsonResult;
//            }
//
//            map.put("data", json);
//            map.put("pages", 1);
//
//            map.put("total", 1);
//            //当前页
//            map.put("pageNum", 1);
//
//            jsonResult.setItem(map);
//            CommonResult.success(jsonResult);
        }catch (Exception e){
            System.out.println(e);
            CommonResult.failed(jsonResult);
        }


        return jsonResult;
    }

}
