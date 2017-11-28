package com.jyall.exception;

import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Optional;

/**
 * 统一错误代码
 *
 * @author guo.guanfei
 */
public enum ErrorCode {

    OK(Response.Status.OK.getStatusCode(), "成功"),

    GENERIC_ERROR(-1, "一般错误"),

    /*****
     * 业务错误: 400xxxyyy
     * 400：  HTTP状态码
     * xxx: 系统模块编号
     * yyy: 模块内错误编号
     ******/
    BIZ_ERROR(Response.Status.BAD_REQUEST.getStatusCode(), "业务错误"),
//	BIZ_ERROR(203, "业务错误"),

    /***** 参数相关错误 *****/
    BIZ_ERROR_PARAMETER(400000000, "参数错误"),
    BIZ_ERROR_PARAMETER_NULL(400000001, "空的请求参数"),
    BIZ_ERROR_PARAMETER_INVALID(400000002, "无效的请求参数"),

    /***** 公共服务 *****/
    BIZ_ERROR_CODE_ERROR(400000010, "请输入正确的验证码"),
    BIZ_ERROR_CODE_NOT_EXIST(400000011, "验证码不存在或者已经过期"),
    BIZ_ERROR_TFS_SUFFIX_EMPTY(400000012, "未指定文件后缀名"),
    BIZ_ERROR_TFS_FILE_EMPTY(400000013, "文件未上传"),
    BIZ_ERROR_SMS_ERROR(400000014, "调用发送短信接口失败"),
    BIZ_ERROR_SMS_NOT_ALLOWED(400000015, "此手机号码不允许发送信息"),
    BIZ_ERROR_SMS_MOBILE_ERROR(400000016, "手机号码不正确"),
    BIZ_ERROR_COMMON_NOT_EXIST(400000017, "数据不存在"),
    BIZ_ERROR_COMMON_EXIST(400000018, "数据已存在"),
    BIZ_ERROR_COMMON_ADD(400000019, "新增失败"),
    BIZ_ERROR_COMMON_UPDATE(400000020, "更新失败"),
    BIZ_ERROR_COMMON_DELETE(400000021, "删除失败"),
    BIZ_ERROR_COMMON_SIGN(400000022, "签名校验不通过"),
    /****ticket的异常****/
    BIZ_ERROR_COMMON_TICKET_PARAMETER(400000023, "ticket无效的请求参数"),
    BIZ_ERROR_COMMON_TICKET_AUTH(400000024, "ticket校验异常"),

