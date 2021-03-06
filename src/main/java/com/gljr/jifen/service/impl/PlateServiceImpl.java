package com.gljr.jifen.service.impl;

import com.gljr.jifen.common.CommonResult;
import com.gljr.jifen.common.JsonResult;
import com.gljr.jifen.common.ValidCheck;
import com.gljr.jifen.constants.DBConstants;
import com.gljr.jifen.dao.*;
import com.gljr.jifen.pojo.*;
import com.gljr.jifen.service.PlateService;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PlateServiceImpl implements PlateService {

    @Autowired
    private PlateMapper plateMapper;

    @Autowired
    private ModuleMapper moduleMapper;

    @Autowired
    private ModulePictureMapper modulePictureMapper;

    @Autowired
    private ModuleProductMapper moduleProductMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private VirtualProductMapper virtualProductMapper;

    @Autowired
    private SystemVirtualProductMapper systemVirtualProductMapper;

    @Autowired
    private ModuleAggregationMapper moduleAggregationMapper;

    @Override
    public JsonResult selectPlates(JsonResult jsonResult) {
        try {
            PlateExample plateExample = new PlateExample();
            plateExample.setOrderByClause("sort asc");

            List<Plate> plates = plateMapper.selectByExample(plateExample);

            if(!ValidCheck.validList(plates)){
                for (Plate plate : plates){
                    Module module = moduleMapper.selectByPrimaryKey(plate.getModuleId());
                    plate.setDescription(module.getDescription());
                    plate.setTitle(module.getTitle());
                    plate.setType(module.getType());
                    plate.setExtType(module.getExtType());
                }
            }
            Map map = new HashMap();
            map.put("data", plates);
            jsonResult.setItem(map);
            CommonResult.success(jsonResult);
        }catch (Exception e){
            CommonResult.sqlFailed(jsonResult);
        }

        return jsonResult;
    }

    @Override
    public JsonResult generatePlates(JsonResult jsonResult) {
        try {
            PlateExample plateExample = new PlateExample();
            PlateExample.Criteria criteria = plateExample.or();
            plateExample.setOrderByClause("sort asc");
            List<Plate> plates = plateMapper.selectByExample(plateExample);

            Map map = new HashMap();

            List list = new ArrayList();

            JSONObject jsonObject = new JSONObject();

            for (Plate plate : plates){

                Module module = moduleMapper.selectByPrimaryKey(plate.getModuleId());
                if(module.getStatus() == DBConstants.ModuleStatus.ACTIVED.getCode()) {
//                    jsonObject.put("title", module.getTitle());
//                    jsonObject.put("description", module.getDescription());
//                    jsonObject.put("type", module.getType());
//                    jsonObject.put()



                    //图片形式，包括banner和4图片导航
                    if(module.getType() == DBConstants.ModuleType.PICTURE.getCode()){
                        ModulePictureExample modulePictureExample = new ModulePictureExample();
                        ModulePictureExample.Criteria criteria1 = modulePictureExample.or();
                        criteria1.andModuleIdEqualTo(module.getId());
                        modulePictureExample.setOrderByClause("id asc");

                        List<ModulePicture> modulePictures = modulePictureMapper.selectByExample(modulePictureExample);
                        if(!ValidCheck.validList(modulePictures)){
                            for (ModulePicture modulePicture : modulePictures){
                                if(module.getExtType() == 1){
                                    modulePicture.setPictureKey(modulePicture.getPictureKey() + "!banner");
                                }
                                if(module.getExtType() == 11){
                                    modulePicture.setPictureKey(modulePicture.getPictureKey() + "!avatar");
                                }

                                if(StringUtils.isEmpty(modulePicture.getLinkUrl())){
                                    modulePicture.setLinkType("5");
                                }else if(modulePicture.getLinkUrl().contains("categories") && modulePicture.getLinkUrl().contains("products")){
                                    int beginIndex = modulePicture.getLinkUrl().indexOf("categories/");
                                    int endIndex = modulePicture.getLinkUrl().indexOf("/products");
                                    String result = modulePicture.getLinkUrl().substring(beginIndex, endIndex).substring("categories/".length());
                                    modulePicture.setLinkUrl(result);
                                    modulePicture.setLinkType("3");
                                }else if(modulePicture.getLinkUrl().contains("categories") && modulePicture.getLinkUrl().contains("stores")){
                                    int beginIndex = modulePicture.getLinkUrl().indexOf("categories/");
                                    int endIndex = modulePicture.getLinkUrl().indexOf("/stores");
                                    String result = modulePicture.getLinkUrl().substring(beginIndex, endIndex).substring("categories/".length());
                                    modulePicture.setLinkUrl(result);
                                    modulePicture.setLinkType("4");
                                }else if(modulePicture.getLinkUrl().contains("moduleaggregations")){
                                    String[] links = modulePicture.getLinkUrl().split("/");
                                    ModuleAggregationExample moduleAggregationExample = new ModuleAggregationExample();
                                    ModuleAggregationExample.Criteria criteria2 = moduleAggregationExample.or();
                                    criteria2.andLinkCodeEqualTo(links[2]);

                                    List<ModuleAggregation> moduleAggregations = moduleAggregationMapper.selectByExample(moduleAggregationExample);
                                    if(!ValidCheck.validList(moduleAggregations)){
                                        if(moduleAggregations.get(0).getType() == 2){
                                            modulePicture.setLinkType("4");
                                        }else {
                                            modulePicture.setLinkType("3");
                                        }
                                    }

                                }else {
                                    modulePicture.setLinkType("2");
                                }
                            }
                        }
                        module.setPicture(modulePictures);
                    }

                    //商品模式，包括商品图片和图片导航
                    if(module.getType() == DBConstants.ModuleType.PRODUCT.getCode()){
                        if(module.getExtType() == 2 || module.getExtType() == 3 || module.getExtType() == 4 || module.getExtType() == 5) {
                            ModuleProductExample moduleProductExample = new ModuleProductExample();
                            ModuleProductExample.Criteria criteria1 = moduleProductExample.or();
                            criteria1.andModuleIdEqualTo(module.getId());
                            moduleProductExample.setOrderByClause("id asc");

                            List<ModuleProduct> moduleProducts = moduleProductMapper.selectByExample(moduleProductExample);
                            for (ModuleProduct moduleProduct : moduleProducts) {
                                Product product = productMapper.selectByPrimaryKey(moduleProduct.getProductId());
                                if (!ValidCheck.validPojo(product)) {
                                    moduleProduct.setLogoKey(product.getLogoKey() + "!popular");
                                }
                            }
                            module.setProduct(moduleProducts);
                        }else {
                            ModulePictureExample modulePictureExample = new ModulePictureExample();
                            ModulePictureExample.Criteria criteria1 = modulePictureExample.or();
                            criteria1.andModuleIdEqualTo(module.getId());
                            modulePictureExample.setOrderByClause("id asc");

                            List<ModulePicture> modulePictures = modulePictureMapper.selectByExample(modulePictureExample);
                            if(!ValidCheck.validList(modulePictures)){
                                for (ModulePicture modulePicture : modulePictures){

                                    modulePicture.setPictureKey(modulePicture.getPictureKey() + "!popular");

                                    if(StringUtils.isEmpty(modulePicture.getLinkUrl())){
                                        modulePicture.setLinkType("5");
                                    }else if(modulePicture.getLinkUrl().contains("categories") && modulePicture.getLinkUrl().contains("products")){
                                        int beginIndex = modulePicture.getLinkUrl().indexOf("categories/");
                                        int endIndex = modulePicture.getLinkUrl().indexOf("/products");
                                        String result = modulePicture.getLinkUrl().substring(beginIndex, endIndex).substring("categories/".length());
                                        modulePicture.setLinkUrl(result);
                                        modulePicture.setLinkType("3");
                                    }else if(modulePicture.getLinkUrl().contains("categories") && modulePicture.getLinkUrl().contains("stores")){
                                        int beginIndex = modulePicture.getLinkUrl().indexOf("categories/");
                                        int endIndex = modulePicture.getLinkUrl().indexOf("/stores");
                                        String result = modulePicture.getLinkUrl().substring(beginIndex, endIndex).substring("categories/".length());
                                        modulePicture.setLinkUrl(result);
                                        modulePicture.setLinkType("4");
                                    }else if(modulePicture.getLinkUrl().contains("moduleaggregations")){
                                        String[] links = modulePicture.getLinkUrl().split("/");
                                        ModuleAggregationExample moduleAggregationExample = new ModuleAggregationExample();
                                        ModuleAggregationExample.Criteria criteria2 = moduleAggregationExample.or();
                                        criteria2.andLinkCodeEqualTo(links[2]);

                                        List<ModuleAggregation> moduleAggregations = moduleAggregationMapper.selectByExample(moduleAggregationExample);
                                        if(!ValidCheck.validList(moduleAggregations)){
                                            if(moduleAggregations.get(0).getType() == 2){
                                                modulePicture.setLinkType("6");
                                            }else {
                                                modulePicture.setLinkType("1");
                                            }
                                        }
                                    }else {
                                        modulePicture.setLinkType("2");
                                    }
                                }
                            }
                            module.setPicture(modulePictures);
                        }
                    }

                    //图片+商品模式，包括banner图片和下面的商品图片以及图片导航
                    if(module.getType() == DBConstants.ModuleType.PICTUREANDPRODUCT.getCode()){
                        if(module.getExtType() == 2 || module.getExtType() == 3 || module.getExtType() == 4 || module.getExtType() == 5) {
                            ModulePictureExample modulePictureExample = new ModulePictureExample();
                            ModulePictureExample.Criteria criteria1 = modulePictureExample.or();
                            criteria1.andModuleIdEqualTo(module.getId());

                            List<ModulePicture> modulePictures = modulePictureMapper.selectByExample(modulePictureExample);
                            if(!ValidCheck.validList(modulePictures)){
                                for (ModulePicture modulePicture : modulePictures){

                                    modulePicture.setPictureKey(modulePicture.getPictureKey() + "!banner");

                                    if(StringUtils.isEmpty(modulePicture.getLinkUrl())){
                                        modulePicture.setLinkType("5");
                                    }else if(modulePicture.getLinkUrl().contains("categories") && modulePicture.getLinkUrl().contains("products")){
                                        int beginIndex = modulePicture.getLinkUrl().indexOf("categories/");
                                        int endIndex = modulePicture.getLinkUrl().indexOf("/products");
                                        String result = modulePicture.getLinkUrl().substring(beginIndex, endIndex).substring("categories/".length());
                                        modulePicture.setLinkUrl(result);
                                        modulePicture.setLinkType("3");
                                    }else if(modulePicture.getLinkUrl().contains("categories") && modulePicture.getLinkUrl().contains("stores")){
                                        int beginIndex = modulePicture.getLinkUrl().indexOf("categories/");
                                        int endIndex = modulePicture.getLinkUrl().indexOf("/stores");
                                        String result = modulePicture.getLinkUrl().substring(beginIndex, endIndex).substring("categories/".length());
                                        modulePicture.setLinkUrl(result);
                                        modulePicture.setLinkType("4");
                                    }else if(modulePicture.getLinkUrl().contains("moduleaggregations")){
                                        String[] links = modulePicture.getLinkUrl().split("/");
                                        ModuleAggregationExample moduleAggregationExample = new ModuleAggregationExample();
                                        ModuleAggregationExample.Criteria criteria2 = moduleAggregationExample.or();
                                        criteria2.andLinkCodeEqualTo(links[2]);

                                        List<ModuleAggregation> moduleAggregations = moduleAggregationMapper.selectByExample(moduleAggregationExample);
                                        if(!ValidCheck.validList(moduleAggregations)){
                                            if(moduleAggregations.get(0).getType() == 2){
                                                modulePicture.setLinkType("6");
                                            }else {
                                                modulePicture.setLinkType("1");
                                            }
                                        }
                                    }else {
                                        modulePicture.setLinkType("2");
                                    }
                                }
                            }
                            module.setPicture(modulePictures);

                            ModuleProductExample moduleProductExample = new ModuleProductExample();
                            ModuleProductExample.Criteria criteria2 = moduleProductExample.or();
                            criteria2.andModuleIdEqualTo(module.getId());
                            List<ModuleProduct> moduleProducts = moduleProductMapper.selectByExample(moduleProductExample);
                            for (ModuleProduct moduleProduct : moduleProducts){
                                Product product = productMapper.selectByPrimaryKey(moduleProduct.getProductId());
                                if(!ValidCheck.validPojo(product)){
                                    moduleProduct.setLogoKey(product.getLogoKey() + "!popular");
                                }
                            }
                            module.setProduct(moduleProducts);
                        }else{
                            ModulePictureExample modulePictureExample = new ModulePictureExample();
                            ModulePictureExample.Criteria criteria1 = modulePictureExample.or();
                            criteria1.andModuleIdEqualTo(module.getId());
                            criteria1.andBannerEqualTo(1);

                            List<ModulePicture> modulePictures = modulePictureMapper.selectByExample(modulePictureExample);
                            if(!ValidCheck.validList(modulePictures)){
                                for (ModulePicture modulePicture : modulePictures){

                                    modulePicture.setPictureKey(modulePicture.getPictureKey() + "!banner");

                                    if(StringUtils.isEmpty(modulePicture.getLinkUrl())){
                                        modulePicture.setLinkType("5");
                                    }else if(modulePicture.getLinkUrl().contains("categories") && modulePicture.getLinkUrl().contains("products")){
                                        int beginIndex = modulePicture.getLinkUrl().indexOf("categories/");
                                        int endIndex = modulePicture.getLinkUrl().indexOf("/products");
                                        String result = modulePicture.getLinkUrl().substring(beginIndex, endIndex).substring("categories/".length());
                                        modulePicture.setLinkUrl(result);
                                        modulePicture.setLinkType("3");
                                    }else if(modulePicture.getLinkUrl().contains("categories") && modulePicture.getLinkUrl().contains("stores")){
                                        int beginIndex = modulePicture.getLinkUrl().indexOf("categories/");
                                        int endIndex = modulePicture.getLinkUrl().indexOf("/stores");
                                        String result = modulePicture.getLinkUrl().substring(beginIndex, endIndex).substring("categories/".length());
                                        modulePicture.setLinkUrl(result);
                                        modulePicture.setLinkType("4");
                                    }else if(modulePicture.getLinkUrl().contains("moduleaggregations")){
                                        String[] links = modulePicture.getLinkUrl().split("/");
                                        ModuleAggregationExample moduleAggregationExample = new ModuleAggregationExample();
                                        ModuleAggregationExample.Criteria criteria2 = moduleAggregationExample.or();
                                        criteria2.andLinkCodeEqualTo(links[2]);

                                        List<ModuleAggregation> moduleAggregations = moduleAggregationMapper.selectByExample(moduleAggregationExample);
                                        if(!ValidCheck.validList(moduleAggregations)){
                                            if(moduleAggregations.get(0).getType() == 2){
                                                modulePicture.setLinkType("6");
                                            }else {
                                                modulePicture.setLinkType("1");
                                            }
                                        }
                                    }else {
                                        modulePicture.setLinkType("2");
                                    }
                                }
                            }
                            module.setPicture(modulePictures);



                            modulePictureExample = new ModulePictureExample();
                            criteria1 = modulePictureExample.or();
                            criteria1.andModuleIdEqualTo(module.getId());
                            criteria1.andBannerNotEqualTo(1);

                            modulePictures = modulePictureMapper.selectByExample(modulePictureExample);
                            if(!ValidCheck.validList(modulePictures)){
                                for (ModulePicture modulePicture : modulePictures){

                                    modulePicture.setPictureKey(modulePicture.getPictureKey() + "!popular");

                                    if(StringUtils.isEmpty(modulePicture.getLinkUrl())){
                                        modulePicture.setLinkType("5");
                                    }else if(modulePicture.getLinkUrl().contains("categories") && modulePicture.getLinkUrl().contains("products")){
                                        int beginIndex = modulePicture.getLinkUrl().indexOf("categories/");
                                        int endIndex = modulePicture.getLinkUrl().indexOf("/products");
                                        String result = modulePicture.getLinkUrl().substring(beginIndex, endIndex).substring("categories/".length());
                                        modulePicture.setLinkUrl(result);
                                        modulePicture.setLinkType("3");
                                    }else if(modulePicture.getLinkUrl().contains("categories") && modulePicture.getLinkUrl().contains("stores")){
                                        int beginIndex = modulePicture.getLinkUrl().indexOf("categories/");
                                        int endIndex = modulePicture.getLinkUrl().indexOf("/stores");
                                        String result = modulePicture.getLinkUrl().substring(beginIndex, endIndex).substring("categories/".length());
                                        modulePicture.setLinkUrl(result);
                                        modulePicture.setLinkType("4");
                                    }else if(modulePicture.getLinkUrl().contains("moduleaggregations")){
                                        String[] links = modulePicture.getLinkUrl().split("/");
                                        ModuleAggregationExample moduleAggregationExample = new ModuleAggregationExample();
                                        ModuleAggregationExample.Criteria criteria2 = moduleAggregationExample.or();
                                        criteria2.andLinkCodeEqualTo(links[2]);

                                        List<ModuleAggregation> moduleAggregations = moduleAggregationMapper.selectByExample(moduleAggregationExample);
                                        if(!ValidCheck.validList(moduleAggregations)){
                                            if(moduleAggregations.get(0).getType() == 2){
                                                modulePicture.setLinkType("6");
                                            }else {
                                                modulePicture.setLinkType("1");
                                            }
                                        }
                                    }else {
                                        modulePicture.setLinkType("2");
                                    }
                                }
                            }
                            module.setProduct(modulePictures);
                        }
                    }

                    if(module.getType() == DBConstants.ModuleType.PACKET.getCode()){
                        ModuleProductExample moduleProductExample = new ModuleProductExample();
                        ModuleProductExample.Criteria criteria1 = moduleProductExample.or();
                        criteria1.andModuleIdEqualTo(module.getId());
                        moduleProductExample.setOrderByClause("sort asc");

                        List<VirtualProduct> virtualProducts = new ArrayList();

                        List<ModuleProduct> moduleProducts = moduleProductMapper.selectByExample(moduleProductExample);
                        if(!ValidCheck.validList(moduleProducts)) {

                            for (ModuleProduct moduleProduct : moduleProducts) {
                                VirtualProduct virtualProduct = virtualProductMapper.selectByPrimaryKey(moduleProduct.getProductId());

                                SystemVirtualProduct systemVirtualProduct = systemVirtualProductMapper.selectByPrimaryKey(virtualProduct.getVpId());

                                virtualProduct.setIntegral(systemVirtualProduct.getIntegral());
                                virtualProduct.setType(Integer.parseInt(systemVirtualProduct.getCode()));

                                virtualProducts.add(virtualProduct);
                            }
                        }
                        module.setProduct(virtualProducts);
                    }

                    list.add(module);
                }
            }

            map.put("data", list);
            jsonResult.setItem(map);
            CommonResult.success(jsonResult);

        }catch (Exception e){
            System.out.println(e);
            CommonResult.sqlFailed(jsonResult);
        }

        return jsonResult;
    }

    @Override
    @Transactional
    public JsonResult changePlateOrder(Integer cur, Integer prev, JsonResult jsonResult) {

        try {
            Plate plate = plateMapper.selectByPrimaryKey(cur);
            Plate plate1 = plateMapper.selectByPrimaryKey(prev);

            cur = plate.getSort();
            prev = plate1.getSort();

            plate.setSort(prev);
            plate1.setSort(cur);

            plateMapper.updateByPrimaryKey(plate);
            plateMapper.updateByPrimaryKey(plate1);

            CommonResult.success(jsonResult);
        }catch (Exception e){
            CommonResult.sqlFailed(jsonResult);
        }

        return jsonResult;
    }
}
