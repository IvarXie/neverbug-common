/*
                            _ooOoo_  
                           o8888888o  
                           88" . "88  
                           (| -_- |)  
                            O\ = /O  
                        ____/`---'\____  
                      .   ' \\| |// `.  
                       / \\||| : |||// \  
                     / _||||| -:- |||||- \  
                       | | \\\ - /// | |  
                     | \_| ''\---/'' | |  
                      \ .-\__ `-` ___/-. /  
                   ___`. .' /--.--\ `. . __  
                ."" '< `.___\_<|>_/___.' >'"".  
               | | : `- \`.;`\ _ /`;.`/ - ` : | |  
                 \ \ `-. \_ __\ /__ _/ .-` / /  
         ======`-.____`-.___\_____/___.-`____.-'======  
                            `=---='  
  
         .............................................  
                  佛祖镇楼                  BUG辟易  
          佛曰:  
                  写字楼里写字间，写字间里程序员；  
                  程序人员写程序，又拿程序换酒钱。  
                  酒醒只在网上坐，酒醉还来网下眠；  
                  酒醉酒醒日复日，网上网下年复年。  
                  但愿老死电脑间，不愿鞠躬老板前；  
                  奔驰宝马贵者趣，公交自行程序员。  
                  别人笑我忒疯癫，我笑自己命太贱；  
                  不见满街漂亮妹，哪个归得程序员？
*/
package com.jyall.util;

import com.google.common.collect.Lists;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.util.List;
import java.util.Map;

/**
 * Google gson的工具类
 *
 * @create by zhao.weiwei
 * @create on 2017年4月18日上午9:02:20
 * @email is zhao.weiwei@jyall.com.
 */
public class GsonUtil {

    /**
     * 将实体转换为json
     *
     * @param obj
     * @return
     */
    public static String toJson(Object obj) {
        Gson gsonDate = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").create();
        return gsonDate.toJson(obj);
    }

    /**
     * 将json转换成实体
     *
     * @param json
     * @param clazz
     * @return
     */
    public static <T> T json2Bean(String json, Class<T> clazz) {
        Gson gsonDate = new GsonBuilder().setDateFormat("yyyy-MM-dd HH:mm:ss.SSS").create();
        return gsonDate.fromJson(json, clazz);
    }

    /**
     * 将json转换实体list
     *
     * @param json
     * @return
     */
    public static <T> List<T> json2BeanList(String json, Class<T> clazz) {
        JsonParser parser = new JsonParser();
        List<T> list = Lists.newArrayList();
        JsonElement element = parser.parse(json);
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                list.add(json2Bean(array.get(i).toString(), clazz));
            }
        } else if (element.isJsonObject()) {
            list.add(json2Bean(element.getAsJsonObject().toString(), clazz));
        }
        return list;
    }

    /**
     * 将json转换map
     *
     * @param json
     * @return
     */
    public static Map<String, String> json2Map(String json) {
        return new Gson().fromJson(json, new TypeToken<Map<String, String>>() {
        }.getType());
    }

    /**
     * 将json转换为JsonArray
     *
     * @param json
     * @return
     */
    public static JsonArray getAsJsonArray(String json) {
        JsonParser jsonParser = new JsonParser();
        JsonElement element = jsonParser.parse(json);
        if (element.isJsonArray()) {
            return element.getAsJsonArray();
        } else if (element.isJsonObject()) {
            JsonArray array = new JsonArray();
            array.add(element.getAsJsonObject());
            return array;
        }
        return null;
    }

    /**
     * 将json转换为json对象
     *
     * @param json
     * @return
     */
    public static JsonObject getAsJsonObject(String json) {
        JsonParser jsonParser = new JsonParser();
        JsonElement element = jsonParser.parse(json);
        if (element.isJsonObject()) {
            return element.getAsJsonObject();
        } else {
            return null;
        }
    }

    /**
     * 使用Gson判断字符串是否是json
     *
     * @param str 要判断的字符串
     * @return
     */
    public static boolean isJson(String str) {
        try {
            new JsonParser().parse(str);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 转换为格式化的json
     *
     * @param obj
     * @return 格式化以后的json
     */
    public static String formatJson(Object obj) throws Exception {
        return new GsonBuilder().setPrettyPrinting().create().toJson(new JsonParser().parse(toJson(obj)));
    }

    /**
     * 格式化json
     *
     * @param json
     * @return
     * @throws Exception
     */
    public static String formatJson(String json) throws Exception {
        Gson google = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(json);
        return google.toJson(je);
    }

    /**
     * 将map转换为bean
     *
     * @param map
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> T map2Bean(Map map, Class<T> clazz) {
        String json = toJson(map);
        return json2Bean(json, clazz);
    }

}
