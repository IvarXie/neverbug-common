

import com.neverbug.common.mybatis.mapper.BaseSqlProvider;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * <p>
 * Created by neverbug
 * Created on 2017/10/11 15:37
 */
public class MybatisBeanTest {
    @Test
    public void testFileds(){
        MybatisBean bean = new MybatisBean();
        Collection<Field> fields = BaseSqlProvider.getAllField(bean);
        System.out.println(fields);
        System.out.println(BaseSqlProvider.getAllFieldString(bean));
    }
}
