/********************************************************
 ***                     _ooOoo_                       ***
 ***                    o8888888o                      ***
 ***                    88" . "88                      ***
 ***                    (| -_- |)                      ***
 ***                    O\  =  /O                      ***
 ***                 ____/`---'\____                   ***
 ***               .'  \\|     |//  `.                 ***
 ***              /  \\|||  :  |||//  \                ***
 ***             /  _||||| -:- |||||-  \               ***
 ***             |   | \\\  -  /// |   |               ***
 ***             | \_|  ''\---/''  |   |               ***
 ***             \  .-\__  `-`  ___/-. /               ***
 ***           ___`. .'  /--.--\  `. . __              ***
 ***        ."" '<  `.___\_<|>_/___.'  >'"".           ***
 ***       | | :  `- \`.;`\ _ /`;.`/ - ` : | |         ***
 ***       \  \ `-.   \_ __\ /__ _/   .-` /  /         ***
 ***  ======`-.____`-.___\_____/___.-`____.-'======    ***
 ***                     `=---='                       ***
 ***   .............................................   ***
 ***         佛祖镇楼                  BUG辟易         ***
 ***   佛曰:                                           ***
 ***           写字楼里写字间，写字间里程序员；        ***
 ***           程序人员写程序，又拿程序换酒钱。        ***
 ***           酒醒只在网上坐，酒醉还来网下眠；        ***
 ***           酒醉酒醒日复日，网上网下年复年。        ***
 ***           但愿老死电脑间，不愿鞠躬老板前；        ***
 ***           奔驰宝马贵者趣，公交自行程序员。        ***
 ***           别人笑我忒疯癫，我笑自己命太贱；        ***
 ***           不见满街漂亮妹，哪个归得程序员？        ***
 *********************************************************
 ***               江城子 . 程序员之歌                 ***
 ***           十年生死两茫茫，写程序，到天亮。        ***
 ***               千行代码，Bug何处藏。               ***
 ***           纵使上线又怎样，朝令改，夕断肠。        ***
 ***           领导每天新想法，天天改，日日忙。        ***
 ***               相顾无言，惟有泪千行。              ***
 ***           每晚灯火阑珊处，夜难寐，加班狂。        ***
 *********************************************************
 ***      .--,       .--,                              ***
 ***      ( (  \.---./  ) )                            ***
 ***       '.__/o   o\__.'                             ***
 ***          {=  ^  =}              高山仰止,         ***
 ***           >  -  <               景行行止.         ***
 ***          /       \              虽不能至,         ***
 ***         //       \\             心向往之。        ***
 ***        //|   .   |\\                              ***
 ***        "'\       /'"_.-~^`'-.                     ***
 ***           \  _  /--'         `                    ***
 ***         ___)( )(___                               ***
 ***        (((__) (__)))                              ***
 ********************************************************/
package com.jyall.util;

import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 获取请求IP的工具类
 *
 * @author zhao.weiwei
 * Created on 2017/11/28 9:36
 * Email is zhao.weiwei@jyall.com
 * Copyright is 金色家园网络科技有限公司
 */
public class IpUtil {

    private static Logger logger = LoggerFactory.getLogger(IpUtil.class);

    private static Pattern pattern = Pattern.compile(
            "^(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])\\.(\\d{1,2}|1\\d\\d|2[0-4]\\d|25[0-5])$");

    /**
     * 获取当前网络ip
     *
     * @param request
     * @return
     */
    public static String getIpAddr(HttpServletRequest request) {
        List<String> list = Lists.newArrayList();
        /** 线上环境发布CDN **/
        list.add("Cdn-Src-Ip");
        /** 非CDN环境使用 X-Real-IP **/
        list.add("X-Real-IP");
        list.add("x-forwarded-for");
        list.add("X-Forwarded-For");
        list.add("Proxy-Client-IP");
        list.add("WL-Proxy-Client-IP");
        list.add("HTTP_CLIENT_IP");
        list.add("HTTP_X_FORWARDED_FOR");
        String ipAddress = null;
        for (String header : list) {
            ipAddress = request.getHeader(header);
            if (StringUtils.isNotEmpty(ipAddress) && ipAddress.contains(",")) {
                String[] ips = ipAddress.split(",");
                for (String ip : ips) {
                    ip = ip.trim();
                    if (checkIp(ip)) {
                        ipAddress = ip;
                        break;
                    }
                }
            }
            if (checkIp(ipAddress)) {
                break;
            }
        }
        if (!checkIp(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            if (ipAddress.equals("127.0.0.1") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
                //根据网卡取本机配置的IP
                try {
                    InetAddress inet = InetAddress.getLocalHost();
                    ipAddress = inet.getHostAddress();
                } catch (UnknownHostException e) {
                    logger.error("UnknownHostException", e);
                }
            }
        }
        //对于通过多个代理的情况，第一个IP为客户端真实IP,多个IP按照','分割
        //"***.***.***.***".length() = 15
        if (ipAddress != null && ipAddress.indexOf(",") > 0) {
            String[] ips = ipAddress.split(",");
            for (String ip : ips) {
                ipAddress = ip.trim();
                if (checkIp(ip.trim())) {
                    break;
                }
            }
        }
        return ipAddress;
    }

    /**
     * header里面的ip检测
     *
     * @param ip
     * @return
     */
    private static boolean checkIp(String ip) {
        if (StringUtils.isEmpty(ip) || "unknown".equalsIgnoreCase(ip)) {
            return false;
        }
        return pattern.matcher(ip.trim()).matches();
    }
}