    /***** 用户 *****/
    BIZ_ERROR_USER_UNKNOWN(400001000, "未知用户错误"),
    BIZ_ERROR_USER_ID_NULL(400001001, "用户ID为空"),
    BIZ_ERROR_USER_NAME_NULL(400001002, "用户名为空"),
    BIZ_ERROR_USER_PWD_NULL(400001003, "用户密码为空"),
    BIZ_ERROR_USER_COUNT_NOT_EXIST(400001004, "用户账号不存在"),
    BIZ_ERROR_USER_COUNT_EXIST(400001005, "用户账号存在"),
    BIZ_ERROR_USER_COUNT_NAME_NOMATHCH(400001006, "用户账号与密码不匹配"),
    BIZ_ERROR_USER_NOT_EXIST(400001007, "用户不存在"),
    BIZ_ERROR_USER_TOKEN_NULL(400001008, "用户TOKEN为空"),
    BIZ_ERROR_USER_ADD_ERROR(400001009, "新增用户失败"),
    BIZ_ERROR_USER_MOBILE_NULL(400001010, "用户手机号为空"),
    BIZ_ERROR_USER_UPDATE_FAIL(400001011, "修改用户失败"),
    BIZ_ERROR_USER_NOT_LOGIN(400001012, "用户未登录"),
    BIZ_ERROR_USER_UPDATE_PWD_FAIL(400001013, "用户密码重置失败"),
    BIZ_ERROR_USER_DELETE_FAIL(400001014, "删除用户失败"),
    BIZ_ERROR_USER_FREEZE_FALI(400001015, "冻结用户失败"),
    BIZ_ERROR_USER_ONSET_FAIL(400001016, "启用用户失败"),
    BIZ_ERROR_USER_EMAIL_NULL(400001017, "用户邮箱为空"),
    BIZ_ERROR_USER_EMAIL_EXIST(400001018, "用户邮箱存在"),
    BIZ_ERROR_USER_EMAIL_NOT_EXIST(400001019, "用户邮箱不存在"),
    BIZ_ERROR_USER_MOBILE_EXIST(400001020, "用户手机号存在"),
    BIZ_ERROR_USER_MOBILE_NOT_EXIST(400001021, "用户手机号不存在"),
    BIZ_ERROR_USER_UPDATE_NAME_FAIL(400001022, "用户名修改失败"),
    BIZ_ERROR_USER_UPDATE_EMAIL_FAIL(400001023, "用户邮箱修改失败"),
    BIZ_ERROR_USER_UPDATE_LOGINTIME_FAIL(400001024, "修改登录时间失败"),
    BIZ_ERROR_USER_UPDATE_LOGINTIMEIP_FAIL(400001025, "修改登录时间与IP失败"),
    BIZ_ERROR_USER_NO_DATA(400001026, "未找到用户数据"),
    BIZ_ERROR_USER_TYPE_NULL(400001027, "用户类别为空"),
    BIZ_ERROR_USER_TYPE_ERROR(400001028, "用户类别错误"),
    BIZ_ERROR_USER_SOURCE_NULL(400001029, "用户来源为空"),
    BIZ_ERROR_USER_STATE_NULL(400001030, "用户状态为空"),
    BIZ_ERROR_USER_MODIFY_TYPE_FAIL(400001031, "用户类别修改失败"),
    BIZ_ERROR_USER_MODIFY_MOBILE_FAIL(400001032, "用户手机号修失败"),
    BIZ_ERROR_USER_ADD_ROLE_ID_EXIST(400001033, "用户ID已存在"),
    BIZ_ERROR_USER_ADD_ROLE_FAIL(400001034, "用户角色添加失败"),
    BIZ_ERROR_USER_NAME_EXIST(400001035, "用户名已存在"),
    BIZ_ERROR_USER_AUTH(400001036, "用户权限不足"),
    BIZ_ERROR_USER_NOT_LOGIN_ADMIN(400001037, "后台用户未登录"),
    BIZ_ERROR_USER_AUTH_ADMIN(400001038, "后台用户权限不足"),
    /**** 用户登录的时候需要验证 ****/
    BIZ_ERROR_USER_LOGIN_VERIFICATION(400001039, "登录需要验证"),
    BIZ_ERROR_USER_LOGIN_VERIFICATION_TIME(400001040, "长时间未登录,需要校验"),
    BIZ_ERROR_USER_LOGIN_VERIFICATION_IP(400001041, "登录IP异常,需要校验"),
    BIZ_ERROR_USER_LOGIN_VERIFICATION_DEVICE(400001042, "切换登录设备,需要检验"),
    BIZ_ERROR_USER_LOGIN_VERIFICATION_ERROR_OVER_TIME(400001043, "失败次数过多,需要检验"),
    BIZ_ERROR_USER_LOGIN_VERIFICATION_ERROR_OVER_TIME_FROZEN(400001044, "失败次数过多,用户已经冻结"),

    /***** 订单 *****/
    BIZ_ERROR_ORDER_UNKNOWN(400002000, "未知订单错误"),

    /***** 家居 *****/
    BIZ_ERROR_HOUSE_UNKNOWN(400003000, "未知家居错误"),
    BIZ_ERROR_HOUSE_PARAM(400003001, "参数错误"),
    BIZ_ERROR_HOUSE_CALL(400003002, "调用失败"),
    BIZ_ERROR_HOUSE_TIMEOUT(400003003, "调用超时"),
    BIZ_ERROR_HOUSE_REFUSE(400003004, "远程服务拒绝"),
    BIZ_ERROR_HOUSE_RIGHT(400003005, "权限认证失败"),
    BIZ_ERROR_HOUSE_NETWORK(400003006, "网络异常"),
    BIZ_ERROR_HOUSE_DATA(400003007, "数据异常"),
    BIZ_ERROR_HOUSE_GOLDEN_UNKNOWN(400003008, "未知金管家异常"),
    BIZ_ERROR_HOUSE_GOLDEN_COUNTY(400003009, "区县分配金管家异常"),
    BIZ_ERROR_HOUSE_GOLDEN_CITY(400003010, "城市分配金管家异常"),
    BIZ_ERROR_HOUSE_GOLDEN_HOUSE(400003011, "房源分配金管家异常"),
    BIZ_ERROR_HOUSE_GOLDEN_DESIGNAT(400003012, "指定分配金管家异常"),
    BIZ_ERROR_HOUSE_GOLDEN_DATA(400003013, "金管家数据异常"),
    BIZ_ERROR_HOUSE_GOLDEN_INVALID(400003014, "更换金管家异常"),
    BIZ_ERROR_HOUSE_GOLDEN_COUNTYCONTACT(400003015, "区县联系金管家异常"),
    BIZ_ERROR_HOUSE_GOLDEN_CITYCONTACT(400003016, "城市联系金管家异常"),
    BIZ_ERROR_HOUSE_GOLDEN_REPLACE(400003017, "换一个金管家异常"),

