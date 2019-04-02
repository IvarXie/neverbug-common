

import com.baj.newretail.common.mybatis.annotation.MyColumn;
import com.baj.newretail.common.mybatis.annotation.MyId;
import com.baj.newretail.common.mybatis.annotation.MyTable;

/**
 * 测试的mybatis映射的bean
 * <p>
 * Created by neverbug
 * Created on 2017/10/11 15:16
 */
@MyTable("zee")
public class MybatisBean {

    @MyId
    private String id;

    @MyColumn
    private String col;

}