    /***** 电商 *****/
    BIZ_ERROR_SHOP_UNKNOWN(400004000, "未知电商错误"),
    BIZ_ERROR_SHOP_NAME_EXIST(400004001, "用户已存在"),
    BIZ_ERROR_SHOP_NOT_EXIST(400004002, "用户不存在"),
    BIZ_ERROR_SHOP_ADD_ERROR(400004003, "新增用户失败"),
    BIZ_ERROR_SHOP_UPDATE_FAIL(400004004, "修改用户失败"),
    BIZ_ERROR_SHOP_SELECT_FAIL(400004005, "查询用户失败"),
    BIZ_ERROR_SHOP_SEARCH_CATEGORY_ERROR(400004006, "查询商品分类错误"),
    BIZ_ERROR_SHOP_GOODS_NULL(400004007, "没有找到相关商品"),
    BIZ_ERROR_SHOP_ACTIVITY(400004008, "没有找到相关活动"),
    BIZ_ERROR_SHOP_SEARCH_ERROR(400004009, "搜索错误"),
    BIZ_ERROR_SHOP_SEARCH_STOCK_ERROR(400004010, "查询库存失败"),
    BIZ_ERROR_SHOP_SEARCH_PRICE_ERROR(400004011, "查询价格失败"),
    BIZ_ERROR_SHOP_SEARCH_FREIGHT_TEMPLATE_ERROR(400004012, "查询运费模板失败"),
    BIZ_ERROR_SHOP_SPECIFICATION_SWITCH_ERROR(400004013, "规格切换失败"),
    BIZ_ERROR_SHOP_ADDRESS_NULL(400004014, "地址不存在"),
    BIZ_ERROR_SHOP_USER_ADDRESS_ERROR(400004015, "用户与地址不匹配"),
    BIZ_ERROR_SHOP_SPECIFICATION_ERROR(400004016, "规格无效"),
    BIZ_ERROR_SHOP_SPECIFICATION_MORE_ERROR(400004017, "同一规格不能添加多规格值"),
    BIZ_ERROR_SHOP_GOODS_NULL_OR_NOT_SHELVES(400004018, "商品不存在或未上架"),
    BIZ_ERROR_SHOP_INVALID_IDENTITY(400004019, "无效的身份标识"),
    BIZ_ERROR_SHOP_NOT_SELECT_GOODS(400004020, "未选择结算的商品"),
    BIZ_ERROR_SHOP_GOODS_UNDERSTOCK(400004021, "库存不足"),
    BIZ_ERROR_SHOP_SEARCH_FREIGHT_ERROR(400004022, "查询运费失败"),
    BIZ_ERROR_SHOP_GOODS_PURCHASELIMIT(400004023, "每件商品最多购买200件哦"),
    BIZ_ERROR_SHOP_CART_PURCHASELIMIT(400004024, "购物车已塞满，赶快去把选好的商品下单吧"),

    /***** 财务*****/
    BIZ_ERROR_FINANCE_UNKNOWN(400005000, "未知财务错误"),

    /***** 支付*****/
    BIZ_ERROR_PAY_UNKNOWN(400006000, "未知支付系统错误"),
    BIZ_ERROR_PAY_PARTNER_NOT_EXIST(400006001, "支付商户不存在"),
    BIZ_ERROR_PAY_SIGN_ERROR(400006002, "支付签名验证不通过"),
    BIZ_ERROR_PAY_CREATE_TRADE(400006003, "创建支付订单失败"),
    BIZ_ERROR_PAY_DOU_ACCOUNT_SUMMARY_NULL(400006101, "无家园豆账户汇总信息"),
    BIZ_ERROR_PAY_DOU_ACCOUNT_NULL(400006102, "无家园豆子账户信息"),
    BIZ_ERROR_PAY_DOU_ACCOUNT_AVAILABLE_NOT_ENOUGH(400006103, "家园豆可用余额不足"),
    BIZ_ERROR_PAY_DOU_ACCOUNT_FROZEN_NOT_ENOUGH(400006104, "家园豆已冻结余额不足"),
    BIZ_ERROR_PAY_DOU_ACCOUNT_FROZEN_UNFROZEN_UNMATCHED(400006105, "家园豆冻结/解冻操作不匹配"),
    BIZ_ERROR_PAY_DOU_DEAL_NULL(400006121, "无家园豆交易信息"),
    BIZ_ERROR_PAY_DOU_DEAL_DUPLICATE(400006121, "家园豆交易重复"),
    BIZ_ERROR_PAY_TRADE_NOT_EXIST(400006004, "支付订单不存在"),
    BIZ_ERROR_PAY_VCODE_ERROR(400006005, "验证码验证不通过"),
    BIZ_ERROR_PAY_JYBEAN_NOTENOUGH(400006006, "家园豆不足"),
    BIZ_ERROR_PAY_RECORD_NOT_EXIST(400006007, "支付记录不存在"),
    BIZ_ERROR_PAY_HAVING_PAY(400006008, "订单已支付成功"),
    /***** 退款 *****/
    BIZ_ERROR_PAY_REFUND_SIGN_ERROR(400006101, "退款签名验证不通过"),
    BIZ_ERROR_PAY_REFUND_CREATE_APPLY(400006102, "创建退款申请失败"),
    BIZ_ERROR_PAY_REFUND_APPLY_NOT_EXIST(400006103, "退款申请不存在"),
    BIZ_ERROR_PAY_REFUND_PARAMETER_INVALID(400006104, "退款参数错误"),
    BIZ_ERROR_PAY_REFUND_AMOUNT_OVERFLOW(400006105, "退款金额超限"),
    BIZ_ERROR_PAY_RECORD_CREATE(400006107, "创建支付记录失败"),

    /**
     * 第三方绑定手机号的异常状态
     **/
    THIRDBM_ALREADYBIND_MOBILE_EXISTS(400007001, "第三方已经绑定手机,更换的新手机号已经存在"),
    THIRDBM_MOBILE_ALREADY_BIND_ANATHER(400007002, "该手机号已经被绑定到其他第三方账号"),
    THIRDBM_ALREADYBIND_MOBILE_NOT_BELONG_MID(400007003, "第三方已经绑定手机号,改手机号不属于当前会员"),
    THIRDBM_ALREADYBIND_MOBILE_BELONG_MID_MODIFY_MOBILE_ERROR(400007004, "第三方曾经绑定过该手机号，修改手机号出错"),
    THIRDBM_ERROR(400007005, "第三方绑定手机号出错"),

    /**
     * 微信小程序和公众号的推送
     **/
    BIZ_ERROR_WX_PUSH_APP_UNAUTH(400008000, "小程序或者公众号未授权"),
    BIZ_ERROR_WX_PUSH_PLATE_TEMPLATE_NOT_FOUND(400008001, "平台模板不存在"),
    BIZ_ERROR_WX_PUSH_APP_TEMPLATE_NOT_FOUND(400008002, "商户模板不存在"),
    BIZ_ERROR_WX_PUSH_TEMPLATE_PARAM_ERROR(400008003, "模板参数匹配异常"),
    BIZ_ERROR_WX_PUSH_ERROR(400008004, "小程序或者公众号推送失败"),


    /*****
     * 系统错误: 500xxxyyy
     * 500：  HTTP状态码
     * xxx: 系统错误类别编号
     * yyy: 类别内错误编号
     ******/
    SYS_ERROR(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "系统错误"),

    /***** 数据库相关错误 *****/
    SYS_ERROR_DB_UNKNOWN(500000000, "未知数据库错误"),
    SYS_ERROR_DB_INSERTING(500000001, "数据库 Insert 错误"),
    SYS_ERROR_DB_UPDATING(500000002, "数据库 Update 错误"),
    SYS_ERROR_DB_DELETING(500000003, "数据 Delete 错误"),

    /***** JSON相关错误 *****/
    SYS_ERROR_JSON_UNKNOWN(500001000, "Json 未知错误"),
    SYS_ERROR_JSON_MAPPING(500001001, "Json 数据映射错误"),

    /***** 远程调用相关错误 *****/
    SYS_ERROR_RPC_UNKNOWN(500002000, "远程调用未知错误"),
    SYS_ERROR_RPC_CONNECTION(500002001, "无法连接远程服务"),;

    private final int value;
    private final String msg;

    private ErrorCode(int value, String msg) {
        this.value = value;
        this.msg = msg;
    }

    public int value() {
        return this.value;
    }

    public String msg() {
        return this.msg;
    }

    public static ErrorCode valueOf(int value) {
        Optional<ErrorCode> optional = Arrays.stream(values()).filter(instance -> instance.value == value).limit(1).findFirst();
        if (optional.isPresent())
            return optional.get();
        throw new IllegalArgumentException("No matching constant for [" + value + "]");
    }
}
